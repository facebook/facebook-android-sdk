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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Intent;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.AuthenticationToken;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.security.OidcSecurityUtil;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.powermock.*"})
@PrepareForTest({LoginClient.class, FacebookSdk.class, OidcSecurityUtil.class})
public class GetTokenLoginMethodHandlerTest extends LoginHandlerTestCase {

  String ID_TOKEN_STRING = AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest();
  String NONCE = AuthenticationTokenTestUtil.NONCE;

  @Before
  @Override
  public void before() throws Exception {
    super.before();
    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.getApplicationId())
        .thenReturn(AuthenticationTokenTestUtil.APP_ID);
    PowerMockito.when(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false);

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil.class);
    when(OidcSecurityUtil.getRawKeyFromEndPoint(any(String.class))).thenReturn("key");
    when(OidcSecurityUtil.getPublicKeyFromString(any(String.class)))
        .thenReturn(PowerMockito.mock(PublicKey.class));
    when(OidcSecurityUtil.verify(any(PublicKey.class), any(String.class), any(String.class)))
        .thenReturn(true);
  }

  @Test
  public void testGetTokenHandlesSuccessWithAllPermissions() {
    Bundle bundle = new Bundle();
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, new ArrayList<String>(PERMISSIONS));
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH,
        new Date().getTime() / 1000 + EXPIRES_IN_DELTA);
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN);
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID);
    bundle.putString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING);

    GetTokenLoginMethodHandler handler = new GetTokenLoginMethodHandler(mockLoginClient);

    LoginClient.Request request = createRequestWithNonce();
    handler.getTokenCompleted(request, bundle);

    ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    LoginClient.Result result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    AccessToken token = result.token;
    assertNotNull(token);
    assertEquals(ACCESS_TOKEN, token.getToken());
    assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
    TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());
  }

  @Test
  public void testGetTokenHandlesSuccessWithOnlySomePermissions() {
    Bundle bundle = new Bundle();
    bundle.putStringArrayList(
        NativeProtocol.EXTRA_PERMISSIONS, new ArrayList<String>(Arrays.asList("go outside")));
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH,
        new Date().getTime() / 1000 + EXPIRES_IN_DELTA);
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN);

    GetTokenLoginMethodHandler handler = new GetTokenLoginMethodHandler(mockLoginClient);

    LoginClient.Request request = createRequest();
    assertEquals(PERMISSIONS.size(), request.getPermissions().size());

    handler.getTokenCompleted(request, bundle);

    verify(mockLoginClient, never()).completeAndValidate(any(LoginClient.Result.class));
    verify(mockLoginClient, times(1)).tryNextHandler();
  }

  @Test
  public void testGetTokenHandlesNoResult() {
    GetTokenLoginMethodHandler handler = new GetTokenLoginMethodHandler(mockLoginClient);

    LoginClient.Request request = createRequest();
    assertEquals(PERMISSIONS.size(), request.getPermissions().size());

    handler.getTokenCompleted(request, null);

    verify(mockLoginClient, never()).completeAndValidate(any(LoginClient.Result.class));
    verify(mockLoginClient, times(1)).tryNextHandler();
  }

  @Test
  public void testFromNativeLogin() {
    ArrayList<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
    String token = "AnImaginaryTokenValue";
    String userId = "1000";

    long nowSeconds = new Date().getTime() / 1000;
    Intent intent = new Intent();
    intent.putExtra(NativeProtocol.EXTRA_ACCESS_TOKEN, token);
    intent.putExtra(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING);
    intent.putExtra(NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, nowSeconds + 60L);
    intent.putExtra(NativeProtocol.EXTRA_PERMISSIONS, permissions);
    intent.putExtra(NativeProtocol.EXTRA_USER_ID, userId);

    AccessToken accessToken =
        GetTokenLoginMethodHandler.createAccessTokenFromNativeLogin(
            intent.getExtras(), AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, "1234");
    TestUtils.assertSamePermissions(permissions, accessToken);
    assertEquals(token, accessToken.getToken());
    assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, accessToken.getSource());
    assertTrue(!accessToken.isExpired());

    AuthenticationToken authenticationToken =
        GetTokenLoginMethodHandler.createAuthenticationTokenFromNativeLogin(
            intent.getExtras(), NONCE);
    assertNotNull(authenticationToken);
    assertEquals(ID_TOKEN_STRING, authenticationToken.getToken());
  }

  /**
   * This can happens when there is compatibility issue, thus no authentication string returned
   * Login still works, but just no id_token is created or returned
   */
  @Test
  public void testAllPermissionWithNoAuthenticationStringStillSuccess() {
    Bundle bundle = new Bundle();
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, new ArrayList<String>(PERMISSIONS));
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH,
        new Date().getTime() / 1000 + EXPIRES_IN_DELTA);
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN);
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID);

    GetTokenLoginMethodHandler handler = new GetTokenLoginMethodHandler(mockLoginClient);

    LoginClient.Request request = createRequestWithNonce();
    handler.getTokenCompleted(request, bundle);

    ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    LoginClient.Result result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    AccessToken token = result.token;
    assertNotNull(token);
    assertEquals(ACCESS_TOKEN, token.getToken());
    assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
    TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());

    // when no id_token string is returned from fb4a, we expect no authentication Token is created
    // but Get Token should still works even id_token is null
    AuthenticationToken authenticationToken = result.authenticationToken;
    assertEquals(authenticationToken, null);
  }

  /** Case when id_token returned, but it is invalid */
  @Test
  public void testAllPermissionWithAuthenticationValidationError() {
    // force token signature validation to fail
    when(OidcSecurityUtil.verify(any(PublicKey.class), any(String.class), any(String.class)))
        .thenReturn(false);

    Bundle bundle = new Bundle();
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, new ArrayList<String>(PERMISSIONS));
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH,
        new Date().getTime() / 1000 + EXPIRES_IN_DELTA);
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN);
    bundle.putString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING);
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID);

    GetTokenLoginMethodHandler handler = new GetTokenLoginMethodHandler(mockLoginClient);

    LoginClient.Request request = createRequestWithNonce();
    handler.getTokenCompleted(request, bundle);

    ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    LoginClient.Result result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.ERROR, result.code);
  }
}
