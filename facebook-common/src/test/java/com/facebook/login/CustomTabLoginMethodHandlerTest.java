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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.AuthenticationToken;
import com.facebook.FacebookActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.internal.security.OidcSecurityUtil;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.powermock.*"})
@PrepareForTest({
  LoginClient.class,
  Validate.class,
  Utility.class,
  FacebookSdk.class,
  AccessToken.class,
  FetchedAppSettings.class,
  FetchedAppSettingsManager.class,
  OidcSecurityUtil.class
})
public class CustomTabLoginMethodHandlerTest extends LoginHandlerTestCase {
  private static final String SIGNED_REQUEST_STR =
      "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ"
          + "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0";
  private static final String CHROME_PACKAGE = "com.android.chrome";
  private static final String DEV_PACKAGE = "com.chrome.dev";
  private static final String BETA_PACKAGE = "com.chrome.beta";

  private LoginClient.Request request;

  @Before
  public void setUp() {
    mockTryAuthorize();
    request = createRequest();
    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.getApplicationId())
        .thenReturn(AuthenticationTokenTestUtil.APP_ID);

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil.class);
    when(OidcSecurityUtil.getRawKeyFromEndPoint(any(String.class))).thenReturn("key");
    when(OidcSecurityUtil.getPublicKeyFromString(any(String.class)))
        .thenReturn(PowerMockito.mock(PublicKey.class));
    when(OidcSecurityUtil.verify(any(PublicKey.class), any(String.class), any(String.class)))
        .thenReturn(true);
  }

  @Test
  public void testCustomTabHandlesSuccess() {
    testCustomTabHandles(getEncodedAuthTokenString());
  }

  @Test
  public void testCustomTabHandlesSuccessWithEmptyAuthenticationToken() {
    LoginClient.Result result = testCustomTabHandles("");

    AuthenticationToken authenticationToken = result.authenticationToken;
    assertEquals(null, authenticationToken);
  }

  @Test
  public void testCustomTabHandlesSuccessWithNoAuthenticationToken() {
    LoginClient.Result result = testCustomTabHandles(null);

    AuthenticationToken authenticationToken = result.authenticationToken;
    assertEquals(null, authenticationToken);
  }

  @Test
  public void testIdTokenWithNonceCustomTabHandlesSuccess() {
    mockCustomTabRedirectActivity(true);
    LoginClient.Request requestWithNonce = createRequestWithNonce();
    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    String expectedIdTokenString = AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest();
    final Bundle bundle = new Bundle();
    bundle.putString("access_token", ACCESS_TOKEN);
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, expectedIdTokenString);
    bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA));
    bundle.putString("signed_request", SIGNED_REQUEST_STR);
    handler.onComplete(requestWithNonce, bundle, null);

    final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    final LoginClient.Result result = resultArgumentCaptor.getValue();
    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    final AuthenticationToken idToken = result.authenticationToken;
    assertNotNull(idToken);
    assertEquals(expectedIdTokenString, idToken.getToken());

    final AccessToken token = result.token;
    assertNotNull(token);
    assertEquals(ACCESS_TOKEN, token.getToken());
    assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
    TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());
  }

  @Test
  public void testIGCustomTabHandlesSuccess() {
    mockCustomTabRedirectActivity(true);
    LoginClient.Request igRequest = createIGWebRequest();
    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    final Bundle bundle = new Bundle();
    bundle.putString("access_token", ACCESS_TOKEN);
    bundle.putString("graph_domain", "instagram");
    bundle.putString("signed_request", SIGNED_REQUEST_STR);
    handler.onComplete(igRequest, bundle, null);

    final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    final LoginClient.Result result = resultArgumentCaptor.getValue();
    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    final AccessToken token = result.token;
    assertNotNull(token);
    assertEquals(ACCESS_TOKEN, token.getToken());
    assertEquals(USER_ID, token.getUserId());
    assertEquals("instagram", token.getGraphDomain());
    assertEquals(AccessTokenSource.INSTAGRAM_CUSTOM_CHROME_TAB, token.getSource());
    TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());
  }

  @Test
  public void testCustomTabHandlesCancel() {
    mockCustomTabRedirectActivity(true);
    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    handler.onComplete(request, null, new FacebookOperationCanceledException());

    final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());
    final LoginClient.Result result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.CANCEL, result.code);
    assertNull(result.token);
    assertNotNull(result.errorMessage);
  }

  @Test
  public void testCustomTabHandlesError() {
    mockCustomTabRedirectActivity(true);
    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    handler.onComplete(request, null, new FacebookException(ERROR_MESSAGE));

    final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());
    final LoginClient.Result result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.ERROR, result.code);
    assertNull(result.token);
    assertNotNull(result.errorMessage);
    assertEquals(ERROR_MESSAGE, result.errorMessage);
  }

  @Test
  public void testTryAuthorizeNeedsRedirectActivity() {
    mockChromeCustomTabsSupported(true, CHROME_PACKAGE);
    mockCustomTabRedirectActivity(true);

    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    assertEquals(handler.tryAuthorize(request), 1);
  }

  @Test
  public void testTryAuthorizeWithChromePackage() {
    mockCustomTabRedirectActivity(true);
    mockChromeCustomTabsSupported(true, CHROME_PACKAGE);

    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);
    assertEquals(handler.tryAuthorize(request), 1);
  }

  @Test
  public void testTryAuthorizeWithChromeBetaPackage() {
    mockCustomTabRedirectActivity(true);
    mockChromeCustomTabsSupported(true, BETA_PACKAGE);

    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);
    assertEquals(handler.tryAuthorize(request), 1);
  }

  @Test
  public void testTryAuthorizeWithChromeDevPackage() {
    mockCustomTabRedirectActivity(true);
    mockChromeCustomTabsSupported(true, DEV_PACKAGE);

    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    assertEquals(handler.tryAuthorize(request), 1);
  }

  private void mockTryAuthorize() {
    mockStatic(FacebookSdk.class);
    when(FacebookSdk.isInitialized()).thenReturn(true);
    mockStatic(AccessToken.class);
    when(AccessToken.getCurrentAccessToken()).thenReturn(null);
    Fragment fragment = mock(LoginFragment.class);
    when(mockLoginClient.getFragment()).thenReturn(fragment);
  }

  private void mockChromeCustomTabsSupported(final boolean supported, final String packageName) {
    final List<ResolveInfo> resolveInfos = new ArrayList<>();
    ResolveInfo resolveInfo = new ResolveInfo();
    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.packageName = packageName;
    resolveInfo.serviceInfo = serviceInfo;
    if (supported) {
      resolveInfos.add(resolveInfo);
    }
    final PackageManager packageManager = mock(PackageManager.class);
    when(packageManager.queryIntentServices(any(Intent.class), anyInt())).thenReturn(resolveInfos);

    activity = mock(FacebookActivity.class);
    when(mockLoginClient.getActivity()).thenReturn(activity);
    when(activity.getPackageManager()).thenReturn(packageManager);

    mockStatic(FacebookSdk.class);
    when(FacebookSdk.getApplicationContext()).thenReturn(activity);
  }

  private void mockCustomTabRedirectActivity(final boolean hasActivity) {
    mockStatic(Validate.class);
    when(Validate.hasCustomTabRedirectActivity(nullable(Context.class), nullable(String.class)))
        .thenReturn(hasActivity);
  }

  private LoginClient.Result testCustomTabHandles(String authenticationTokenString) {
    mockCustomTabRedirectActivity(true);
    CustomTabLoginMethodHandler handler = new CustomTabLoginMethodHandler(mockLoginClient);

    final Bundle bundle = new Bundle();
    bundle.putString("access_token", ACCESS_TOKEN);
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, authenticationTokenString);
    bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA));
    bundle.putString("code", "Something else");
    bundle.putString("signed_request", SIGNED_REQUEST_STR);
    handler.onComplete(request, bundle, null);

    final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

    final LoginClient.Result result = resultArgumentCaptor.getValue();
    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    final AccessToken token = result.token;
    assertNotNull(token);
    assertEquals(ACCESS_TOKEN, token.getToken());
    assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
    TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());

    return result;
  }
}
