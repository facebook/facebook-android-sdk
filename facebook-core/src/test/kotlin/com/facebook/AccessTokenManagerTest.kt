/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.FeatureManager
import com.facebook.internal.Utility
import com.facebook.util.common.mockLocalBroadcastManager
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@PrepareForTest(
    FacebookSdk::class,
    AccessTokenCache::class,
    Utility::class,
    LocalBroadcastManager::class,
    FeatureManager::class)
class AccessTokenManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val TOKEN_STRING = "A token of my esteem"
    private const val USER_ID = "1000"
    private const val PACKAGE_NAME = "com.testapp"
    private val PERMISSIONS = listOf("walk", "chew gum")
    private val EXPIRES = Date(2_025, 5, 3)
    private val LAST_REFRESH = Date(2_023, 8, 15)
    private val DATA_ACCESS_EXPIRATION_TIME = Date(2_025, 5, 3)
    private const val APP_ID = "1234"
  }
  private lateinit var localBroadcastManager: LocalBroadcastManager
  private lateinit var accessTokenCache: AccessTokenCache
  private lateinit var mockGraphRequestCompanionObject: GraphRequest.Companion
  private lateinit var mockGraphRequest: GraphRequest
  private lateinit var mockApplicationContext: Context
  private lateinit var mockAlarmManager: AlarmManager

  override fun setup() {
    super.setup()
    mockApplicationContext = mock()
    whenever(mockApplicationContext.applicationContext).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.packageName).thenReturn(PACKAGE_NAME)
    mockAlarmManager = mock()
    whenever(mockApplicationContext.getSystemService(Context.ALARM_SERVICE))
        .thenReturn(mockAlarmManager)

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)

    MemberModifier.suppress(MemberMatcher.method(Utility::class.java, "clearFacebookCookies"))
    accessTokenCache = mock()
    localBroadcastManager = mockLocalBroadcastManager(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any())).thenReturn(localBroadcastManager)
    mockGraphRequestCompanionObject = mock()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
    mockGraphRequest = mock()
    whenever(mockGraphRequestCompanionObject.newGraphPathRequest(any(), any(), any()))
        .thenReturn(mockGraphRequest)
    PowerMockito.mockStatic(FeatureManager::class.java)
  }

  @Test
  fun testDefaultsToNoCurrentAccessToken() {
    val accessTokenManager = createAccessTokenManager()
    assertThat(accessTokenManager.currentAccessToken).isNull()
  }

  @Test
  fun testCanSetCurrentAccessToken() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    assertThat(accessTokenManager.currentAccessToken).isEqualTo(accessToken)
  }

  @Test
  @Config(sdk = [21])
  fun `test set current access token will register an expiration pending intent without immutable flag on API lower than 23`() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    val pendingIntentCaptor = argumentCaptor<PendingIntent>()

    accessTokenManager.currentAccessToken = accessToken

    verify(mockAlarmManager).set(anyOrNull(), anyOrNull(), pendingIntentCaptor.capture())
    val capturedPendingIntent = pendingIntentCaptor.firstValue
    val shadowPendingIntent = shadowOf(capturedPendingIntent)
    assertThat(shadowPendingIntent.savedIntent.component?.packageName).isEqualTo(PACKAGE_NAME)
    assertThat(shadowPendingIntent.savedIntent.component?.className)
        .isEqualTo(CurrentAccessTokenExpirationBroadcastReceiver::class.java.name)
    assertThat(shadowPendingIntent.flags and PendingIntent.FLAG_IMMUTABLE).isEqualTo(0)
  }

  @Test
  @Config(sdk = [23])
  fun `test set current access token will register an expiration pending intent with correct flag on API 23+`() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    val pendingIntentCaptor = argumentCaptor<PendingIntent>()
    accessTokenManager.currentAccessToken = accessToken
    verify(mockAlarmManager).set(anyOrNull(), anyOrNull(), pendingIntentCaptor.capture())
    val capturedPendingIntent = pendingIntentCaptor.firstValue
    val shadowPendingIntent = shadowOf(capturedPendingIntent)
    assertThat(shadowPendingIntent.savedIntent.component?.packageName).isEqualTo(PACKAGE_NAME)
    assertThat(shadowPendingIntent.savedIntent.component?.className)
        .isEqualTo(CurrentAccessTokenExpirationBroadcastReceiver::class.java.name)
    assertThat(shadowPendingIntent.flags and PendingIntent.FLAG_IMMUTABLE)
        .isEqualTo(PendingIntent.FLAG_IMMUTABLE)
  }

  @Test
  fun testChangingAccessTokenSendsBroadcast() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    val intents = arrayOfNulls<Intent>(1)
    val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            intents[0] = intent
          }
        }
    localBroadcastManager.registerReceiver(
        broadcastReceiver, IntentFilter(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED))
    val anotherAccessToken = createAccessToken("another string", "1000")
    accessTokenManager.currentAccessToken = anotherAccessToken
    localBroadcastManager.unregisterReceiver(broadcastReceiver)
    val intent = intents[0]
    checkNotNull(intent)
    val oldAccessToken: AccessToken =
        checkNotNull(intent.getParcelableExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN))
    val newAccessToken: AccessToken =
        checkNotNull(intent.getParcelableExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN))
    assertThat(accessToken.token).isEqualTo(oldAccessToken.token)
    assertThat(anotherAccessToken.token).isEqualTo(newAccessToken.token)
  }

  @Test
  fun testLoadReturnsFalseIfNoCachedToken() {
    val accessTokenManager = createAccessTokenManager()
    assertThat(accessTokenManager.loadCurrentAccessToken()).isFalse
  }

  @Test
  fun testLoadReturnsTrueIfCachedToken() {
    val accessToken = createAccessToken()
    whenever(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    assertThat(accessTokenManager.loadCurrentAccessToken()).isTrue
  }

  @Test
  fun testLoadSetsCurrentTokenIfCached() {
    val accessToken = createAccessToken()
    whenever(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()
    assertThat(accessToken).isEqualTo(accessTokenManager.currentAccessToken)
  }

  @Test
  fun testSaveWritesToCacheIfToken() {
    val accessToken = createAccessToken()
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.currentAccessToken = accessToken
    verify(accessTokenCache, times(1)).save(any())
  }

  @Test
  fun testSetEmptyTokenClearsCache() {
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.currentAccessToken = null
    verify(accessTokenCache, times(1)).clear()
  }

  @Test
  fun testLoadDoesNotSave() {
    val accessToken = createAccessToken()
    whenever(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()
    verify(accessTokenCache, never()).save(any())
  }

  @Test
  fun testRefreshingAccessTokenDoesSendRequests() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    val grantedPermissionsCallbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val extendedTokenCallbackCaptor = argumentCaptor<GraphRequest.Callback>()
    whenever(
            mockGraphRequestCompanionObject.newGraphPathRequest(
                any(), eq("me/permissions"), grantedPermissionsCallbackCaptor.capture()))
        .thenReturn(mock())
    whenever(
            mockGraphRequestCompanionObject.newGraphPathRequest(
                any(), eq("oauth/access_token"), extendedTokenCallbackCaptor.capture()))
        .thenReturn(mock())

    accessTokenManager.currentAccessToken = accessToken
    accessTokenManager.refreshCurrentAccessToken(null)

    // verify the requests are sent
    val requestBatchCaptor = argumentCaptor<GraphRequestBatch>()
    verify(mockGraphRequestCompanionObject, times(1))
        .executeBatchAsync(requestBatchCaptor.capture())
    val capturedRequestBatch = requestBatchCaptor.firstValue
    assertThat(capturedRequestBatch.size).isEqualTo(2)

    // mock the response for granted permissions
    val mockGrantedPermissionsResponse = mock<GraphResponse>()
    val mockGrantedPermissionsResponseData =
        JSONObject(
            """{"data":[{"permission":"walk","status":"granted"},{"permission":"chew gum","status":"declined"}]}""")
    whenever(mockGrantedPermissionsResponse.jsonObject)
        .thenReturn(mockGrantedPermissionsResponseData)
    grantedPermissionsCallbackCaptor.firstValue.onCompleted(mockGrantedPermissionsResponse)

    // mock the response for extend access token request
    val mockExtendedAccessTokenResponse = mock<GraphResponse>()
    val mockExtendedAccessTokenResponseData =
        JSONObject(
            """{"access_token":"new token string","expires_at":${EXPIRES.time},"graph_domain":"facebook"}""")
    whenever(mockExtendedAccessTokenResponse.jsonObject)
        .thenReturn(mockExtendedAccessTokenResponseData)
    extendedTokenCallbackCaptor.firstValue.onCompleted(mockExtendedAccessTokenResponse)

    // invoke the batch callback to finish refreshing
    capturedRequestBatch.callbacks.forEach { it.onBatchCompleted(capturedRequestBatch) }

    val currentAccessToken = checkNotNull(accessTokenManager.currentAccessToken)
    assertThat(currentAccessToken.token).isEqualTo("new token string")
    assertThat(currentAccessToken.permissions).containsExactlyInAnyOrder("walk")
    assertThat(currentAccessToken.declinedPermissions).containsExactlyInAnyOrder("chew gum")
  }

  @Test
  fun testRefreshingAccessTokenBlocksSecondAttempt() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    accessTokenManager.refreshCurrentAccessToken(null)
    var capturedException: FacebookException? = null
    accessTokenManager.refreshCurrentAccessToken(
        object : AccessToken.AccessTokenRefreshCallback {
          override fun OnTokenRefreshed(accessToken: AccessToken?) {
            return fail("AccessToken should not be refresh")
          }
          override fun OnTokenRefreshFailed(exception: FacebookException?) {
            capturedException = exception
          }
        })
    assertThat(capturedException).isNotNull
  }

  @Test
  fun testExtendFBAccessToken() {
    val accessToken = createAccessToken()
    whenever(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()
    whenever(mockGraphRequestCompanionObject.executeBatchAsync(any<GraphRequestBatch>()))
        .thenReturn(mock<GraphRequestAsyncTask>())
    val mockRefreshTokenRequest = mock<GraphRequest>()
    whenever(
            mockGraphRequestCompanionObject.newGraphPathRequest(
                eq(accessToken), eq("oauth/access_token"), any()))
        .thenReturn(mockRefreshTokenRequest)
    val bundleArgumentCaptor = argumentCaptor<Bundle>()

    accessTokenManager.refreshCurrentAccessToken(null)

    verify(mockGraphRequestCompanionObject)
        .newGraphPathRequest(eq(accessToken), eq("me/permissions"), any())
    verify(mockGraphRequestCompanionObject)
        .newGraphPathRequest(eq(accessToken), eq("oauth/access_token"), any())
    verify(mockGraphRequest).httpMethod = HttpMethod.GET
    verify(mockRefreshTokenRequest).httpMethod = HttpMethod.GET
    verify(mockRefreshTokenRequest).parameters = bundleArgumentCaptor.capture()
    val capturedGrantType = bundleArgumentCaptor.firstValue.getString("grant_type")
    assertThat(capturedGrantType).contains("fb_extend_sso_token")
  }

  @Test
  fun testExtendIGAccessToken() {
    val accessToken = createAccessToken(TOKEN_STRING, USER_ID, "instagram")
    whenever(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()

    val mockRefreshTokenRequest = mock<GraphRequest>()
    whenever(
            mockGraphRequestCompanionObject.newGraphPathRequest(
                eq(accessToken), eq("refresh_access_token"), any()))
        .thenReturn(mockRefreshTokenRequest)
    val bundleArgumentCaptor = argumentCaptor<Bundle>()

    accessTokenManager.refreshCurrentAccessToken(null)
    verify(mockGraphRequestCompanionObject)
        .newGraphPathRequest(eq(accessToken), eq("me/permissions"), any())
    verify(mockGraphRequestCompanionObject)
        .newGraphPathRequest(eq(accessToken), eq("refresh_access_token"), any())
    verify(mockGraphRequest).httpMethod = HttpMethod.GET
    verify(mockRefreshTokenRequest).parameters = bundleArgumentCaptor.capture()
    val capturedGrantType = bundleArgumentCaptor.firstValue.getString("grant_type")
    assertThat(capturedGrantType).contains("ig_refresh_token")
  }

  @Test
  fun `test current access token changed will broadcast`() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    val receiver: BroadcastReceiver = mock()
    val intentCaptor = argumentCaptor<Intent>()
    localBroadcastManager.registerReceiver(receiver, mock())
    whenever(localBroadcastManager.sendBroadcast(intentCaptor.capture())).thenReturn(true)

    accessTokenManager.currentAccessTokenChanged()

    val capturedIntent = intentCaptor.firstValue
    assertThat(capturedIntent.action)
        .isEqualTo(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED)
    assertThat(
            capturedIntent.getParcelableExtra<AccessToken>(
                AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN))
        .isEqualTo(accessToken)
    assertThat(
            capturedIntent.getParcelableExtra<AccessToken>(
                AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN))
        .isEqualTo(accessToken)
  }

  @Test
  fun `test getInstance`() {
    val instance = AccessTokenManager.getInstance()
    assertThat(instance).isNotNull
  }

  @Test
  fun `test extendAccessTokenIfNeeded when it should extend`() {
    val mockAccessToken = mock<AccessToken>()
    val mockAccessTokenSource = mock<AccessTokenSource>()
    whenever(mockAccessToken.source).thenReturn(mockAccessTokenSource)
    whenever(mockAccessTokenSource.canExtendToken()).thenReturn(true)
    whenever(mockAccessToken.lastRefresh).thenReturn(Date(1))
    whenever(mockAccessToken.toJSONObject()).thenReturn(JSONObject())
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.currentAccessToken = mockAccessToken
    accessTokenManager.extendAccessTokenIfNeeded()
    verify(mockGraphRequestCompanionObject).executeBatchAsync(any<GraphRequestBatch>())
  }

  @Test
  fun `test extendAccessTokenIfNeeded when the token cannot be extended`() {
    val mockAccessToken = mock<AccessToken>()
    val mockAccessTokenSource = mock<AccessTokenSource>()
    whenever(mockAccessToken.source).thenReturn(mockAccessTokenSource)
    whenever(mockAccessTokenSource.canExtendToken()).thenReturn(false)
    whenever(mockAccessToken.lastRefresh).thenReturn(Date(1))
    whenever(mockAccessToken.toJSONObject()).thenReturn(JSONObject())
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.currentAccessToken = mockAccessToken
    accessTokenManager.extendAccessTokenIfNeeded()
    verify(mockGraphRequestCompanionObject, never()).executeBatchAsync(any<GraphRequestBatch>())
  }

  private fun createAccessTokenManager(): AccessTokenManager {
    val accessTokenManager = AccessTokenManager(localBroadcastManager, accessTokenCache)
    Whitebox.setInternalState(AccessTokenManager::class.java, "instanceField", accessTokenManager)
    return accessTokenManager
  }

  private fun createAccessToken(
      tokenString: String = TOKEN_STRING,
      userId: String = USER_ID,
      graphDomain: String = "facebook"
  ): AccessToken {
    return AccessToken(
        tokenString,
        APP_ID,
        userId,
        PERMISSIONS,
        null,
        null,
        AccessTokenSource.WEB_VIEW,
        EXPIRES,
        LAST_REFRESH,
        DATA_ACCESS_EXPIRATION_TIME,
        graphDomain)
  }
}
