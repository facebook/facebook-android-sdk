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

package com.facebook.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Looper
import android.util.Pair
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.AuthenticationToken
import com.facebook.FacebookActivity
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdkNotInitializedException
import com.facebook.LoginStatusCallback
import com.facebook.MockSharedPreference
import com.facebook.Profile
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.NativeProtocol
import com.facebook.internal.PlatformServiceClient
import com.facebook.internal.ServerProtocol
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import java.util.Date
import java.util.concurrent.ThreadPoolExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class)
class LoginManagerTest : FacebookPowerMockTestCase() {
  @Mock lateinit var mockActivity: Activity
  @Mock lateinit var mockFragment: Fragment
  @Mock lateinit var mockApplicationContext: Context
  @Mock lateinit var mockPackageManager: PackageManager
  @Mock lateinit var mockCallback: FacebookCallback<LoginResult>
  @Mock lateinit var threadExecutor: ThreadPoolExecutor
  @Mock lateinit var mockFragmentActivity: FragmentActivity
  @Mock lateinit var mockLooper: Looper
  @Mock lateinit var mockAccessTokenCompanion: AccessToken.Companion
  @Mock lateinit var mockAuthenticationTokenCompanion: AuthenticationToken.Companion
  @Mock lateinit var mockProfileCompanion: Profile.Companion

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(null)
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)
    whenever(mockAuthenticationTokenCompanion.getCurrentAuthenticationToken()).thenReturn(null)
    Whitebox.setInternalState(
        AuthenticationToken::class.java, "Companion", mockAuthenticationTokenCompanion)
    Whitebox.setInternalState(Profile::class.java, "Companion", mockProfileCompanion)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(FacebookSdk.getExecutor()).thenReturn(threadExecutor)
    whenever(FacebookSdk.getGraphApiVersion()).thenReturn(ServerProtocol.getDefaultAPIVersion())
    whenever(mockFragment.activity).thenReturn(mockFragmentActivity)
    whenever(mockActivity.applicationContext).thenReturn(mockApplicationContext)
    whenever(FacebookSdk.getApplicationContext().getSharedPreferences(any<String>(), any()))
        .thenReturn(MockSharedPreference())

    // We use mocks rather than RobolectricPackageManager because it's simpler to not
    // have to specify Intents. Default to resolving all intents to something.
    val resolveInfo = ResolveInfo()
    whenever(mockApplicationContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockApplicationContext.mainLooper).thenReturn(mockLooper)
    whenever(mockApplicationContext.applicationContext).thenReturn(mockApplicationContext)
    whenever(mockPackageManager.resolveActivity(any(), any())).thenReturn(resolveInfo)
    whenever(mockPackageManager.resolveService(any(), any())).thenReturn(resolveInfo)

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any())).thenReturn(mock())
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
  }

  @Test(expected = FacebookSdkNotInitializedException::class)
  fun testRequiresSdkToBeInitialized() {
    whenever(FacebookSdk.isInitialized()).thenReturn(false)
    LoginManager()
  }

  @Test
  fun testGetInstance() {
    val loginManager = LoginManager.getInstance()
    assertThat(loginManager).isNotNull
  }

  @Test
  fun testLoginBehaviorDefaultsToSsoWithFallback() {
    val loginManager = LoginManager()
    assertThat(loginManager.loginBehavior).isEqualTo(LoginBehavior.NATIVE_WITH_FALLBACK)
  }

  @Test
  fun testCanChangeLoginBehavior() {
    val loginManager = LoginManager()
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY)
    assertThat(loginManager.loginBehavior).isEqualTo(LoginBehavior.NATIVE_ONLY)
  }

  @Test
  fun testLoginTargetAppDefaultsToFacebook() {
    val loginManager = LoginManager()
    assertThat(loginManager.loginTargetApp).isEqualTo(LoginTargetApp.FACEBOOK)
  }

  @Test
  fun testCanChangeLoginTargetApp() {
    val loginManager = LoginManager()
    loginManager.setLoginTargetApp(LoginTargetApp.INSTAGRAM)
    assertThat(loginManager.loginTargetApp).isEqualTo(LoginTargetApp.INSTAGRAM)
  }

  @Test
  fun testDefaultAudienceDefaultsToFriends() {
    val loginManager = LoginManager()
    assertThat(loginManager.defaultAudience).isEqualTo(DefaultAudience.FRIENDS)
  }

  @Test
  fun testCanChangeDefaultAudience() {
    val loginManager = LoginManager()
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE)
    assertThat(loginManager.defaultAudience).isEqualTo(DefaultAudience.EVERYONE)
  }

  @Test(expected = FacebookException::class)
  fun testLogInWithReadAndActivityThrowsIfPublishPermissionGiven() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "publish_actions"))
  }

  @Test(expected = FacebookException::class)
  fun testLogInWithPublishAndActivityThrowsIfPublishPermissionGiven() {
    val loginManager = LoginManager()
    loginManager.logInWithPublishPermissions(
        mockActivity, listOf("public_profile", "publish_actions"))
  }

  @Test(expected = FacebookException::class)
  fun testLogInThrowsIfCannotResolveFacebookActivity() {
    whenever(mockPackageManager.resolveActivity(any(), any())).thenReturn(null)
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
  }

  @Test(expected = FacebookException::class)
  fun testLogInThrowsIfCannotStartFacebookActivity() {
    whenever(mockActivity.startActivityForResult(any(), any()))
        .thenThrow(ActivityNotFoundException())
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
  }

  @Test
  fun testLogInWithReadDoesNotThrowWithReadPermissions() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
  }

  @Test
  fun testLogInWithReadListCreatesPendingRequestWithCorrectValues() {
    val loginManager = LoginManager()
    // Change some defaults so we can verify the pending request picks them up.
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY)
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE)
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    verifyTestLogInCreatesPendingRequestWithCorrectValues(listOf("public_profile", "user_friends"))
  }

  @Test
  fun testLogInWithReadAndAccessTokenCreatesReauthRequest() {
    val accessToken = createAccessToken()
    whenever(AccessToken.getCurrentAccessToken()).thenReturn(accessToken)
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()
    verify(mockActivity).startActivityForResult(any(), eq(loginRequestCode))
  }

  @Test
  fun testLogInWithReadAndActivityStartsFacebookActivityWithCorrectRequest() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val intentArgumentCaptor = argumentCaptor<Intent>()
    verify(mockActivity).startActivityForResult(intentArgumentCaptor.capture(), any())
    val intent = intentArgumentCaptor.firstValue
    val componentName = intent.component
    assertThat(componentName?.className).isEqualTo(FacebookActivity::class.java.name)
    assertThat(intent.action).isEqualTo(LoginBehavior.NATIVE_WITH_FALLBACK.name)
  }

  @Test
  fun testLogInWithReadAndFragmentStartsFacebookActivityWithCorrectRequest() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockFragment, listOf("public_profile", "user_friends"))
    val intentArgumentCaptor = argumentCaptor<Intent>()
    verify(mockFragment).startActivityForResult(intentArgumentCaptor.capture(), any())
    val intent = intentArgumentCaptor.firstValue
    val componentName = intent.component
    assertThat(componentName?.className).isEqualTo(FacebookActivity::class.java.name)
    assertThat(intent.action).isEqualTo(LoginBehavior.NATIVE_WITH_FALLBACK.name)
  }

  @Test
  fun testLogInWitPublishDoesNotThrowWithPublishPermissions() {
    val loginManager = LoginManager()
    loginManager.logInWithPublishPermissions(
        mockActivity, listOf("publish_actions", "publish_stream"))
  }

  @Test
  fun testLogInWithPublishListCreatesPendingRequestWithCorrectValues() {
    val loginManager = LoginManager()
    // Change some defaults so we can verify the pending request picks them up.
    loginManager.setLoginBehavior(LoginBehavior.NATIVE_ONLY)
    loginManager.setDefaultAudience(DefaultAudience.EVERYONE)
    loginManager.logInWithPublishPermissions(
        mockActivity, listOf("publish_actions", "publish_stream"))
    verifyTestLogInCreatesPendingRequestWithCorrectValues(
        listOf("publish_actions", "publish_stream"))
  }

  @Test
  fun testLogInWithPublishAndAccessTokenCreatesReauthRequest() {
    val accessToken = createAccessToken()
    whenever(AccessToken.getCurrentAccessToken()).thenReturn(accessToken)
    val loginManager = LoginManager()
    loginManager.logInWithPublishPermissions(
        mockActivity, listOf("publish_actions", "publish_stream"))
    val loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()
    verify(mockActivity).startActivityForResult(any(), eq(loginRequestCode))
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResultCode() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result = loginManager.onActivityResult(Activity.RESULT_CANCELED, null, mockCallback)
    assertThat(result).isTrue
    verify(mockCallback).onCancel()
    verify(mockCallback, never()).onSuccess(any())
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResultCodeEvenWithData() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val intent = createSuccessResultIntent()
    val result = loginManager.onActivityResult(Activity.RESULT_CANCELED, intent, mockCallback)
    assertThat(result).isTrue
    verify(mockCallback).onCancel()
    verify(mockCallback, never()).onSuccess(any())
  }

  @Test
  fun testOnActivityResultDoesNotModifyCurrentAccessTokenOnCancelResultCode() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    loginManager.onActivityResult(Activity.RESULT_CANCELED, null, mockCallback)
    verify(mockAccessTokenCompanion, never()).setCurrentAccessToken(any())
  }

  @Test
  fun testOnActivityResultHandlesMissingCallbackOnCancelResultCode() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result = loginManager.onActivityResult(Activity.RESULT_CANCELED, null)
    assertThat(result).isTrue
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnNullData() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result = loginManager.onActivityResult(Activity.RESULT_OK, null, mockCallback)
    assertThat(result).isTrue
    verify(mockCallback).onError(any())
    verify(mockCallback, never()).onSuccess(any())
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnMissingResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val intent = createSuccessResultIntent()
    intent.removeExtra(LoginFragment.RESULT_KEY)
    val result = loginManager.onActivityResult(Activity.RESULT_OK, intent, mockCallback)
    assertThat(result)
    verify(mockCallback).onError(any())
    verify(mockCallback, never()).onSuccess(any())
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnErrorResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result =
        loginManager.onActivityResult(Activity.RESULT_OK, createErrorResultIntent(), mockCallback)
    assertThat(result).isTrue
    val exceptionCaptor = argumentCaptor<FacebookException>()
    verify(mockCallback).onError(exceptionCaptor.capture())
    verify(mockCallback, never()).onSuccess(any())
    assertThat(exceptionCaptor.firstValue.message).isEqualTo("foo: bar")
  }
  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnCancelResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result =
        loginManager.onActivityResult(
            Activity.RESULT_CANCELED, createCancelResultIntent(), mockCallback)
    assertThat(result).isTrue
    verify(mockCallback).onCancel()
    verify(mockCallback, never()).onSuccess(any())
  }
  @Test
  fun testOnActivityResultDoesNotModifyCurrentAccessTokenOnErrorResultCode() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    loginManager.onActivityResult(Activity.RESULT_CANCELED, createErrorResultIntent(), mockCallback)
    verify(mockAccessTokenCompanion, never()).setCurrentAccessToken(any())
  }

  @Test
  fun testOnActivityResultDoesNotModifyCurrentAuthenticationTokenOnErrorResultCode() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(
        mockActivity, listOf("public_profile", "user_friends", "openid"))
    loginManager.onActivityResult(
        Activity.RESULT_CANCELED, createSuccessResultIntentForOIDC(), mockCallback)
    verify(mockAuthenticationTokenCompanion, never()).setCurrentAuthenticationToken(any())
  }

  @Test
  fun testOnActivityResultReturnsTrueAndCallsCallbackOnSuccessResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result =
        loginManager.onActivityResult(Activity.RESULT_OK, createSuccessResultIntent(), mockCallback)
    assertThat(result).isTrue
    verify(mockCallback, never()).onError(any())
    verify(mockCallback).onSuccess(any())
  }

  @Test
  fun testOnHandlesMissingCallbackOnSuccessResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    val result =
        loginManager.onActivityResult(Activity.RESULT_OK, createSuccessResultIntent(), null)
    assertThat(result).isTrue
  }

  @Test
  fun testOnActivityResultSetsCurrentAccessTokenOnSuccessResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    loginManager.onActivityResult(Activity.RESULT_OK, createSuccessResultIntent(), mockCallback)
    verify(mockAccessTokenCompanion).setCurrentAccessToken(eq(createAccessToken()))
  }

  @Test
  fun testOnActivityResultSetsCurrentAuthenticationTokenOnSuccessResult() {
    val loginManager = LoginManager()
    loginManager.logInWithReadPermissions(
        mockActivity, listOf("public_profile", "user_friends", "openid"))
    loginManager.onActivityResult(
        Activity.RESULT_OK, createSuccessResultIntentForOIDC(), mockCallback)
    verify(mockAuthenticationTokenCompanion)
        .setCurrentAuthenticationToken(eq(createAuthenticationToken()))
  }

  @Test
  fun testLogInWithFamilyExperience() {
    createTestForFamilyLoginExperience(true, true)
  }

  @Test
  fun testLogInWithStandardExperience() {
    createTestForFamilyLoginExperience(false, false)
  }

  @Test
  fun testLogInWithFamilyExperienceAndSkipAccountDedupe() {
    createTestForFamilyLoginExperience(true, false)
  }

  @Test
  fun testLogoutToEnsureAccessTokenAndAuthenticationTokenSetToNull() {
    val loginManager = LoginManager()
    loginManager.logOut()
    verify(mockAuthenticationTokenCompanion).setCurrentAuthenticationToken(null)
    verify(mockAccessTokenCompanion).setCurrentAccessToken(null)
  }

  @Test
  fun testLogInWithAndroidxComponentActivity() {
    val mockRegistry = mock<ActivityResultRegistry>()
    val mockActivity = mock<ComponentActivity>()
    val mockLauncher = mock<ActivityResultLauncher<Intent>>()
    whenever(
            mockRegistry.register(
                any(),
                any<ActivityResultContract<Intent, Pair<Int, Intent>>>(),
                any<ActivityResultCallback<Pair<Int, Intent>>>()))
        .thenReturn(mockLauncher)
    whenever(mockActivity.activityResultRegistry).thenReturn(mockRegistry)
    val permissions = setOf("public_profile", "user_friends")
    LoginManager.getInstance()
        .logInWithReadPermissions(mockActivity, CallbackManagerImpl(), permissions)
    verify(mockLauncher).launch(any())
  }

  @Test
  fun testLogInWithAndroidxFragment() {
    val mockRegistry = mock<ActivityResultRegistry>()
    val mockActivity = mock<FragmentActivity>()
    val mockFragment = mock<Fragment>()
    val mockLauncher = mock<ActivityResultLauncher<Intent>>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    whenever(
            mockRegistry.register(
                any(),
                any<ActivityResultContract<Intent, Pair<Int, Intent>>>(),
                any<ActivityResultCallback<Pair<Int, Intent>>>()))
        .thenReturn(mockLauncher)
    whenever(mockActivity.activityResultRegistry).thenReturn(mockRegistry)
    val permissions = setOf("public_profile", "user_friends")
    LoginManager.getInstance()
        .logInWithReadPermissions(mockFragment, CallbackManagerImpl(), permissions)
    verify(mockLauncher).launch(any())
  }

  @Test
  fun `test retrieve login status will set a completed listener and start connection with login status client`() {
    val mockLoginStatusClientCompanion: LoginStatusClient.Companion = mock()
    val mockLoginStatusClient: LoginStatusClient = mock()
    whenever(
            mockLoginStatusClientCompanion.newInstance(
                eq(mockApplicationContext), any(), any(), any(), any(), anyOrNull()))
        .thenReturn(mockLoginStatusClient)
    whenever(mockLoginStatusClient.start()).thenReturn(true)
    Whitebox.setInternalState(
        LoginStatusClient::class.java, "Companion", mockLoginStatusClientCompanion)

    LoginManager.getInstance().retrieveLoginStatus(mockApplicationContext, mock())

    val completedListenerCaptor = argumentCaptor<PlatformServiceClient.CompletedListener>()
    verify(mockLoginStatusClient).setCompletedListener(completedListenerCaptor.capture())
    verify(mockLoginStatusClient).start()
  }

  @Test
  fun `test retrieve login status if a correct result is received`() {
    val mockLoginStatusClientCompanion: LoginStatusClient.Companion = mock()
    val mockLoginStatusClient: LoginStatusClient = mock()
    whenever(
            mockLoginStatusClientCompanion.newInstance(
                eq(mockApplicationContext), any(), any(), any(), any(), anyOrNull()))
        .thenReturn(mockLoginStatusClient)
    whenever(mockLoginStatusClient.start()).thenReturn(true)
    Whitebox.setInternalState(
        LoginStatusClient::class.java, "Companion", mockLoginStatusClientCompanion)

    val mockLoginStatusCallback = mock<LoginStatusCallback>()
    LoginManager.getInstance().retrieveLoginStatus(mockApplicationContext, mockLoginStatusCallback)

    val completedListenerCaptor = argumentCaptor<PlatformServiceClient.CompletedListener>()
    verify(mockLoginStatusClient).setCompletedListener(completedListenerCaptor.capture())
    val completedListener = checkNotNull(completedListenerCaptor.firstValue)

    val successResult = Bundle()
    successResult.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, TOKEN_STRING)
    successResult.putStringArrayList(
        NativeProtocol.EXTRA_PERMISSIONS, arrayListOf(*PERMISSIONS.toTypedArray()))
    successResult.putString(NativeProtocol.RESULT_ARGS_SIGNED_REQUEST, SIGNATURE_AND_PAYLOAD)
    successResult.putString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN, GRAPH_DOMAIN)
    completedListener.completed(successResult)

    // verify a access token will be constructed from the result
    val accessTokenCaptor = argumentCaptor<AccessToken>()
    verify(mockLoginStatusCallback).onCompleted(accessTokenCaptor.capture())
    val capturedAccessToken = checkNotNull(accessTokenCaptor.firstValue)
    assertThat(capturedAccessToken.token).isEqualTo(TOKEN_STRING)
    assertThat(capturedAccessToken.userId).isEqualTo(USER_ID)
    assertThat(capturedAccessToken.permissions)
        .containsExactlyInAnyOrder(*PERMISSIONS.toTypedArray())
    assertThat(capturedAccessToken.graphDomain).isEqualTo(GRAPH_DOMAIN)
    // verify the same access token will be set to global status
    verify(mockAccessTokenCompanion).setCurrentAccessToken(eq(capturedAccessToken))
  }

  @Test
  fun `test retrieve login status when an invalid result is received`() {
    val mockLoginStatusClientCompanion: LoginStatusClient.Companion = mock()
    val mockLoginStatusClient: LoginStatusClient = mock()
    whenever(
            mockLoginStatusClientCompanion.newInstance(
                eq(mockApplicationContext), any(), any(), any(), any(), anyOrNull()))
        .thenReturn(mockLoginStatusClient)
    whenever(mockLoginStatusClient.start()).thenReturn(true)
    Whitebox.setInternalState(
        LoginStatusClient::class.java, "Companion", mockLoginStatusClientCompanion)

    val mockLoginStatusCallback = mock<LoginStatusCallback>()
    LoginManager.getInstance().retrieveLoginStatus(mockApplicationContext, mockLoginStatusCallback)

    val completedListenerCaptor = argumentCaptor<PlatformServiceClient.CompletedListener>()
    verify(mockLoginStatusClient).setCompletedListener(completedListenerCaptor.capture())
    val completedListener = checkNotNull(completedListenerCaptor.firstValue)

    val result = Bundle()
    // empty access token string
    result.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, "")
    result.putStringArrayList(
        NativeProtocol.EXTRA_PERMISSIONS, arrayListOf(*PERMISSIONS.toTypedArray()))
    result.putString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN, GRAPH_DOMAIN)
    completedListener.completed(result)

    // verify that onFailure will be called if the received result is invalid
    verify(mockLoginStatusCallback).onFailure()
    // and no access token will be set
    verify(mockAccessTokenCompanion, never()).setCurrentAccessToken(any())
  }

  @Test
  fun `test failing to start connection in retrieving login status will call onFailure at the callback`() {
    val mockLoginStatusClientCompanion: LoginStatusClient.Companion = mock()
    val mockLoginStatusClient: LoginStatusClient = mock()
    whenever(
            mockLoginStatusClientCompanion.newInstance(
                eq(mockApplicationContext), any(), any(), any(), any(), anyOrNull()))
        .thenReturn(mockLoginStatusClient)
    whenever(mockLoginStatusClient.start()).thenReturn(false)
    Whitebox.setInternalState(
        LoginStatusClient::class.java, "Companion", mockLoginStatusClientCompanion)

    val mockLoginStatusCallback = mock<LoginStatusCallback>()
    LoginManager.getInstance().retrieveLoginStatus(mockApplicationContext, mockLoginStatusCallback)
    verify(mockLoginStatusCallback).onFailure()
  }

  @Test
  fun `test retrieve login status when an error result is received`() {
    val mockLoginStatusClientCompanion: LoginStatusClient.Companion = mock()
    val mockLoginStatusClient: LoginStatusClient = mock()
    whenever(
            mockLoginStatusClientCompanion.newInstance(
                eq(mockApplicationContext), any(), any(), any(), any(), anyOrNull()))
        .thenReturn(mockLoginStatusClient)
    whenever(mockLoginStatusClient.start()).thenReturn(true)
    Whitebox.setInternalState(
        LoginStatusClient::class.java, "Companion", mockLoginStatusClientCompanion)

    val mockLoginStatusCallback = mock<LoginStatusCallback>()
    LoginManager.getInstance().retrieveLoginStatus(mockApplicationContext, mockLoginStatusCallback)

    val completedListenerCaptor = argumentCaptor<PlatformServiceClient.CompletedListener>()
    verify(mockLoginStatusClient).setCompletedListener(completedListenerCaptor.capture())
    val completedListener = checkNotNull(completedListenerCaptor.firstValue)

    val result = Bundle()
    result.putString(NativeProtocol.STATUS_ERROR_TYPE, "test_error")
    result.putString(NativeProtocol.STATUS_ERROR_DESCRIPTION, "test error description")
    completedListener.completed(result)

    val exceptionCaptor = argumentCaptor<FacebookException>()
    verify(mockLoginStatusCallback).onError(exceptionCaptor.capture())
    val capturedException = exceptionCaptor.firstValue
    assertThat(capturedException.message)
        .contains(result.getString(NativeProtocol.STATUS_ERROR_TYPE))
    assertThat(capturedException.message)
        .contains(result.getString(NativeProtocol.STATUS_ERROR_DESCRIPTION))
    // and no access token will be set
    verify(mockAccessTokenCompanion, never()).setCurrentAccessToken(any())
  }

  private fun verifyTestLogInCreatesPendingRequestWithCorrectValues(
      expectedPermissions: Collection<String>
  ) {
    val loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()
    val intentCaptor = argumentCaptor<Intent>()
    verify(mockActivity).startActivityForResult(intentCaptor.capture(), eq(loginRequestCode))
    val capturedIntent = intentCaptor.firstValue
    val bundle = capturedIntent.getBundleExtra(LoginFragment.REQUEST_KEY)
    checkNotNull(bundle)
    val request = bundle.getParcelable<LoginClient.Request>(LoginFragment.EXTRA_REQUEST)
    checkNotNull(request)
    assertThat(request.applicationId).isEqualTo(MOCK_APP_ID)
    assertThat(request.loginBehavior).isEqualTo(LoginBehavior.NATIVE_ONLY)
    assertThat(request.defaultAudience).isEqualTo(DefaultAudience.EVERYONE)
    assertThat(request.permissions).containsAll(expectedPermissions)
  }

  private fun createTestForFamilyLoginExperience(isEnabled: Boolean, shouldSkip: Boolean) {
    val loginManager = LoginManager()
    loginManager.setFamilyLogin(isEnabled)
    loginManager.setShouldSkipAccountDeduplication(shouldSkip)
    val loginRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()
    val intentCaptor = argumentCaptor<Intent>()
    loginManager.logInWithReadPermissions(mockActivity, listOf("public_profile", "user_friends"))
    verify(
            mockActivity,
        )
        .startActivityForResult(intentCaptor.capture(), eq(loginRequestCode))
    val bundle = intentCaptor.firstValue.getBundleExtra(LoginFragment.REQUEST_KEY)
    checkNotNull(bundle)
    val request = bundle.getParcelable<LoginClient.Request>(LoginFragment.EXTRA_REQUEST)
    checkNotNull(request)
    assertThat(request.isFamilyLogin).isEqualTo(isEnabled)
    assertThat(request.shouldSkipAccountDeduplication()).isEqualTo(shouldSkip)
  }
  companion object {
    private const val MOCK_APP_ID = AuthenticationTokenTestUtil.APP_ID
    private const val TOKEN_STRING = "A token of my esteem"
    private const val USER_ID = "54321"
    // user_id = 54321 for this base64 code
    private const val SIGNATURE_AND_PAYLOAD = "signature.eyJ1c2VyX2lkIjo1NDMyMX0="
    private val PERMISSIONS = listOf("walk", "chew gum")
    private val EXPIRES = Date(2025, 5, 3)
    private val LAST_REFRESH = Date(2023, 8, 15)
    private val DATA_ACCESS_EXPIRATION_TIME = Date(2025, 5, 3)
    private val GRAPH_DOMAIN = "test.facebook.com"
    private const val codeVerifier = "codeVerifier"
    private const val codeChallenge = "codeChallenge"
    private val codeChallengeMethod = CodeChallengeMethod.S256

    private fun createAccessToken(): AccessToken {
      return AccessToken(
          TOKEN_STRING,
          MOCK_APP_ID,
          USER_ID,
          PERMISSIONS,
          null,
          null,
          AccessTokenSource.WEB_VIEW,
          EXPIRES,
          LAST_REFRESH,
          DATA_ACCESS_EXPIRATION_TIME)
    }

    private fun createSuccessResultIntent(): Intent {
      val intent = Intent()
      val permissions = setOf("public_profile", "user_friends")
      val request =
          LoginClient.Request(
              LoginBehavior.NATIVE_WITH_FALLBACK,
              permissions,
              DefaultAudience.NONE,
              "test auth",
              "123456789",
              "12345")
      val accessToken = createAccessToken()
      val result = LoginClient.Result.createTokenResult(request, accessToken)
      intent.putExtra(LoginFragment.RESULT_KEY, result)
      return intent
    }

    private fun createErrorResultIntent(): Intent {
      val intent = Intent()
      val request = PowerMockito.mock(LoginClient.Request::class.java)
      val result = LoginClient.Result.createErrorResult(request, "foo", "bar")
      intent.putExtra(LoginFragment.RESULT_KEY, result)
      return intent
    }

    private fun createCancelResultIntent(): Intent {
      val intent = Intent()
      val request = PowerMockito.mock(LoginClient.Request::class.java)
      val result = LoginClient.Result.createCancelResult(request, null)
      intent.putExtra(LoginFragment.RESULT_KEY, result)
      return intent
    }
    private fun createSuccessResultIntentForOIDC(): Intent {
      val intent = Intent()
      val permissions = setOf("public_profile", "user_friends", "openid")
      val request =
          LoginClient.Request(
              LoginBehavior.NATIVE_WITH_FALLBACK,
              permissions,
              DefaultAudience.ONLY_ME,
              "auth type",
              "123456789",
              "12345",
              LoginTargetApp.FACEBOOK,
              AuthenticationTokenTestUtil.NONCE,
              codeVerifier,
              codeChallenge,
              codeChallengeMethod)
      val accessToken = createAccessToken()
      val authenticationToken = createAuthenticationToken()
      val result =
          LoginClient.Result.createCompositeTokenResult(request, accessToken, authenticationToken)
      intent.putExtra(LoginFragment.RESULT_KEY, result)
      return intent
    }
    private fun createAuthenticationToken(): AuthenticationToken {
      return AuthenticationToken(
          getEncodedAuthTokenStringForTest(), AuthenticationTokenTestUtil.NONCE)
    }
  }
}
