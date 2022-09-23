/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import java.lang.IllegalArgumentException
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Validate::class)
class FacebookDialogBaseTest : FacebookPowerMockTestCase() {
  private lateinit var mockLegacyActivity: Activity
  private lateinit var mockAndroidxActivity: ComponentActivity
  private lateinit var mockActivityResultRegistry: ActivityResultRegistry
  private lateinit var mockLauncher: ActivityResultLauncher<Intent>
  private lateinit var callbackManager: CallbackManagerImpl

  class TestDialog(activity: Activity, requestCode: Int = 0) :
      FacebookDialogBase<Int, Void>(activity, requestCode) {
    val mockAppCall: AppCall = mock()
    val mockIntent: Intent = mock()
    var capturedMode: Any? = null
    override fun registerCallbackImpl(
        callbackManager: CallbackManagerImpl,
        callback: FacebookCallback<Void>
    ) = Unit

    override val orderedModeHandlers = mutableListOf<ModeHandler>()

    override fun createBaseAppCall(): AppCall = mockAppCall
    override fun canShowImpl(content: Int, mode: Any): Boolean {
      capturedMode = mode
      return true
    }

    val defaultMode: Any = BASE_AUTOMATIC_MODE

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
    PowerMockito.`when`(FacebookSdk.getCallbackRequestCodeOffset()).thenReturn(0xface)
    PowerMockito.`when`(FacebookSdk.isFacebookRequestCode(any())).thenCallRealMethod()
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

  @Test(expected = IllegalArgumentException::class)
  fun `test set request code`() {
    val dialog = TestDialog(mockAndroidxActivity)
    dialog.requestCode = FacebookSdk.getCallbackRequestCodeOffset() + 1
  }

  @Test
  fun `test create a dialog with sdk request code code`() {
    TestDialog(mockAndroidxActivity, FacebookSdk.getCallbackRequestCodeOffset() + 1)
  }
}
