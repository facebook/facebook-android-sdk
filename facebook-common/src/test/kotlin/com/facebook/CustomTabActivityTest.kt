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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController

@PrepareForTest(LocalBroadcastManager::class)
class CustomTabActivityTest : FacebookPowerMockTestCase() {
  private lateinit var launchIntent: Intent
  private lateinit var mockLocalBroadcastManager: LocalBroadcastManager
  private lateinit var activityController: ActivityController<CustomTabActivity>
  override fun setup() {
    super.setup()
    launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developers.facebook.com"))
    mockLocalBroadcastManager = mock()
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any())).thenReturn(mockLocalBroadcastManager)
    activityController = Robolectric.buildActivity(CustomTabActivity::class.java, launchIntent)
    activityController.create(null).start()
  }

  @Test
  fun `test CustomTabActivity start CustomTabMainActivity when it's created`() {
    val shadowCustomTabActivity = Shadows.shadowOf(activityController.get())
    val capturedIntent = shadowCustomTabActivity.nextStartedActivityForResult.intent
    assertThat(capturedIntent.action).isEqualTo(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION)
    assertThat(capturedIntent.getStringExtra(CustomTabMainActivity.EXTRA_URL))
        .isEqualTo("https://developers.facebook.com")
  }

  @Test
  fun `test broadcasting intent if start activity failed`() {
    val shadowCustomTabActivity = Shadows.shadowOf(activityController.get())
    val capturedIntent = shadowCustomTabActivity.nextStartedActivityForResult.intent

    shadowCustomTabActivity.receiveResult(capturedIntent, Activity.RESULT_CANCELED, Intent())

    val intentCaptor = argumentCaptor<Intent>()
    verify(mockLocalBroadcastManager).sendBroadcast(intentCaptor.capture())
    verify(mockLocalBroadcastManager).registerReceiver(any(), any())

    assertThat(intentCaptor.firstValue.action)
        .isEqualTo(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION)
    assertThat(intentCaptor.firstValue.getStringExtra(CustomTabMainActivity.EXTRA_URL))
        .isEqualTo("https://developers.facebook.com")
  }

  @Test
  fun `test destroy the activity will unregister the close receiver`() {
    val shadowCustomTabActivity = Shadows.shadowOf(activityController.get())
    val capturedIntent = shadowCustomTabActivity.nextStartedActivityForResult.intent
    shadowCustomTabActivity.receiveResult(capturedIntent, Activity.RESULT_CANCELED, Intent())
    activityController.destroy()
    verify(mockLocalBroadcastManager).unregisterReceiver(any())
  }

  @Test
  fun `test destroy the activity won't call unregister if there is no close receiver`() {
    activityController.destroy()
    verify(mockLocalBroadcastManager, never()).unregisterReceiver(any())
  }
}
