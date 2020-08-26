// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookException;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudGameLoginHandler {

  private static final int DEFAULT_TIMEOUT_IN_SEC = 5;
  private static boolean IS_RUNNING_IN_CLOUD = false;
  private static SDKLogger mLogger = null;

  /**
   * Fetch Game Login information from Cloud and initalize current AccessToken
   *
   * @param context application context
   * @throws FacebookException
   */
  public static synchronized @Nullable AccessToken init(Context context) throws FacebookException {
    return CloudGameLoginHandler.init(context, DEFAULT_TIMEOUT_IN_SEC);
  }

  /**
   * Fetch Game Login information from Cloud and initalize current AccessToken with timeout
   *
   * @param context application context
   * @param timeoutInSec timeout in second
   * @throws FacebookException
   */
  public static synchronized @Nullable AccessToken init(Context context, int timeoutInSec)
      throws FacebookException {
    int timeout = timeoutInSec <= 0 ? DEFAULT_TIMEOUT_IN_SEC : timeoutInSec;
    boolean isCloudEnvReady = CloudGameLoginHandler.isCloudEnvReady(context, timeout);

    if (!isCloudEnvReady) {
      throw new FacebookException("Not running in Cloud environment.");
    }
    mLogger = SDKLogger.getInstance(context);

    GraphResponse response =
        DaemonRequest.executeAndWait(context, null, SDKMessageEnum.GET_ACCESS_TOKEN, timeout);

    if (response == null || response.getJSONObject() == null) {
      throw new FacebookException("Cannot receive response.");
    }

    if (response.getError() != null) {
      throw new FacebookException(response.getError().getErrorMessage());
    }

    setPackageName(response.getJSONObject(), context);
    try {
      AccessToken token = setCurrentAccessToken(response.getJSONObject());
      Profile.fetchProfileForCurrentAccessToken();
      CloudGameLoginHandler.IS_RUNNING_IN_CLOUD = true;
      mLogger.logLoginSuccess();
      return token;
    } catch (JSONException ex) {
      throw new FacebookException("Cannot properly handle response.", ex);
    }
  }

  /** Return whether this App is running in FB's Cloud Environment or not. */
  public static boolean isRunningInCloud() {
    return CloudGameLoginHandler.IS_RUNNING_IN_CLOUD;
  }

  private static boolean isCloudEnvReady(Context context, int timeoutInSec) {
    GraphResponse response =
        DaemonRequest.executeAndWait(context, null, SDKMessageEnum.IS_ENV_READY, timeoutInSec);
    if (response == null || response.getJSONObject() == null) {
      return false;
    }
    return response.getError() == null;
  }

  private static void setPackageName(JSONObject jsonObject, Context context) {
    String daemonPackageName = jsonObject.optString(SDKConstants.PARAM_DAEMON_PACKAGE_NAME);

    if (daemonPackageName.isEmpty()) {
      throw new FacebookException("Could not establish a secure connection.");
    }

    SharedPreferences.Editor sharedPreferences =
        context
            .getSharedPreferences(SDKConstants.PREF_DAEMON_PACKAGE_NAME, Activity.MODE_PRIVATE)
            .edit();
    sharedPreferences.putString(SDKConstants.PARAM_DAEMON_PACKAGE_NAME, daemonPackageName);
    sharedPreferences.commit();
  }

  private static @Nullable AccessToken setCurrentAccessToken(JSONObject jsonObject)
      throws JSONException {
    String token = jsonObject.optString(SDKConstants.PARAM_ACCESS_TOKEN);
    String accessTokenSource = jsonObject.optString(SDKConstants.PARAM_ACCESS_TOKEN_SOURCE);
    String appID = jsonObject.optString(SDKConstants.PARAM_APP_ID);
    String declinedPermissionsString =
        jsonObject.optString(SDKConstants.PARAM_DECLINED_PERMISSIONS);
    String expiredPermissionsString = jsonObject.optString(SDKConstants.PARAM_EXPIRED_PERMISSIONS);
    String expirationTime = jsonObject.optString(SDKConstants.PARAM_EXPIRATION_TIME);
    String dataAccessExpirationTime =
        jsonObject.optString(SDKConstants.PARAM_DATA_ACCESS_EXPIRATION_TIME);
    String graphDomain = jsonObject.optString(SDKConstants.PARAM_GRAPH_DOMAIN);
    String lastRefreshTime = jsonObject.optString(SDKConstants.PARAM_LAST_REFRESH_TIME);
    String permissionsString = jsonObject.optString(SDKConstants.PARAM_PERMISSIONS);
    String userID = jsonObject.optString(SDKConstants.PARAM_USER_ID);
    String sessionID = jsonObject.optString(SDKConstants.PARAM_SESSION_ID);
    // Skip when any access token info is empty to avoid crash for now
    if (token.isEmpty() || appID.isEmpty() || userID.isEmpty()) {
      return null;
    }
    if (mLogger != null) {
      mLogger.setAppID(appID);
      mLogger.setUserID(userID);
      mLogger.setSessionID(sessionID);
    }
    final List<String> permissions = convertPermissionsStringIntoPermissionsList(permissionsString);
    final List<String> declinedPermissions =
        convertPermissionsStringIntoPermissionsList(declinedPermissionsString);
    final List<String> expiredPermissions =
        convertPermissionsStringIntoPermissionsList(expiredPermissionsString);

    final AccessToken accessToken =
        new AccessToken(
            token,
            appID,
            userID,
            permissions,
            declinedPermissions,
            expiredPermissions,
            !accessTokenSource.isEmpty() ? AccessTokenSource.valueOf(accessTokenSource) : null,
            !expirationTime.isEmpty() ? new Date(Integer.parseInt(expirationTime) * 1000L) : null,
            !lastRefreshTime.isEmpty() ? new Date(Integer.parseInt(lastRefreshTime) * 1000L) : null,
            !dataAccessExpirationTime.isEmpty()
                ? new Date(Integer.parseInt(dataAccessExpirationTime) * 1000L)
                : null,
            !graphDomain.isEmpty() ? graphDomain : null);

    AccessToken.setCurrentAccessToken(accessToken);
    return accessToken;
  }

  private static List<String> convertPermissionsStringIntoPermissionsList(String permissionsString)
      throws JSONException {
    List<String> permissions = new ArrayList<>();
    if (!permissionsString.isEmpty()) {
      JSONArray permissionsJSONArray = new JSONArray(permissionsString);
      for (int i = 0; i < permissionsJSONArray.length(); i++) {
        permissions.add(permissionsJSONArray.get(i).toString());
      }
    }
    return permissions;
  }
}
