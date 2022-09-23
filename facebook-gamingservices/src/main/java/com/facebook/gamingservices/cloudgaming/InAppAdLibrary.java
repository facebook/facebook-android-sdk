/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.cloudgaming;

import android.content.Context;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppAdLibrary {

  /**
   * Sets a callback to be triggered after the rewarded video is loaded. This MUST be called before
   * showRewardedVideo().
   *
   * @param context the application context
   * @param placementID the placement ID of the ad
   * @param callback callback for success and error
   */
  public static void loadRewardedVideo(
      Context context, String placementID, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.LOAD_REWARDED_VIDEO);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.LOAD_REWARDED_VIDEO, e);
    }
  }

  /**
   * Sets a callback to be triggered after the interstitial ad is loaded. This MUST be called before
   * showInterstitialAd().
   *
   * @param context the application context
   * @param placementID the placement ID of the ad
   * @param callback callback for success and error
   */
  public static void loadInterstitialAd(
      Context context, String placementID, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.LOAD_INTERSTITIAL_AD);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.LOAD_INTERSTITIAL_AD, e);
    }
  }

  /**
   * Sets a callback to be triggered after the rewarded video is shown. This can only be called
   * after the loadRewardVideo() returns successfully.
   *
   * @param context the application context
   * @param placementID the placement ID of the ad
   * @param callback callback for success and error
   */
  public static void showRewardedVideo(
      Context context, String placementID, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.SHOW_REWARDED_VIDEO);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.SHOW_REWARDED_VIDEO, e);
    }
  }

  /**
   * Sets a callback to be triggered after the interstitial ad is shown. This can only be called
   * after the loadInterstitialAd() returns successfully.
   *
   * @param context the application context
   * @param placementID the placement ID of the ad
   * @param callback callback for success and error
   */
  public static void showInterstitialAd(
      Context context, String placementID, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.SHOW_INTERSTITIAL_AD);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.SHOW_INTERSTITIAL_AD, e);
    }
  }
}
