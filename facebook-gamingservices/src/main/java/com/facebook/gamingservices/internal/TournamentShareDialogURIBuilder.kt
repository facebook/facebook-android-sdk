// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.gamingservices.internal

import android.os.Build
import android.os.Bundle
import com.facebook.gamingservices.TournamentConfig
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants

internal object TournamentShareDialogURIBuilder {

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
