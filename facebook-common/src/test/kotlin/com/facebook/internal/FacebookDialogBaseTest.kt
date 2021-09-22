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

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookCallback
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Validate::class)
class FacebookDialogBaseTest : FacebookPowerMockTestCase() {
  private lateinit var mockLegacyActivity: Activity
  private lateinit var mockAndroidxActivity: ComponentActivity
  private lateinit var mockActivityResultRegistry: ActivityResultRegistry
  private lateinit var mockLauncher: ActivityResultLauncher<Intent>
  private lateinit var callbackManager: CallbackManagerImpl

  class TestDialog(activity: Activity) : FacebookDialogBase<Int, Void>(activity, 0) {
    val mockAppCall: AppCall = mock()
    val mockIntent: Intent = mock()
    var capturedMode: Any? = null
    override fun registerCallbackImpl(
        callbackManager: CallbackManagerImpl,
        callback: FacebookCallback<Void>
    ) = Unit

    override fun getOrderedModeHandlers(): MutableList<ModeHandler> = mutableListOf()

    override fun createBaseAppCall(): AppCall = mockAppCall
    override fun canShowImpl(content: Int?, mode: Any?): Boolean {
      capturedMode = mode
      return true
    }

    val defaultMode: Any? = BASE_AUTOMATIC_MODE

    init {
      whenever(mockAppCall.requestIntent).thenReturn(mockIntent)
      whenever(mockAppCall.requestCode).thenReturn(0)
      whenever(mockAppCall.callId).thenReturn(UUID.randomUUID())
    }
  }

  override fun setup() {
    super.setup()
    mockLegacyActivity = mock()
    mockAndroidxActivity = mock()
    mockActivityResultRegistry = mock()
    mockLauncher = mock()
    whenever(mockAndroidxActivity.activityResultRegistry).thenReturn(mockActivityResultRegistry)
    whenever(
            mockActivityResultRegistry.register(
                any(), any<ActivityResultContract<Intent, Pair<Int, Intent>>>(), any()))
        .thenReturn(mockLauncher)
    callbackManager = CallbackManagerImpl()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(Validate::class.java)
  }

  @Test
  fun `test dialog will memorize callback manager when registering callbacks`() {
    val dialog = TestDialog(mockAndroidxActivity)
    dialog.registerCallback(callbackManager, mock())
    assertThat(dialog.callbackManager).isSameAs(callbackManager)
  }

  @Test
  fun `test dialog will memorize callback manager when registering callbacks with request code`() {
    val dialog = TestDialog(mockAndroidxActivity)
    dialog.registerCallback(callbackManager, mock(), 0)
    assertThat(dialog.callbackManager).isSameAs(callbackManager)
  }

  @Test
  fun `test show with legacy activity`() {
    val dialog = TestDialog(mockLegacyActivity)
    dialog.show(1)
    verify(mockLegacyActivity).startActivityForResult(dialog.mockIntent, dialog.requestCode)
  }

  @Test
  fun `test show with androidx activity`() {
    val dialog = TestDialog(mockAndroidxActivity)
    dialog.show(1)
    verify(mockLauncher).launch(dialog.mockIntent)
  }

  @Test
  fun `test canShow call canShowImpl with the default mode`() {
    val dialog = TestDialog(mockAndroidxActivity)
    dialog.canShow(1)
    assertThat(dialog.capturedMode).isEqualTo(dialog.defaultMode)
  }
}
