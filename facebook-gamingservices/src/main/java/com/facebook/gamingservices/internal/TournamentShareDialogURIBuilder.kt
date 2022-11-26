/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.internal

import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.facebook.gamingservices.TournamentConfig
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants

internal object TournamentShareDialogURIBuilder {
  const val scheme = "https"
  const val authority = "fb.gg"
  const val me = "me"
  const val tournament = "instant_tournament"

  internal fun uriForUpdating(tournamentID: String, score: Number, appID: String): Uri {
    return Uri.Builder()
        .scheme(scheme)
        .authority(authority)
        .appendPath(me)
        .appendPath(tournament)
        .appendPath(appID)
        .appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_ID, tournamentID)
        .appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_SCORE, score.toString())
        .build()
  }

  internal fun uriForCreating(config: TournamentConfig, score: Number, appID: String): Uri {
    val builder =
        Uri.Builder()
            .scheme(scheme)
            .authority(authority)
            .appendPath(me)
            .appendPath(tournament)
            .appendPath(appID)
            .appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_SCORE, score.toString())
    config.endTime?.let {
      builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_END_TIME, it.toString())
    }
    config.sortOrder?.let {
      builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_SORT_ORDER, it.toString())
    }
    config.scoreType?.let {
      builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_SCORE_FORMAT, it.toString())
    }
    config.title?.let { builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_TITLE, it) }
    config.payload?.let { builder.appendQueryParameter(SDKConstants.PARAM_TOURNAMENTS_PAYLOAD, it) }
    return builder.build()
  }

  internal fun bundleForUpdating(tournamentID: String, score: Number, appID: String): Bundle {
    val args = Bundle()
    args.putString(SDKConstants.PARAM_TOURNAMENTS_DEEPLINK, SDKConstants.PARAM_TOURNAMENTS)
    args.putString(SDKConstants.PARAM_TOURNAMENTS_APP_ID, appID)
    args.putString(SDKConstants.PARAM_TOURNAMENTS_SCORE, score.toString())
    args.putString(SDKConstants.PARAM_TOURNAMENTS_ID, tournamentID)
    return args
  }

  internal fun bundleForCreating(config: TournamentConfig, score: Number, appID: String): Bundle {
    val args = Bundle()
    args.putString(SDKConstants.PARAM_TOURNAMENTS_DEEPLINK, SDKConstants.PARAM_TOURNAMENTS)
    args.putString(SDKConstants.PARAM_TOURNAMENTS_APP_ID, appID)
    args.putString(SDKConstants.PARAM_TOURNAMENTS_SCORE, score.toString())

    config.sortOrder?.let {
      args.putString(SDKConstants.PARAM_TOURNAMENTS_SORT_ORDER, it.toString())
    }
    config.scoreType?.let {
      args.putString(SDKConstants.PARAM_TOURNAMENTS_SCORE_FORMAT, it.toString())
    }
    config.title?.let { args.putString(SDKConstants.PARAM_TOURNAMENTS_TITLE, it.toString()) }
    config.payload?.let { args.putString(SDKConstants.PARAM_TOURNAMENTS_PAYLOAD, it.toString()) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      config.endTime?.let {
        val endTime = it.epochSecond.toInt()
        args.putString(SDKConstants.PARAM_TOURNAMENTS_END_TIME, endTime.toString())
      }
    }
    return args
  }
}
