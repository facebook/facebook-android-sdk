/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.login.DefaultAudience
import com.facebook.util.common.AuthenticationTokenTestUtil
import java.util.TreeSet
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, FacebookSignatureValidator::class)
class NativeProtocolTest : FacebookPowerMockTestCase() {

  private val mockAppID = "123456789"
  private val mockPackageName = "com.example"
  private lateinit var mockCallId: String
  private lateinit var mockNativeAppInfo: NativeProtocol.NativeAppInfo

  override fun setup() {
    super.setup()
    mockNativeAppInfo = mock()
    val mockAvailableVersions = TreeSet<Int>()
    mockAvailableVersions.add(NativeProtocol.PROTOCOL_VERSION_20210906)
    whenever(mockNativeAppInfo.getAvailableVersions()).thenReturn(mockAvailableVersions)
    whenever(mockNativeAppInfo.getPackage()).thenReturn("com.facebook.test")
    whenever(mockNativeAppInfo.getLoginActivity()).thenReturn("com.facebook.test.LoginActivity")
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    whenever(FacebookSdk.getApplicationName()).thenReturn(mockPackageName)
    mockCallId = UUID.randomUUID().toString()
  }

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

  @Test
  fun `test create platform activity intent with valid native app`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val extras = Bundle()
    extras.putString("test_param_field", "test_param_value")
    val versionResult =
        NativeProtocol.ProtocolVersionQueryResult.create(
            mockNativeAppInfo, NativeProtocol.PROTOCOL_VERSION_20210906)

    val intent =
        NativeProtocol.createPlatformActivityIntent(
            mockContext, mockCallId, NativeProtocol.ACTION_LIKE_DIALOG, versionResult, extras)

    checkNotNull(intent)
    assertThat(intent.action).isEqualTo(NativeProtocol.INTENT_ACTION_PLATFORM_ACTIVITY)
    assertThat(intent.`package`).isEqualTo(mockNativeAppInfo.getPackage())
    assertThat(intent.getStringExtra(NativeProtocol.EXTRA_APPLICATION_ID)).isEqualTo(mockAppID)
    val protocolMethodArgs =
        checkNotNull(intent.getBundleExtra(NativeProtocol.EXTRA_PROTOCOL_METHOD_ARGS))
    assertThat(protocolMethodArgs.getString("test_param_field")).isEqualTo("test_param_value")
  }

  @Test
  fun `test create platform activity intent without native app query result`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)

    val intent =
        NativeProtocol.createPlatformActivityIntent(
            mockContext, mockCallId, NativeProtocol.ACTION_LIKE_DIALOG, null, null)

    assertThat(intent).isNull()
  }

  @Test
  fun `test create platform activity intent with no native app in the query result`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val emptyResult = NativeProtocol.ProtocolVersionQueryResult.createEmpty()

    val intent =
        NativeProtocol.createPlatformActivityIntent(
            mockContext, mockCallId, NativeProtocol.ACTION_LIKE_DIALOG, emptyResult, null)

    assertThat(intent).isNull()
  }

  @Test
  fun `test create protocol result intent with results`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val extras = Bundle()
    extras.putString("test_param_field", "test_param_value")
    val versionResult =
        NativeProtocol.ProtocolVersionQueryResult.create(
            mockNativeAppInfo, NativeProtocol.PROTOCOL_VERSION_20210906)
    val requestIntent =
        NativeProtocol.createPlatformActivityIntent(
            mockContext, mockCallId, NativeProtocol.ACTION_LIKE_DIALOG, versionResult, extras)
    val results = Bundle()
    results.putString("test_result_field", "test_result_value")

    val resultIntent =
        checkNotNull(
            NativeProtocol.createProtocolResultIntent(checkNotNull(requestIntent), results, null))

    assertThat(resultIntent.getIntExtra(NativeProtocol.EXTRA_PROTOCOL_VERSION, 0))
        .isEqualTo(versionResult.protocolVersion)
    assertThat(
            resultIntent
                .getBundleExtra(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS)
                ?.getString(NativeProtocol.BRIDGE_ARG_ACTION_ID_STRING))
        .isEqualTo(mockCallId)
    assertThat(
            resultIntent
                .getBundleExtra(NativeProtocol.EXTRA_PROTOCOL_METHOD_RESULTS)
                ?.getString("test_result_field"))
        .isEqualTo("test_result_value")
  }

  @Test
  fun `test create protocol result intent with error`() {
    val mockContext = mock<Context>()
    setUpMockingForNativeIntentGeneration(mockContext)
    val extras = Bundle()
    extras.putString("test_param_field", "test_param_value")
    val versionResult =
        NativeProtocol.ProtocolVersionQueryResult.create(
            mockNativeAppInfo, NativeProtocol.PROTOCOL_VERSION_20210906)
    val requestIntent =
        NativeProtocol.createPlatformActivityIntent(
            mockContext, mockCallId, NativeProtocol.ACTION_LIKE_DIALOG, versionResult, extras)
    val error = FacebookException("test error message")

    val resultIntent =
        checkNotNull(
            NativeProtocol.createProtocolResultIntent(checkNotNull(requestIntent), null, error))

    assertThat(resultIntent.getIntExtra(NativeProtocol.EXTRA_PROTOCOL_VERSION, 0))
        .isEqualTo(versionResult.protocolVersion)
    assertThat(
            resultIntent
                .getBundleExtra(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS)
                ?.getString(NativeProtocol.BRIDGE_ARG_ACTION_ID_STRING))
        .isEqualTo(mockCallId)
    val errorBundle =
        resultIntent
            .getBundleExtra(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS)
            ?.getBundle(NativeProtocol.BRIDGE_ARG_ERROR_BUNDLE)
    checkNotNull(errorBundle)
    assertThat(errorBundle.getString(NativeProtocol.BRIDGE_ARG_ERROR_DESCRIPTION))
        .isEqualTo(error.toString())
  }

  @Test
  fun `test parse error data from bridge arg error data bundle`() {
    val data = Bundle()
    val errorType = "TestError"
    val errorDescription = "test error description"
    data.putString(NativeProtocol.BRIDGE_ARG_ERROR_TYPE, errorType)
    data.putString(NativeProtocol.BRIDGE_ARG_ERROR_DESCRIPTION, errorDescription)
    val exception = NativeProtocol.getExceptionFromErrorData(data)
    checkNotNull(exception)
    assertThat(exception.message).isEqualTo(errorDescription)
  }

  @Test
  fun `test parse error data from bridge arg canceled error bundle`() {
    val data = Bundle()
    val errorType = "USERCANCELED" // full capitalized only for testing
    val errorDescription = "test error description"
    data.putString(NativeProtocol.BRIDGE_ARG_ERROR_TYPE, errorType)
    data.putString(NativeProtocol.BRIDGE_ARG_ERROR_DESCRIPTION, errorDescription)
    val exception = NativeProtocol.getExceptionFromErrorData(data)
    checkNotNull(exception)
    assertThat(exception).isInstanceOf(FacebookOperationCanceledException::class.java)
    assertThat(exception.message).isEqualTo(errorDescription)
  }

  @Test
  fun `test recognizing error result intent with bridge arguments and obtain error data`() {
    val errorType = "TestError"
    val errorDescription = "test error description"
    val intent = Intent()
    val bridgeArgs = Bundle()
    val errorBundle = Bundle()
    errorBundle.putString(NativeProtocol.BRIDGE_ARG_ERROR_TYPE, errorType)
    errorBundle.putString(NativeProtocol.BRIDGE_ARG_ERROR_DESCRIPTION, errorDescription)
    bridgeArgs.putBundle(NativeProtocol.BRIDGE_ARG_ERROR_BUNDLE, errorBundle)
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_VERSION, NativeProtocol.PROTOCOL_VERSION_20171115)
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArgs)

    assertThat(NativeProtocol.isErrorResult(intent)).isTrue
    val parsedException =
        NativeProtocol.getExceptionFromErrorData(
            NativeProtocol.getErrorDataFromResultIntent(intent))
    checkNotNull(parsedException)
    assertThat(parsedException.message).isEqualTo(errorDescription)
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
