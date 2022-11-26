/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.*
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler
import com.facebook.gamingservices.cloudgaming.DaemonRequest
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum
import com.facebook.gamingservices.model.ContextCreateContent
import com.facebook.internal.*
import com.facebook.share.internal.ResultProcessor
import com.facebook.share.internal.ShareInternalUtility
import org.json.JSONException
import org.json.JSONObject

class ContextCreateDialog : FacebookDialogBase<ContextCreateContent, ContextCreateDialog.Result> {
  private var callback: FacebookCallback<Result>? = null

  /**
   * Constructs a new ContextCreateDialog.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  constructor(activity: Activity) : super(activity, DEFAULT_REQUEST_CODE)

  /**
   * Constructs a new ContextCreateDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

  /**
   * Constructs a new ContextCreateDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))

  private constructor(
      fragmentWrapper: FragmentWrapper
  ) : super(fragmentWrapper, DEFAULT_REQUEST_CODE)

  /**
   * Indicates whether the context create dialog can be shown.
   *
   * @return true if the dialog can be shown
   */
  override fun canShow(content: ContextCreateContent): Boolean =
      CloudGameLoginHandler.isRunningInCloud() ||
          FacebookAppHandler().canShow(content, true) ||
          WebHandler().canShow(content, true)

  override fun showImpl(content: ContextCreateContent, mode: Any) =
      if (CloudGameLoginHandler.isRunningInCloud()) {
        showForCloud(content)
      } else {
        super.showImpl(content, mode)
      }

  private fun showForCloud(content: ContextCreateContent) {
    val currentAccessToken = AccessToken.getCurrentAccessToken()
    if (currentAccessToken == null || currentAccessToken.isExpired) {
      throw FacebookException("Attempted to open ContextCreateContent with an invalid access token")
    }
    val requestCallback =
        DaemonRequest.Callback { response ->
          callback?.let { callback ->
            response.error?.let { error -> callback.onError(FacebookException(error.errorMessage)) }
                ?: callback.onSuccess(Result(response))
          }
        }
    val parameters = JSONObject()
    try {
      if (content.suggestedPlayerID != null) {
        parameters.put(SDKConstants.PARAM_CONTEXT_ID, content.suggestedPlayerID)
      }
      DaemonRequest.executeAsync(
          activityContext, parameters, requestCallback, SDKMessageEnum.CONTEXT_CREATE)
    } catch (e: JSONException) {
      callback?.onError(FacebookException("Couldn't prepare Context Create Dialog"))
    }
  }

  override fun registerCallbackImpl(
      callbackManager: CallbackManagerImpl,
      callback: FacebookCallback<Result>
  ) {
    this.callback = callback
    val resultProcessor: ResultProcessor =
        object : ResultProcessor(callback) {
          override fun onSuccess(appCall: AppCall, results: Bundle?) {
            if (results != null) {
              if (results.getString("error_message") != null) {
                callback.onError(FacebookException(results.getString("error_message")))
                return
              }
              val contextId = results.getString(SDKConstants.PARAM_CONTEXT_ID)
              val contextContextId = results.getString(SDKConstants.PARAM_CONTEXT_CONTEXT_ID)
              if (contextId != null) {
                GamingContext.setCurrentGamingContext(GamingContext(contextId))
                callback.onSuccess(Result(contextId))
              } else if (contextContextId != null) {
                GamingContext.setCurrentGamingContext(GamingContext(contextContextId))
                callback.onSuccess(Result(contextContextId))
              }
              callback.onError(
                  FacebookException(results.getString("Invalid response received from server.")))
            } else {
              onCancel(appCall)
            }
          }
        }
    callbackManager.registerCallback(requestCode) { resultCode, data ->
      ShareInternalUtility.handleActivityResult(requestCode, resultCode, data, resultProcessor)
    }
  }

  override val orderedModeHandlers: List<ModeHandler>
    get() = listOf(FacebookAppHandler(), WebHandler())

  override fun createBaseAppCall(): AppCall = AppCall(requestCode)

  /*
   * Describes the result of a Context Create Dialog.
   */
  class Result {
    /**
     * Returns the context ID.
     *
     * @return the context ID.
     */
    var contextID: String? = null

    internal constructor(contextID: String) {
      this.contextID = contextID
    }

    internal constructor(response: GraphResponse) {
      try {
        response.getJSONObject()?.let { data ->
          data.optJSONObject("data")?.let { contextID = it.getString("id") }
        }
      } catch (e: JSONException) {
        contextID = null
      }
    }
  }

  private inner class WebHandler : ModeHandler() {
    override fun canShow(content: ContextCreateContent, isBestEffort: Boolean): Boolean = true

    override fun createAppCall(content: ContextCreateContent): AppCall {
      val appCall = createBaseAppCall()
      val webParams = Bundle()
      webParams.putString("player_id", content.suggestedPlayerID)
      val currentToken = AccessToken.getCurrentAccessToken()
      if (currentToken != null) {
        webParams.putString("dialog_access_token", currentToken.token)
      }
      DialogPresenter.setupAppCallForWebDialog(appCall, "context", webParams)
      return appCall
    }
  }

  private inner class FacebookAppHandler : ModeHandler() {
    override fun canShow(content: ContextCreateContent, isBestEffort: Boolean): Boolean {
      val packageManager = activityContext?.packageManager
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val fbAppCanShow = packageManager?.let { intent.resolveActivity(it) } != null
      val currentToken = AccessToken.getCurrentAccessToken()
      val isGamingLoggedIn =
          currentToken?.graphDomain != null && FacebookSdk.GAMING == currentToken.graphDomain
      return fbAppCanShow && isGamingLoggedIn
    }

    override fun createAppCall(content: ContextCreateContent): AppCall {
      val appCall = createBaseAppCall()
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val accessToken = AccessToken.getCurrentAccessToken()
      val args = Bundle()
      args.putString("deeplink", "CONTEXT_CREATE")
      if (accessToken != null) {
        args.putString("game_id", accessToken.applicationId)
      } else {
        args.putString("game_id", FacebookSdk.getApplicationId())
      }
      if (content.suggestedPlayerID != null) {
        args.putString("player_id", content.suggestedPlayerID)
      }
      NativeProtocol.setupProtocolRequestIntent(
          intent, appCall.callId.toString(), "", NativeProtocol.getLatestKnownVersion(), args)
      appCall.requestIntent = intent
      return appCall
    }
  }

  companion object {
    private val DEFAULT_REQUEST_CODE =
        CallbackManagerImpl.RequestCodeOffset.GamingContextCreate.toRequestCode()
  }
}
