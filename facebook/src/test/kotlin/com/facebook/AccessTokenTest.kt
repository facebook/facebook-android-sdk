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

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import com.facebook.internal.Utility.arrayList
import com.facebook.internal.Utility.hashSet
import com.nhaarman.mockitokotlin2.any
import java.util.Date
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(Utility::class, FacebookSdk::class)
class AccessTokenTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    MemberModifier.stub<Any>(
            PowerMockito.method(Utility::class.java, "awaitGetGraphMeRequestWithCache"))
        .toReturn(JSONObject().put("id", "1000"))
  }

  @Test
  fun `test empty token throws`() {
    try {
      val token =
          AccessToken(
              "",
              "1234",
              "1000",
              arrayList<String?>("something"),
              arrayList<String?>("something_else"),
              arrayList<String?>("something_else_e"),
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
              arrayList<String?>("something"),
              arrayList<String?>("something_else"),
              arrayList<String?>("something_else_e"),
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
              arrayList<String?>("something"),
              arrayList<String?>("something_else"),
              arrayList<String?>("something_else_e"),
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
            arrayList<String?>("stream_publish"),
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
    val permissions: Set<String> = hashSet("stream_publish", "go_outside_and_play")
    val declinedPermissions: Set<String> = hashSet("no you may not", "no soup for you")
    val expiredPermissions: Set<String> = hashSet("expired", "oh no")
    val token = "AnImaginaryTokenValue"
    val later = TestUtils.nowPlusSeconds(60)
    val earlier = TestUtils.nowPlusSeconds(-60)
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
    TestUtils.assertSamePermissions(permissions, accessToken)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
    Assert.assertTrue(!accessToken.isExpired)
    val cache = AccessTokenTestHelper.toLegacyCacheBundle(accessToken)
    TestUtils.assertEqualContentsWithoutOrder(bundle, cache)
  }

  @Test
  fun `test from cache with missing application Id`() {
    val token = "AnImaginaryTokenValue"
    val applicationId = "1234"
    val bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, token)
    // no app id
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(applicationId)
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
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
    val emptyList: List<String?> = emptyList<String>()
    val normalArrayList = HashSet(normalList)
    val emptyArrayList = HashSet<String?>()
    val permissionLists = listOf(normalList, emptyList, normalArrayList, emptyArrayList)
    for (list in permissionLists) {
      LegacyTokenHelper.putPermissions(bundle, list)
      TestUtils.assertSamePermissions(list, LegacyTokenHelper.getPermissions(bundle))
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
    PowerMockito.`when`(Utility.getGraphMeRequestWithCacheAsync(any(), any())).thenAnswer {
      capturedGraphRequestCallback = it.arguments[1] as Utility.GraphMeRequestWithCacheCallback?
      Unit
    }
    PowerMockito.`when`(Utility.getBundleLongAsDate(any(), any(), any())).thenCallRealMethod()
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
            Date(2015, 3, 3),
            Date(2015, 1, 1),
            Date(2015, 3, 3))
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
            Date(2015, 3, 3),
            Date(2015, 1, 1),
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
    val accessToken2 = TestUtils.parcelAndUnparcel(accessToken1)
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
    Assert.assertTrue(delta < 1000)
  }

  @Test
  fun `test AccessToken constructor`() {
    val token = "A token of my esteem"
    val permissions: Set<String?> = hashSet<String?>("walk", "chew gum")
    val declinedPermissions: Set<String?> = hashSet<String?>("jump")
    val expiredPermissions: Set<String?> = hashSet<String?>("smile")
    val expires = Date(2025, 5, 3)
    val lastRefresh = Date(2023, 8, 15)
    val dataAccessExpirationTime = Date(2025, 5, 3)
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
}
