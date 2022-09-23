/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.os.Bundle
import com.facebook.internal.FacebookDialogFragment
import com.facebook.internal.NativeProtocol
import com.facebook.login.LoginFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.Robolectric

@PrepareForTest(FacebookSdk::class, NativeProtocol::class)
class FacebookActivityTest : FacebookPowerMockTestCase() {
  private lateinit var activity: FacebookActivity
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    activity = spy(Robolectric.buildActivity(FacebookActivity::class.java).get())
    PowerMockito.doNothing().`when`(activity).setContentView(any<Int>())

    PowerMockito.mockStatic(NativeProtocol::class.java)
  }

  @Test
  fun `test start activity with default fragment`() {
    activity.onCreate(Bundle())
    val fragment = activity.currentFragment
    assertThat(fragment).isNotNull
    assertThat(fragment is LoginFragment).isTrue
  }

  @Test
  fun `test start activity with dialog fragment`() {
    val intent = Intent()
    intent.action = FacebookDialogFragment.TAG
    whenever(activity.intent).thenReturn(intent)
    whenever(NativeProtocol.getMethodArgumentsFromIntent(any())).thenReturn(Bundle())

    activity.onCreate(Bundle())
    val fragment = activity.currentFragment
    assertThat(fragment).isNotNull
    assertThat(fragment is FacebookDialogFragment).isTrue
  }

  @Test
  fun `test start activity to handle error`() {
    val intent = Intent()
    intent.action = FacebookActivity.PASS_THROUGH_CANCEL_ACTION
    whenever(activity.intent).thenReturn(intent)

    var nativeProtocolCount = 0
    val exception = FacebookException()
    val resultIntent = Intent()

    whenever(NativeProtocol.getMethodArgumentsFromIntent(any())).thenAnswer {
      nativeProtocolCount++
      Bundle()
    }
    whenever(NativeProtocol.getExceptionFromErrorData(any())).thenAnswer {
      nativeProtocolCount++
      exception
    }

    whenever(NativeProtocol.createProtocolResultIntent(intent, null, exception)).thenAnswer {
      nativeProtocolCount++
      resultIntent
    }

    activity.onCreate(Bundle())

    assertThat(nativeProtocolCount).isEqualTo(3)
    verify(activity).setResult(RESULT_CANCELED, resultIntent)
    verify(activity).finish()
  }
}
