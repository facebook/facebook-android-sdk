/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.internal.NativeProtocol
import com.facebook.internal.PlatformServiceClient
import com.facebook.internal.Utility
import com.facebook.internal.Utility.getGraphMeRequestWithCacheAsync
import java.util.HashSet
import org.json.JSONException
import org.json.JSONObject

class GetTokenLoginMethodHandler : LoginMethodHandler {
  private var getTokenClient: GetTokenClient? = null

  constructor(loginClient: LoginClient) : super(loginClient)

  override val nameForLogging = "get_token"

  override fun cancel() {
    getTokenClient?.let {
      it.cancel()
      it.setCompletedListener(null)
      getTokenClient = null
    }
  }

  override fun tryAuthorize(request: LoginClient.Request): Int {
    getTokenClient =
        GetTokenClient(loginClient.activity ?: FacebookSdk.getApplicationContext(), request)
    if (getTokenClient?.start() == false) {
      return 0
    }
    loginClient.notifyBackgroundProcessingStart()
    val callback =
        PlatformServiceClient.CompletedListener { result -> getTokenCompleted(request, result) }
    getTokenClient?.setCompletedListener(callback)
    return 1
  }

  fun getTokenCompleted(request: LoginClient.Request, result: Bundle?) {
    getTokenClient?.setCompletedListener(null)
    getTokenClient = null
    loginClient.notifyBackgroundProcessingStop()
    if (result != null) {
      val currentPermissions =
          result.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS) ?: emptyList()
      val permissions = request.permissions ?: emptySet()
      val idTokenString = result.getString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN)

      // if request param has openid but result does not have id_token
      // fallback to try next handler to get id_token
      if (permissions.contains("openid") && idTokenString.isNullOrEmpty()) {
        loginClient.tryNextHandler()
        return
      }
      if (currentPermissions.containsAll(permissions)) {
        // We got all the permissions we needed, so we can complete the auth now.
        complete(request, result)
        return
      }

      // We didn't get all the permissions we wanted, so update the request with just the
      // permissions we still need.
      val newPermissions = HashSet<String>()
      for (permission in permissions) {
        if (!currentPermissions.contains(permission)) {
          newPermissions.add(permission)
        }
      }
      if (newPermissions.isNotEmpty()) {
        addLoggingExtra(
            LoginLogger.EVENT_EXTRAS_NEW_PERMISSIONS, TextUtils.join(",", newPermissions))
      }
      request.permissions = newPermissions
    }
    loginClient.tryNextHandler()
  }

  fun onComplete(request: LoginClient.Request, result: Bundle) {
    val outcome =
        try {
          val token =
              createAccessTokenFromNativeLogin(
                  result, AccessTokenSource.FACEBOOK_APPLICATION_SERVICE, request.applicationId)
          val authenticationToken = createAuthenticationTokenFromNativeLogin(result, request.nonce)
          LoginClient.Result.createCompositeTokenResult(request, token, authenticationToken)
        } catch (ex: FacebookException) {
          LoginClient.Result.createErrorResult(loginClient.pendingRequest, null, ex.message)
        }
    loginClient.completeAndValidate(outcome)
  }

  // Workaround for old facebook apps that don't return the userid.
  fun complete(request: LoginClient.Request, result: Bundle) {
    val userId = result.getString(NativeProtocol.EXTRA_USER_ID)
    // If the result is missing the UserId request it
    if (userId.isNullOrEmpty()) {
      loginClient.notifyBackgroundProcessingStart()
      val accessToken = checkNotNull(result.getString(NativeProtocol.EXTRA_ACCESS_TOKEN))
      getGraphMeRequestWithCacheAsync(
          accessToken,
          object : Utility.GraphMeRequestWithCacheCallback {
            override fun onSuccess(userInfo: JSONObject?) {
              try {
                result.putString(NativeProtocol.EXTRA_USER_ID, userInfo?.getString("id"))
                onComplete(request, result)
              } catch (ex: JSONException) {
                loginClient.complete(
                    LoginClient.Result.createErrorResult(
                        loginClient.pendingRequest, "Caught exception", ex.message))
              }
            }

            override fun onFailure(error: FacebookException?) {
              loginClient.complete(
                  LoginClient.Result.createErrorResult(
                      loginClient.pendingRequest, "Caught exception", error?.message))
            }
          })
    } else {
      onComplete(request, result)
    }
  }

  constructor(source: Parcel) : super(source)

  override fun describeContents(): Int = 0

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<GetTokenLoginMethodHandler> =
        object : Parcelable.Creator<GetTokenLoginMethodHandler> {
          override fun createFromParcel(source: Parcel): GetTokenLoginMethodHandler {
            return GetTokenLoginMethodHandler(source)
          }

          override fun newArray(size: Int): Array<GetTokenLoginMethodHandler?> {
            return arrayOfNulls(size)
          }
        }
  }
}
