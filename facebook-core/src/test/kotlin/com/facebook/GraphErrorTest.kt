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

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.AccessToken.Companion.setCurrentAccessToken
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.Utility
import java.net.HttpURLConnection
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    AccessToken::class,
    AccessTokenCache::class,
    FacebookSdk::class,
    FetchedAppGateKeepersManager::class,
    GraphRequest::class,
    Utility::class,
    LocalBroadcastManager::class)
class GraphErrorTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    MemberModifier.suppress(MemberMatcher.method(Utility::class.java, "clearFacebookCookies"))
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())

    MemberModifier.stub<Any?>(MemberMatcher.method(AccessTokenCache::class.java, "save"))
        .toReturn(null)
    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    PowerMockito.`when`(LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext()))
        .thenReturn(mockLocalBroadcastManager)
  }

  @Test
  fun testAccessTokenNotResetOnTokenExpirationError() {
    val accessToken = PowerMockito.mock(AccessToken::class.java)
    PowerMockito.`when`(accessToken.token).thenReturn("token")
    PowerMockito.`when`(accessToken.userId).thenReturn("user_id")
    PowerMockito.`when`(accessToken.applicationId).thenReturn("application_id")
    MemberModifier.suppress(
        MemberMatcher.method(Utility::class.java, "isNullOrEmpty", String::class.java))
    setCurrentAccessToken(accessToken)
    val errorBody = JSONObject()
    errorBody.put("message", "Invalid OAuth access token.")
    errorBody.put("type", "OAuthException")
    errorBody.put("code", FacebookRequestErrorClassification.EC_INVALID_TOKEN)
    errorBody.put("error_subcode", FacebookRequestErrorClassification.ESC_APP_INACTIVE)
    val error = JSONObject()
    error.put("error", errorBody)
    val errorString = error.toString()
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(400)
    val request = PowerMockito.mock(GraphRequest::class.java)
    PowerMockito.`when`(request.accessToken).thenReturn(accessToken)
    val batch = GraphRequestBatch(request)
    Assert.assertNotNull(getCurrentAccessToken())
    GraphResponse.createResponsesFromString(errorString, connection, batch)
    Assert.assertNotNull(getCurrentAccessToken())
  }

  @Test
  fun testAccessTokenResetOnTokenInstallError() {
    val accessToken = PowerMockito.mock(AccessToken::class.java)
    setCurrentAccessToken(accessToken)
    val errorBody = JSONObject()
    errorBody.put("message", "User has not installed the application.")
    errorBody.put("type", "OAuthException")
    errorBody.put("code", FacebookRequestErrorClassification.EC_INVALID_TOKEN)
    val error = JSONObject()
    error.put("error", errorBody)
    val errorString = error.toString()
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(400)
    val request = PowerMockito.mock(GraphRequest::class.java)
    PowerMockito.`when`(request.accessToken).thenReturn(accessToken)
    val batch = GraphRequestBatch(request)
    Assert.assertNotNull(getCurrentAccessToken())
    GraphResponse.createResponsesFromString(errorString, connection, batch)
    Assert.assertNull(getCurrentAccessToken())
  }
}
