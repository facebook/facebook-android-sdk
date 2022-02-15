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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.AccessToken;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;
import com.facebook.internal.FetchedAppSettingsManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

@PrepareForTest({FetchedAppSettingsManager.class, FacebookSdk.class})
public class LoginClientTest extends FacebookPowerMockTestCase {

  private static final String ACCESS_TOKEN = "An access token for user 1";
  private static final String USER_ID = "1001";
  private static final String APP_ID = "2002";
  private static final String AUTH_TYPE = "test auth type";
  private static final String AUTH_ID = UUID.randomUUID().toString();

  private static final HashSet<String> PERMISSIONS =
      new HashSet<String>(Arrays.asList("go outside", "come back in"));
  private static final String ERROR_MESSAGE = "This is bad!";

  private final Executor serialExecutor = new FacebookSerialExecutor();

  @Mock private Fragment mockFragment;

  @Before
  public void before() throws Exception {
    PowerMockito.mockStatic(FacebookSdk.class);
    when(FacebookSdk.isInitialized()).thenReturn(true);
    when(FacebookSdk.getApplicationId()).thenReturn("123456789");
    when(FacebookSdk.getExecutor()).thenReturn(serialExecutor);
    when(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false);
    when(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext());

    FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).create().get();
    when(mockFragment.getActivity()).thenReturn(activity);
    PowerMockito.mockStatic(FetchedAppSettingsManager.class);
  }

  @Test
  public void testReauthorizationWithSameFbidSucceeds() throws Exception {
    LoginClient.Request request = createRequest(ACCESS_TOKEN);

    AccessToken token =
        new AccessToken(
            ACCESS_TOKEN, APP_ID, USER_ID, PERMISSIONS, null, null, null, null, null, null);
    LoginClient.Result result = LoginClient.Result.createTokenResult(request, token);

    LoginClient.OnCompletedListener listener = mock(LoginClient.OnCompletedListener.class);

    LoginClient client = new LoginClient(mockFragment);
    client.setOnCompletedListener(listener);

    client.completeAndValidate(result);

    ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
        ArgumentCaptor.forClass(LoginClient.Result.class);

    verify(listener).onCompleted(resultArgumentCaptor.capture());

    result = resultArgumentCaptor.getValue();

    assertNotNull(result);
    assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

    AccessToken resultToken = result.token;
    assertNotNull(resultToken);
    assertEquals(ACCESS_TOKEN, resultToken.getToken());

    // We don't care about ordering.
    assertEquals(PERMISSIONS, resultToken.getPermissions());
  }

  @Test
  public void testRequestParceling() {
    LoginClient.Request request = createRequest(ACCESS_TOKEN);

    LoginClient.Request unparceledRequest = TestUtils.parcelAndUnparcel(request);

    assertEquals(LoginBehavior.NATIVE_WITH_FALLBACK, unparceledRequest.getLoginBehavior());
    assertEquals(LoginTargetApp.FACEBOOK, unparceledRequest.getLoginTargetApp());
    assertEquals(new HashSet<String>(PERMISSIONS), unparceledRequest.getPermissions());
    assertEquals(DefaultAudience.FRIENDS, unparceledRequest.getDefaultAudience());
    assertEquals("1234", unparceledRequest.getApplicationId());
    assertEquals("5678", unparceledRequest.getAuthId());
    assertFalse(unparceledRequest.isRerequest());
    assertThat(unparceledRequest.isFamilyLogin()).isFalse();
    assertThat(unparceledRequest.shouldSkipAccountDeduplication()).isFalse();
  }

  @Test
  public void testResultParceling() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.WEB_ONLY,
            null,
            DefaultAudience.EVERYONE,
            AUTH_TYPE,
            FacebookSdk.getApplicationId(),
            AUTH_ID,
            LoginTargetApp.FACEBOOK);
    request.setRerequest(true);
    request.setResetMessengerState(false);
    request.setMessengerPageId("1928");
    request.setFamilyLogin(true);
    request.setShouldSkipAccountDeduplication(true);
    AccessToken token1 =
        new AccessToken(
            "Token2",
            FacebookSdk.getApplicationId(),
            "1000",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    LoginClient.Result result =
        new LoginClient.Result(request, LoginClient.Result.Code.SUCCESS, token1, "error 1", "123");

    LoginClient.Result unparceledResult = TestUtils.parcelAndUnparcel(result);
    LoginClient.Request unparceledRequest = unparceledResult.request;

    assertEquals(LoginBehavior.WEB_ONLY, unparceledRequest.getLoginBehavior());
    assertEquals(LoginTargetApp.FACEBOOK, unparceledRequest.getLoginTargetApp());
    assertEquals(new HashSet<String>(), unparceledRequest.getPermissions());
    assertEquals(DefaultAudience.EVERYONE, unparceledRequest.getDefaultAudience());
    assertEquals(FacebookSdk.getApplicationId(), unparceledRequest.getApplicationId());
    assertEquals(AUTH_ID, unparceledRequest.getAuthId());
    assertTrue(unparceledRequest.isRerequest());
    assertThat(unparceledRequest.isFamilyLogin()).isTrue();
    assertThat(unparceledRequest.shouldSkipAccountDeduplication()).isTrue();

    assertEquals(LoginClient.Result.Code.SUCCESS, unparceledResult.code);
    assertEquals(token1, unparceledResult.token);
    assertEquals("error 1", unparceledResult.errorMessage);
    assertEquals("123", unparceledResult.errorCode);
    assertEquals("1928", unparceledRequest.getMessengerPageId());
    assertEquals(false, unparceledRequest.getResetMessengerState());
  }

  @Test
  public void testGetHandlersForFBSSOLogin() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            null,
            DefaultAudience.EVERYONE,
            "test auth",
            FacebookSdk.getApplicationId(),
            "test auth id",
            LoginTargetApp.FACEBOOK);
    LoginClient client = new LoginClient(mockFragment);
    LoginMethodHandler[] handlers = client.getHandlersToTry(request);

    assertThat(handlers.length).isEqualTo(4);
    assertThat(handlers[0]).isInstanceOf(GetTokenLoginMethodHandler.class);
    assertThat(handlers[1]).isInstanceOf(KatanaProxyLoginMethodHandler.class);
    assertThat(handlers[2]).isInstanceOf(CustomTabLoginMethodHandler.class);
    assertThat(handlers[3]).isInstanceOf(WebViewLoginMethodHandler.class);
  }

  @Test
  public void testGetHandlersForFBWebLoginOnly() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.WEB_ONLY,
            null,
            DefaultAudience.EVERYONE,
            AUTH_TYPE,
            FacebookSdk.getApplicationId(),
            AUTH_ID,
            LoginTargetApp.FACEBOOK);
    LoginClient client = new LoginClient(mockFragment);
    LoginMethodHandler[] handlers = client.getHandlersToTry(request);

    assertThat(handlers.length).isEqualTo(2);
    assertThat(handlers[0]).isInstanceOf(CustomTabLoginMethodHandler.class);
    assertThat(handlers[1]).isInstanceOf(WebViewLoginMethodHandler.class);
  }

  @Test
  public void testGetHandlersForIGSSOLogin() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            null,
            DefaultAudience.EVERYONE,
            "test auth",
            FacebookSdk.getApplicationId(),
            "test auth id",
            LoginTargetApp.INSTAGRAM);
    LoginClient client = new LoginClient(mockFragment);
    LoginMethodHandler[] handlers = client.getHandlersToTry(request);

    assertThat(handlers.length).isEqualTo(3);
    assertThat(handlers[0]).isInstanceOf(InstagramAppLoginMethodHandler.class);
    assertThat(handlers[1]).isInstanceOf(CustomTabLoginMethodHandler.class);
    assertThat(handlers[2]).isInstanceOf(WebViewLoginMethodHandler.class);
  }

  @Test
  public void testGetHandlersForIGWebLoginOnly() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.WEB_ONLY,
            null,
            DefaultAudience.EVERYONE,
            AUTH_TYPE,
            FacebookSdk.getApplicationId(),
            AUTH_ID,
            LoginTargetApp.INSTAGRAM);
    LoginClient client = new LoginClient(mockFragment);
    LoginMethodHandler[] handlers = client.getHandlersToTry(request);

    assertThat(handlers.length).isEqualTo(2);
    assertThat(handlers[0]).isInstanceOf(CustomTabLoginMethodHandler.class);
    assertThat(handlers[1]).isInstanceOf(WebViewLoginMethodHandler.class);
  }

  protected LoginClient.Request createRequest(String previousAccessTokenString) {
    return new LoginClient.Request(
        LoginBehavior.NATIVE_WITH_FALLBACK,
        new HashSet<String>(PERMISSIONS),
        DefaultAudience.FRIENDS,
        "rerequest",
        "1234",
        "5678",
        LoginTargetApp.FACEBOOK);
  }
}
