// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and API s provided by Facebook.
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
