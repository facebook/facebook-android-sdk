// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.gamingservices.cloudgaming;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppAdLibrary {
  // Valid parameter keys
  @Deprecated public static final String PLACEMENT_ID = SDKConstants.PARAM_PLACEMENT_ID;

  /**
   * Sets a callback to be triggered after the rewarded video is loaded. This MUST be called before
   * showRewardedVideo().
   *
   * @param context the application context
   * @param placementID the placement ID of the ad
   * @param callback callback for success and error
   */
  public static void loadRewardedVideo(
      Context context, String placementID, DaemonRequest.Callback callback) throws JSONException {
    JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.LOAD_REWARDED_VIDEO);
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
      Context context, String placementID, DaemonRequest.Callback callback) throws JSONException {
    JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.LOAD_INTERSTITIAL_AD);
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
      Context context, String placementID, DaemonRequest.Callback callback) throws JSONException {
    JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.SHOW_REWARDED_VIDEO);
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
      Context context, String placementID, DaemonRequest.Callback callback) throws JSONException {
    JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PLACEMENT_ID, placementID);
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.SHOW_INTERSTITIAL_AD);
  }

  /**
   * Sets a callback to be triggered after the rewarded video is loaded. This MUST be called before
   * showRewardedVideo().
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PLACEMENT_ID: the placement ID of the ad }
   * @param callback callback for success and error
   */
  public static void loadRewardedVideo(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.LOAD_REWARDED_VIDEO);
  }

  /**
   * Sets a callback to be triggered after the interstitial ad is loaded. This MUST be called before
   * showInterstitialAd().
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PLACEMENT_ID: the placement ID of the ad }
   * @param callback callback for success and error
   */
  public static void loadInterstitialAd(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.LOAD_INTERSTITIAL_AD);
  }

  /**
   * Sets a callback to be triggered after the rewarded video is shown. This can only be called
   * after the loadRewardVideo() returns successfully.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PLACEMENT_ID: the placement ID of the ad }
   * @param callback callback for success and error
   */
  public static void showRewardedVideo(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.SHOW_REWARDED_VIDEO);
  }

  /**
   * Sets a callback to be triggered after the interstitial ad is shown. This can only be called
   * after the loadInterstitialAd() returns successfully.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PLACEMENT_ID: the placement ID of the ad }
   * @param callback callback for success and error
   */
  public static void showInterstitialAd(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.SHOW_INTERSTITIAL_AD);
  }
}
