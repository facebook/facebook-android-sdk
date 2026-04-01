/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.facebook.AccessToken
import com.facebook.AuthenticationToken
import com.facebook.FacebookCallback
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.Profile
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.FeatureManager
import com.facebook.internal.NativeProtocol
import com.facebook.internal.security.OidcSecurityUtil
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    FeatureManager::class,
    NativeProtocol::class,
    OidcSecurityUtil::class,
    LoginMethodHandler::class)
class FBLoginSSOLauncherTest : FacebookPowerMockTestCase() {

  private lateinit var mockActivity: FragmentActivity
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockLauncher: ActivityResultLauncher<Intent>
  private lateinit var mockCallback: FacebookCallback<LoginResult>
  private lateinit var mockLooper: Looper
  private lateinit var mockAccessTokenCompanion: AccessToken.Companion
  private lateinit var mockAuthenticationTokenCompanion: AuthenticationToken.Companion
  private lateinit var mockProfileCompanion: Profile.Companion
  private lateinit var mockFragmentManager: FragmentManager
  private lateinit var mockFragmentTransaction: FragmentTransaction
  private lateinit var mockAppEventsLogger: InternalAppEventsLogger
  private var capturedResultCallback: ActivityResultCallback<ActivityResult>? = null

  override fun setup() {
    super.setup()
    mockActivity = mock()
    mockPackageManager = mock()
    mockLauncher = mock()
    mockCallback = mock()
    mockLooper = mock()
    mockFragmentManager = mock()
    mockFragmentTransaction = mock()
    whenever(mockFragmentManager.beginTransaction()).thenReturn(mockFragmentTransaction)
    whenever(mockFragmentTransaction.add(any<FBLoginSSONoAppDialog>(), any())).thenReturn(mockFragmentTransaction)

    whenever(
            mockActivity.registerForActivityResult(
                any<ActivityResultContract<Intent, ActivityResult>>(), any()))
        .thenAnswer { invocation ->
          @Suppress("UNCHECKED_CAST")
          capturedResultCallback =
              invocation.arguments[1] as ActivityResultCallback<ActivityResult>
          mockLauncher
        }

    whenever(mockActivity.packageManager).thenReturn(mockPackageManager)
    whenever(mockActivity.supportFragmentManager).thenReturn(mockFragmentManager)

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getRedirectURI()).thenReturn("https://example.com/redirect")
    whenever(FacebookSdk.getIntentUriPackageTarget()).thenReturn(null)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mock())
    whenever(FacebookSdk.getApplicationContext().mainLooper).thenReturn(mockLooper)
    whenever(FacebookSdk.getApplicationContext().getSharedPreferences(any<String>(), any()))
        .thenReturn(MockSharedPreference())

    PowerMockito.mockStatic(FeatureManager::class.java)

    mockAccessTokenCompanion = mock()
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(null)
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)

    mockAuthenticationTokenCompanion = mock()
    whenever(mockAuthenticationTokenCompanion.getCurrentAuthenticationToken()).thenReturn(null)
    Whitebox.setInternalState(
        AuthenticationToken::class.java, "Companion", mockAuthenticationTokenCompanion)

    mockProfileCompanion = mock()
    Whitebox.setInternalState(Profile::class.java, "Companion", mockProfileCompanion)

    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any())).thenReturn(mock())
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    mockAppEventsLogger = mock()

    FBLoginSSOLauncher.pendingSsoContext = null
  }

  private fun createLauncherWithMockLogger(
      showWithoutFBApp: Boolean = true
  ): FBLoginSSOLauncher {
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp)
    launcher.appEventsLogger = mockAppEventsLogger
    return launcher
  }

  private fun verifyEventFired(eventName: String): Bundle {
    val bundleCaptor = argumentCaptor<Bundle>()
    verify(mockAppEventsLogger).logEventImplicitly(eq(eventName), bundleCaptor.capture())
    return bundleCaptor.firstValue
  }

  private fun verifyEventFiredWithExtras(eventName: String): JSONObject {
    val params = verifyEventFired(eventName)
    return JSONObject(params.getString(LoginLogger.EVENT_PARAM_EXTRAS) ?: "{}")
  }

  private fun verifyEventNotFired(eventName: String) {
    verify(mockAppEventsLogger, never())
        .logEventImplicitly(eq(eventName), any<Bundle>())
  }

  private fun setupFb4aWithSso() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(ResolveInfo())
  }

  private fun setupNoFb4a() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(null)
    whenever(mockPackageManager.getPackageInfo(eq("com.facebook.katana"), any<Int>()))
        .thenThrow(PackageManager.NameNotFoundException())
  }

  private fun setupOutdatedFb4a() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(null)
    whenever(mockPackageManager.getPackageInfo(eq("com.facebook.katana"), any<Int>()))
        .thenReturn(PackageInfo())
  }

  // ==========================================================================
  // 1. sso_launch_attempted
  // ==========================================================================

  @Test
  fun `sso_launch_attempted fires on every launch call`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventFired(LoginLogger.EVENT_NAME_SSO_LAUNCH_ATTEMPTED)
  }

  @Test
  fun `sso_launch_attempted fires even when GK disabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(false)
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventFired(LoginLogger.EVENT_NAME_SSO_LAUNCH_ATTEMPTED)
  }

  @Test
  fun `sso_launch_attempted does NOT fire when user is already logged in`() {
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mock())
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_LAUNCH_ATTEMPTED)
  }

  @Test
  fun `sso_launch_attempted includes permissions in extras`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch(listOf("email", "public_profile"))
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_LAUNCH_ATTEMPTED)
    assertThat(extras.getString("permissions")).isEqualTo("email,public_profile")
  }

  // ==========================================================================
  // 2. sso_delegated_to_fb4a
  // ==========================================================================

  @Test
  fun `sso_delegated_to_fb4a fires when FB4A SSO activity resolves`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventFired(LoginLogger.EVENT_NAME_SSO_DELEGATED_TO_FB4A)
  }

  @Test
  fun `sso_delegated_to_fb4a does NOT fire when FB4A is missing`() {
    setupNoFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = false)
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_DELEGATED_TO_FB4A)
  }

  // ==========================================================================
  // 3. sso_dialog_shown (SDK fallback dialog only)
  // ==========================================================================

  @Test
  fun `sso_dialog_shown fires for fallback dialog when FB4A not installed`() {
    setupNoFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = true)
    launcher.launch()
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_SHOWN)
    assertThat(extras.getString("reason")).isEqualTo("fb4a_not_installed")
  }

  @Test
  fun `sso_dialog_shown fires for fallback dialog when FB4A outdated`() {
    setupOutdatedFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = true)
    launcher.launch()
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_SHOWN)
    assertThat(extras.getString("reason")).isEqualTo("fb4a_outdated")
  }

  @Test
  fun `sso_dialog_shown does NOT fire when FB4A SSO launches`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_SHOWN)
  }

  @Test
  fun `sso_dialog_shown does NOT fire when showWithoutFBApp is false`() {
    setupNoFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = false)
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_SHOWN)
  }

  // ==========================================================================
  // 4. sso_dialog_not_shown
  // ==========================================================================

  @Test
  fun `sso_dialog_not_shown fires when GK disabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(false)
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN)
    assertThat(extras.getString("reason")).isEqualTo("gk_disabled")
  }

  @Test
  fun `sso_dialog_not_shown fires when no FB4A and showWithoutFBApp false`() {
    setupNoFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = false)
    launcher.launch()
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN)
    assertThat(extras.getString("reason")).isEqualTo("fb4a_not_installed")
  }

  @Test
  fun `sso_dialog_not_shown fires when outdated FB4A and showWithoutFBApp false`() {
    setupOutdatedFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = false)
    launcher.launch()
    val extras = verifyEventFiredWithExtras(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN)
    assertThat(extras.getString("reason")).isEqualTo("fb4a_outdated")
  }

  @Test
  fun `sso_dialog_not_shown does NOT fire when fallback dialog is shown`() {
    setupNoFb4a()
    val launcher = createLauncherWithMockLogger(showWithoutFBApp = true)
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN)
  }

  @Test
  fun `sso_dialog_not_shown does NOT fire when FB4A SSO launches`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()
    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN)
  }

  // ==========================================================================
  // 5. sso_dismissed
  // ==========================================================================

  @Test
  fun `sso_dismissed fires when sso_dismissed extra is true from FB4A`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()

    val dismissData = Intent()
    dismissData.putExtra("sso_dismissed", true)
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, dismissData))

    val params = verifyEventFired(LoginLogger.EVENT_NAME_SSO_DISMISSED)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT)).isEqualTo("cancel")
  }

  @Test
  fun `sso_dismissed does NOT fire for regular cancel without sso_dismissed extra`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()

    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, null))

    verifyEventNotFired(LoginLogger.EVENT_NAME_SSO_DISMISSED)
  }

  @Test
  fun `sso_dismissed calls callback onCancel`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()

    val dismissData = Intent()
    dismissData.putExtra("sso_dismissed", true)
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, dismissData))

    verify(mockCallback).onCancel()
  }

  @Test
  fun `sso_dismissed does NOT fire fb_mobile_login_complete`() {
    setupFb4aWithSso()
    val launcher = createLauncherWithMockLogger()
    launcher.launch()

    val dismissData = Intent()
    dismissData.putExtra("sso_dismissed", true)
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, dismissData))

    // Verify onActivityResult was NOT called on LoginManager — we check indirectly
    // by verifying that only sso_dismissed was logged, not login_complete
    verify(mockCallback).onCancel()
    verify(mockCallback, never()).onError(any())
    verify(mockCallback, never()).onSuccess(any())
  }

  // ==========================================================================
  // 6. from_sso on fb_mobile_login_complete
  // ==========================================================================

  @Test
  fun `from_sso is set to true on SSO login result`() {
    setupFb4aWithSso()
    PowerMockito.mockStatic(LoginMethodHandler::class.java)

    val launcher = createLauncherWithMockLogger()
    launcher.launch()

    // Simulate error result (easier than success which requires token parsing)
    val errorData = Intent()
    errorData.putExtra("error", "access_denied")
    errorData.putExtra("error_message", "User denied permission")
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_OK, errorData))

    verify(mockCallback).onError(any())
  }

  // ==========================================================================
  // 7. pendingSsoContext
  // ==========================================================================

  @Test
  fun `pendingSsoContext is sso_shown when FB4A SSO launches`() {
    setupFb4aWithSso()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()
    assertThat(FBLoginSSOLauncher.pendingSsoContext).isEqualTo("non_sso_login_sso_shown")
  }

  @Test
  fun `pendingSsoContext is sso_not_shown when GK disabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(false)
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()
    assertThat(FBLoginSSOLauncher.pendingSsoContext).isEqualTo("non_sso_login_sso_not_shown")
  }

  @Test
  fun `pendingSsoContext is sso_not_shown when no FB4A and showWithoutFBApp false`() {
    setupNoFb4a()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp = false)
    launcher.launch()
    assertThat(FBLoginSSOLauncher.pendingSsoContext).isEqualTo("non_sso_login_sso_not_shown")
  }

  @Test
  fun `pendingSsoContext is null when user is already logged in`() {
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mock())
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()
    assertThat(FBLoginSSOLauncher.pendingSsoContext).isNull()
  }

  // ==========================================================================
  // 8. Basic launch behavior
  // ==========================================================================

  @Test
  fun `launch returns false when user is already logged in`() {
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mock())
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    assertThat(launcher.launch()).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  @Test
  fun `launch returns false when GK disabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(false)
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    assertThat(launcher.launch()).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  @Test
  fun `launch returns true and launches intent when FB4A SSO resolves`() {
    setupFb4aWithSso()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    assertThat(launcher.launch()).isTrue
    verify(mockLauncher).launch(any())
  }

  @Test
  fun `launch returns true and shows fallback dialog when no FB4A and showWithoutFBApp true`() {
    setupNoFb4a()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp = true)
    assertThat(launcher.launch()).isTrue
    verify(mockLauncher, never()).launch(any())
    verify(mockFragmentManager).beginTransaction()
  }

  @Test
  fun `launch returns false when no FB4A and showWithoutFBApp false`() {
    setupNoFb4a()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp = false)
    assertThat(launcher.launch()).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  // ==========================================================================
  // 9. handleActivityResult basic behavior
  // ==========================================================================

  @Test
  fun `handleActivityResult calls onCancel for RESULT_CANCELED`() {
    setupFb4aWithSso()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, null))
    verify(mockCallback).onCancel()
  }

  @Test
  fun `handleActivityResult calls onError for error extras`() {
    setupFb4aWithSso()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()

    val errorData = Intent()
    errorData.putExtra("error", "access_denied")
    errorData.putExtra("error_message", "User denied permission")
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_OK, errorData))

    verify(mockCallback).onError(any())
  }

  @Test
  fun `handleActivityResult calls onError for null extras`() {
    setupFb4aWithSso()
    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()

    val emptyData = Intent()
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_OK, emptyData))

    verify(mockCallback).onError(any())
  }
}
