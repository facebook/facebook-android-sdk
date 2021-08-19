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
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONException;
import org.json.JSONObject;

public class GameFeaturesLibrary {

  public static void getPayload(
      Context context, JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.GET_PAYLOAD);
  }

  public static void canCreateShortcut(
      Context context, JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.CAN_CREATE_SHORTCUT);
  }

  public static void createShortcut(
      Context context, JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.CREATE_SHORTCUT);
  }

  public static void postSessionScore(Context context, int score, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_SCORE, score);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.POST_SESSION_SCORE);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.POST_SESSION_SCORE, e);
    }
  }

  public static void postSessionScoreAsync(
      Context context, int score, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_SCORE, score);
      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.POST_SESSION_SCORE_ASYNC);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.POST_SESSION_SCORE_ASYNC, e);
    }
  }

  public static void getTournamentAsync(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.GET_TOURNAMENT_ASYNC);
  }

  public static void createTournamentAsync(
      Context context,
      int score,
      @Nullable String title,
      @Nullable String image,
      @Nullable String sortOrder,
      @Nullable String scoreFormat,
      @Nullable Integer endTime,
      @Nullable JSONObject payload,
      DaemonRequest.Callback callback) {
    try {
      JSONObject parameters =
          (new JSONObject())
              .put(SDKConstants.PARAM_INITIAL_SCORE, score)
              .put(SDKConstants.PARAM_TITLE, title)
              .put(SDKConstants.PARAM_IMAGE, image)
              .put(SDKConstants.PARAM_SORT_ORDER, sortOrder)
              .put(SDKConstants.PARAM_SCORE_FORMAT, scoreFormat)
              .put(SDKConstants.PARAM_END_TIME, endTime)
              .put(SDKConstants.PARAM_DATA, payload);

      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.TOURNAMENT_CREATE_ASYNC);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.TOURNAMENT_CREATE_ASYNC, e);
    }
  }

  public static void shareTournamentAsync(
      Context context,
      @Nullable Integer score,
      @Nullable JSONObject payload,
      DaemonRequest.Callback callback) {
    try {
      JSONObject parameters =
          (new JSONObject())
              .put(SDKConstants.PARAM_SCORE, score)
              .put(SDKConstants.PARAM_DATA, payload);
      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.TOURNAMENT_SHARE_ASYNC);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.TOURNAMENT_SHARE_ASYNC, e);
    }
  }

  public static void postTournamentScoreAsync(
      Context context, int score, DaemonRequest.Callback callback) throws JSONException {
    JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_SCORE, score);
    DaemonRequest.executeAsync(
        context, parameters, callback, SDKMessageEnum.TOURNAMENT_POST_SCORE_ASYNC);
  }

  public static void performHapticFeedback(Context context) {
    DaemonRequest.Callback callback =
        new DaemonRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {}
        };

    DaemonRequest.executeAsync(
        context, null, callback, SDKMessageEnum.PERFORM_HAPTIC_FEEDBACK_ASYNC);
  }
}
