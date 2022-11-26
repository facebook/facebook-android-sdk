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
import com.facebook.AccessToken
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphResponse
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler
import com.facebook.gamingservices.cloudgaming.DaemonRequest
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum
import com.facebook.gamingservices.model.ContextChooseContent
import com.facebook.internal.*
import com.facebook.share.internal.ResultProcessor
import com.facebook.share.internal.ShareInternalUtility
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ContextChooseDialog : FacebookDialogBase<ContextChooseContent, ContextChooseDialog.Result> {
  private var callback: FacebookCallback<Result>? = null

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  constructor(activity: Activity) : super(activity, DEFAULT_REQUEST_CODE)

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))

  private constructor(
      fragmentWrapper: FragmentWrapper
  ) : super(fragmentWrapper, DEFAULT_REQUEST_CODE)

  /**
   * Indicates whether the context choose dialog can be shown.
   *
   * @return true if the dialog can be shown
   */
  override fun canShow(content: ContextChooseContent): Boolean =
      CloudGameLoginHandler.isRunningInCloud() ||
          FacebookAppHandler().canShow(content, true) ||
          ChromeCustomTabHandler().canShow(content, true)

  override fun showImpl(content: ContextChooseContent, mode: Any) =
      if (CloudGameLoginHandler.isRunningInCloud()) {
        showForCloud(content)
      } else {
        super.showImpl(content, mode)
      }

  private fun showForCloud(content: ContextChooseContent) {
    val currentAccessToken = AccessToken.getCurrentAccessToken()
    if (currentAccessToken == null || currentAccessToken.isExpired) {
      throw FacebookException("Attempted to open ContextChooseContent with an invalid access token")
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
      parameters.put(SDKConstants.PARAM_CONTEXT_FILTERS, content.getFilters())
      parameters.put(SDKConstants.PARAM_CONTEXT_MIN_SIZE, content.minSize)
      content.getFilters()?.let {
        val filtersArray = JSONArray()
        for (filter in it) {
          filtersArray.put(filter)
        }
        parameters.put(SDKConstants.PARAM_CONTEXT_FILTERS, filtersArray)
      }
      DaemonRequest.executeAsync(
          activityContext, parameters, requestCallback, SDKMessageEnum.CONTEXT_CHOOSE)
    } catch (e: JSONException) {
      callback?.onError(FacebookException("Couldn't prepare Context Choose Dialog"))
    }
  }

  override fun registerCallbackImpl(
      callbackManager: CallbackManagerImpl,
      callback: FacebookCallback<Result>
  ) {
    this.callback = callback
    val resultProcessor =
        object : ResultProcessor(callback) {
          override fun onSuccess(appCall: AppCall, results: Bundle?) {
            if (results != null) {
              if (results.getString("error_message") != null) {
                callback.onError(FacebookException(results.getString("error_message")))
                return
              }
              val contextId = results.getString(SDKConstants.PARAM_CONTEXT_ID)
              if (contextId != null) {
                GamingContext.setCurrentGamingContext(GamingContext(contextId))
                callback.onSuccess(Result(results))
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
    get() = listOf(FacebookAppHandler(), ChromeCustomTabHandler())

  override fun createBaseAppCall(): AppCall = AppCall(requestCode)

  /*
   * Describes the result of a Context Choose Dialog.
   */
  class Result {
    /**
     * Returns the context ID.
     *
     * @return the context ID.
     */
    var contextID: String? = null

    constructor(results: Bundle) {
      contextID = results.getString(SDKConstants.PARAM_CONTEXT_ID)
    }

    constructor(response: GraphResponse) {
      try {
        response.getJSONObject()?.let { data ->
          data.optJSONObject("data")?.let { contextID = it.getString("id") }
        }
      } catch (e: JSONException) {
        contextID = null
      }
    }
  }

  private inner class FacebookAppHandler : ModeHandler() {
    override fun canShow(content: ContextChooseContent, isBestEffort: Boolean): Boolean {
      val packageManager = activityContext?.packageManager
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val fbAppCanShow = packageManager?.let { intent.resolveActivity(it) } != null
      val currentToken = AccessToken.getCurrentAccessToken()
      val isGamingLoggedIn =
          currentToken?.graphDomain != null && FacebookSdk.GAMING == currentToken.graphDomain
      return fbAppCanShow && isGamingLoggedIn
    }

    override fun createAppCall(content: ContextChooseContent): AppCall {
      val appCall = createBaseAppCall()
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val accessToken = AccessToken.getCurrentAccessToken()
      val args = Bundle()
      args.putString("deeplink", "CONTEXT_CHOOSE")
      if (accessToken != null) {
        args.putString("game_id", accessToken.applicationId)
      } else {
        args.putString("game_id", FacebookSdk.getApplicationId())
      }

      // TODO: Pass along context key
      if (content.minSize != null) {
        args.putString("min_thread_size", content.minSize.toString())
      }
      if (content.maxSize != null) {
        args.putString("max_thread_size", content.maxSize.toString())
      }
      if (content.getFilters() != null) {
        val jsonList = JSONArray(content.getFilters())
        args.putString("filters", jsonList.toString())
      }
      NativeProtocol.setupProtocolRequestIntent(
          intent, appCall.callId.toString(), "", NativeProtocol.getLatestKnownVersion(), args)
      appCall.requestIntent = intent
      return appCall
    }
  }

  private inner class ChromeCustomTabHandler : ModeHandler() {
    override fun canShow(content: ContextChooseContent, isBestEffort: Boolean): Boolean =
        CustomTabUtils.getChromePackage() != null

    override fun createAppCall(content: ContextChooseContent): AppCall {
      val appCall = createBaseAppCall()
      val accessToken = AccessToken.getCurrentAccessToken()
      val contextChooseParams = Bundle()
      val payload = Bundle()
      val filters = Bundle()
      contextChooseParams.putString(
          ServerProtocol.DIALOG_PARAM_APP_ID,
          accessToken?.applicationId ?: FacebookSdk.getApplicationId())
      if (content.minSize != null) {
        filters.putString("min_size", content.minSize.toString())
      }
      if (content.maxSize != null) {
        filters.putString("max_size", content.maxSize.toString())
      }
      if (content.getFilters() != null) {
        val filterArray = JSONArray(content.getFilters())
        filters.putString("filters", filterArray.toString())
      }
      payload.putString("filters", filters.toString())
      contextChooseParams.putString("payload", payload.toString())
      contextChooseParams.putString(
          ServerProtocol.DIALOG_PARAM_REDIRECT_URI, CustomTabUtils.getDefaultRedirectURI())
      DialogPresenter.setupAppCallForCustomTabDialog(
          appCall, CONTEXT_CHOOSE_DIALOG, contextChooseParams)
      return appCall
    }
  }

  companion object {
    private val DEFAULT_REQUEST_CODE =
        CallbackManagerImpl.RequestCodeOffset.GamingContextChoose.toRequestCode()
    private const val CONTEXT_CHOOSE_DIALOG = "context_choose"
  }
}
