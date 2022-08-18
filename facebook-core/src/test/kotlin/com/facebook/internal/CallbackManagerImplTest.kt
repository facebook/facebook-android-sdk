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

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.CallbackManagerImpl.Companion.registerStaticCallback
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class CallbackManagerImplTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    // Reset the static state every time so tests don't interfere with each other.
    Whitebox.setInternalState(
        CallbackManagerImpl::class.java,
        "staticCallbacks",
        HashMap<Int, CallbackManagerImpl.Callback>())
  }

  @Test
  fun `test callback executed`() {
    var capturedResult = false
    val callbackManagerImpl = CallbackManagerImpl()
    callbackManagerImpl.registerCallback(
        CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean {
            capturedResult = true
            return true
          }
        })
    callbackManagerImpl.onActivityResult(FacebookSdk.getCallbackRequestCodeOffset(), 1, Intent())
    assertThat(capturedResult).isTrue
  }

  @Test
  fun `test right callback executed`() {
    var capturedResult = false
    val callbackManagerImpl = CallbackManagerImpl()
    callbackManagerImpl.registerCallback(
        123,
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean {
            capturedResult = true
            return true
          }
        })
    callbackManagerImpl.registerCallback(
        456,
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean = false
        })
    callbackManagerImpl.onActivityResult(123, 1, Intent())
    assertThat(capturedResult).isTrue
  }

  @Test
  fun `test static callback executed`() {
    var capturedResult = false
    val callbackManagerImpl = CallbackManagerImpl()
    registerStaticCallback(
        CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean {
            capturedResult = true
            return true
          }
        })
    callbackManagerImpl.onActivityResult(FacebookSdk.getCallbackRequestCodeOffset(), 1, Intent())
    assertThat(capturedResult).isTrue
  }

  @Test
  fun `test static callback skipped`() {
    var capturedResult = false
    var capturedResultStatic = false
    val callbackManagerImpl = CallbackManagerImpl()
    callbackManagerImpl.registerCallback(
        CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean {
            capturedResult = true
            return true
          }
        })
    registerStaticCallback(
        CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
        object : CallbackManagerImpl.Callback {
          override fun onActivityResult(resultCode: Int, data: Intent?): Boolean {
            capturedResultStatic = true
            return true
          }
        })
    callbackManagerImpl.onActivityResult(FacebookSdk.getCallbackRequestCodeOffset(), 1, Intent())
    assertThat(capturedResult).isTrue
    assertThat(capturedResultStatic).isFalse
  }
}
