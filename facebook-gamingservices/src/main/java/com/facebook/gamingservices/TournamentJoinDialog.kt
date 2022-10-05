// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.gamingservices

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants
import com.facebook.gamingservices.internal.TournamentJoinDialogURIBuilder
import com.facebook.internal.*
import com.facebook.internal.CustomTabUtils.getChromePackage
import com.facebook.internal.CustomTabUtils.getDefaultRedirectURI
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.share.internal.ResultProcessor
import com.facebook.share.internal.ShareConstants
import com.facebook.share.internal.ShareInternalUtility

@AutoHandleExceptions
class TournamentJoinDialog : FacebookDialogBase<String?, TournamentJoinDialog.Result?> {
    companion object {
        private val DEFAULT_REQUEST_CODE =
                CallbackManagerImpl.RequestCodeOffset.TournamentJoinDialog.toRequestCode()
        private const val JOIN_TOURNAMENT_ACCESS_TOKEN_KEY = "access_token"
        private const val JOIN_TOURNAMENT_ACTION = "com.facebook.games.gaming_services.DEEPLINK"
        private const val JOIN_TOURNAMENT_CONTENT_TYPE = "text/plain"
        private const val JOIN_TOURNAMENT_DIALOG = "join_tournament"
        private const val JOIN_TOURNAMENT_ERROR_MESSAGE_KEY = "error_message"
    }

    private var tournamentID: String? = null
    private var requestID: Number? = null
    private var payload: String? = null

    /**
     * Constructs a new TournamentJoinDialog.
     *
     * @param activity Activity hosting the dialog.
     */
    constructor(activity: Activity) : super(activity, DEFAULT_REQUEST_CODE)

    /**
     * Constructs a new TournamentJoinDialog.
     *
     * @param fragment androidx.fragment.app.Fragment hosting the dialog.
     */
    constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

    /**
     * Constructs a new TournamentJoinDialog.
     *
     * @param fragment android.app.Fragment hosting the dialog.
     */
    constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))
    private constructor(
            fragmentWrapper: FragmentWrapper
    ) : super(fragmentWrapper, DEFAULT_REQUEST_CODE)

    /**
     * Shows the tournament join dialog, where the user may join a specified tournament. If
     * no tournamentID is provided, the dialog present a list of suggested tournaments instead.
     *
     * @param tournamentID The tournament to be joined (optional).
     * @param payload The tournament payload.
     */
    fun show(tournamentID: String?, payload: String?) {
        this.tournamentID = tournamentID
        this.payload = payload
        super.showImpl(tournamentID, BASE_AUTOMATIC_MODE)
    }

    override fun showImpl(content: String?, mode: Any) {
        if (CloudGameLoginHandler.isRunningInCloud()) {
            return
        }
        super.showImpl(content, mode)
    }

    override fun registerCallbackImpl(
        callbackManager: CallbackManagerImpl,
        callback: FacebookCallback<Result?>
    ) {
        val resultProcessor: ResultProcessor =
                object : ResultProcessor(callback) {
                    override fun onSuccess(appCall: AppCall, results: Bundle?) {
                        if (results != null) {
                            if (results.getString(JOIN_TOURNAMENT_ERROR_MESSAGE_KEY) != null) {
                                callback.onError(FacebookException(results.getString(JOIN_TOURNAMENT_ERROR_MESSAGE_KEY)))
                                return
                            }

                            if (results.getString(SDKConstants.PARAM_PAYLOAD) != null) {
                                callback.onSuccess(Result(results))
                                return
                            }
                        }

                        onCancel(appCall)
                    }

                }
        callbackManager.registerCallback(requestCode) { resultCode, data ->
            ShareInternalUtility.handleActivityResult(requestCode, resultCode, data, resultProcessor)

        }
    }

    override val orderedModeHandlers: List<ModeHandler>
        get() {
            return listOf(FacebookAppHandler(), ChromeCustomTabHandler())
        }

    override fun createBaseAppCall(): AppCall {
        return AppCall(requestCode)
    }

    /**
     * The result of a successful TournamentJoinDialog.
     */
    class Result(results: Bundle) {
        var requestID: String? = null
        var tournamentID: String? = null
        var payload: String? = null

        init {
            if (results.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_REQUEST_ID) != null) {
                this.requestID = results.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_REQUEST_ID)
            }
            this.tournamentID = results.getString(SDKConstants.PARAM_TOURNAMENTS_ID)
            this.payload = results.getString(SDKConstants.PARAM_PAYLOAD)
        }
    }

    override fun canShow(content: String?): Boolean {
        return if (CloudGameLoginHandler.isRunningInCloud()) {
            false
        } else if (FacebookAppHandler().canShow(content, true)) {
            true
        } else {
            ChromeCustomTabHandler().canShow(content, true)
        }
    }

    private inner class FacebookAppHandler : ModeHandler() {
        override fun canShow(content: String?, isBestEffort: Boolean): Boolean {
            val packageManager: PackageManager = FacebookSdk.getApplicationContext().packageManager
            val intent = Intent(JOIN_TOURNAMENT_ACTION)
            intent.type = JOIN_TOURNAMENT_CONTENT_TYPE
            return intent.resolveActivity(packageManager) != null
        }

        override fun createAppCall(content: String?): AppCall {
            val currentAccessToken = AccessToken.getCurrentAccessToken()
            val appCall = createBaseAppCall()
            val intent = Intent(JOIN_TOURNAMENT_ACTION)
            intent.type = JOIN_TOURNAMENT_CONTENT_TYPE

            if (currentAccessToken == null || currentAccessToken.isExpired) {
                throw FacebookException("Attempted to present TournamentJoinDialog with an invalid access token")
            }
            if (currentAccessToken.graphDomain != null &&
                    FacebookSdk.GAMING != currentAccessToken.graphDomain) {
                throw FacebookException("Attempted to present TournamentJoinDialog while user is not gaming logged in")
            }
            val appID = currentAccessToken.applicationId
            val args = TournamentJoinDialogURIBuilder.bundle(appID, tournamentID, payload)

            NativeProtocol.setupProtocolRequestIntent(
                intent,
                appCall.callId.toString(),
                "",
                NativeProtocol.PROTOCOL_VERSION_20210906,
                args
            )
            appCall.requestIntent = intent

            return appCall
        }
    }

    private inner class ChromeCustomTabHandler : ModeHandler() {
        override fun canShow(content: String?, isBestEffort: Boolean): Boolean {
            val chromePackage = getChromePackage()
            return chromePackage != null
        }

        override fun createAppCall(content: String?): AppCall {
            val appCall = createBaseAppCall()
            val accessToken = AccessToken.getCurrentAccessToken()

            val params = Bundle()
            val payload = Bundle()

            params.putString(
                    ServerProtocol.DIALOG_PARAM_APP_ID,
                    accessToken?.applicationId ?: FacebookSdk.getApplicationId())
            params.putString(SDKConstants.PARAM_PAYLOAD, payload.toString())
            accessToken?.token.let {
                params.putString(JOIN_TOURNAMENT_ACCESS_TOKEN_KEY, accessToken?.token)
            }
            params.putString(
                    ServerProtocol.DIALOG_PARAM_REDIRECT_URI, getDefaultRedirectURI()) // fbconnect://cct.{packageName}
            DialogPresenter.setupAppCallForCustomTabDialog(
                    appCall, JOIN_TOURNAMENT_DIALOG, params)
            return appCall
        }
    }
}