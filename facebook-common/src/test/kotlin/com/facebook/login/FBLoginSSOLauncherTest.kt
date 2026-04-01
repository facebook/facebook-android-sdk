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
import com.facebook.internal.FeatureManager
import com.facebook.internal.NativeProtocol
import com.facebook.internal.security.OidcSecurityUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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

    // Capture the ActivityResultCallback passed to registerForActivityResult
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

    // Mock FacebookSdk
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getRedirectURI()).thenReturn("https://example.com/redirect")
    whenever(FacebookSdk.getIntentUriPackageTarget()).thenReturn(null)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mock())
    whenever(FacebookSdk.getApplicationContext().mainLooper).thenReturn(mockLooper)
    whenever(FacebookSdk.getApplicationContext().getSharedPreferences(any<String>(), any()))
        .thenReturn(MockSharedPreference())

    // Mock FeatureManager
    PowerMockito.mockStatic(FeatureManager::class.java)

    // Mock token companions to avoid NPEs during LoginManager operations
    mockAccessTokenCompanion = mock()
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(null)
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)

    mockAuthenticationTokenCompanion = mock()
    whenever(mockAuthenticationTokenCompanion.getCurrentAuthenticationToken()).thenReturn(null)
    Whitebox.setInternalState(
        AuthenticationToken::class.java, "Companion", mockAuthenticationTokenCompanion)

    mockProfileCompanion = mock()
    Whitebox.setInternalState(Profile::class.java, "Companion", mockProfileCompanion)

    // Mock OIDC verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any())).thenReturn(mock())
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
  }

  @Test
  fun `launch returns false when user is already logged in`() {
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mock())
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    val result = launcher.launch()

    assertThat(result).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  @Test
  fun `launch returns false when LoginSSO feature is disabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(false)

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    val result = launcher.launch()

    assertThat(result).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  @Test
  fun `launch returns true and launches intent when activity is resolvable`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(ResolveInfo())

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    val result = launcher.launch()

    assertThat(result).isTrue
    verify(mockLauncher).launch(any())
  }

  @Test
  fun `launch shows no-app dialog when no resolvable intent and showWithoutFBApp is true`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(null)

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp = true)
    val result = launcher.launch()

    assertThat(result).isTrue
    verify(mockLauncher, never()).launch(any())
    verify(mockFragmentManager).beginTransaction()
  }

  @Test
  fun `launch returns false when no resolvable intent and showWithoutFBApp is false`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(null)

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback, showWithoutFBApp = false)
    val result = launcher.launch()

    assertThat(result).isFalse
    verify(mockLauncher, never()).launch(any())
  }

  @Test
  fun `handleActivityResult calls onCancel when result is RESULT_CANCELED`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(ResolveInfo())

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()

    // Simulate cancel result
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_CANCELED, null))

    verify(mockCallback).onCancel()
  }

  @Test
  fun `handleActivityResult calls onError when extras contain error fields`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(ResolveInfo())

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()

    // Simulate error result
    val errorData = Intent()
    errorData.putExtra("error", "access_denied")
    errorData.putExtra("error_message", "User denied permission")
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_OK, errorData))

    verify(mockCallback).onError(any())
  }

  @Test
  fun `handleActivityResult calls onError when RESULT_OK but extras are null`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)).thenReturn(true)
    whenever(mockPackageManager.resolveActivity(any(), any<Int>()))
        .thenReturn(ResolveInfo())

    val launcher = FBLoginSSOLauncher(mockActivity, mockCallback)
    launcher.launch()

    // Simulate OK result with no extras
    val emptyData = Intent()
    capturedResultCallback?.onActivityResult(ActivityResult(Activity.RESULT_OK, emptyData))

    verify(mockCallback).onError(any())
  }
}
