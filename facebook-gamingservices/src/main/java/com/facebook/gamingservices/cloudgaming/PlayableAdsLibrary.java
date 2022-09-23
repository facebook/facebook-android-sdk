/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.cloudgaming;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONObject;

public class PlayableAdsLibrary {

  /**
   * Opens the app store for the current playable ad.
   *
   * @param context the application context
   * @param parameters {}
   * @param callback callback for success and error
   */
  public static void openAppStore(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.OPEN_APP_STORE);
  }

  /**
   * Signal that the current playable ad has finished in-game loading.
   *
   * @param context the application context
   * @param parameters {}
   * @param callback callback for success and error
   */
  public static void markGameLoaded(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.MARK_GAME_LOADED);
  }
}
