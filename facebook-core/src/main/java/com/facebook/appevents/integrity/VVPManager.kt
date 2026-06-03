/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * VPPA Video Viewing Protections — Android SDK side. Mirrors the JS pixel plugin
 * (`SignalsFBEvents.plugins.vvp.js`). Consumes the `vvp_config` field served by
 * `GraphApplicationProtectedModeRulesNode` (parsed into `FetchedAppSettings.vvpConfig`) and exposes
 * a typed [VVPConfig] for the per-event hook to consult.
 *
 * This file owns parsing + lifecycle only. Detection / sanitization / payload tagging will be added
 * in subsequent diffs.
 */
@AutoHandleExceptions
object VVPManager {

  // Mirror of `SignalsIntegrityCheckPlace` int values. Only these two reach the SDK today.
  internal const val PLACE_CUSTOM_DATA = 1
  internal const val PLACE_EVENT_NAME = 3

  // Wire field names — match the server-side TVVPAppConfig / TVVPAppRule shape.
  private const val ENABLED_KEY = "enabled"
  private const val IS_SHADOW_ENABLED_KEY = "isShadowEnabled"
  private const val RULES_KEY = "rules"
  private const val STANDARD_PARAMS_KEY = "standardParams"
  private const val IN_SCOPE_EVENT_NAMES_KEY = "inScopeEventNames"
  private const val PLACE_KEY = "place"
  private const val KEY_REGEX_KEY = "keyRegex"
  private const val KEY_NEGATIVE_REGEX_KEY = "keyNegativeRegex"
  private const val VALUE_REGEX_KEY = "valueRegex"

  // Outgoing payload keys appended to the event Bundle when VVP enforces.
  // Mirror of the JS plugin's `vvp` / `vvp_md` and the server-side
  // `AdsPixelRequestParams::VVP_CLIENT_SIDE_ENFORCED / VVP_CLIENT_SIDE_METADATA`.
  private const val VVP_IS_APPLIED_KEY = "vvp"
  private const val VVP_IS_APPLIED_VALUE = "1"
  private const val VVP_METADATA_KEY = "vvp_md"

  // Sub-buckets inside the JSON-encoded vvp_md payload — mirror PHP
  // SIEventContextParams::RESTRICTED_PARAMS ("rp") prefixed with "vp_".
  private const val VP_RP = "vp_rp"
  private const val VP_RP_EV = "vp_rp_ev"

  // Sentinel pushed to vp_rp_ev when an event-name rule fires; we never
  // echo the actual event name back (it can itself be sensitive).
  private const val EVENT_NAME_SENTINEL = "1"

  // CustomData keys whose values are sanitized (replaced with SANITIZED_VALUE)
  // instead of being deleted outright when VVP enforces. Mirror of PHP
  // `SignalsIntegrityVVPUtils::APP_CONTENT_ID_KEYS`.
  private val CONTENT_ID_SANITIZE_KEYS = setOf("fb_content_ids", "fb_content_id")
  private const val SANITIZED_VALUE = "_removed_"

  // The contents-array key whose nested `id` fields are scrubbed. Mirrors
  // `CustomEvents::FB_CONTENT` on the app server and the JS pixel's
  // `customData.contents`.
  private const val CONTENTS_KEY = "fb_content"

  /**
   * One detection rule, with regexes pre-compiled at parse time. [keyRegex] and [valueRegex] can
   * each be null (meaning "no constraint on that side"); a rule with both null is dropped during
   * parse. [keyNegativeRegex] is an optional exclusion filter — when non-null, a customData key
   * or event name matching it is treated as a non-match even if [keyRegex] would have matched.
   * Mirrors the JS pixel plugin's `keyOk = keyPos && !keyNeg` semantics. A rule with only
   * [keyNegativeRegex] set (no positive constraint) is also dropped, since it would match every
   * key not in the exclusion set.
   */
  data class CompiledRule(
      val place: Int,
      val keyRegex: Pattern?,
      val keyNegativeRegex: Pattern?,
      val valueRegex: Pattern?,
  )

  data class VVPConfig(
      val rules: List<CompiledRule>,
      val standardParams: Set<String>,
      // null means no event-name gate (NON_RETAIL); non-null restricts detection to this set.
      val inScopeEventNames: Set<String>?,
      // Drives the shadow-vs-enforce split. Fail-open: a missing or null wire value coerces to
      // `true` (shadow) so a misconfigured/legacy server payload never accidentally mutates
      // advertiser data — only an explicit `false` selects the enforcing path. Mirrors JS
      // `config.isShadowEnabled !== false`.
      val isShadowEnabled: Boolean,
  )

  private var enabled: Boolean = false

  @Volatile
  internal var config: VVPConfig? = null
    private set

  @JvmStatic
  fun enable() {
    enabled = true
    loadConfig()
  }

  @JvmStatic
  fun disable() {
    enabled = false
  }

  @JvmStatic fun isEnabled(): Boolean = enabled

  /** Reload [config] from the latest [FetchedAppSettings.vvpConfig] payload. */
  internal fun loadConfig() {
    val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
    val raw = settings?.vvpConfig
    config =
        if (raw.isNullOrEmpty()) {
          // Empty string is the server's "not in VVP scope" signal.
          null
        } else {
          parseConfig(raw)
        }
  }

  /** Parse the JSON-encoded `vvp_config` payload. Returns null on any structural failure. */
  internal fun parseConfig(jsonStr: String): VVPConfig? {
    return try {
      val obj = JSONObject(jsonStr)
      if (!obj.optBoolean(ENABLED_KEY, false)) {
        return null
      }
      val rules = parseRules(obj)
      // Empty rules list -> nothing to detect; treat as out-of-scope so callers can no-op fast.
      if (rules.isEmpty()) {
        return null
      }
      VVPConfig(
          rules = rules,
          standardParams = parseStandardParams(obj),
          inScopeEventNames = parseInScopeEventNames(obj),
          // Fail-open: missing / JSONNull / explicit `true` all coerce to shadow.
          // Only the literal boolean `false` selects enforce.
          isShadowEnabled = obj.optBoolean(IS_SHADOW_ENABLED_KEY, true),
      )
    } catch (_: JSONException) {
      null
    }
  }

  private fun parseRules(obj: JSONObject): List<CompiledRule> {
    val arr = obj.optJSONArray(RULES_KEY) ?: return emptyList()
    val out = mutableListOf<CompiledRule>()
    for (i in 0 until arr.length()) {
      val ruleObj = arr.optJSONObject(i) ?: continue
      val compiled = compileRule(ruleObj) ?: continue
      out.add(compiled)
    }
    return out
  }

  internal fun compileRule(ruleObj: JSONObject): CompiledRule? {
    val place = ruleObj.optInt(PLACE_KEY, -1)
    if (place != PLACE_CUSTOM_DATA && place != PLACE_EVENT_NAME) {
      // Unknown place -> drop silently. Mirror of the JS plugin behaviour.
      return null
    }
    val keyRegex = optRegex(ruleObj, KEY_REGEX_KEY)
    val keyNegativeRegex = optRegex(ruleObj, KEY_NEGATIVE_REGEX_KEY)
    val valueRegex = optRegex(ruleObj, VALUE_REGEX_KEY)
    if (keyRegex == null && valueRegex == null) {
      // Rule with no positive constraint would match every event (or every
      // key not in the negative set) -> drop. Mirrors JS plugin.
      return null
    }
    return CompiledRule(place, keyRegex, keyNegativeRegex, valueRegex)
  }

  private fun optRegex(obj: JSONObject, key: String): Pattern? {
    if (!obj.has(key) || obj.isNull(key)) {
      return null
    }
    val raw = obj.optString(key, "")
    if (raw.isEmpty()) {
      // Empty string is treated as null (matches JS plugin: both mean "no constraint").
      return null
    }
    return try {
      Pattern.compile(raw, Pattern.CASE_INSENSITIVE)
    } catch (_: Exception) {
      // Malformed regex -> drop.
      null
    }
  }

  private fun parseStandardParams(obj: JSONObject): Set<String> {
    val mapObj = obj.optJSONObject(STANDARD_PARAMS_KEY) ?: return emptySet()
    val out = HashSet<String>()
    val it = mapObj.keys()
    while (it.hasNext()) {
      val key = it.next()
      // Server emits {key: true} for every entry; preserve that contract by only including
      // keys whose value is truthy.
      if (mapObj.optBoolean(key, false)) {
        out.add(key)
      }
    }
    return out
  }

  private fun parseInScopeEventNames(obj: JSONObject): Set<String>? {
    if (!obj.has(IN_SCOPE_EVENT_NAMES_KEY) || obj.isNull(IN_SCOPE_EVENT_NAMES_KEY)) {
      return null
    }
    val arr = obj.optJSONArray(IN_SCOPE_EVENT_NAMES_KEY) ?: return null
    val out = HashSet<String>()
    for (i in 0 until arr.length()) {
      val s = arr.optString(i, "")
      if (s.isNotEmpty()) {
        out.add(s)
      }
    }
    return out
  }

  /**
   * Per-event hook. Called from the SDK's event send pipeline (alongside
   * [ProtectedModeManager.processParametersForProtectedMode]). Returns early if VVP is disabled,
   * the event isn't in scope, or no rule matches. On match: sanitizes / strips customData keys per
   * the standardParams allowlist (with content-ID keys replaced rather than dropped), and tags the
   * bundle with `vvp=1` plus a JSON-encoded `vvp_md` describing which keys / event-name triggered
   * the match.
   */
  @JvmStatic
  fun processParametersForVVP(eventName: String, parameters: Bundle?) {
    if (!enabled || parameters == null || parameters.isEmpty) {
      return
    }
    val cfg = config ?: return

    // RETAIL purchase-funnel gate (null/empty = no gate, NON_RETAIL config).
    val gate = cfg.inScopeEventNames
    if (gate != null && gate.isNotEmpty() && eventName !in gate) {
      return
    }

    val cdKeys = LinkedHashSet<String>()
    val evNames = LinkedHashSet<String>()
    var matched = false

    for (rule in cfg.rules) {
      when (rule.place) {
        PLACE_EVENT_NAME -> {
          // Event-name rule — keyRegex matches the eventName itself.
          // keyNegativeRegex excludes event names the positive regex would match.
          if (rule.keyRegex != null && rule.keyRegex.matcher(eventName).find()) {
            val keyNeg = rule.keyNegativeRegex?.matcher(eventName)?.find() ?: false
            if (!keyNeg) {
              matched = true
              // Sentinel — never echo the actual event name back (it can be sensitive).
              evNames.add(EVENT_NAME_SENTINEL)
            }
          }
        }
        PLACE_CUSTOM_DATA -> {
          // CustomData rule — walk every (key, value) pair.
          //   keyRegex null = no key constraint
          //   valueRegex null = no value constraint
          // Both null already filtered out at compile time (compileRule).
          for (key in parameters.keySet().toList()) {
            val value = parameters.get(key)?.toString() ?: continue
            val keyPos = rule.keyRegex?.matcher(key)?.find() ?: true
            val keyNeg = rule.keyNegativeRegex?.matcher(key)?.find() ?: false
            val keyOk = keyPos && !keyNeg
            val valOk = rule.valueRegex?.matcher(value)?.find() ?: true
            if (keyOk && valOk) {
              matched = true
              cdKeys.add(key)
            }
          }
        }
      }
    }

    if (!matched) {
      return
    }

    // Sanitize / filter customData. Skipped entirely in shadow mode so we
    // emit adoption tags without mutating advertiser data — only the explicit
    // enforce path (isShadowEnabled == false) touches the Bundle. Mirrors JS
    // plugin's shadow-vs-enforce split.
    if (!cfg.isShadowEnabled && cfg.standardParams.isNotEmpty()) {
      val keysSnapshot = parameters.keySet().toList()
      for (key in keysSnapshot) {
        if (key in cfg.standardParams) {
          continue
        }
        if (key in CONTENT_ID_SANITIZE_KEYS) {
          // Preserve the key on the payload (downstream attribution expects
          // it) but scrub the actual identifier.
          parameters.putString(key, SANITIZED_VALUE)
        } else {
          parameters.remove(key)
        }
      }

      // Scrub `id` inside each entry of the contents array. `fb_content`
      // is in the standardParams allowlist (so the loop above keeps it),
      // but each entry's nested `id` carries the same video identifier
      // the top-level `fb_content_ids` would. Mirrors the JS pixel
      // plugin and server-side
      // `SignalsIntegrityVVPPreprocessor::scrubIdInContentsArray`.
      scrubIdInContentsArray(parameters)
    }

    parameters.putString(VVP_IS_APPLIED_KEY, VVP_IS_APPLIED_VALUE)

    // Emit vvp_md only when there's something to report. JSON shape mirrors
    // the JS plugin: { vp_rp: [...customData keys], vp_rp_ev: ["1"] }.
    // Empty buckets are omitted; the entire field is omitted if both empty.
    if (cdKeys.isNotEmpty() || evNames.isNotEmpty()) {
      val md = JSONObject()
      if (cdKeys.isNotEmpty()) {
        md.put(VP_RP, JSONArray(cdKeys.toList()))
      }
      if (evNames.isNotEmpty()) {
        md.put(VP_RP_EV, JSONArray(evNames.toList()))
      }
      parameters.putString(VVP_METADATA_KEY, md.toString())
    }
  }

  /**
   * Walk the JSON-encoded `fb_content` array and replace each entry's `id`
   * with [SANITIZED_VALUE]. Other fields (quantity, item_price, brand, …)
   * are preserved. No-op when the key is missing, not a valid JSON array,
   * or no entry has an `id` field.
   */
  internal fun scrubIdInContentsArray(parameters: Bundle) {
    val raw = parameters.getString(CONTENTS_KEY) ?: return
    val arr = try { JSONArray(raw) } catch (_: JSONException) { return }

    var didMutate = false
    for (i in 0 until arr.length()) {
      val entry = arr.optJSONObject(i) ?: continue
      if (entry.has("id")) {
        entry.put("id", SANITIZED_VALUE)
        didMutate = true
      }
    }

    if (didMutate) {
      parameters.putString(CONTENTS_KEY, arr.toString())
    }
  }

  /** Reset all state — for tests. */
  internal fun clearForTests() {
    enabled = false
    config = null
  }
}
