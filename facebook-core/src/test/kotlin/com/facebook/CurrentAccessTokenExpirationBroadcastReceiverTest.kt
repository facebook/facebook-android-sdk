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
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class CurrentAccessTokenExpirationBroadcastReceiverTest : FacebookPowerMockTestCase() {
  private lateinit var mockAccessTokenManager: AccessTokenManager
  override fun setup() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    mockAccessTokenManager = mock()
    val mockAccessTokenManagerCompanion: AccessTokenManager.Companion = mock()
    whenever(mockAccessTokenManagerCompanion.getInstance()).thenReturn(mockAccessTokenManager)
    Whitebox.setInternalState(
        AccessTokenManager::class.java, "Companion", mockAccessTokenManagerCompanion)
  }

  @Test
  fun `test on receiving current access token changed intent`() {
    val intent = Intent(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED)
    val receiver = CurrentAccessTokenExpirationBroadcastReceiver()
    receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
    verify(mockAccessTokenManager).currentAccessTokenChanged()
  }

  @Test
  fun `test on receiving irrelevant intent`() {
    val intent = Intent("irrelevant action")
    val receiver = CurrentAccessTokenExpirationBroadcastReceiver()
    receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
    verify(mockAccessTokenManager, never()).currentAccessTokenChanged()
  }
}
