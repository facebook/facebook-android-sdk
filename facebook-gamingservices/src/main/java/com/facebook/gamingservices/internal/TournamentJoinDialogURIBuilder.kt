// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.gamingservices.internal

import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants

internal object TournamentJoinDialogURIBuilder {

    private fun baseUriBuilder(): Uri.Builder {
        return Uri.Builder()
                .scheme("https")
                .authority(FacebookSdk.getFacebookGamingDomain())
                .appendPath("dialog")
                .appendPath("join_tournament")
    }

    // If we don't send a tournamentID, the user will be presented with a list of suggested tournaments.
    internal fun uri(tournamentID: String? = null, payload: String? = null): Uri {
        val builder = baseUriBuilder()
        tournamentID?.let {
            builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_ID, it)
        }
        payload?.let {
            builder.appendQueryParameter(SDKConstants.PARAM_PAYLOAD, it)
        }
        return builder.build()
    }


    internal fun bundle(
            appID: String,
            tournamentID: String? = null,
            payload: String? = null
    ): Bundle {
        val args = Bundle()
        args.putString(SDKConstants.PARAM_TOURNAMENTS_DEEPLINK, SDKConstants.PARAM_TOURNAMENTS)
        args.putString(SDKConstants.PARAM_TOURNAMENTS_APP_ID, appID)
        tournamentID?.let { args.putString(SDKConstants.PARAM_TOURNAMENTS_ID, it) }
        payload?.let { args.putString(SDKConstants.PARAM_PAYLOAD, it) }
        return args
    }
}