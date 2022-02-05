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

package com.facebook.internal

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import com.facebook.FacebookPowerMockTestCase
import com.facebook.login.DefaultAudience
import com.facebook.util.common.AuthenticationTokenTestUtil
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.TreeSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSignatureValidator::class)
class NativeProtocolTest : FacebookPowerMockTestCase() {

  private val mockAppID = "123456789"
  private val mockPackageName = "com.example"

  @Test
  fun `sdk version older than app with version spec open`() {
    // Base case where a feature was enabled a while ago and the SDK and Native app have been
    // updated since then.
    val versionSpec = intArrayOf(3)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `sdk version newer than app with version spec open`() {
    // Base case where a feature was enabled a while ago and the SDK and Native app have been
    // updated since then.
    val versionSpec = intArrayOf(3)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `sdk version older than app with version spec disabled`() {
    // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have
    // been updated since then.
    val versionSpec = intArrayOf(1, 3, 7, 8)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `sdk version newer than app with version spec disabled`() {
    // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have
    // been updated since then.
    val versionSpec = intArrayOf(1, 3, 6, 7)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `test sdk version older than app with version spec newer and enabled`() {
    // Case where the sdk and app are older, but the app is still enabled
    val versionSpec = intArrayOf(1, 3, 7, 9, 10, 11, 12, 13)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `test sdk version newer than app with version spec newer and enabled`() {
    // Case where the sdk and app are older, but the app is still enabled
    val versionSpec = intArrayOf(1, 3, 7, 9, 10, 11, 12, 13)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `test sdk version older than app with version spec newer and disabled`() {
    // Case where the sdk and app are older, and the app is a disabled version
    val versionSpec = intArrayOf(1, 3, 7, 8, 10, 11, 12, 13)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `test sdk version newer than app with version spec newer and disabled`() {
    // Case where the sdk and app are older, and the app is a disabled version
    val versionSpec = intArrayOf(1, 3, 6, 7, 10, 11, 12, 13)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  fun getTreeSetFromIntArray(array: IntArray): TreeSet<Int> {
    val treeSet = TreeSet<Int>()
    for (a in array) {
      treeSet.add(a)
    }
    return treeSet
  }

  @Test
  fun `native intent generation for FB app`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val intents =
        NativeProtocol.createProxyAuthIntents(
            mockContext,
            mockAppID,
            listOf<String>(), // permissions
            "", // e2e
            false, // isRerequest
            false, // isForPublish
            DefaultAudience.FRIENDS, // defaultAudience
            "", // clientState
            "", // authType
            false, // ignoreAppSwitchToLoggedOut
            null, // messengerPageId
            false, // resetMessengerState
            false, // isFamilyLogin
            false, // shouldSkipAccountDedupe
            AuthenticationTokenTestUtil.NONCE,
            "codeChallenge",
            "S256")

    assertThat(intents.size).isEqualTo(2)
    val katanaIntent = intents[0]
    assertThat(katanaIntent.getComponent()?.getClassName())
        .isEqualTo("com.facebook.katana.ProxyAuth")
    assertThat(katanaIntent.getStringExtra(NativeProtocol.FACEBOOK_PROXY_AUTH_APP_ID_KEY))
        .isEqualTo(mockAppID)
    assertThat(katanaIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE))
        .isEqualTo(ServerProtocol.DIALOG_RESPONSE_TYPE_ID_TOKEN_AND_SIGNED_REQUEST)
    assertThat(katanaIntent.getBooleanExtra(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, false))
        .isFalse()
    // TODO T111412069
    //    assertThat(katanaIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
    //        .isEqualTo("S256")

    val wakizashiIntent = intents[1]
    assertThat(wakizashiIntent.getComponent()?.getClassName())
        .isEqualTo("com.facebook.katana.ProxyAuth")
    assertThat(wakizashiIntent.getStringExtra(NativeProtocol.FACEBOOK_PROXY_AUTH_APP_ID_KEY))
        .isEqualTo(mockAppID)
    assertThat(wakizashiIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE))
        .isEqualTo(ServerProtocol.DIALOG_RESPONSE_TYPE_ID_TOKEN_AND_SIGNED_REQUEST)
    assertThat(wakizashiIntent.getBooleanExtra(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, false))
        .isFalse()
    // TODO T111412069
    // assertThat(wakizashiIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
    //        .isEqualTo("S256")
  }

  // TODO T111412069
  //  @Test
  //  fun `native intent generation for FB app without code challenge method will use S256 as the
  // default method`() {
  //    val mockContext = mock<Context>()
  //    setUpMockingForNativeIntentGeneration(mockContext)
  //    val intents =
  //        NativeProtocol.createProxyAuthIntents(
  //            mockContext,
  //            mockAppID,
  //            listOf<String>(), // permissions
  //            "", // e2e
  //            false, // isRerequest
  //            false, // isForPublish
  //            DefaultAudience.FRIENDS, // defaultAudience
  //            "", // clientState
  //            "", // authType
  //            false, // ignoreAppSwitchToLoggedOut
  //            null, // messengerPageId
  //            false, // resetMessengerState
  //            false, // isFamilyLogin
  //            false, // shouldSkipAccountDedupe
  //            AuthenticationTokenTestUtil.NONCE,
  //            "codeChallenge")
  //
  //    assertThat(intents.size).isEqualTo(2)
  //    val katanaIntent = intents[0]
  //    assertThat(katanaIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
  //        .isEqualTo("S256")
  //
  //    val wakizashiIntent = intents[1]
  //
  // assertThat(wakizashiIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
  //        .isEqualTo("S256")
  //  }

  @Test
  fun `native intent generation for FB app with null code challenge method will not have code challenge method`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val intents =
        NativeProtocol.createProxyAuthIntents(
            mockContext,
            mockAppID,
            listOf<String>(), // permissions
            "", // e2e
            false, // isRerequest
            false, // isForPublish
            DefaultAudience.FRIENDS, // defaultAudience
            "", // clientState
            "", // authType
            false, // ignoreAppSwitchToLoggedOut
            null, // messengerPageId
            false, // resetMessengerState
            false, // isFamilyLogin
            false, // shouldSkipAccountDedupe
            AuthenticationTokenTestUtil.NONCE,
            null)

    assertThat(intents.size).isEqualTo(2)
    val katanaIntent = intents[0]
    assertThat(katanaIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
        .isNull()

    val wakizashiIntent = intents[1]
    assertThat(wakizashiIntent.getStringExtra(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD))
        .isNull()
  }

  @Test
  fun `native intent generation for IG app`() {
    val mockMessengerPageId = "123456789"
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val instagramIntent =
        NativeProtocol.createInstagramIntent(
            mockContext,
            mockAppID,
            listOf<String>(), // permissions
            "", // e2e
            false, // isRerequest
            false, // isForPublish
            DefaultAudience.FRIENDS, // defaultAudience
            "", // clientState
            "", // authType
            mockMessengerPageId, // messengerPageId
            true, // resetMessengerState
            true, // isFamilyLogin
            true) // shouldSkipAccountDedupe

    assertThat(instagramIntent?.getComponent()?.getClassName())
        .isEqualTo("com.instagram.platform.AppAuthorizeActivity")
    assertThat(instagramIntent?.getStringExtra(NativeProtocol.FACEBOOK_PROXY_AUTH_APP_ID_KEY))
        .isEqualTo(mockAppID)
    assertThat(instagramIntent?.getStringExtra(ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE))
        .isEqualTo(ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES)
    assertThat(instagramIntent?.getBooleanExtra(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, false))
        .isTrue()
    assertThat(instagramIntent?.getStringExtra(ServerProtocol.DIALOG_PARAM_MESSENGER_PAGE_ID))
        .isEqualTo(mockMessengerPageId)
    assertThat(
            instagramIntent?.getBooleanExtra(
                ServerProtocol.DIALOG_PARAM_RESET_MESSENGER_STATE, false))
        .isTrue()
  }

  @Test
  fun `test validate null intent for service intent`() {
    val mockContext = mock<Context>()
    setUpMockingForServiceIntentGeneration(mockContext)
    assertThat(NativeProtocol.validateServiceIntent(mockContext, null, mock())).isNull()
  }

  @Test
  fun `test validate service intent when signature validation passes`() {
    val mockContext = mock<Context>()
    setUpMockingForServiceIntentGeneration(mockContext)
    val intent = mock<Intent>()
    assertThat(NativeProtocol.validateServiceIntent(mockContext, intent, mock())).isEqualTo(intent)
  }

  @Test
  fun `test validate service intent when signature validation fails`() {
    val mockContext = mock<Context>()
    setUpMockingForServiceIntentGeneration(mockContext, signatureValidationResult = false)
    val intent = mock<Intent>()
    assertThat(NativeProtocol.validateServiceIntent(mockContext, intent, mock())).isNull()
  }

  @Test
  fun `test validate null intent for activity intent`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    assertThat(NativeProtocol.validateServiceIntent(mockContext, null, mock())).isNull()
  }

  @Test
  fun `test validate activity intent when signature validation passes`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val intent = mock<Intent>()
    assertThat(NativeProtocol.validateActivityIntent(mockContext, intent, mock())).isEqualTo(intent)
  }

  @Test
  fun `test validate activity intent when signature validation fails`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext, false)
    val intent = mock<Intent>()
    assertThat(NativeProtocol.validateActivityIntent(mockContext, intent, mock())).isNull()
  }

  @Test
  fun `test create token refresh intent from a valid context`() {
    val mockContext = mock<Context>()
    setUpMockingForServiceIntentGeneration(mockContext)
    assertThat(NativeProtocol.createPlatformServiceIntent(mockContext)).isNotNull
  }

  @Test
  fun `test latest known protocol version is compatible with bucketed intent`() {
    val latestVersion = NativeProtocol.getLatestKnownVersion()
    assertThat(NativeProtocol.isVersionCompatibleWithBucketedIntent(latestVersion))
  }

  @Test
  fun `test create token refresh intent from a context without service intent available`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    assertThat(NativeProtocol.createPlatformServiceIntent(mockContext)).isNull()
  }

  fun setUpMockingForNativeIntentGeneration(
      mockContext: Context,
      signatureValidationResult: Boolean = true
  ) {
    PowerMockito.mockStatic(FacebookSignatureValidator::class.java)
    whenever(FacebookSignatureValidator.validateSignature(any(), any()))
        .thenReturn(signatureValidationResult)
    val mockPackageManager = mock<PackageManager>()
    val mockResolveInfo = mock<ResolveInfo>()
    val mockActivityInfo = mock<ActivityInfo>()
    mockActivityInfo.packageName = mockPackageName
    mockResolveInfo.activityInfo = mockActivityInfo
    whenever(mockContext.getPackageManager()).thenReturn(mockPackageManager)
    whenever(mockPackageManager.resolveActivity(any(), any())).thenReturn(mockResolveInfo)
  }

  fun setUpMockingForServiceIntentGeneration(
      mockContext: Context,
      signatureValidationResult: Boolean = true
  ) {
    PowerMockito.mockStatic(FacebookSignatureValidator::class.java)
    whenever(FacebookSignatureValidator.validateSignature(any(), any()))
        .thenReturn(signatureValidationResult)
    val mockPackageManager = mock<PackageManager>()
    val mockResolveInfo = mock<ResolveInfo>()
    val mockServiceInfo = mock<ServiceInfo>()
    mockServiceInfo.packageName = mockPackageName
    mockResolveInfo.serviceInfo = mockServiceInfo
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockPackageManager.resolveService(any(), any())).thenReturn(mockResolveInfo)
  }
}
