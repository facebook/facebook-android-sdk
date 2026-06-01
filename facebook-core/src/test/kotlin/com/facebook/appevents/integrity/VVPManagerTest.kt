/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class VVPManagerTest : FacebookPowerMockTestCase() {

  @Mock
  private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
  private val mockAppID = "123"
  private val emptyJSONArray = JSONArray()

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
  }

  @After
  fun tearDown() {
    VVPManager.clearForTests()
  }

  private fun initMockFetchedAppSettings(vvpConfig: String?) {
    val mockFetchedAppSettings =
        FetchedAppSettings(
            false,
            "",
            false,
            1,
            SmartLoginOption.parseOptions(0),
            emptyMap(),
            false,
            mockFacebookRequestErrorClassification,
            "",
            "",
            false,
            codelessEventsEnabled = false,
            eventBindings = emptyJSONArray,
            sdkUpdateMessage = "",
            trackUninstallEnabled = false,
            monitorViaDialogEnabled = false,
            rawAamRules = "",
            suggestedEventsSetting = "",
            restrictiveDataSetting = "",
            protectedModeStandardParamsSetting = emptyJSONArray,
            MACARuleMatchingSetting = emptyJSONArray,
            migratedAutoLogValues = null,
            blocklistEvents = emptyJSONArray,
            redactedEvents = emptyJSONArray,
            sensitiveParams = emptyJSONArray,
            schemaRestrictions = emptyJSONArray,
            bannedParams = emptyJSONArray,
            vvpConfig = vvpConfig,
            currencyDedupeParameters = emptyList(),
            purchaseValueDedupeParameters = emptyList(),
            prodDedupeParameters = emptyList(),
            testDedupeParameters = emptyList(),
            dedupeWindow = 0L,
        )
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
        .thenReturn(mockFetchedAppSettings)
  }

  // --- lifecycle ---

  @Test
  fun `disabled by default`() {
    assertThat(VVPManager.isEnabled()).isFalse
    assertThat(VVPManager.config).isNull()
  }

  @Test
  fun `enable flips state`() {
    initMockFetchedAppSettings(null)
    VVPManager.enable()
    assertThat(VVPManager.isEnabled()).isTrue
  }

  @Test
  fun `enable with null vvpConfig leaves config null`() {
    initMockFetchedAppSettings(null)
    VVPManager.enable()
    assertThat(VVPManager.config).isNull()
  }

  @Test
  fun `enable with empty vvpConfig leaves config null`() {
    initMockFetchedAppSettings("")
    VVPManager.enable()
    assertThat(VVPManager.config).isNull()
  }

  @Test
  fun `disable preserves cached config`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    assertThat(VVPManager.config).isNotNull
    VVPManager.disable()
    assertThat(VVPManager.isEnabled()).isFalse
    assertThat(VVPManager.config).isNotNull
  }

  // --- top-level shape ---

  @Test
  fun `parseConfig returns valid config with rules and standardParams and null inScopeEventNames`() {
    val cfg = VVPManager.parseConfig(VALID_NON_RETAIL_CONFIG)
    assertThat(cfg).isNotNull
    assertThat(cfg!!.rules).hasSize(1)
    assertThat(cfg.standardParams).containsExactlyInAnyOrder("fb_currency", "fb_value")
    assertThat(cfg.inScopeEventNames).isNull()
  }

  @Test
  fun `parseConfig returns valid config with inScopeEventNames for retail`() {
    val cfg = VVPManager.parseConfig(VALID_RETAIL_CONFIG)
    assertThat(cfg).isNotNull
    assertThat(cfg!!.inScopeEventNames).containsExactlyInAnyOrder("Purchase", "AddToCart")
  }

  @Test
  fun `parseConfig returns null when enabled is false`() {
    val json =
        """{
          "enabled": false,
          "rules": [{"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}],
          "standardParams": {"fb_currency": true}
        }"""
    assertThat(VVPManager.parseConfig(json)).isNull()
  }

  @Test
  fun `parseConfig returns null when rules array is empty`() {
    val json =
        """{
          "enabled": true,
          "rules": [],
          "standardParams": {"fb_currency": true}
        }"""
    assertThat(VVPManager.parseConfig(json)).isNull()
  }

  @Test
  fun `parseConfig returns null on malformed JSON`() {
    assertThat(VVPManager.parseConfig("not valid json")).isNull()
  }

  // --- rule compilation ---

  @Test
  fun `compileRule drops unknown place`() {
    val ruleObj =
        JSONObject().apply {
          put("place", 99)
          put("keyRegex", "video")
          put("valueRegex", "")
        }
    assertThat(VVPManager.compileRule(ruleObj)).isNull()
  }

  @Test
  fun `compileRule drops rule with both regexes empty`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "")
          put("valueRegex", "")
        }
    assertThat(VVPManager.compileRule(ruleObj)).isNull()
  }

  @Test
  fun `compileRule drops malformed regex`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "[invalid")
          put("valueRegex", "")
        }
    assertThat(VVPManager.compileRule(ruleObj)).isNull()
  }

  @Test
  fun `compileRule treats null and empty regex strings the same`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", JSONObject.NULL)
          put("valueRegex", "tt\\d+")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    assertThat(rule!!.keyRegex).isNull()
    assertThat(rule.valueRegex).isNotNull
  }

  @Test
  fun `compileRule parses populated keyNegativeRegex`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "video")
          put("keyNegativeRegex", "^safe_")
          put("valueRegex", "")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    assertThat(rule!!.keyNegativeRegex).isNotNull
    // Case-insensitive flag should be applied (mirrors keyRegex/valueRegex).
    assertThat(rule.keyNegativeRegex!!.matcher("SAFE_VIDEO").find()).isTrue
  }

  @Test
  fun `compileRule treats missing keyNegativeRegex as null`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "video")
          put("valueRegex", "")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    assertThat(rule!!.keyNegativeRegex).isNull()
  }

  @Test
  fun `compileRule treats empty keyNegativeRegex as null`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "video")
          put("keyNegativeRegex", "")
          put("valueRegex", "")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    assertThat(rule!!.keyNegativeRegex).isNull()
  }

  @Test
  fun `compileRule treats JSONNull keyNegativeRegex as null`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "video")
          put("keyNegativeRegex", JSONObject.NULL)
          put("valueRegex", "")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    assertThat(rule!!.keyNegativeRegex).isNull()
  }

  @Test
  fun `compileRule drops malformed keyNegativeRegex but keeps the rule`() {
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "video")
          put("keyNegativeRegex", "[invalid")
          put("valueRegex", "")
        }
    val rule = VVPManager.compileRule(ruleObj)
    assertThat(rule).isNotNull
    // Malformed negative regex is silently dropped; the rule itself survives
    // since keyRegex is still valid.
    assertThat(rule!!.keyRegex).isNotNull
    assertThat(rule.keyNegativeRegex).isNull()
  }

  @Test
  fun `compileRule drops rule with only keyNegativeRegex set`() {
    // No positive constraint (no keyRegex, no valueRegex) -> rule would mark
    // every key NOT in the negative set. Mirror JS plugin: drop.
    val ruleObj =
        JSONObject().apply {
          put("place", VVPManager.PLACE_CUSTOM_DATA)
          put("keyRegex", "")
          put("keyNegativeRegex", "^safe_")
          put("valueRegex", "")
        }
    assertThat(VVPManager.compileRule(ruleObj)).isNull()
  }

  @Test
  fun `event-name rule keeps only keyRegex`() {
    val json =
        """{
          "enabled": true,
          "rules": [{"place": 3, "keyRegex": "video_view", "valueRegex": ""}],
          "standardParams": {}
        }"""
    val cfg = VVPManager.parseConfig(json)
    assertThat(cfg).isNotNull
    assertThat(cfg!!.rules).hasSize(1)
    val rule = cfg.rules[0]
    assertThat(rule.place).isEqualTo(VVPManager.PLACE_EVENT_NAME)
    assertThat(rule.keyRegex).isNotNull
    assertThat(rule.valueRegex).isNull()
  }

  @Test
  fun `compiled regex is case-insensitive`() {
    val cfg = VVPManager.parseConfig(VALID_NON_RETAIL_CONFIG)
    val rule = cfg!!.rules[0]
    // The valueRegex is "\\btt\\d{7,}\\b" — should match "TT1234567" (uppercase).
    assertThat(rule.valueRegex!!.matcher("TT1234567").find()).isTrue
  }

  // --- standardParams parsing ---

  @Test
  fun `parseConfig drops standardParams entries whose value is false`() {
    val json =
        """{
          "enabled": true,
          "rules": [{"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}],
          "standardParams": {"keep": true, "drop": false}
        }"""
    val cfg = VVPManager.parseConfig(json)
    assertThat(cfg!!.standardParams).containsExactly("keep")
  }

  @Test
  fun `parseConfig handles missing standardParams`() {
    val json =
        """{
          "enabled": true,
          "rules": [{"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}]
        }"""
    val cfg = VVPManager.parseConfig(json)
    assertThat(cfg!!.standardParams).isEmpty()
  }

  // --- inScopeEventNames parsing ---

  @Test
  fun `parseConfig handles JSONNull inScopeEventNames as no gate`() {
    val raw = JSONObject(VALID_NON_RETAIL_CONFIG)
    raw.put("inScopeEventNames", JSONObject.NULL)
    val cfg = VVPManager.parseConfig(raw.toString())
    assertThat(cfg!!.inScopeEventNames).isNull()
  }

  @Test
  fun `parseConfig handles missing inScopeEventNames as no gate`() {
    val cfg = VVPManager.parseConfig(VALID_NON_RETAIL_CONFIG)
    assertThat(cfg!!.inScopeEventNames).isNull()
  }

  // --- loadConfig integration with FetchedAppSettingsManager ---

  @Test
  fun `enable picks up config from FetchedAppSettings`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    assertThat(VVPManager.config).isNotNull
    assertThat(VVPManager.config!!.rules).hasSize(1)
  }

  @Test
  fun `loadConfig reflects subsequent settings changes`() {
    initMockFetchedAppSettings(null)
    VVPManager.enable()
    assertThat(VVPManager.config).isNull()
    // Simulate settings change
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.loadConfig()
    assertThat(VVPManager.config).isNotNull
  }

  // --- processParametersForVVP: short-circuit branches ---

  @Test
  fun `processParametersForVVP no-ops when disabled`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    // VVPManager.enable() not called → enabled=false.
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp")).isNull()
    assertThat(params.getString("fb_content_ids")).isEqualTo("tt1234567")
  }

  @Test
  fun `processParametersForVVP no-ops when parameters null or empty`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()

    VVPManager.processParametersForVVP("Purchase", null)
    val empty = Bundle()
    VVPManager.processParametersForVVP("Purchase", empty)

    assertThat(empty.isEmpty).isTrue
  }

  @Test
  fun `processParametersForVVP no-ops when config missing`() {
    initMockFetchedAppSettings(null)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp")).isNull()
  }

  @Test
  fun `processParametersForVVP no-ops for retail event not in scope`() {
    initMockFetchedAppSettings(RETAIL_CONFIG)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    // ViewContent isn't in the inScopeEventNames allowlist (purchase-funnel only).
    VVPManager.processParametersForVVP("ViewContent", params)

    assertThat(params.getString("vvp")).isNull()
  }

  @Test
  fun `processParametersForVVP fires for retail event in scope`() {
    initMockFetchedAppSettings(RETAIL_CONFIG)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("fb_mobile_purchase", params)

    assertThat(params.getString("vvp")).isEqualTo("1")
  }

  // --- detection + payload tagging ---

  @Test
  fun `processParametersForVVP tags vvp and emits vvp_md when value matches`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567", "fb_currency" to "USD")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp")).isEqualTo("1")
    val md = JSONObject(params.getString("vvp_md")!!)
    val rp = md.getJSONArray("vp_rp")
    assertThat(rp.length()).isEqualTo(1)
    assertThat(rp.getString(0)).isEqualTo("fb_content_ids")
    assertThat(md.has("vp_rp_ev")).isFalse
  }

  @Test
  fun `processParametersForVVP no detection when no value matches`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "sku-42")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp")).isNull()
    assertThat(params.getString("fb_content_ids")).isEqualTo("sku-42")
  }

  @Test
  fun `processParametersForVVP emits vp_rp_ev sentinel for event-name match`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{"place": 3, "keyRegex": "video_view", "valueRegex": ""}],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("foo" to "bar")

    VVPManager.processParametersForVVP("video_view_started", params)

    assertThat(params.getString("vvp")).isEqualTo("1")
    val md = JSONObject(params.getString("vvp_md")!!)
    val ev = md.getJSONArray("vp_rp_ev")
    assertThat(ev.length()).isEqualTo(1)
    // Sentinel — never echoes the actual event name.
    assertThat(ev.getString(0)).isEqualTo("1")
    assertThat(md.has("vp_rp")).isFalse
  }

  @Test
  fun `processParametersForVVP emits both vp_rp and vp_rp_ev when both fire`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [
            {"place": 3, "keyRegex": "video_view", "valueRegex": ""},
            {"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}
          ],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("video_view_started", params)

    val md = JSONObject(params.getString("vvp_md")!!)
    assertThat(md.getJSONArray("vp_rp").getString(0)).isEqualTo("fb_content_ids")
    assertThat(md.getJSONArray("vp_rp_ev").getString(0)).isEqualTo("1")
  }

  @Test
  fun `processParametersForVVP omits vvp_md when both buckets empty`() {
    // Use a value-only rule that matches — vvp_md should be present.
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp_md")).isNotNull
  }

  // --- sanitization / customData filter ---

  @Test
  fun `processParametersForVVP sanitizes fb_content_ids instead of dropping`() {
    initMockFetchedAppSettings(VALID_NON_RETAIL_CONFIG)
    VVPManager.enable()
    val params =
        bundleOf(
            "fb_content_ids" to "tt1234567",
            "fb_currency" to "USD",
            "video_title" to "Finding Nemo",
        )

    VVPManager.processParametersForVVP("Purchase", params)

    // Standard param preserved.
    assertThat(params.getString("fb_currency")).isEqualTo("USD")
    // Content-ID key sanitized (kept on payload, value scrubbed).
    assertThat(params.containsKey("fb_content_ids")).isTrue
    assertThat(params.getString("fb_content_ids")).isEqualTo("_removed_")
    // Other non-standard key dropped.
    assertThat(params.containsKey("video_title")).isFalse
  }

  @Test
  fun `processParametersForVVP sanitizes fb_content_id singular too`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{"place": 1, "keyRegex": "^fb_content_id$", "valueRegex": "tt\\d+"}],
          "standardParams": {"fb_currency": true}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("fb_content_id" to "tt9876543", "fb_currency" to "USD")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("fb_content_id")).isEqualTo("_removed_")
    assertThat(params.getString("fb_currency")).isEqualTo("USD")
  }

  @Test
  fun `processParametersForVVP skips customData filter when standardParams empty`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567", "video_title" to "Nemo")

    VVPManager.processParametersForVVP("Purchase", params)

    // vvp=1 still emitted, but no filtering since allowlist empty.
    assertThat(params.getString("vvp")).isEqualTo("1")
    assertThat(params.getString("fb_content_ids")).isEqualTo("tt1234567")
    assertThat(params.getString("video_title")).isEqualTo("Nemo")
  }

  // --- keyNegativeRegex detection (customData) ---

  @Test
  fun `processParametersForVVP keyNegativeRegex excludes a key the positive regex would match`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{
            "place": 1,
            "keyRegex": "video",
            "keyNegativeRegex": "^safe_",
            "valueRegex": "tt\\d+"
          }],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    // Both keys would match `keyRegex` and value `tt\d+`, but `safe_video`
    // is excluded by `keyNegativeRegex`. Only `unsafe_video` should match.
    val params = bundleOf("safe_video" to "tt1234567", "unsafe_video" to "tt9876543")

    VVPManager.processParametersForVVP("Purchase", params)

    assertThat(params.getString("vvp")).isEqualTo("1")
    val md = JSONObject(params.getString("vvp_md")!!)
    val rp = md.getJSONArray("vp_rp")
    assertThat(rp.length()).isEqualTo(1)
    assertThat(rp.getString(0)).isEqualTo("unsafe_video")
  }

  @Test
  fun `processParametersForVVP keyNegativeRegex blocks the only match yielding no detection`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{
            "place": 1,
            "keyRegex": "video",
            "keyNegativeRegex": "^safe_",
            "valueRegex": "tt\\d+"
          }],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("safe_video" to "tt1234567")

    VVPManager.processParametersForVVP("Purchase", params)

    // Only candidate was excluded by negative regex -> no match -> no vvp tag.
    assertThat(params.getString("vvp")).isNull()
    assertThat(params.getString("safe_video")).isEqualTo("tt1234567")
  }

  // --- keyNegativeRegex detection (event name) ---

  @Test
  fun `processParametersForVVP keyNegativeRegex excludes event name the positive regex would match`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{
            "place": 3,
            "keyRegex": "video",
            "keyNegativeRegex": "^safe_"
          }],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("foo" to "bar")

    VVPManager.processParametersForVVP("safe_video_view", params)

    // Event name matches keyRegex ("video") but also matches keyNegativeRegex
    // ("^safe_") -> excluded -> no detection.
    assertThat(params.getString("vvp")).isNull()
  }

  @Test
  fun `processParametersForVVP event name fires when keyNegativeRegex does not match`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [{
            "place": 3,
            "keyRegex": "video",
            "keyNegativeRegex": "^safe_"
          }],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("foo" to "bar")

    VVPManager.processParametersForVVP("video_view_started", params)

    // Event name matches keyRegex but NOT keyNegativeRegex -> match fires.
    assertThat(params.getString("vvp")).isEqualTo("1")
    val md = JSONObject(params.getString("vvp_md")!!)
    val ev = md.getJSONArray("vp_rp_ev")
    assertThat(ev.length()).isEqualTo(1)
    assertThat(ev.getString(0)).isEqualTo("1")
  }

  @Test
  fun `processParametersForVVP event name keyNeg blocks while customData still matches`() {
    val cfg =
        """{
          "enabled": true,
          "rules": [
            {"place": 3, "keyRegex": "video", "keyNegativeRegex": "^safe_"},
            {"place": 1, "keyRegex": "", "valueRegex": "tt\\d+"}
          ],
          "standardParams": {}
        }"""
    initMockFetchedAppSettings(cfg)
    VVPManager.enable()
    val params = bundleOf("fb_content_ids" to "tt1234567")

    VVPManager.processParametersForVVP("safe_video_view", params)

    // Event name blocked by keyNeg, but customData rule still fires.
    assertThat(params.getString("vvp")).isEqualTo("1")
    val md = JSONObject(params.getString("vvp_md")!!)
    assertThat(md.getJSONArray("vp_rp").getString(0)).isEqualTo("fb_content_ids")
    assertThat(md.has("vp_rp_ev")).isFalse
  }

  // --- helpers ---

  private fun bundleOf(vararg pairs: Pair<String, String>): Bundle {
    val b = Bundle()
    for ((k, v) in pairs) b.putString(k, v)
    return b
  }

  companion object {
    private const val VALID_NON_RETAIL_CONFIG =
        """{
              "enabled": true,
              "rules": [{"place": 1, "keyRegex": "", "valueRegex": "\\btt\\d{7,}\\b"}],
              "standardParams": {"fb_currency": true, "fb_value": true},
              "inScopeEventNames": null
            }"""

    private const val VALID_RETAIL_CONFIG =
        """{
              "enabled": true,
              "rules": [{"place": 1, "keyRegex": "content_id", "valueRegex": "tt\\d+"}],
              "standardParams": {"fb_currency": true},
              "inScopeEventNames": ["Purchase", "AddToCart"]
            }"""

    private const val RETAIL_CONFIG =
        """{
              "enabled": true,
              "rules": [{"place": 1, "keyRegex": "", "valueRegex": "\\btt\\d{7,}\\b"}],
              "standardParams": {"fb_currency": true, "fb_value": true},
              "inScopeEventNames": ["fb_mobile_purchase", "fb_mobile_add_to_cart"]
            }"""
  }
}
