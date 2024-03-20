/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.facebook.FacebookException
import com.facebook.LoggingBehavior
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.processDeprecatedParameters
import com.facebook.appevents.integrity.IntegrityManager
import com.facebook.appevents.integrity.ProtectedModeManager.protectedModeIsApplied
import com.facebook.appevents.integrity.RedactedEventsManager
import com.facebook.appevents.integrity.SensitiveParamsManager.processFilterSensitiveParams
import com.facebook.appevents.internal.AppEventUtility.bytesToHex
import com.facebook.appevents.internal.Constants
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.processEvent
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.processParameters
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Utility.logd
import java.io.ObjectStreamException
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.UUID
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AppEvent : Serializable {
  val jsonObject: JSONObject
  val isImplicit: Boolean
  private val inBackground: Boolean
  val name: String
  private val checksum: String?

  @Throws(JSONException::class, FacebookException::class)
  constructor(
          contextName: String,
          eventName: String,
          valueToSum: Double?,
          parameters: Bundle?,
          isImplicitlyLogged: Boolean,
          isInBackground: Boolean,
          currentSessionId: UUID?
  ) {
    isImplicit = isImplicitlyLogged
    inBackground = isInBackground
    name = eventName
    jsonObject =
            getJSONObjectForAppEvent(contextName, eventName, valueToSum, parameters, currentSessionId)
    checksum = calculateChecksum()
  }

  private constructor(
          jsonString: String,
          isImplicit: Boolean,
          inBackground: Boolean,
          checksum: String?
  ) {
    jsonObject = JSONObject(jsonString)
    this.isImplicit = isImplicit
    name = jsonObject.optString(Constants.EVENT_NAME_EVENT_KEY)
    this.checksum = checksum
    this.inBackground = inBackground
  }

  fun getIsImplicit(): Boolean = isImplicit

  fun getJSONObject(): JSONObject = jsonObject

  // for old events we don't have a checksum
  val isChecksumValid: Boolean
    get() =
      if (checksum == null) {
        // for old events we don't have a checksum
        true
      } else calculateChecksum() == checksum

  private fun getJSONObjectForAppEvent(
          contextName: String,
          eventName: String,
          valueToSum: Double?,
          parameters: Bundle?,
          currentSessionId: UUID?
  ): JSONObject {
    validateIdentifier(eventName)
    val eventObject = JSONObject()
    var finalEventName = processEvent(eventName)

    if (finalEventName == eventName) {
      /* move forward to next check on event name redaction */
      finalEventName = RedactedEventsManager.processEventsRedaction(eventName)
    }

    eventObject.put(Constants.EVENT_NAME_EVENT_KEY, finalEventName)
    eventObject.put(Constants.EVENT_NAME_MD5_EVENT_KEY, md5Checksum(finalEventName))
    eventObject.put(Constants.LOG_TIME_APP_EVENT_KEY, System.currentTimeMillis() / 1000)
    eventObject.put("_ui", contextName)
    if (currentSessionId != null) {
      eventObject.put("_session_id", currentSessionId)
    }
    if (parameters != null) {
      val processedParam = validateParameters(parameters)
      for (key in processedParam.keys) {
        eventObject.put(key, processedParam[key])
      }
    }
    if (valueToSum != null) {
      eventObject.put(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, valueToSum.toDouble())
    }
    if (inBackground) {
      eventObject.put("_inBackground", "1")
    }
    if (isImplicit) {
      eventObject.put("_implicitlyLogged", "1")
    } else {
      log(LoggingBehavior.APP_EVENTS, "AppEvents", "Created app event '%s'", eventObject.toString())
    }
    return eventObject
  }

  private fun validateParameters(parameters: Bundle): Map<String, String?> {
    val paramMap: MutableMap<String, String> = hashMapOf()
    for (key in parameters.keySet()) {
      validateIdentifier(key)
      val value = parameters[key]
      if (value !is String && value !is Number) {
        throw FacebookException(
                String.format(
                        "Parameter value '%s' for key '%s' should be a string" + " or a numeric type.",
                        value,
                        key))
      }
      paramMap[key] = value.toString()
    }
    if (!protectedModeIsApplied(parameters)) {
      processFilterSensitiveParams(paramMap as MutableMap<String, String?>, name)
    }
    IntegrityManager.processParameters(paramMap)
    processParameters(paramMap as MutableMap<String, String?>, name)
    processDeprecatedParameters(paramMap as MutableMap<String, String?>, name)
    return paramMap
  }

  internal class SerializationProxyV2
  constructor(
          private val jsonString: String,
          private val isImplicit: Boolean,
          private val inBackground: Boolean,
          private val checksum: String?
  ) : Serializable {
    @Throws(JSONException::class, ObjectStreamException::class)
    private fun readResolve(): Any {
      return AppEvent(jsonString, isImplicit, inBackground, checksum)
    }

    companion object {
      private const val serialVersionUID = 20160803001L
    }
  }

  @Throws(ObjectStreamException::class)
  private fun writeReplace(): Any {
    return SerializationProxyV2(jsonObject.toString(), isImplicit, inBackground, checksum)
  }

  override fun toString(): String {
    return String.format(
            "\"%s\", implicit: %b, json: %s",
            jsonObject.optString("_eventName"),
            isImplicit,
            jsonObject.toString())
  }

  private fun calculateChecksum(): String {
    // jsonObject.toString() doesn't guarantee order of the keys on KitKat
    // (API Level 19) and below as JSONObject used HashMap internally,
    // starting Android API Level 20+, JSONObject changed to use LinkedHashMap
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
      return md5Checksum(jsonObject.toString())
    }
    val keys = arrayListOf<String>()
    val iterator = jsonObject.keys()
    while (iterator.hasNext()) {
      keys.add(iterator.next())
    }
    keys.sort()
    val sb = StringBuilder()
    for (key in keys) {
      sb.append(key).append(" = ").append(jsonObject.optString(key)).append('\n')
    }
    return md5Checksum(sb.toString())
  }

  companion object {
    private const val serialVersionUID = 1L
    private val validatedIdentifiers = HashSet<String>()
    private const val MAX_IDENTIFIER_LENGTH = 40

    // throw exception if not valid.
    private fun validateIdentifier(identifier: String) {

      // Identifier should be 40 chars or less, and only have 0-9A-Za-z, underscore, hyphen,
      // and space (but no hyphen or space in the first position).
      var identifier: String? = identifier
      val regex = "^[0-9a-zA-Z_]+[0-9a-zA-Z _-]*$"
      if (identifier == null || identifier.isEmpty() || identifier.length > MAX_IDENTIFIER_LENGTH) {
        if (identifier == null) {
          identifier = "<None Provided>"
        }
        throw FacebookException(
                String.format(
                        Locale.ROOT,
                        "Identifier '%s' must be less than %d characters",
                        identifier,
                        MAX_IDENTIFIER_LENGTH))
      }
      var alreadyValidated: Boolean
      synchronized(validatedIdentifiers) {
        alreadyValidated = validatedIdentifiers.contains(identifier)
      }
      if (!alreadyValidated) {
        if (identifier.matches(Regex(regex))) {
          synchronized(validatedIdentifiers) { validatedIdentifiers.add(identifier) }
        } else {
          throw FacebookException(
                  String.format(
                          "Skipping event named '%s' due to illegal name - must be " +
                                  "under 40 chars and alphanumeric, _, - or space, and " +
                                  "not start with a space or hyphen.",
                          identifier))
        }
      }
    }

    private fun md5Checksum(toHash: String): String {
      val hash: String
      try {
        val digest = MessageDigest.getInstance("MD5")
        var bytes = toHash.toByteArray(charset("UTF-8"))
        digest.update(bytes, 0, bytes.size)
        bytes = digest.digest()
        hash = bytesToHex(bytes)
      } catch (e: NoSuchAlgorithmException) {
        logd("Failed to generate checksum: ", e)
        return "0"
      } catch (e: UnsupportedEncodingException) {
        logd("Failed to generate checksum: ", e)
        return "1"
      }
      return hash
    }
  }
}
