/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
