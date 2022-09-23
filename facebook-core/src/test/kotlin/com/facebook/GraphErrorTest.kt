/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.AccessToken.Companion.setCurrentAccessToken
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.Utility
import com.facebook.util.common.mockLocalBroadcastManager
import java.net.HttpURLConnection
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

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
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())

    MemberModifier.stub<Any?>(MemberMatcher.method(AccessTokenCache::class.java, "save"))
        .toReturn(null)
    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    val mockLocalBroadcastManager = mockLocalBroadcastManager(FacebookSdk.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    PowerMockito.`when`(LocalBroadcastManager.getInstance(any()))
        .thenReturn(mockLocalBroadcastManager)

    val accessTokenManager = AccessTokenManager(mockLocalBroadcastManager, AccessTokenCache())
    val mockAccessTokenManagerCompanion = mock<AccessTokenManager.Companion>()
    whenever(mockAccessTokenManagerCompanion.getInstance()).thenReturn(accessTokenManager)
    Whitebox.setInternalState(
        AccessTokenManager::class.java, "Companion", mockAccessTokenManagerCompanion)
  }

  @Test
  fun testAccessTokenNotResetOnTokenExpirationError() {
    val accessToken = PowerMockito.mock(AccessToken::class.java)
    whenever(accessToken.token).thenReturn("token")
    whenever(accessToken.userId).thenReturn("user_id")
    whenever(accessToken.applicationId).thenReturn("application_id")
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
    whenever(connection.responseCode).thenReturn(400)
    val request = PowerMockito.mock(GraphRequest::class.java)
    whenever(request.accessToken).thenReturn(accessToken)
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
    whenever(connection.responseCode).thenReturn(400)
    val request = PowerMockito.mock(GraphRequest::class.java)
    whenever(request.accessToken).thenReturn(accessToken)
    val batch = GraphRequestBatch(request)
    Assert.assertNotNull(getCurrentAccessToken())
    GraphResponse.createResponsesFromString(errorString, connection, batch)
    Assert.assertNull(getCurrentAccessToken())
  }
}
