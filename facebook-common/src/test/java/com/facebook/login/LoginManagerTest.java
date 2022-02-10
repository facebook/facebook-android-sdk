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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.AuthenticationToken;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import com.facebook.Profile;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.security.OidcSecurityUtil;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({FacebookSdk.class, OidcSecurityUtil.class})
public class LoginManagerTest extends FacebookPowerMockTestCase {

  private static final String MOCK_APP_ID = AuthenticationTokenTestUtil.APP_ID;
  private static final String USER_ID = "1000";
  private final String TOKEN_STRING = "A token of my esteem";
  private final List<String> PERMISSIONS = Arrays.asList("walk", "chew gum");
  private final Date EXPIRES = new Date(2025, 5, 3);
  private final Date LAST_REFRESH = new Date(2023, 8, 15);
  private final Date DATA_ACCESS_EXPIRATION_TIME = new Date(2025, 5, 3);
  private final String codeVerifier = "codeVerifier";
  private final String codeChallenge = "codeChallenge";
  private final CodeChallengeMethod codeChallengeMethod = CodeChallengeMethod.S256;

  @Mock public Activity mockActivity;
  @Mock public Fragment mockFragment;
  @Mock public Context mockApplicationContext;
  @Mock public PackageManager mockPackageManager;
  @Mock public FacebookCallback<LoginResult> mockCallback;
  @Mock public ThreadPoolExecutor threadExecutor;
  @Mock public FragmentActivity mockFragmentActivity;
  @Mock public SharedPreferences mockSharedPreferences;
  @Mock public SharedPreferences.Editor mockEditor;
  @Mock public Looper mockLooper;
  @Mock public AccessToken.Companion mockAccessTokenCompanion;
  @Mock public AuthenticationToken.Companion mockAuthenticationTokenCompanion;
  @Mock public Profile.Companion mockProfileCompanion;

  @Before
  public void before() throws Exception {
    mockStatic(FacebookSdk.class);

    when(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(null);
    Whitebox.setInternalState(AccessToken.class, "Companion", mockAccessTokenCompanion);

    when(mockAuthenticationTokenCompanion.getCurrentAuthenticationToken()).thenReturn(null);
    Whitebox.setInternalState(
        AuthenticationToken.class, "Companion", mockAuthenticationTokenCompanion);

    Whitebox.setInternalState(Profile.class, "Companion", mockProfileCompanion);

    when(FacebookSdk.isInitialized()).thenReturn(true);
    when(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID);
    when(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext);
    when(FacebookSdk.getExecutor()).thenReturn(threadExecutor);
    when(mockFragment.getActivity()).thenReturn(mockFragmentActivity);
    when(mockActivity.getApplicationContext()).thenReturn(mockApplicationContext);
    when(FacebookSdk.getApplicationContext().getSharedPreferences(anyString(), anyInt()))
        .thenReturn(mockSharedPreferences);
    when(mockSharedPreferences.edit()).thenReturn(mockEditor);
    when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);

    // We use mocks rather than RobolectricPackageManager because it's simpler to not
    // have to specify Intents. Default to resolving all intents to something.
    ResolveInfo resolveInfo = new ResolveInfo();
    when(mockApplicationContext.getPackageManager()).thenReturn(mockPackageManager);
    when(mockApplicationContext.getMainLooper()).thenReturn(mockLooper);
    when(mockApplicationContext.getApplicationContext()).thenReturn(mockApplicationContext);
    when(mockPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(resolveInfo);

    // mock and bypass signature verification
    mockStatic(OidcSecurityUtil.class);
    when(OidcSecurityUtil.getRawKeyFromEndPoint(any(String.class))).thenReturn("key");
    when(OidcSecurityUtil.getPublicKeyFromString(any(String.class)))
        .thenReturn(PowerMockito.mock(PublicKey.class));
    when(OidcSecurityUtil.verify(any(PublicKey.class), any(String.class), any(String.class)))
        .thenReturn(true);
  }

  @Test
  public void testRequiresSdkToBeInitialized() {
    try {
      when(FacebookSdk.isInitialized()).thenReturn(false);

      LoginManager loginManager = new LoginManager();

      fail();
    } catch (FacebookSdkNotInitializedException exception) {
    }
  }

  @Test
  public void testGetInstance() {
    LoginManager loginManager = LoginManager.getInstance();
    assertNotNull(loginManager);
  }

  @Test
  public void testLoginBehaviorDefaultsToSsoWithFallback() {
    LoginManager loginManager = new LoginManager();
    assertEquals(LoginBehavior.NATIVE_WITH_FALLBACK, loginManager.getLoginBehavior());
  }

  @Test
  public void testCanChangeLoginBehavior() {
    LoginManager loginManager = new LoginManager();
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY);
    assertEquals(LoginBehavior.NATIVE_ONLY, loginManager.getLoginBehavior());
  }

  @Test
  public void testLoginTargetAppDefaultsToFacebook() {
    LoginManager loginManager = new LoginManager();
    assertEquals(LoginTargetApp.FACEBOOK, loginManager.getLoginTargetApp());
  }

  @Test
  public void testCanChangeLoginTargetApp() {
    LoginManager loginManager = new LoginManager();
    loginManager.setLoginTargetApp(LoginTargetApp.INSTAGRAM);
    assertEquals(LoginTargetApp.INSTAGRAM, loginManager.getLoginTargetApp());
  }

  @Test
  public void testDefaultAudienceDefaultsToFriends() {
    LoginManager loginManager = new LoginManager();
    assertEquals(DefaultAudience.FRIENDS, loginManager.getDefaultAudience());
  }

  @Test
  public void testCanChangeDefaultAudience() {
    LoginManager loginManager = new LoginManager();
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE);
    assertEquals(DefaultAudience.EVERYONE, loginManager.getDefaultAudience());
  }

  @Test
  public void testLogInWithReadAndActivityThrowsIfPublishPermissionGiven() {
    LoginManager loginManager = new LoginManager();
    try {
      loginManager.logInWithReadPermissions(
          mockActivity, Arrays.asList("public_profile", "publish_actions"));
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testLogInWithPublishAndActivityThrowsIfPublishPermissionGiven() {
    LoginManager loginManager = new LoginManager();
    try {
      loginManager.logInWithPublishPermissions(
          mockActivity, Arrays.asList("public_profile", "publish_actions"));
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testLogInThrowsIfCannotResolveFacebookActivity() {
    when(mockPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(null);

    LoginManager loginManager = new LoginManager();

    try {
      loginManager.logInWithReadPermissions(
          mockActivity, Arrays.asList("public_profile", "user_friends"));
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testLogInThrowsIfCannotStartFacebookActivity() {
    doThrow(new ActivityNotFoundException())
        .when(mockActivity)
        .startActivityForResult(any(Intent.class), anyInt());

    LoginManager loginManager = new LoginManager();

    try {
      loginManager.logInWithReadPermissions(
          mockActivity, Arrays.asList("public_profile", "user_friends"));
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testRequiresNonNullActivity() {
    try {
      LoginManager loginManager = new LoginManager();
      loginManager.logInWithReadPermissions(
          (Activity) null, Arrays.asList("public_profile", "user_friends"));
      fail();
    } catch (NullPointerException exception) {
    }
  }

  @Test
  public void testRequiresNonNullFragment() {
    try {
      LoginManager loginManager = new LoginManager();
      loginManager.logInWithReadPermissions(
          (Fragment) null, Arrays.asList("public_profile", "user_friends"));
      fail();
    } catch (NullPointerException exception) {
    }
  }

  @Test
  public void testLogInWithReadDoesNotThrowWithReadPermissions() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
  }

  @Test
  public void testLogInWithReadListCreatesPendingRequestWithCorrectValues() {
    LoginManager loginManager = new LoginManager();
    // Change some defaults so we can verify the pending request picks them up.
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY);
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE);
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    implTestLogInCreatesPendingRequestWithCorrectValues(
        loginManager, Arrays.asList("public_profile", "user_friends"));
  }

  @Test
  public void testLogInWithReadAndAccessTokenCreatesReauthRequest() {
    AccessToken accessToken = createAccessToken();
    when(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(accessToken);

    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    int loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
    verify(mockActivity, times(1)).startActivityForResult(any(Intent.class), eq(loginRequestCode));
  }

  public void implTestLogInCreatesPendingRequestWithCorrectValues(
      LoginManager loginManager, final Collection<String> expectedPermissions) {

    ArgumentMatcher<Intent> m =
        new ArgumentMatcher<Intent>() {
          @Override
          public boolean matches(Intent argument) {
            Intent orig = argument;
            Bundle bundle = orig.getBundleExtra(LoginFragment.REQUEST_KEY);
            LoginClient.Request request =
                (LoginClient.Request) bundle.getParcelable(LoginFragment.EXTRA_REQUEST);
            assertEquals(MOCK_APP_ID, request.getApplicationId());
            assertEquals(LoginBehavior.NATIVE_ONLY, request.getLoginBehavior());
            assertEquals(DefaultAudience.EVERYONE, request.getDefaultAudience());

            Set<String> permissions = request.getPermissions();
            for (String permission : expectedPermissions) {
              assertTrue(permissions.contains(permission));
            }

            return true;
          }
        };
    int loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
    verify(mockActivity, times(1)).startActivityForResult(argThat(m), eq(loginRequestCode));
  }

  @Test
  public void testLogInWithReadAndActivityStartsFacebookActivityWithCorrectRequest() {

    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockActivity).startActivityForResult(intentArgumentCaptor.capture(), anyInt());
    Intent intent = intentArgumentCaptor.getValue();

    ComponentName componentName = intent.getComponent();
    assertEquals(FacebookActivity.class.getName(), componentName.getClassName());
    assertEquals(LoginBehavior.NATIVE_WITH_FALLBACK.name(), intent.getAction());
  }

  @Test
  public void testLogInWithReadAndFragmentStartsFacebookActivityWithCorrectRequest() {

    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockFragment, Arrays.asList("public_profile", "user_friends"));

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockFragment).startActivityForResult(intentArgumentCaptor.capture(), anyInt());
    Intent intent = intentArgumentCaptor.getValue();

    ComponentName componentName = intent.getComponent();
    assertEquals(FacebookActivity.class.getName(), componentName.getClassName());
    assertEquals(LoginBehavior.NATIVE_WITH_FALLBACK.name(), intent.getAction());
  }

  @Test
  public void testLogInWitPublishDoesNotThrowWithPublishPermissions() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithPublishPermissions(
        mockActivity, Arrays.asList("publish_actions", "publish_stream"));
  }

  @Test
  public void testLogInWithPublishListCreatesPendingRequestWithCorrectValues() {
    LoginManager loginManager = new LoginManager();
    // Change some defaults so we can verify the pending request picks them up.
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY);
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE);
    loginManager.logInWithPublishPermissions(
        mockActivity, Arrays.asList("publish_actions", "publish_stream"));

    implTestLogInCreatesPendingRequestWithCorrectValues(
        loginManager, Arrays.asList("publish_actions", "publish_stream"));
  }

  @Test
  public void testLogInWithPublishAndAccessTokenCreatesReauthRequest() {
    AccessToken accessToken = createAccessToken();
    when(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(accessToken);

    LoginManager loginManager = new LoginManager();
    loginManager.logInWithPublishPermissions(
        mockActivity, Arrays.asList("publish_actions", "publish_stream"));

    int loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
    verify(mockActivity, times(1)).startActivityForResult(any(Intent.class), eq(loginRequestCode));
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResultCode() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result = loginManager.onActivityResult(Activity.RESULT_CANCELED, null, mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onCancel();
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResultCodeEvenWithData() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    Intent intent = createSuccessResultIntent();
    boolean result = loginManager.onActivityResult(Activity.RESULT_CANCELED, intent, mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onCancel();
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnActivityResultDoesNotModifyCurrentAccessTokenOnCancelResultCode()
      throws Exception {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    final int[] setTokenTimes = {0};
    PowerMockito.when(mockAccessTokenCompanion, "setCurrentAccessToken", any(AccessToken.class))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setTokenTimes[0]++;
                return null;
              }
            });
    loginManager.onActivityResult(Activity.RESULT_CANCELED, null, mockCallback);
    assertEquals(0, setTokenTimes[0]);
  }

  @Test
  public void testOnActivityResultHandlesMissingCallbackOnCancelResultCode() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result = loginManager.onActivityResult(Activity.RESULT_CANCELED, null);

    assertTrue(result);
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnNullData() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result = loginManager.onActivityResult(Activity.RESULT_OK, null, mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onError(isA(FacebookException.class));
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnMissingResult() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    Intent intent = createSuccessResultIntent();
    intent.removeExtra(LoginFragment.RESULT_KEY);
    boolean result = loginManager.onActivityResult(Activity.RESULT_OK, intent, mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onError(isA(FacebookException.class));
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnErrorResult() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result =
        loginManager.onActivityResult(Activity.RESULT_OK, createErrorResultIntent(), mockCallback);

    ArgumentCaptor<FacebookException> exceptionArgumentCaptor =
        ArgumentCaptor.forClass(FacebookException.class);

    assertTrue(result);
    verify(mockCallback, times(1)).onError(exceptionArgumentCaptor.capture());
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
    assertEquals("foo: bar", exceptionArgumentCaptor.getValue().getMessage());
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResult() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result =
        loginManager.onActivityResult(
            Activity.RESULT_CANCELED, createCancelResultIntent(), mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onCancel();
    verify(mockCallback, never()).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnActivityResultDoesNotModifyCurrentAccessTokenOnErrorResultCode()
      throws Exception {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    final int[] setTokenTimes = {0};
    PowerMockito.when(mockAccessTokenCompanion, "setCurrentAccessToken", any(AccessToken.class))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setTokenTimes[0]++;
                return null;
              }
            });

    loginManager.onActivityResult(
        Activity.RESULT_CANCELED, createErrorResultIntent(), mockCallback);
    assertEquals(0, setTokenTimes[0]);
  }

  @Test
  public void testOnActivityResultDoesNotModifyCurrentAuthenticationTokenOnErrorResultCode()
      throws Exception {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends", "openid"));
    final int[] setTokenTimes = {0};
    PowerMockito.when(
            mockAuthenticationTokenCompanion,
            "setCurrentAuthenticationToken",
            eq(createAuthenticationToken()))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setTokenTimes[0]++;
                return null;
              }
            });
    loginManager.onActivityResult(
        Activity.RESULT_CANCELED, createSuccessResultIntentForOIDC(), mockCallback);
    assertEquals(0, setTokenTimes[0]);
  }

  @Test
  public void testOnActivityResultReturnsTrueAndCallsCallbackOnSuccessResult() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    boolean result =
        loginManager.onActivityResult(
            Activity.RESULT_OK, createSuccessResultIntent(), mockCallback);

    assertTrue(result);
    verify(mockCallback, never()).onError(any(FacebookException.class));
    verify(mockCallback, times(1)).onSuccess(isA(LoginResult.class));
  }

  @Test
  public void testOnHandlesMissingCallbackkOnSuccessResult() {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    boolean result =
        loginManager.onActivityResult(Activity.RESULT_OK, createSuccessResultIntent(), null);

    assertTrue(result);
  }

  @Test
  public void testOnActivityResultSetsCurrentAccessTokenOnSuccessResult() throws Exception {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));
    final int[] setTokenTimes = {0};
    PowerMockito.when(mockAccessTokenCompanion, "setCurrentAccessToken", eq(createAccessToken()))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setTokenTimes[0]++;
                return null;
              }
            });
    loginManager.onActivityResult(Activity.RESULT_OK, createSuccessResultIntent(), mockCallback);
    assertEquals(1, setTokenTimes[0]);
  }

  @Test
  public void testOnActivityResultSetsCurrentAuthenticationTokenOnSuccessResult() throws Exception {
    LoginManager loginManager = new LoginManager();
    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends", "openid"));
    final int[] setTokenTimes = {0};
    PowerMockito.when(
            mockAuthenticationTokenCompanion,
            "setCurrentAuthenticationToken",
            eq(createAuthenticationToken()))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setTokenTimes[0]++;
                return null;
              }
            });
    loginManager.onActivityResult(
        Activity.RESULT_OK, createSuccessResultIntentForOIDC(), mockCallback);
    assertEquals(1, setTokenTimes[0]);
  }

  @Test
  public void testLogInWithFamilyExperience() {
    createTestForFamilyLoginExperience(true, true);
  }

  @Test
  public void testLogInWithStandardExperience() {
    createTestForFamilyLoginExperience(false, false);
  }

  @Test
  public void testLogInWithFamilyExperienceAndSkipAccountDedupe() {
    createTestForFamilyLoginExperience(true, false);
  }

  @Test
  public void testLogoutToEnsureAccessTokenAndAuthenticationTokenSetToNull() throws Exception {
    LoginManager loginManager = new LoginManager();
    final int[] setAccessTokenTimes = {0};
    final int[] setAuthenticationTokenTimes = {0};
    PowerMockito.when(mockAuthenticationTokenCompanion, "setCurrentAuthenticationToken", eq(null))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setAuthenticationTokenTimes[0]++;
                return null;
              }
            });
    PowerMockito.when(mockAccessTokenCompanion, "setCurrentAccessToken", eq(null))
        .thenAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                setAccessTokenTimes[0]++;
                return null;
              }
            });
    loginManager.logOut();
    assertThat(setAccessTokenTimes[0]).isEqualTo(1);
    assertThat(setAuthenticationTokenTimes[0]).isEqualTo(1);
  }

  @Test
  public void testLogInWithAndroidxComponentActivity() {
    androidx.activity.result.ActivityResultRegistry mockRegistry =
        PowerMockito.mock(androidx.activity.result.ActivityResultRegistry.class);
    androidx.activity.ComponentActivity mockActivity =
        PowerMockito.mock(androidx.activity.ComponentActivity.class);
    androidx.activity.result.ActivityResultLauncher mockLauncher =
        PowerMockito.mock(androidx.activity.result.ActivityResultLauncher.class);
    PowerMockito.when(
            mockRegistry.register(
                anyString(),
                any(androidx.activity.result.contract.ActivityResultContract.class),
                any(androidx.activity.result.ActivityResultCallback.class)))
        .thenReturn(mockLauncher);
    PowerMockito.when(mockActivity.getActivityResultRegistry()).thenReturn(mockRegistry);

    Set<String> permissions = Sets.newSet("public_profile", "user_friends");
    LoginManager.getInstance()
        .logInWithReadPermissions(mockActivity, new CallbackManagerImpl(), permissions);
    verify(mockLauncher).launch(any(Intent.class));
  }

  @Test
  public void testLogInWithAndroidxFragment() {
    androidx.activity.result.ActivityResultRegistry mockRegistry =
        PowerMockito.mock(androidx.activity.result.ActivityResultRegistry.class);
    androidx.fragment.app.FragmentActivity mockActivity =
        PowerMockito.mock(androidx.fragment.app.FragmentActivity.class);
    androidx.fragment.app.Fragment mockFragment =
        PowerMockito.mock(androidx.fragment.app.Fragment.class);
    androidx.activity.result.ActivityResultLauncher mockLauncher =
        PowerMockito.mock(androidx.activity.result.ActivityResultLauncher.class);
    PowerMockito.when(mockFragment.getActivity()).thenReturn(mockActivity);
    PowerMockito.when(
            mockRegistry.register(
                anyString(),
                any(androidx.activity.result.contract.ActivityResultContract.class),
                any(androidx.activity.result.ActivityResultCallback.class)))
        .thenReturn(mockLauncher);
    PowerMockito.when(mockActivity.getActivityResultRegistry()).thenReturn(mockRegistry);

    Set<String> permissions = Sets.newSet("public_profile", "user_friends");
    LoginManager.getInstance()
        .logInWithReadPermissions(mockFragment, new CallbackManagerImpl(), permissions);
    verify(mockLauncher).launch(any(Intent.class));
  }

  private void createTestForFamilyLoginExperience(boolean isEnabled, boolean shouldSkip) {
    LoginManager loginManager = new LoginManager();
    loginManager.setFamilyLogin(isEnabled);
    loginManager.setShouldSkipAccountDeduplication(shouldSkip);

    int loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
    final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

    loginManager.logInWithReadPermissions(
        mockActivity, Arrays.asList("public_profile", "user_friends"));

    verify(mockActivity, times(1))
        .startActivityForResult(intentCaptor.capture(), eq(loginRequestCode));
    Bundle bundle = intentCaptor.getValue().getBundleExtra(LoginFragment.REQUEST_KEY);
    LoginClient.Request request =
        (LoginClient.Request) bundle.getParcelable(LoginFragment.EXTRA_REQUEST);
    assertThat(request.isFamilyLogin()).isEqualTo(isEnabled);
    assertThat(request.shouldSkipAccountDeduplication()).isEqualTo(shouldSkip);
  }

  private Intent createSuccessResultIntent() {
    Intent intent = new Intent();

    Set<String> permissions = Sets.newSet("public_profile", "user_friends");
    LoginClient.Request request =
        new LoginClient.Request(null, permissions, null, null, null, null);

    AccessToken accessToken = createAccessToken();
    LoginClient.Result result = LoginClient.Result.createTokenResult(request, accessToken);
    intent.putExtra(LoginFragment.RESULT_KEY, result);

    return intent;
  }

  private Intent createSuccessResultIntentForOIDC() {
    Intent intent = new Intent();

    Set<String> permissions = Sets.newSet("public_profile", "user_friends", "opendid");
    LoginClient.Request request =
        new LoginClient.Request(
            null,
            permissions,
            null,
            null,
            null,
            null,
            LoginTargetApp.FACEBOOK,
            AuthenticationTokenTestUtil.NONCE,
            codeVerifier,
            codeChallenge,
            codeChallengeMethod);

    AccessToken accessToken = createAccessToken();
    AuthenticationToken authenticationToken = createAuthenticationToken();
    LoginClient.Result result =
        LoginClient.Result.createCompositeTokenResult(request, accessToken, authenticationToken);
    intent.putExtra(LoginFragment.RESULT_KEY, result);

    return intent;
  }

  private Intent createErrorResultIntent() {
    Intent intent = new Intent();

    LoginClient.Request request = mock(LoginClient.Request.class);

    LoginClient.Result result = LoginClient.Result.createErrorResult(request, "foo", "bar");
    intent.putExtra(LoginFragment.RESULT_KEY, result);

    return intent;
  }

  private Intent createCancelResultIntent() {
    Intent intent = new Intent();

    LoginClient.Request request = mock(LoginClient.Request.class);

    LoginClient.Result result = LoginClient.Result.createCancelResult(request, null);
    intent.putExtra(LoginFragment.RESULT_KEY, result);

    return intent;
  }

  private AccessToken createAccessToken() {
    return new AccessToken(
        TOKEN_STRING,
        MOCK_APP_ID,
        USER_ID,
        PERMISSIONS,
        null,
        null,
        AccessTokenSource.WEB_VIEW,
        EXPIRES,
        LAST_REFRESH,
        DATA_ACCESS_EXPIRATION_TIME);
  }

  private AuthenticationToken createAuthenticationToken() {
    return new AuthenticationToken(
        AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest(),
        AuthenticationTokenTestUtil.NONCE);
  }
}
