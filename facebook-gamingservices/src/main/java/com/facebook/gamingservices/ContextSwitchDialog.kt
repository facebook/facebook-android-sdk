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
import com.facebook.gamingservices.model.ContextSwitchContent
import com.facebook.internal.*
import com.facebook.share.internal.ResultProcessor
import com.facebook.share.internal.ShareInternalUtility
import org.json.JSONException
import org.json.JSONObject

/** Provides functionality to switch contexts in games. */
class ContextSwitchDialog : FacebookDialogBase<ContextSwitchContent, ContextSwitchDialog.Result> {
  private var callback: FacebookCallback<Result>? = null

  /**
   * Constructs a new ContextSwitchDialog.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  constructor(activity: Activity) : super(activity, DEFAULT_REQUEST_CODE)

  /**
   * Constructs a new ContextSwitchDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

  /**
   * Constructs a new ContextSwitchDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))

  private constructor(
      fragmentWrapper: FragmentWrapper
  ) : super(fragmentWrapper, DEFAULT_REQUEST_CODE)

  /**
   * Indicates whether the context switch dialog can be shown.
   *
   * @return true if the dialog can be shown
   */
  override fun canShow(content: ContextSwitchContent): Boolean =
      CloudGameLoginHandler.isRunningInCloud() ||
          FacebookAppHandler().canShow(content, true) ||
          WebHandler().canShow(content, true)

  override fun showImpl(content: ContextSwitchContent, mode: Any) =
      if (CloudGameLoginHandler.isRunningInCloud()) {
        showForCloud(content)
      } else {
        super.showImpl(content, mode)
      }

  private fun showForCloud(content: ContextSwitchContent) {
    val currentAccessToken = AccessToken.getCurrentAccessToken()
    if (currentAccessToken == null || currentAccessToken.isExpired) {
      throw FacebookException("Attempted to open ContextSwitchContent with an invalid access token")
    }
    val requestCallback =
        DaemonRequest.Callback { response ->
          callback?.let { callback ->
            response.error?.let { error -> callback.onError(FacebookException(error.errorMessage)) }
                ?: callback.onSuccess(Result(response))
          }
        }
    val contextID = content.contextID
    if (contextID == null) {
      callback?.onError(FacebookException("Required string contextID not provided."))
      return
    }
    val parameters = JSONObject()
    try {
      parameters.put(SDKConstants.PARAM_CONTEXT_ID, contextID)
      DaemonRequest.executeAsync(
          activityContext, parameters, requestCallback, SDKMessageEnum.CONTEXT_SWITCH)
    } catch (e: JSONException) {
      callback?.onError(FacebookException("Couldn't prepare Context Switch Dialog"))
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
   * Describes the result of a Context Switch Dialog.
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
    override fun canShow(content: ContextSwitchContent, isBestEffort: Boolean): Boolean = true

    override fun createAppCall(content: ContextSwitchContent): AppCall {
      val appCall = createBaseAppCall()
      val webParams = Bundle()
      webParams.putString("context_id", content.contextID)
      AccessToken.getCurrentAccessToken()?.let {
        webParams.putString("dialog_access_token", it.token)
      }
      DialogPresenter.setupAppCallForWebDialog(appCall, "context", webParams)
      return appCall
    }
  }

  private inner class FacebookAppHandler : ModeHandler() {
    override fun canShow(content: ContextSwitchContent, isBestEffort: Boolean): Boolean {
      val packageManager = activityContext?.packageManager
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val fbAppCanShow = packageManager?.let { intent.resolveActivity(it) } != null
      val currentToken = AccessToken.getCurrentAccessToken()
      val isGamingLoggedIn =
          currentToken?.graphDomain != null && FacebookSdk.GAMING == currentToken.graphDomain
      return fbAppCanShow && isGamingLoggedIn
    }

    override fun createAppCall(content: ContextSwitchContent): AppCall {
      val appCall = createBaseAppCall()
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val accessToken = AccessToken.getCurrentAccessToken()
      val args = Bundle()
      args.putString("deeplink", "CONTEXT_SWITCH")
      if (accessToken != null) {
        args.putString("game_id", accessToken.applicationId)
      } else {
        args.putString("game_id", FacebookSdk.getApplicationId())
      }
      if (content.contextID != null) {
        args.putString("context_token_id", content.contextID)
      }
      NativeProtocol.setupProtocolRequestIntent(
          intent, appCall.callId.toString(), "", NativeProtocol.getLatestKnownVersion(), args)
      appCall.requestIntent = intent
      return appCall
    }
  }

  companion object {
    private val DEFAULT_REQUEST_CODE =
        CallbackManagerImpl.RequestCodeOffset.GamingContextSwitch.toRequestCode()
  }
}
