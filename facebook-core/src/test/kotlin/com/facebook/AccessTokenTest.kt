/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(Utility::class, FacebookSdk::class, LocalBroadcastManager::class)
class AccessTokenTest : FacebookPowerMockTestCase() {

  private val mockTokenString = "A token of my esteem"
  private val mockAppID = "1234"
  private val mockUserID = "1000"

  @Before
  fun before() {
    MemberModifier.stub<Any>(
            PowerMockito.method(Utility::class.java, "awaitGetGraphMeRequestWithCache"))
        .toReturn(JSONObject().put("id", mockUserID))

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(Matchers.isA(Context::class.java)))
        .thenReturn(mockLocalBroadcastManager)
  }

  @Test
  fun `test empty token throws`() {
    try {
      val token =
          AccessToken(
              "",
              "1234",
              "1000",
              arrayListOf("something"),
              arrayListOf("something_else"),
              arrayListOf("something_else_e"),
              AccessTokenSource.CLIENT_TOKEN,
              Date(),
              Date(),
              Date())
      Assert.fail()
    } catch (e: IllegalArgumentException) {}
  }

  @Test
  fun `test empty userId throws`() {
    try {
      val token =
          AccessToken(
              "a token",
              "1234",
              "",
              arrayListOf("something"),
              arrayListOf("something_else"),
              arrayListOf("something_else_e"),
              AccessTokenSource.CLIENT_TOKEN,
              Date(),
              Date(),
              Date())
      Assert.fail()
    } catch (e: IllegalArgumentException) {}
  }

  @Test
  fun `test empty application Id throws`() {
    try {
      val token =
          AccessToken(
              "a token",
              "",
              "0000",
              arrayListOf("something"),
              arrayListOf("something_else"),
              arrayListOf("something_else_e"),
              AccessTokenSource.CLIENT_TOKEN,
              Date(),
              Date(),
              Date())
      Assert.fail()
    } catch (e: IllegalArgumentException) {}
  }

  @Test
  fun `test create from refresh failure`() {
    val accessToken =
        AccessToken(
            "a token",
            "1234",
            "1000",
            arrayListOf("stream_publish"),
            null,
            null,
            AccessTokenSource.WEB_VIEW,
            null,
            null,
            null)
    val token = "AnImaginaryTokenValue"
    val bundle = Bundle()
    bundle.putString("access_token", "AnImaginaryTokenValue")
    bundle.putString("expires_in", "60")
    try {
      AccessToken.createFromRefresh(accessToken, bundle)
      Assert.fail("Expected exception")
    } catch (ex: FacebookException) {
      Assert.assertEquals("Invalid token source: " + AccessTokenSource.WEB_VIEW, ex.message)
    }
  }

  @Test
  fun `test cache roundtrip`() {
    val permissions: Set<String> = hashSetOf("stream_publish", "go_outside_and_play")
    val declinedPermissions: Set<String> = hashSetOf("no you may not", "no soup for you")
    val expiredPermissions: Set<String> = hashSetOf("expired", "oh no")
    val token = "AnImaginaryTokenValue"
    val later = FacebookTestUtility.nowPlusSeconds(60)
    val earlier = FacebookTestUtility.nowPlusSeconds(-60)
    val applicationId = "1234"
    val bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, token)
    LegacyTokenHelper.putExpirationDate(bundle, later)
    LegacyTokenHelper.putSource(bundle, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    LegacyTokenHelper.putLastRefreshDate(bundle, earlier)
    LegacyTokenHelper.putPermissions(bundle, permissions)
    LegacyTokenHelper.putDeclinedPermissions(bundle, declinedPermissions)
    LegacyTokenHelper.putExpiredPermissions(bundle, expiredPermissions)
    LegacyTokenHelper.putApplicationId(bundle, applicationId)
    val accessToken = AccessToken.createFromLegacyCache(bundle)
    Assert.assertNotNull(accessToken)
    checkNotNull(accessToken)
    FacebookTestUtility.assertSameCollectionContents(permissions, accessToken.permissions)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
    assertThat(accessToken.isExpired).isFalse
    val cache = AccessTokenTestHelper.toLegacyCacheBundle(accessToken)
    FacebookTestUtility.assertEqualContentsWithoutOrder(bundle, cache)
  }

  @Test
  fun `test from cache with missing application Id`() {
    val token = "AnImaginaryTokenValue"
    val applicationId = "1234"
    val bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, token)
    // no app id
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(applicationId)
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    val accessToken = AccessToken.createFromLegacyCache(bundle)
    Assert.assertNotNull(accessToken)
    checkNotNull(accessToken)
    Assert.assertEquals(applicationId, accessToken.applicationId)
  }

  @Test
  fun `test cache push get`() {
    val bundle = Bundle()
    for (token in arrayOf("", "A completely random token value")) {
      LegacyTokenHelper.putToken(bundle, token)
      Assert.assertEquals(token, LegacyTokenHelper.getToken(bundle))
    }
    for (date in arrayOf(Date(42), Date())) {
      LegacyTokenHelper.putExpirationDate(bundle, date)
      Assert.assertEquals(date, LegacyTokenHelper.getExpirationDate(bundle))
      LegacyTokenHelper.putLastRefreshDate(bundle, date)
      Assert.assertEquals(date, LegacyTokenHelper.getLastRefreshDate(bundle))
    }
    for (milliseconds in longArrayOf(0, -1, System.currentTimeMillis())) {
      LegacyTokenHelper.putExpirationMilliseconds(bundle, milliseconds)
      Assert.assertEquals(milliseconds, LegacyTokenHelper.getExpirationMilliseconds(bundle))
      LegacyTokenHelper.putLastRefreshMilliseconds(bundle, milliseconds)
      Assert.assertEquals(milliseconds, LegacyTokenHelper.getLastRefreshMilliseconds(bundle))
    }
    for (source in AccessTokenSource.values()) {
      LegacyTokenHelper.putSource(bundle, source)
      Assert.assertEquals(source, LegacyTokenHelper.getSource(bundle))
    }
    val normalList = listOf("", "Another completely random token value")
    val emptyList: List<String> = emptyList<String>()
    val normalArrayList = HashSet(normalList)
    val emptyArrayList = HashSet<String>()
    val permissionLists = listOf(normalList, emptyList, normalArrayList, emptyArrayList)
    for (list in permissionLists) {
      LegacyTokenHelper.putPermissions(bundle, list)
      FacebookTestUtility.assertSameCollectionContents(
          list, LegacyTokenHelper.getPermissions(bundle))
    }
    normalArrayList.add(null)
  }

  @Test
  fun `test create from native linking intent with user id`() {
    val intent = Intent()
    val tokenString = "a token"
    val userId = "1234"
    val applicationId = "1000"
    intent.putExtra(AccessToken.ACCESS_TOKEN_KEY, tokenString)
    intent.putExtra(AccessToken.USER_ID_KEY, userId)
    intent.putExtra(AccessToken.EXPIRES_IN_KEY, "0")
    var capturedAccessToken: AccessToken? = null
    AccessToken.createFromNativeLinkingIntent(
        intent,
        applicationId,
        object : AccessToken.AccessTokenCreationCallback {
          override fun onSuccess(token: AccessToken?) {
            capturedAccessToken = token
          }

          override fun onError(error: FacebookException?) = Unit
        })
    Assert.assertNotNull(capturedAccessToken)
    val accessToken = checkNotNull(capturedAccessToken)
    Assert.assertEquals(tokenString, accessToken.token)
    Assert.assertEquals(userId, accessToken.userId)
    Assert.assertEquals(applicationId, accessToken.applicationId)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
  }

  @Test
  fun `test create from native linking intent without user id`() {
    val intent = Intent()
    val tokenString = "a token"
    val userId = "1234"
    val applicationId = "1000"
    var capturedGraphRequestCallback: Utility.GraphMeRequestWithCacheCallback? = null
    PowerMockito.mockStatic(Utility::class.java)
    whenever(Utility.getGraphMeRequestWithCacheAsync(any(), any())).thenAnswer {
      capturedGraphRequestCallback = it.arguments[1] as Utility.GraphMeRequestWithCacheCallback?
      Unit
    }
    whenever(Utility.getBundleLongAsDate(any(), any(), any())).thenCallRealMethod()
    intent.putExtra(AccessToken.ACCESS_TOKEN_KEY, tokenString)
    intent.putExtra(AccessToken.EXPIRES_IN_KEY, "0")
    var capturedAccessToken: AccessToken? = null
    AccessToken.createFromNativeLinkingIntent(
        intent,
        applicationId,
        object : AccessToken.AccessTokenCreationCallback {
          override fun onSuccess(token: AccessToken?) {
            capturedAccessToken = token
          }

          override fun onError(error: FacebookException?) = Unit
        })

    Assert.assertNotNull(capturedGraphRequestCallback)
    val userInfo = JSONObject()
    userInfo.put("id", userId)
    capturedGraphRequestCallback?.onSuccess(userInfo)
    Assert.assertNotNull(capturedAccessToken)
    val accessToken = checkNotNull(capturedAccessToken)
    Assert.assertEquals(tokenString, accessToken.token)
    Assert.assertEquals(userId, accessToken.userId)
    Assert.assertEquals(applicationId, accessToken.applicationId)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
  }

  @Test
  fun `test create from empty intent`() {
    val intent = Intent()
    var capturedError: FacebookException? = null
    AccessToken.createFromNativeLinkingIntent(
        intent,
        "1234",
        object : AccessToken.AccessTokenCreationCallback {
          override fun onSuccess(token: AccessToken?) = Unit

          override fun onError(error: FacebookException?) {
            capturedError = error
          }
        })
    Assert.assertNotNull(capturedError)
  }

  @Test
  fun `test roundtrip JSONObject`() {
    val accessToken =
        AccessToken(
            "a token",
            "1234",
            "1000",
            listOf("permission_1", "permission_2"),
            listOf("declined permission_1", "declined permission_2"),
            listOf("expired permission_1", "expired permission_2"),
            AccessTokenSource.WEB_VIEW,
            Date(2_015, 3, 3),
            Date(2_015, 1, 1),
            Date(2_015, 3, 3))
    val jsonObject = accessToken.toJSONObject()
    val deserializedAccessToken = AccessToken.createFromJSONObject(jsonObject)
    Assert.assertEquals(accessToken, deserializedAccessToken)
  }

  @Test
  fun `test JSONObject without data access or expired permissions`() {
    val accessToken =
        AccessToken(
            "a token",
            "1234",
            "1000",
            listOf("permission_1", "permission_2"),
            listOf("declined permission_1", "declined permission_2"),
            listOf(),
            AccessTokenSource.WEB_VIEW,
            Date(2_015, 3, 3),
            Date(2_015, 1, 1),
            Date(0))
    val jsonObject = accessToken.toJSONObject()
    jsonObject.remove("data_access_expiration_time")
    jsonObject.remove("expired_permissions")
    val deserializedAccessToken = AccessToken.createFromJSONObject(jsonObject)
    Assert.assertEquals(accessToken, deserializedAccessToken)
  }

  @Test
  fun `test parceling`() {
    val token = "a token"
    val appId = "1234"
    val userId = "1000"
    val permissions: Set<String?> = hashSetOf("permission_1", "permission_2")
    val declinedPermissions: Set<String?> = hashSetOf("permission_3")
    val expiredPermissions: Set<String?> = hashSetOf("permission_4")
    val source = AccessTokenSource.WEB_VIEW
    val accessToken1 =
        AccessToken(
            token,
            appId,
            userId,
            permissions,
            declinedPermissions,
            expiredPermissions,
            source,
            null,
            null,
            null)
    val accessToken2 = FacebookTestUtility.parcelAndUnparcel(accessToken1)
    checkNotNull(accessToken2)
    Assert.assertEquals(accessToken1, accessToken2)
    Assert.assertEquals(token, accessToken2.token)
    Assert.assertEquals(appId, accessToken2.applicationId)
    Assert.assertEquals(permissions, accessToken2.permissions)
    Assert.assertEquals(declinedPermissions, accessToken2.declinedPermissions)
    Assert.assertEquals(expiredPermissions, accessToken2.expiredPermissions)
    Assert.assertEquals(accessToken1.expires, accessToken2.expires)
    Assert.assertEquals(accessToken1.lastRefresh, accessToken2.lastRefresh)
    Assert.assertEquals(accessToken1.userId, accessToken2.userId)
    Assert.assertEquals(accessToken1.expires, accessToken2.expires)
    Assert.assertEquals(
        accessToken1.dataAccessExpirationTime, accessToken2.dataAccessExpirationTime)
  }

  @Test
  fun `test create from existing token defaults`() {
    val token = "A token of my esteem"
    val applicationId = "1234"
    val userId = "1000"
    val accessToken =
        AccessToken(token, applicationId, userId, null, null, null, null, null, null, null)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(Date(Long.MAX_VALUE), accessToken.expires)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
    Assert.assertEquals(0, accessToken.permissions.size.toLong())
    Assert.assertEquals(applicationId, accessToken.applicationId)
    Assert.assertEquals(userId, accessToken.userId)
    // Allow slight variation for test execution time
    val delta = accessToken.lastRefresh.time - Date().time
    assertThat(delta).isLessThan(1_000)
  }

  @Test
  fun `test AccessToken constructor`() {
    val token = "A token of my esteem"
    val permissions: Set<String?> = hashSetOf("walk", "chew gum")
    val declinedPermissions: Set<String?> = hashSetOf("jump")
    val expiredPermissions: Set<String?> = hashSetOf("smile")
    val expires = Date(2_025, 5, 3)
    val lastRefresh = Date(2_023, 8, 15)
    val dataAccessExpirationTime = Date(2_025, 5, 3)
    val source = AccessTokenSource.WEB_VIEW
    val applicationId = "1234"
    val userId = "1000"
    val accessToken =
        AccessToken(
            token,
            applicationId,
            userId,
            permissions,
            declinedPermissions,
            expiredPermissions,
            source,
            expires,
            lastRefresh,
            dataAccessExpirationTime)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(expires, accessToken.expires)
    Assert.assertEquals(lastRefresh, accessToken.lastRefresh)
    Assert.assertEquals(source, accessToken.source)
    Assert.assertEquals(permissions, accessToken.permissions)
    Assert.assertEquals(declinedPermissions, accessToken.declinedPermissions)
    Assert.assertEquals(expiredPermissions, accessToken.expiredPermissions)
    Assert.assertEquals(applicationId, accessToken.applicationId)
    Assert.assertEquals(userId, accessToken.userId)
    Assert.assertEquals(dataAccessExpirationTime, accessToken.dataAccessExpirationTime)
  }

  @Test
  fun `test access token creation with default graph domain`() {
    val accessToken =
        AccessToken(
            mockTokenString, mockAppID, mockUserID, null, null, null, null, null, null, null)
    Assert.assertEquals(accessToken.graphDomain, "facebook")
  }

  @Test
  fun `test access token creation with Instagram graph domain`() {
    val accessToken =
        AccessToken(
            mockTokenString,
            mockAppID,
            mockUserID,
            null, // permissions
            null, // declined permissions
            null, // expired permissions
            null, // token source
            null, // expiration time
            null, // last refresh
            null, // data access expiration time
            "instagram")
    Assert.assertEquals(accessToken.graphDomain, "instagram")
  }

  @Test
  fun `test is logged in with Instagram`() {
    val instagramAccessToken =
        AccessToken(
            mockTokenString,
            mockAppID,
            mockUserID,
            null, // permissions
            null, // declined permissions
            null, // expired permissions
            null, // token source
            null, // expiration time
            null, // last refresh
            null, // data access expiration time
            "instagram")
    val facebookAccessToken =
        AccessToken(
            mockTokenString,
            mockAppID,
            mockUserID,
            null, // permissions
            null, // declined permissions
            null, // expired permissions
            null, // token source
            null, // expiration time
            null, // last refresh
            null, // data access expiration time
            "facebok")

    AccessToken.setCurrentAccessToken(instagramAccessToken)
    assertThat(AccessToken.isLoggedInWithInstagram()).isTrue
    AccessToken.setCurrentAccessToken(facebookAccessToken)
    assertThat(AccessToken.isLoggedInWithInstagram()).isFalse
  }

  @Test
  fun `test default token source for Facebook domain`() {
    val token = getAccessTokenWithSpecifiedSource("facebook", null)
    Assert.assertEquals(token.source, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
  }

  @Test
  fun `test application web source for Facebook domain`() {
    val token =
        getAccessTokenWithSpecifiedSource("facebook", AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    Assert.assertEquals(token.source, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
  }

  @Test
  fun `test chrome custom tab source for Facebook domain`() {
    val token = getAccessTokenWithSpecifiedSource("facebook", AccessTokenSource.CHROME_CUSTOM_TAB)
    Assert.assertEquals(token.source, AccessTokenSource.CHROME_CUSTOM_TAB)
  }

  @Test
  fun `test webview source for Facebook domain`() {
    val token = getAccessTokenWithSpecifiedSource("facebook", AccessTokenSource.WEB_VIEW)
    Assert.assertEquals(token.source, AccessTokenSource.WEB_VIEW)
  }

  @Test
  fun `test default token source for Gaming domain`() {
    val token = getAccessTokenWithSpecifiedSource("gaming", null)
    Assert.assertEquals(token.source, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
  }

  @Test
  fun `test application web source for Gaming domain`() {
    val token =
        getAccessTokenWithSpecifiedSource("gaming", AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    Assert.assertEquals(token.source, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
  }

  @Test
  fun `test chrome custom tab source for Gaming domain`() {
    val token = getAccessTokenWithSpecifiedSource("gaming", AccessTokenSource.CHROME_CUSTOM_TAB)
    Assert.assertEquals(token.source, AccessTokenSource.CHROME_CUSTOM_TAB)
  }

  @Test
  fun `test webview source for Gaming domain`() {
    val token = getAccessTokenWithSpecifiedSource("gaming", AccessTokenSource.WEB_VIEW)
    Assert.assertEquals(token.source, AccessTokenSource.WEB_VIEW)
  }

  @Test
  fun `test default token source for Instagram domain`() {
    val token = getAccessTokenWithSpecifiedSource("instagram", null)
    Assert.assertEquals(token.source, AccessTokenSource.INSTAGRAM_APPLICATION_WEB)
  }

  @Test
  fun `test application web source for Instagram domain`() {
    val token =
        getAccessTokenWithSpecifiedSource("instagram", AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    Assert.assertEquals(token.source, AccessTokenSource.INSTAGRAM_APPLICATION_WEB)
  }

  @Test
  fun `test chrome custom tab source for Instagram domain`() {
    val token = getAccessTokenWithSpecifiedSource("instagram", AccessTokenSource.CHROME_CUSTOM_TAB)
    Assert.assertEquals(token.source, AccessTokenSource.INSTAGRAM_CUSTOM_CHROME_TAB)
  }

  @Test
  fun `test webview source for Instagram domain`() {
    val token = getAccessTokenWithSpecifiedSource("instagram", AccessTokenSource.WEB_VIEW)
    Assert.assertEquals(token.source, AccessTokenSource.INSTAGRAM_WEB_VIEW)
  }

  fun getAccessTokenWithSpecifiedSource(
      graphDomain: String,
      tokenSource: AccessTokenSource?
  ): AccessToken {
    return AccessToken(
        mockTokenString,
        mockAppID,
        mockUserID,
        null, // permissions
        null, // declined permissions
        null, // expired permissions
        tokenSource,
        null, // expiration time
        null, // last refresh
        null, // data access expiration time
        graphDomain)
  }
}
