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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.FacebookSdk.isInitialized
import com.facebook.util.common.mockLocalBroadcastManager
import com.nhaarman.mockitokotlin2.whenever
import java.util.Arrays
import java.util.Date
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@PrepareForTest(FacebookSdk::class, LocalBroadcastManager::class)
class AccessTokenTrackerTest : FacebookPowerMockTestCase() {
  private val PERMISSIONS = Arrays.asList("walk", "chew gum")
  private val EXPIRES = Date(2025, 5, 3)
  private val LAST_REFRESH = Date(2023, 8, 15)
  private val DATA_ACCESS_EXPIRATION_TIME = Date(2025, 5, 3)
  private val APP_ID = "1234"
  private val USER_ID = "1000"
  private var accessTokenTracker: AccessTokenTracker? = null
  private var localBroadcastManager: LocalBroadcastManager? = null
  private val mockExecutor: Executor = FacebookSerialExecutor()
  @Before
  fun before() {
    mockStatic(FacebookSdk::class.java)
    whenever(isInitialized()).thenReturn(true)
    whenever(getApplicationContext()).thenReturn(RuntimeEnvironment.application)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    localBroadcastManager = mockLocalBroadcastManager(RuntimeEnvironment.application)
    mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(getApplicationContext()))
        .thenReturn(localBroadcastManager)
  }

  @After
  fun after() {
    accessTokenTracker?.let {
      if (it.isTracking) {
        it.stopTracking()
      }
    }
  }

  @Test(expected = FacebookSdkNotInitializedException::class)
  fun testRequiresSdkToBeInitialized() {
    whenever(isInitialized()).thenReturn(false)
    accessTokenTracker = TestAccessTokenTracker()
  }

  @Test
  fun testDefaultsToTracking() {
    accessTokenTracker = TestAccessTokenTracker()
    assertThat(accessTokenTracker?.isTracking).isTrue
  }

  @Test
  fun testCanTurnTrackingOff() {
    accessTokenTracker = TestAccessTokenTracker()
    accessTokenTracker?.let {
      it.stopTracking()
      Assert.assertFalse(it.isTracking)
    }
  }

  @Test
  fun testCanTurnTrackingOn() {
    accessTokenTracker = TestAccessTokenTracker()
    accessTokenTracker?.let {
      it.stopTracking()
      it.startTracking()
      assertThat(it.isTracking).isTrue
    }
  }

  @Test
  fun testCallbackCalledOnBroadcastReceived() {
    accessTokenTracker = TestAccessTokenTracker()
    val oldAccessToken = createAccessToken("I'm old!")
    val currentAccessToken = createAccessToken("I'm current!")
    sendBroadcast(oldAccessToken, currentAccessToken)
    val tracker = checkNotNull(accessTokenTracker) as TestAccessTokenTracker
    assertNotNull(tracker.currentAccessToken)
    assertEquals(currentAccessToken.token, tracker.currentAccessToken?.token)
    assertNotNull(tracker.oldAccessToken)
    assertEquals(oldAccessToken.token, tracker.oldAccessToken?.token)
  }

  private fun createAccessToken(tokenString: String): AccessToken {
    return AccessToken(
        tokenString,
        APP_ID,
        USER_ID,
        PERMISSIONS,
        null,
        null,
        AccessTokenSource.WEB_VIEW,
        EXPIRES,
        LAST_REFRESH,
        DATA_ACCESS_EXPIRATION_TIME)
  }

  private fun sendBroadcast(oldAccessToken: AccessToken, currentAccessToken: AccessToken) {
    val intent = Intent(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED)
    intent.putExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN, oldAccessToken)
    intent.putExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN, currentAccessToken)
    localBroadcastManager?.sendBroadcast(intent)
  }

  internal inner class TestAccessTokenTracker : AccessTokenTracker() {
    var currentAccessToken: AccessToken? = null
    var oldAccessToken: AccessToken? = null
    override fun onCurrentAccessTokenChanged(
        oldAccessToken: AccessToken?,
        currentAccessToken: AccessToken?
    ) {
      this.oldAccessToken = oldAccessToken
      this.currentAccessToken = currentAccessToken
    }
  }
}
