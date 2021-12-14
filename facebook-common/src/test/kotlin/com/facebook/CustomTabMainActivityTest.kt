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
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.browser.customtabs.CustomTabsIntent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController

@PrepareForTest(LocalBroadcastManager::class, CustomTabsIntent::class)
class CustomTabMainActivityTest : FacebookPowerMockTestCase() {
  private lateinit var activityController: ActivityController<CustomTabMainActivity>
  private lateinit var launchIntent: Intent
  private lateinit var mockLocalBroadcastManager: LocalBroadcastManager
  private lateinit var mockCustomTabsIntent: CustomTabsIntent
  private val mockChromePackageString = "mock chrome package string"

  override fun setup() {
    super.setup()
    mockLocalBroadcastManager = mock()
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    PowerMockito.`when`(LocalBroadcastManager.getInstance(any()))
        .thenReturn(mockLocalBroadcastManager)
    mockCustomTabsIntent = mock()
    Whitebox.setInternalState(mockCustomTabsIntent, "intent", Intent(Intent.ACTION_VIEW))
    PowerMockito.whenNew(CustomTabsIntent::class.java)
        .withAnyArguments()
        .thenReturn(mockCustomTabsIntent)

    launchIntent = Intent(Intent.ACTION_VIEW)
    launchIntent.putExtra(CustomTabMainActivity.EXTRA_ACTION, "oauth_dialog")
    launchIntent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, mockChromePackageString)
  }

  @Test
  fun `test launch with custom tab redirect action`() {
    launchIntent = Intent(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION)
    activityController = Robolectric.buildActivity(CustomTabMainActivity::class.java, launchIntent)
    activityController.create(null)
    val activity = activityController.get()

    // verify the state of the activity
    assertThat(activity.isFinishing).isTrue
    val shadowActivity = Shadows.shadowOf(activity)
    assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
  }

  @Test
  fun `test launch with an valid action will open custom tab`() {
    activityController = Robolectric.buildActivity(CustomTabMainActivity::class.java, launchIntent)
    activityController.create(null)
    val activity = activityController.get()
    val shadowActivity = Shadows.shadowOf(activity)
    // verify that custom tab is launched with CustomTabsIntent
    verify(mockCustomTabsIntent).launchUrl(eq(activity), any())

    // verify the redirect receiver is registered
    val receiverCaptor = argumentCaptor<BroadcastReceiver>()
    val intentFilterCaptor = argumentCaptor<IntentFilter>()
    verify(mockLocalBroadcastManager)
        .registerReceiver(receiverCaptor.capture(), intentFilterCaptor.capture())
    assertThat(
            intentFilterCaptor.firstValue.hasAction(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION))
        .isTrue
    val capturedReceiver = receiverCaptor.firstValue

    // verify the redirect receiver will start refresh action on receive
    capturedReceiver.onReceive(activity, mock())
    assertThat(shadowActivity.nextStartedActivity.action)
        .isEqualTo(CustomTabMainActivity.REFRESH_ACTION)
  }

  @Test
  fun `test receiving intent of refresh action`() {
    activityController = Robolectric.buildActivity(CustomTabMainActivity::class.java, launchIntent)
    activityController.create(null)
    activityController.newIntent(Intent(CustomTabMainActivity.REFRESH_ACTION))

    // verify that it will broadcast destroy action
    val intentCaptor = argumentCaptor<Intent>()
    verify(mockLocalBroadcastManager).sendBroadcast(intentCaptor.capture())
    assertThat(intentCaptor.firstValue.action).isEqualTo(CustomTabActivity.DESTROY_ACTION)

    // verify the result code
    val shadowActivity = Shadows.shadowOf(activityController.get())
    assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    assertThat(activityController.get().isFinishing).isTrue
  }

  @Test
  fun `test receiving intent of redirect action`() {
    activityController = Robolectric.buildActivity(CustomTabMainActivity::class.java, launchIntent)
    activityController.create(null)
    activityController.newIntent(Intent(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION))

    // verify the result code
    val shadowActivity = Shadows.shadowOf(activityController.get())
    assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    assertThat(activityController.get().isFinishing).isTrue
  }

  @Test
  fun `test resume`() {
    activityController = Robolectric.buildActivity(CustomTabMainActivity::class.java, launchIntent)
    activityController.create(null)
    activityController.resume()
    val activity = activityController.get()
    // first resume should not finish the activity.
    assertThat(activity.isFinishing).isFalse

    activityController.resume()
    // second resume will cancel the activity
    assertThat(activity.isFinishing).isTrue
    val shadowActivity = Shadows.shadowOf(activity)
    assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
  }
}
