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
package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import com.facebook.util.common.mockLocalBroadcastManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class, AccessTokenCache::class, AccessTokenManager::class, Utility::class)
class AccessTokenManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val TOKEN_STRING = "A token of my esteem"
    private const val USER_ID = "1000"
    private val PERMISSIONS = listOf("walk", "chew gum")
    private val EXPIRES = Date(2025, 5, 3)
    private val LAST_REFRESH = Date(2023, 8, 15)
    private val DATA_ACCESS_EXPIRATION_TIME = Date(2025, 5, 3)
    private const val APP_ID = "1234"
  }
  private lateinit var localBroadcastManager: LocalBroadcastManager
  private lateinit var accessTokenCache: AccessTokenCache

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    MemberModifier.suppress(MemberMatcher.method(Utility::class.java, "clearFacebookCookies"))
    accessTokenCache = mock()
    localBroadcastManager = mockLocalBroadcastManager(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testDefaultsToNoCurrentAccessToken() {
    val accessTokenManager = createAccessTokenManager()
    Assert.assertNull(accessTokenManager.currentAccessToken)
  }

  @Test
  fun testCanSetCurrentAccessToken() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    Assert.assertEquals(accessToken, accessTokenManager.currentAccessToken)
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
    Assert.assertNotNull(intent)
    checkNotNull(intent)
    val oldAccessToken: AccessToken =
        checkNotNull(intent.getParcelableExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN))
    val newAccessToken: AccessToken =
        checkNotNull(intent.getParcelableExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN))
    Assert.assertEquals(accessToken.token, oldAccessToken.token)
    Assert.assertEquals(anotherAccessToken.token, newAccessToken.token)
  }

  @Test
  fun testLoadReturnsFalseIfNoCachedToken() {
    val accessTokenManager = createAccessTokenManager()
    val result = accessTokenManager.loadCurrentAccessToken()
    Assert.assertFalse(result)
  }

  @Test
  fun testLoadReturnsTrueIfCachedToken() {
    val accessToken = createAccessToken()
    PowerMockito.`when`(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    val result = accessTokenManager.loadCurrentAccessToken()
    Assert.assertTrue(result)
  }

  @Test
  fun testLoadSetsCurrentTokenIfCached() {
    val accessToken = createAccessToken()
    PowerMockito.`when`(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()
    Assert.assertEquals(accessToken, accessTokenManager.currentAccessToken)
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
    PowerMockito.`when`(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()
    verify(accessTokenCache, never()).save(any())
  }

  @Test
  fun testRefreshingAccessTokenDoesSendRequests() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    val mockGraphRequestCompanionObject = mock<GraphRequest.Companion>()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
    accessTokenManager.refreshCurrentAccessToken(null)
    verify(mockGraphRequestCompanionObject, times(1)).executeBatchAsync(isA<GraphRequestBatch>())
  }

  @Test
  fun testRefreshingAccessTokenBlocksSecondAttempt() {
    val accessTokenManager = createAccessTokenManager()
    val accessToken = createAccessToken()
    accessTokenManager.currentAccessToken = accessToken
    val mockGraphRequestCompanionObject = mock<GraphRequest.Companion>()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
    accessTokenManager.refreshCurrentAccessToken(null)
    var capturedException: FacebookException? = null
    accessTokenManager.refreshCurrentAccessToken(
        object : AccessToken.AccessTokenRefreshCallback {
          override fun OnTokenRefreshed(accessToken: AccessToken?) {
            Assert.fail()
          }
          override fun OnTokenRefreshFailed(exception: FacebookException?) {
            capturedException = exception
          }
        })
    Assert.assertNotNull(capturedException)
  }

  @Test
  fun testExtendFBAccessToken() {
    val accessToken = createAccessToken()
    PowerMockito.`when`(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()

    val mockGraphRequestCompanionObject = mock<GraphRequest.Companion>()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
    PowerMockito.whenNew(GraphRequest::class.java)
        .withAnyArguments()
        .thenReturn(mock<GraphRequest>())
    whenever(mockGraphRequestCompanionObject.executeBatchAsync(any<GraphRequestBatch>()))
        .thenReturn(mock<GraphRequestAsyncTask>())
    val bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle::class.java)

    accessTokenManager.refreshCurrentAccessToken(null)

    PowerMockito.verifyNew(GraphRequest::class.java)
        .withArguments(
            eq(accessToken),
            eq("me/permissions"),
            any(),
            eq(HttpMethod.GET),
            any(),
            isNull(),
            any<Int>(),
            isNull()) // @JvmOverloads adds extra arguments
    PowerMockito.verifyNew(GraphRequest::class.java)
        .withArguments(
            eq(accessToken),
            eq("oauth/access_token"),
            bundleArgumentCaptor.capture(),
            eq(HttpMethod.GET),
            any(),
            isNull(),
            any<Int>(),
            isNull()) // @JvmOverloads adds extra arguments
    val parameters = bundleArgumentCaptor.getValue()
    Assert.assertEquals("fb_extend_sso_token", parameters.getString("grant_type"))
  }

  @Test
  fun testExtendIGAccessToken() {
    val accessToken = createAccessToken(TOKEN_STRING, USER_ID, "instagram")
    PowerMockito.`when`(accessTokenCache.load()).thenReturn(accessToken)
    val accessTokenManager = createAccessTokenManager()
    accessTokenManager.loadCurrentAccessToken()

    val mockGraphRequestCompanionObject = mock<GraphRequest.Companion>()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
    PowerMockito.whenNew(GraphRequest::class.java)
        .withAnyArguments()
        .thenReturn(mock<GraphRequest>())
    whenever(mockGraphRequestCompanionObject.executeBatchAsync(any<GraphRequestBatch>()))
        .thenReturn(mock<GraphRequestAsyncTask>())
    val bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle::class.java)

    accessTokenManager.refreshCurrentAccessToken(null)

    PowerMockito.verifyNew(GraphRequest::class.java)
        .withArguments(
            eq(accessToken),
            eq("me/permissions"),
            any(),
            eq(HttpMethod.GET),
            any(),
            isNull(),
            any<Int>(),
            isNull()) // @JvmOverloads adds extra arguments
    PowerMockito.verifyNew(GraphRequest::class.java)
        .withArguments(
            eq(accessToken),
            eq("refresh_access_token"),
            bundleArgumentCaptor.capture(),
            eq(HttpMethod.GET),
            any(),
            isNull(),
            any<Int>(),
            isNull()) // @JvmOverloads adds extra arguments
    val parameters = bundleArgumentCaptor.getValue()
    Assert.assertEquals("ig_refresh_token", parameters.getString("grant_type"))
  }

  private fun createAccessTokenManager(): AccessTokenManager {
    return AccessTokenManager(localBroadcastManager, accessTokenCache)
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
