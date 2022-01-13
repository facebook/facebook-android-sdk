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

package com.facebook.login

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.AuthenticationToken
import com.facebook.FacebookException
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.NativeProtocol
import com.facebook.internal.Utility.getBundleLongAsDate
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.readStringMapFromParcel
import com.facebook.internal.Utility.writeStringMapToParcel
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.util.Date
import org.json.JSONException
import org.json.JSONObject

/**
 * This is an internal class in Facebook SDK and it should not be used directly from external code.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
abstract class LoginMethodHandler : Parcelable {
  var methodLoggingExtras: MutableMap<String?, String?>? = null
  lateinit var loginClient: LoginClient
  abstract val nameForLogging: String

  /** Main constructor. */
  constructor(loginClient: LoginClient) {
    this.loginClient = loginClient
  }

  /**
   * Constructor for restoring from a parcel. It should only be called by subclass's parcel
   * restoring constructors.
   */
  protected constructor(source: Parcel) {
    methodLoggingExtras = readStringMapFromParcel(source)?.toMutableMap()
  }

  abstract fun tryAuthorize(request: LoginClient.Request): Int
  open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean = false

  open fun needsInternetPermission(): Boolean = false

  open fun cancel() = Unit

  @Throws(JSONException::class) open fun putChallengeParam(param: JSONObject) = Unit

  protected open fun getClientState(authId: String): String {
    val param = JSONObject()
    try {
      param.put(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID, authId)
      param.put(LoginLogger.EVENT_PARAM_METHOD, nameForLogging)
      putChallengeParam(param)
    } catch (e: JSONException) {
      Log.w("LoginMethodHandler", "Error creating client state json: " + e.message)
    }
    return param.toString()
  }

  protected open fun addLoggingExtra(key: String?, value: Any?) {
    if (methodLoggingExtras == null) {
      methodLoggingExtras = HashMap()
    }
    methodLoggingExtras?.put(key, value?.toString())
  }

  protected open fun logWebLoginCompleted(e2e: String?) {
    val applicationId = loginClient.getPendingRequest().applicationId
    val logger = InternalAppEventsLogger(loginClient.activity, applicationId)
    val parameters = Bundle()
    parameters.putString(AnalyticsEvents.PARAMETER_WEB_LOGIN_E2E, e2e)
    parameters.putLong(
        AnalyticsEvents.PARAMETER_WEB_LOGIN_SWITCHBACK_TIME, System.currentTimeMillis())
    parameters.putString(AnalyticsEvents.PARAMETER_APP_ID, applicationId)
    logger.logEventImplicitly(AnalyticsEvents.EVENT_WEB_LOGIN_COMPLETE, null, parameters)
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    writeStringMapToParcel(dest, methodLoggingExtras)
  }

  open fun shouldKeepTrackOfMultipleIntents(): Boolean = false

  companion object {
    @JvmStatic
    @Throws(FacebookException::class)
    fun createAuthenticationTokenFromNativeLogin(
        bundle: Bundle,
        expectedNonce: String?
    ): AuthenticationToken? {
      val authenticationTokenString = bundle.getString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN)
      return if (authenticationTokenString == null ||
          authenticationTokenString.isEmpty() ||
          expectedNonce == null ||
          expectedNonce.isEmpty()) {
        null
      } else
          try {
            AuthenticationToken(authenticationTokenString, expectedNonce)
          } catch (_ex: Exception) {
            // any exception happens we need to bubble that to FacebookException
            throw FacebookException(_ex.message)
          }
    }

    @JvmStatic
    fun createAccessTokenFromNativeLogin(
        bundle: Bundle,
        source: AccessTokenSource?,
        applicationId: String
    ): AccessToken? {
      val expires =
          getBundleLongAsDate(bundle, NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date(0))
      val permissions = bundle.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS)
      val token = bundle.getString(NativeProtocol.EXTRA_ACCESS_TOKEN)
      val dataAccessExpirationTime =
          getBundleLongAsDate(bundle, NativeProtocol.EXTRA_DATA_ACCESS_EXPIRATION_TIME, Date(0))
      if (token == null || token.isEmpty()) {
        return null
      }
      val userId = bundle.getString(NativeProtocol.EXTRA_USER_ID)
      if (userId == null || userId.isEmpty()) {
        return null
      }
      val graphDomain = bundle.getString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN)
      return AccessToken(
          token,
          applicationId,
          userId,
          permissions,
          null,
          null,
          source,
          expires,
          Date(),
          dataAccessExpirationTime,
          graphDomain)
    }

    /**
     * WARNING: This feature is currently in development and not intended for external usage.
     *
     * @param bundle
     * @param expectedNonce the nonce expected to have created with the AuthenticationToken
     * @return AuthenticationToken
     * @throws FacebookException
     */
    @JvmStatic
    @Throws(FacebookException::class)
    fun createAuthenticationTokenFromWebBundle(
        bundle: Bundle,
        expectedNonce: String?
    ): AuthenticationToken? {
      val authenticationTokenString = bundle.getString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY)
      return if (authenticationTokenString == null ||
          authenticationTokenString.isEmpty() ||
          expectedNonce == null ||
          expectedNonce.isEmpty()) {
        null
      } else
          try {
            /**
             * TODO T96881697: create factory class for authentication token, remove this try-catch
             * and replace AuthenticationToken with factory method
             */
            AuthenticationToken(authenticationTokenString, expectedNonce)
          } catch (ex: Exception) {
            throw FacebookException(ex.message, ex)
          }
    }

    @JvmStatic
    @Throws(FacebookException::class)
    fun createAccessTokenFromWebBundle(
        requestedPermissions: Collection<String?>?,
        bundle: Bundle,
        source: AccessTokenSource?,
        applicationId: String
    ): AccessToken? {
      var grantedRequestedPermissions = requestedPermissions
      val expires = getBundleLongAsDate(bundle, AccessToken.EXPIRES_IN_KEY, Date())
      val token = bundle.getString(AccessToken.ACCESS_TOKEN_KEY) ?: return null
      val dataAccessExpirationTime =
          getBundleLongAsDate(bundle, AccessToken.DATA_ACCESS_EXPIRATION_TIME, Date(0))

      // With Login v4, we now get back the actual permissions granted, so update the permissions
      // to be the real thing
      val grantedPermissions = bundle.getString("granted_scopes")
      if (grantedPermissions != null && grantedPermissions.isNotEmpty()) {
        grantedRequestedPermissions = arrayListOf(*grantedPermissions.split(",").toTypedArray())
      }
      val deniedPermissions = bundle.getString("denied_scopes")
      var declinedPermissions: List<String?>? = null
      if (deniedPermissions != null && deniedPermissions.isNotEmpty()) {
        declinedPermissions = arrayListOf(*deniedPermissions.split(",").toTypedArray())
      }
      val expiredScopes = bundle.getString("expired_scopes")
      var expiredPermissions: List<String?>? = null
      if (expiredScopes != null && expiredScopes.isNotEmpty()) {
        expiredPermissions = arrayListOf(*expiredScopes.split(",").toTypedArray())
      }
      if (isNullOrEmpty(token)) {
        return null
      }
      val graphDomain = bundle.getString(AccessToken.GRAPH_DOMAIN)
      val signedRequest = bundle.getString("signed_request")
      val userId = getUserIDFromSignedRequest(signedRequest)
      return AccessToken(
          token,
          applicationId,
          userId,
          grantedRequestedPermissions,
          declinedPermissions,
          expiredPermissions,
          source,
          expires,
          Date(),
          dataAccessExpirationTime,
          graphDomain)
    }

    @JvmStatic
    @Throws(FacebookException::class)
    fun getUserIDFromSignedRequest(signedRequest: String?): String {
      if (signedRequest == null || signedRequest.isEmpty()) {
        throw FacebookException("Authorization response does not contain the signed_request")
      }
      try {
        val signatureAndPayload = signedRequest.split(".").toTypedArray()
        if (signatureAndPayload.size == 2) {
          val data = Base64.decode(signatureAndPayload[1], Base64.DEFAULT)
          val dataStr = String(data, Charsets.UTF_8)
          val jsonObject = JSONObject(dataStr)
          return jsonObject.getString("user_id")
        }
      } catch (ex: UnsupportedEncodingException) {} catch (ex: JSONException) {}
      throw FacebookException("Failed to retrieve user_id from signed_request")
    }
  }
}
