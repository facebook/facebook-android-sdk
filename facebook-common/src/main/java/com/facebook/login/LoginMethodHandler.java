/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.Nullable;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.AuthenticationToken;
import com.facebook.FacebookException;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
abstract class LoginMethodHandler implements Parcelable {
  Map<String, String> methodLoggingExtras;
  protected LoginClient loginClient;

  LoginMethodHandler(LoginClient loginClient) {
    this.loginClient = loginClient;
  }

  LoginMethodHandler(Parcel source) {
    methodLoggingExtras = Utility.readStringMapFromParcel(source);
  }

  // This should only be used if restoring from a Parcel
  void setLoginClient(LoginClient loginClient) {
    if (this.loginClient != null) {
      throw new FacebookException("Can't set LoginClient if it is already set.");
    }
    this.loginClient = loginClient;
  }

  abstract int tryAuthorize(LoginClient.Request request);

  abstract String getNameForLogging();

  boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    return false;
  }

  boolean needsInternetPermission() {
    return false;
  }

  void cancel() {}

  void putChallengeParam(JSONObject param) throws JSONException {}

  protected String getClientState(String authId) {
    JSONObject param = new JSONObject();
    try {
      param.put(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID, authId);
      param.put(LoginLogger.EVENT_PARAM_METHOD, getNameForLogging());
      putChallengeParam(param);
    } catch (JSONException e) {
      Log.w("LoginMethodHandler", "Error creating client state json: " + e.getMessage());
    }
    return param.toString();
  }

  protected void addLoggingExtra(String key, Object value) {
    if (methodLoggingExtras == null) {
      methodLoggingExtras = new HashMap<String, String>();
    }
    methodLoggingExtras.put(key, value == null ? null : value.toString());
  }

  protected void logWebLoginCompleted(String e2e) {
    String applicationId = loginClient.getPendingRequest().getApplicationId();
    InternalAppEventsLogger logger =
        new InternalAppEventsLogger(loginClient.getActivity(), applicationId);

    Bundle parameters = new Bundle();
    parameters.putString(AnalyticsEvents.PARAMETER_WEB_LOGIN_E2E, e2e);
    parameters.putLong(
        AnalyticsEvents.PARAMETER_WEB_LOGIN_SWITCHBACK_TIME, System.currentTimeMillis());
    parameters.putString(AnalyticsEvents.PARAMETER_APP_ID, applicationId);

    logger.logEventImplicitly(AnalyticsEvents.EVENT_WEB_LOGIN_COMPLETE, null, parameters);
  }

  static AccessToken createAccessTokenFromNativeLogin(
      Bundle bundle, AccessTokenSource source, String applicationId) {
    Date expires =
        Utility.getBundleLongAsDate(
            bundle, NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, new Date(0));
    ArrayList<String> permissions = bundle.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS);
    String token = bundle.getString(NativeProtocol.EXTRA_ACCESS_TOKEN);
    Date dataAccessExpirationTime =
        Utility.getBundleLongAsDate(
            bundle, NativeProtocol.EXTRA_DATA_ACCESS_EXPIRATION_TIME, new Date(0));

    if (Utility.isNullOrEmpty(token)) {
      return null;
    }

    String userId = bundle.getString(NativeProtocol.EXTRA_USER_ID);
    String graphDomain = bundle.getString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN);
    return new AccessToken(
        token,
        applicationId,
        userId,
        permissions,
        null,
        null,
        source,
        expires,
        new Date(),
        dataAccessExpirationTime,
        graphDomain);
  }

  /**
   * WARNING: This feature is currently in development and not intended for external usage.
   *
   * @param bundle
   * @param expectedNonce the nonce expected to have created with the AuthenticationToken
   * @return AuthenticationToken
   * @throws FacebookException
   */
  @Nullable
  public static AuthenticationToken createAuthenticationTokenFromWebBundle(
      Bundle bundle, String expectedNonce) throws FacebookException {
    String authenticationTokenString =
        bundle.getString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY);

    if (Utility.isNullOrEmpty(authenticationTokenString)) {
      return null;
    }

    try {
      /**
       * TODO T96881697: create factory class for authentication token, remove this try-catch and
       * replace AuthenticationToken with factory method
       */
      return new AuthenticationToken(authenticationTokenString, expectedNonce);
    } catch (Exception ex) {
      // will remove this after factory class created
      return null;
    }
  }

  public static AccessToken createAccessTokenFromWebBundle(
      Collection<String> requestedPermissions,
      Bundle bundle,
      AccessTokenSource source,
      String applicationId)
      throws FacebookException {
    Date expires = Utility.getBundleLongAsDate(bundle, AccessToken.EXPIRES_IN_KEY, new Date());
    String token = bundle.getString(AccessToken.ACCESS_TOKEN_KEY);

    Date dataAccessExpirationTime =
        Utility.getBundleLongAsDate(bundle, AccessToken.DATA_ACCESS_EXPIRATION_TIME, new Date(0));

    // With Login v4, we now get back the actual permissions granted, so update the permissions
    // to be the real thing
    String grantedPermissions = bundle.getString("granted_scopes");
    if (!Utility.isNullOrEmpty(grantedPermissions)) {
      requestedPermissions = new ArrayList<String>(Arrays.asList(grantedPermissions.split(",")));
    }
    String deniedPermissions = bundle.getString("denied_scopes");
    List<String> declinedPermissions = null;
    if (!Utility.isNullOrEmpty(deniedPermissions)) {
      declinedPermissions = new ArrayList<String>(Arrays.asList(deniedPermissions.split(",")));
    }
    String expiredScopes = bundle.getString("expired_scopes");
    List<String> expiredPermissions = null;
    if (!Utility.isNullOrEmpty(expiredScopes)) {
      expiredPermissions = new ArrayList<String>(Arrays.asList(expiredScopes.split(",")));
    }

    if (Utility.isNullOrEmpty(token)) {
      return null;
    }

    String graphDomain = bundle.getString(AccessToken.GRAPH_DOMAIN);
    String signed_request = bundle.getString("signed_request");
    String userId = getUserIDFromSignedRequest(signed_request);

    return new AccessToken(
        token,
        applicationId,
        userId,
        requestedPermissions,
        declinedPermissions,
        expiredPermissions,
        source,
        expires,
        new Date(),
        dataAccessExpirationTime,
        graphDomain);
  }

  static String getUserIDFromSignedRequest(String signedRequest) throws FacebookException {
    if (signedRequest == null || signedRequest.isEmpty()) {
      throw new FacebookException("Authorization response does not contain the signed_request");
    }

    try {
      String[] signatureAndPayload = signedRequest.split("\\.");
      if (signatureAndPayload.length == 2) {
        byte[] data = Base64.decode(signatureAndPayload[1], Base64.DEFAULT);
        String dataStr = new String(data, "UTF-8");
        JSONObject jsonObject = new JSONObject(dataStr);
        return jsonObject.getString("user_id");
      }
    } catch (UnsupportedEncodingException ex) {
    } catch (JSONException ex) {
    }
    throw new FacebookException("Failed to retrieve user_id from signed_request");
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    Utility.writeStringMapToParcel(dest, methodLoggingExtras);
  }

  public boolean shouldKeepTrackOfMultipleIntents() {
    return false;
  }
}
