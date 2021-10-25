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
package com.facebook.gamingservices

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler
import com.facebook.gamingservices.internal.TournamentShareDialogURIBuilder
import com.facebook.internal.AppCall
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.FacebookDialogBase
import com.facebook.internal.FragmentWrapper
import com.facebook.internal.NativeProtocol
import com.facebook.internal.NativeProtocol.setupProtocolRequestIntent
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.share.internal.ResultProcessor
import com.facebook.share.internal.ShareConstants
import com.facebook.share.internal.ShareInternalUtility
import java.util.ArrayList

@AutoHandleExceptions
class TournamentShareDialog : FacebookDialogBase<TournamentConfig?, TournamentShareDialog.Result?> {
  var score: Number? = null
  var tournament: Tournament? = null

  /**
   * Constructs a new TournamentShareDialog.
   *
   * @param activity Activity hosting the dialog.
   */
  constructor(activity: Activity) : super(activity, defaultRequestCode)

  /**
   * Constructs a new TournamentShareDialog.
   *
   * @param fragment androidx.fragment.app.Fragment hosting the dialog.
   */
  constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

  /**
   * Constructs a new TournamentShareDialog.
   *
   * @param fragment android.app.Fragment hosting the dialog.
   */
  constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))
  private constructor(fragmentWrapper: FragmentWrapper) : super(fragmentWrapper, defaultRequestCode)

  /**
   * Shows the tournament share dialog, where the user has the option to share a newly created
   * tournament with the provided score and configuration.
   *
   * @param score A number representing an score initial score for the tournament that will be
   * created with the provided config
   * @param newTournamentConfig A tournament configuration that will be used to create a new
   * tournament and post the provided score in said tournament
   */
  fun show(
      score: Number,
      newTournamentConfig: TournamentConfig,
  ) {
    this.score = score
    showImpl(newTournamentConfig, BASE_AUTOMATIC_MODE)
  }

  /**
   * Shows the tournament share dialog, where the user has the option to share the provided
   * tournament with the provided score if it's greater than their previous score.
   *
   * @param score A number representing a score
   * @param tournament An existing tournament that the user wants to post the provided score and
   * share.
   */
  fun show(score: Number, tournament: Tournament) {
    this.score = score
    this.tournament = tournament
    showImpl(null, BASE_AUTOMATIC_MODE)
  }

  override fun showImpl(content: TournamentConfig?, mode: Any) {
    if (CloudGameLoginHandler.isRunningInCloud()) {
      return
    }
    super.showImpl(content, mode)
  }

  override fun registerCallbackImpl(
      callbackManager: CallbackManagerImpl,
      callback: FacebookCallback<Result?>
  ) {
    val resultProcessor: ResultProcessor? =
        object : ResultProcessor(callback) {
          override fun onSuccess(appCall: AppCall, results: Bundle) {
            update(results)
            callback.onSuccess(Result())
          }
        }
    callbackManager.registerCallback(requestCode) { resultCode, data ->
      ShareInternalUtility.handleActivityResult(requestCode, resultCode, data, resultProcessor)
    }
  }

  override val orderedModeHandlers: List<ModeHandler>
    get() {
      val handlers = ArrayList<ModeHandler>()
      handlers.add(FacebookAppHandler())
      handlers.add(AppSwitchHandler())
      return handlers
    }

  override fun createBaseAppCall(): AppCall {
    return AppCall(requestCode)
  }

  /*
   * Describes the result of a Tournament Share Dialog.
   *
   */
  class Result
  companion object {
    private val defaultRequestCode =
        CallbackManagerImpl.RequestCodeOffset.TournamentShareDialog.toRequestCode()
    @JvmField var requestId: String? = null

    private fun update(results: Bundle) {
      this.requestId = results.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_REQUEST_ID)
    }
  }

  private inner class FacebookAppHandler : ModeHandler() {

    override fun canShow(content: TournamentConfig?, isBestEffort: Boolean): Boolean {
      val packageManager: PackageManager = getApplicationContext().packageManager
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"
      val fbAppCanShow = intent.resolveActivity(packageManager) != null

      return fbAppCanShow
    }

    override fun createAppCall(content: TournamentConfig?): AppCall {
      val currentAccessToken = getCurrentAccessToken()
      val appCall = createBaseAppCall()
      val intent = Intent("com.facebook.games.gaming_services.DEEPLINK")
      intent.type = "text/plain"

      if (currentAccessToken == null || currentAccessToken.isExpired) {
        throw FacebookException("Attempted to share tournament with an invalid access token")
      }
      if (currentAccessToken.graphDomain != null &&
          FacebookSdk.GAMING != currentAccessToken.graphDomain) {
        throw FacebookException("Attempted to share tournament while user is not gaming logged in")
      }
      val appID = currentAccessToken.applicationId
      val score = score ?: throw FacebookException("Attempted to share tournament without a score")
      val args =
          if (content != null)
              TournamentShareDialogURIBuilder.bundleForCreating(content, score, appID)
          else
              tournament?.let {
                TournamentShareDialogURIBuilder.bundleForUpdating(it.identifier, score, appID)
              }

      setupProtocolRequestIntent(
          intent, appCall.callId.toString(), "", NativeProtocol.PROTOCOL_VERSION_20210906, args)
      appCall.requestIntent = intent

      return appCall
    }
  }

  private inner class AppSwitchHandler : ModeHandler() {

    override fun canShow(content: TournamentConfig?, isBestEffort: Boolean): Boolean {
      return true
    }

    override fun createAppCall(content: TournamentConfig?): AppCall {
      val appCall: AppCall = this@TournamentShareDialog.createBaseAppCall()
      val currentAccessToken = getCurrentAccessToken()
      if (currentAccessToken == null || currentAccessToken.isExpired) {
        throw FacebookException("Attempted to share tournament with an invalid access token")
      }
      if (currentAccessToken.graphDomain != null &&
          FacebookSdk.GAMING != currentAccessToken.graphDomain) {
        throw FacebookException("Attempted to share tournament without without gaming login")
      }
      val score: Number =
          score ?: throw FacebookException("Attempted to share tournament without a score")

      val uri =
          if (content != null)
              TournamentShareDialogURIBuilder.uriForCreating(
                  content, score, currentAccessToken.applicationId)
          else {
            this@TournamentShareDialog.tournament?.let {
              TournamentShareDialogURIBuilder.uriForUpdating(
                  it.identifier, score, currentAccessToken.applicationId)
            }
          }

      val intent = Intent(Intent.ACTION_VIEW, uri)
      startActivityForResult(intent, requestCode)
      return appCall
    }
  }
}
