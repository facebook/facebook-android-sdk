/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
