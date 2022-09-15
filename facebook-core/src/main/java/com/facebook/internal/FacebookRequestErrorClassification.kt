/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal

import com.facebook.FacebookRequestError
import org.json.JSONArray
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class FacebookRequestErrorClassification
internal constructor(
    // Key is error code, value is the subcodes. Null subcodes means all subcodes are accepted.
    val otherErrors: Map<Int, Set<Int>?>?,
    val transientErrors: Map<Int, Set<Int>?>?,
    val loginRecoverableErrors: Map<Int, Set<Int>?>?,
    private val otherRecoveryMessage: String?,
    private val transientRecoveryMessage: String?,
    private val loginRecoverableRecoveryMessage: String?
) {
  fun getRecoveryMessage(category: FacebookRequestError.Category?): String? {
    return when (category) {
      FacebookRequestError.Category.OTHER -> otherRecoveryMessage
      FacebookRequestError.Category.LOGIN_RECOVERABLE -> loginRecoverableRecoveryMessage
      FacebookRequestError.Category.TRANSIENT -> transientRecoveryMessage
      else -> null
    }
  }

  fun classify(
      errorCode: Int,
      errorSubCode: Int,
      isTransient: Boolean
  ): FacebookRequestError.Category {
    if (isTransient) {
      return FacebookRequestError.Category.TRANSIENT
    }
    if (otherErrors != null && otherErrors.containsKey(errorCode)) {
      val subCodes = otherErrors[errorCode]
      if (subCodes == null || subCodes.contains(errorSubCode)) {
        return FacebookRequestError.Category.OTHER
      }
    }
    if (loginRecoverableErrors != null && loginRecoverableErrors.containsKey(errorCode)) {
      val subCodes = loginRecoverableErrors[errorCode]
      if (subCodes == null || subCodes.contains(errorSubCode)) {
        return FacebookRequestError.Category.LOGIN_RECOVERABLE
      }
    }
    if (transientErrors != null && transientErrors.containsKey(errorCode)) {
      val subCodes = transientErrors[errorCode]
      if (subCodes == null || subCodes.contains(errorSubCode)) {
        return FacebookRequestError.Category.TRANSIENT
      }
    }
    return FacebookRequestError.Category.OTHER
  }

  companion object {
    const val EC_SERVICE_UNAVAILABLE = 2
    const val EC_APP_TOO_MANY_CALLS = 4
    const val EC_RATE = 9
    const val EC_USER_TOO_MANY_CALLS = 17
    const val EC_INVALID_SESSION = 102
    const val EC_INVALID_TOKEN = 190
    const val EC_APP_NOT_INSTALLED = 412
    const val EC_TOO_MANY_USER_ACTION_CALLS = 341
    const val ESC_APP_NOT_INSTALLED = 458
    const val ESC_APP_INACTIVE = 493
    const val KEY_RECOVERY_MESSAGE = "recovery_message"
    const val KEY_NAME = "name"
    const val KEY_OTHER = "other"
    const val KEY_TRANSIENT = "transient"
    const val KEY_LOGIN_RECOVERABLE = "login_recoverable"
    private var defaultInstance: FacebookRequestErrorClassification? = null

    @JvmStatic
    val defaultErrorClassification: FacebookRequestErrorClassification
      @Synchronized
      get() {
        if (this.defaultInstance == null) {
          this.defaultInstance = defaultErrorClassificationImpl
        }
        return defaultInstance as FacebookRequestErrorClassification
      }

    private val defaultErrorClassificationImpl: FacebookRequestErrorClassification
      private get() {
        val transientErrors: HashMap<Int, Set<Int>?> =
            hashMapOf(
                EC_SERVICE_UNAVAILABLE to null,
                EC_APP_TOO_MANY_CALLS to null,
                EC_RATE to null,
                EC_USER_TOO_MANY_CALLS to null,
                EC_TOO_MANY_USER_ACTION_CALLS to null,
            )
        val loginRecoverableErrors: Map<Int, Set<Int>?> =
            hashMapOf(
                EC_INVALID_SESSION to null,
                EC_INVALID_TOKEN to null,
                EC_APP_NOT_INSTALLED to null,
            )
        return FacebookRequestErrorClassification(
            null, transientErrors, loginRecoverableErrors, null, null, null)
      }

    private fun parseJSONDefinition(definition: JSONObject): Map<Int, Set<Int>?>? {
      val itemsArray = definition.optJSONArray("items")
      if (itemsArray.length() == 0) {
        return null
      }
      val items: MutableMap<Int, Set<Int>?> = HashMap()
      for (i in 0 until itemsArray.length()) {
        val item = itemsArray.optJSONObject(i) ?: continue
        val code = item.optInt("code")
        if (code == 0) {
          continue
        }
        var subcodes: MutableSet<Int>? = null
        val subcodesArray = item.optJSONArray("subcodes")
        if (subcodesArray != null && subcodesArray.length() > 0) {
          subcodes = HashSet()
          for (j in 0 until subcodesArray.length()) {
            val subCode = subcodesArray.optInt(j)
            if (subCode != 0) {
              subcodes.add(subCode)
            }
          }
        }
        items[code] = subcodes
      }
      return items
    }

    @JvmStatic
    fun createFromJSON(jsonArray: JSONArray?): FacebookRequestErrorClassification? {
      if (jsonArray == null) {
        return null
      }
      var otherErrors: Map<Int, Set<Int>?>? = null
      var transientErrors: Map<Int, Set<Int>?>? = null
      var loginRecoverableErrors: Map<Int, Set<Int>?>? = null
      var otherRecoveryMessage: String? = null
      var transientRecoveryMessage: String? = null
      var loginRecoverableRecoveryMessage: String? = null
      for (i in 0 until jsonArray.length()) {
        val definition = jsonArray.optJSONObject(i) ?: continue
        val name = definition.optString(KEY_NAME) ?: continue
        if (name.equals(KEY_OTHER, ignoreCase = true)) {
          otherRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null)
          otherErrors = parseJSONDefinition(definition)
        } else if (name.equals(KEY_TRANSIENT, ignoreCase = true)) {
          transientRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null)
          transientErrors = parseJSONDefinition(definition)
        } else if (name.equals(KEY_LOGIN_RECOVERABLE, ignoreCase = true)) {
          loginRecoverableRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null)
          loginRecoverableErrors = parseJSONDefinition(definition)
        }
      }
      return FacebookRequestErrorClassification(
          otherErrors,
          transientErrors,
          loginRecoverableErrors,
          otherRecoveryMessage,
          transientRecoveryMessage,
          loginRecoverableRecoveryMessage)
    }
  }
}
