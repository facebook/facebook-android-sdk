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
import android.os.Bundle
import android.util.Pair
import androidx.test.core.app.ApplicationProvider
import com.facebook.CallbackManager
import com.facebook.CustomTabMainActivity
import com.facebook.FacebookActivity
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, Validate::class, NativeProtocol::class, DialogPresenter::class)
class DialogPresenterTest : FacebookPowerMockTestCase() {
  private lateinit var mockAppCall: AppCall
  private lateinit var mockRequestIntent: Intent
  private lateinit var mockFetchedAppSettingsCompanion: FetchedAppSettings.Companion
  override fun setup() {
    super.setup()
    mockRequestIntent = mock()
    mockAppCall = mock()
    whenever(mockAppCall.requestIntent).thenReturn(mockRequestIntent)
    whenever(mockAppCall.requestCode).thenReturn(REQUEST_CODE)
    whenever(mockAppCall.callId).thenReturn(UUID.randomUUID())
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_ID)
    PowerMockito.`when`(FacebookSdk.getApplicationName()).thenReturn("application")
    PowerMockito.mockStatic(Validate::class.java)
    PowerMockito.mockStatic(NativeProtocol::class.java)
    mockFetchedAppSettingsCompanion = mock()
    Whitebox.setInternalState(
        FetchedAppSettings::class.java, "Companion", mockFetchedAppSettingsCompanion)
  }

  @Test
  fun `test present with native activity`() {
    val mockActivity = mock<android.app.Activity>()
    DialogPresenter.present(mockAppCall, mockActivity)
    verify(mockActivity).startActivityForResult(mockRequestIntent, REQUEST_CODE)
    verify(mockAppCall).setPending()
  }

  @Test
  fun `test present with fragment wrapper`() {
    val mockFragmentWrapper = mock<FragmentWrapper>()
    DialogPresenter.present(mockAppCall, mockFragmentWrapper)
    verify(mockFragmentWrapper).startActivityForResult(mockRequestIntent, REQUEST_CODE)
    verify(mockAppCall).setPending()
  }

  @Test
  fun `test present with androidx activity result registry`() {
    val mockActivityResultRegistry = mock<androidx.activity.result.ActivityResultRegistry>()
    val mockLauncher = mock<androidx.activity.result.ActivityResultLauncher<Intent>>()
    whenever(
            mockActivityResultRegistry.register(
                any(),
                any<
                    androidx.activity.result.contract.ActivityResultContract<
                        Intent, Pair<Int, Intent>>>(),
                any()))
        .thenReturn(mockLauncher)
    DialogPresenter.present(mockAppCall, mockActivityResultRegistry, null)
    verify(mockLauncher).launch(mockRequestIntent)
    verify(mockAppCall).setPending()
  }

  @Test
  fun `test startActivityForResultWithAndroidX`() {
    val mockActivityResultRegistry = mock<androidx.activity.result.ActivityResultRegistry>()
    val mockLauncher = mock<androidx.activity.result.ActivityResultLauncher<Intent>>()
    val mockCallbackManager = mock<CallbackManager>()
    val callbackCaptor =
        argumentCaptor<androidx.activity.result.ActivityResultCallback<Pair<Int, Intent>>>()
    val mockResultData = mock<Intent>()
    whenever(
            mockActivityResultRegistry.register(
                any(),
                any<
                    androidx.activity.result.contract.ActivityResultContract<
                        Intent, Pair<Int, Intent>>>(),
                any()))
        .thenReturn(mockLauncher)

    DialogPresenter.startActivityForResultWithAndroidX(
        mockActivityResultRegistry, mockCallbackManager, mockRequestIntent, REQUEST_CODE)

    verify(mockActivityResultRegistry)
        .register(
            any(),
            any<
                androidx.activity.result.contract.ActivityResultContract<
                    Intent, Pair<Int, Intent>>>(),
            callbackCaptor.capture())
    verify(mockLauncher).launch(mockRequestIntent)

    // test it will forward the result to callback manager and unregister the launcher
    callbackCaptor.firstValue.onActivityResult(Pair(0xb00c, mockResultData))

    verify(mockCallbackManager).onActivityResult(REQUEST_CODE, 0xb00c, mockResultData)
    verify(mockLauncher).unregister()
  }

  @Test
  fun `test setupAppCallForCannotShowError`() {
    DialogPresenter.setupAppCallForCannotShowError(mockAppCall)
    val resultIntentCaptor = argumentCaptor<Intent>()
    verify(mockAppCall).requestIntent = resultIntentCaptor.capture()
    val resultIntent = resultIntentCaptor.firstValue
    assertThat(resultIntent.action).isEqualTo(FacebookActivity.PASS_THROUGH_CANCEL_ACTION)
  }

  @Test
  fun `test setupAppCallForValidationError`() {
    DialogPresenter.setupAppCallForValidationError(mockAppCall, FacebookException("test exception"))
    val resultIntentCaptor = argumentCaptor<Intent>()
    verify(mockAppCall).requestIntent = resultIntentCaptor.capture()
    val resultIntent = resultIntentCaptor.firstValue
    assertThat(resultIntent.action).isEqualTo(FacebookActivity.PASS_THROUGH_CANCEL_ACTION)
  }

  @Test
  fun `test setupAppCallForErrorResult`() {
    DialogPresenter.setupAppCallForErrorResult(mockAppCall, FacebookException("test exception"))
    val resultIntentCaptor = argumentCaptor<Intent>()
    verify(mockAppCall).requestIntent = resultIntentCaptor.capture()
    val resultIntent = resultIntentCaptor.firstValue
    assertThat(resultIntent.action).isEqualTo(FacebookActivity.PASS_THROUGH_CANCEL_ACTION)
  }

  @Test
  fun `test setupAppCallForErrorResult with null error`() {
    DialogPresenter.setupAppCallForErrorResult(mockAppCall, null)
    verify(mockAppCall, never()).requestIntent = any()
  }

  @Test
  fun `test canPresentNativeDialog query action on NativeProtocol`() {
    val actionCaptor = argumentCaptor<String>()
    val versionSpecCaptor = argumentCaptor<IntArray>()
    PowerMockito.`when`(
            NativeProtocol.getLatestAvailableProtocolVersionForAction(
                actionCaptor.capture(), versionSpecCaptor.capture()))
        .thenReturn(NativeProtocol.ProtocolVersionQueryResult.createEmpty())
    DialogPresenter.canPresentNativeDialogWithFeature(testDialogFeature)
    assertThat(actionCaptor.firstValue).isEqualTo(testDialogFeature.getAction())
    assertThat(versionSpecCaptor.firstValue).containsExactly(testDialogFeature.getMinVersion())
  }

  @Test
  fun `test canPresentWebFallbackDialogWithFeature query action on FetchedAppSettings`() {
    DialogPresenter.canPresentWebFallbackDialogWithFeature(testDialogFeature)
    verify(mockFetchedAppSettingsCompanion)
        .getDialogFeatureConfig(
            APPLICATION_ID, testDialogFeature.getAction(), testDialogFeature.name)
  }

  @Test
  fun `test setupAppCallForWebDialog will call FacebookDialogFragment`() {
    val intentCaptor = argumentCaptor<Intent>()
    val params = Bundle()
    DialogPresenter.setupAppCallForWebDialog(mockAppCall, testDialogFeature.getAction(), params)
    verify(mockAppCall).requestIntent = intentCaptor.capture()
    val capturedIntent = intentCaptor.firstValue
    assertThat(capturedIntent.action).isEqualTo(FacebookDialogFragment.TAG)
  }

  @Test(expected = FacebookException::class)
  fun `test setupAppCallForWebFallbackDialog will throw exception with unknown action`() {
    val params = Bundle()
    DialogPresenter.setupAppCallForWebFallbackDialog(mockAppCall, params, testDialogFeature)
  }

  @Test
  fun `test setupAppCallForNativeDialog will query action on NativeProtocol`() {
    val actionCaptor = argumentCaptor<String>()
    var capturedException: Exception? = null
    PowerMockito.`when`(
            NativeProtocol.getLatestAvailableProtocolVersionForAction(
                actionCaptor.capture(), any()))
        .thenReturn(NativeProtocol.ProtocolVersionQueryResult.createEmpty())
    try {
      DialogPresenter.setupAppCallForNativeDialog(mockAppCall, mock(), testDialogFeature)
    } catch (exception: FacebookException) {
      // ignore the exception since the action is invalid and there must be an exception
      capturedException = exception
    } finally {
      assertThat(actionCaptor.firstValue).isEqualTo(testDialogFeature.getAction())
      assertThat(capturedException).isNotNull
    }
  }

  @Test
  fun `test setupAppCallForCustomTabDialog will call CustomTabMainActivity`() {
    val intentCaptor = argumentCaptor<Intent>()
    val params = Bundle()
    DialogPresenter.setupAppCallForCustomTabDialog(
        mockAppCall, testDialogFeature.getAction(), params)
    verify(mockAppCall).requestIntent = intentCaptor.capture()
    assertThat(intentCaptor.firstValue.component?.className)
        .isEqualTo(CustomTabMainActivity::class.java.name)
  }

  @Test
  fun `test getProtocolVersionForNativeDialog will action on NativeProtocol`() {
    val actionCaptor = argumentCaptor<String>()
    val versionSpecCaptor = argumentCaptor<IntArray>()
    PowerMockito.`when`(
            NativeProtocol.getLatestAvailableProtocolVersionForAction(
                actionCaptor.capture(), versionSpecCaptor.capture()))
        .thenReturn(NativeProtocol.ProtocolVersionQueryResult.createEmpty())
    DialogPresenter.getProtocolVersionForNativeDialog(testDialogFeature)
    assertThat(actionCaptor.firstValue).isEqualTo(testDialogFeature.getAction())
    assertThat(versionSpecCaptor.firstValue).containsExactly(testDialogFeature.getMinVersion())
  }

  @Test
  fun `test logDialogActivity`() {
    val mockLogger: InternalAppEventsLogger = mock()
    val parametersCaptor = argumentCaptor<Bundle>()
    PowerMockito.whenNew(InternalAppEventsLogger::class.java)
        .withAnyArguments()
        .thenReturn(mockLogger)
    DialogPresenter.logDialogActivity(mock(), "TEST_EVENT", "TEST_OUTCOME")
    verify(mockLogger).logEventImplicitly(eq("TEST_EVENT"), parametersCaptor.capture())
    assertThat(parametersCaptor.firstValue.getString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME))
        .isEqualTo("TEST_OUTCOME")
  }

  companion object {
    const val REQUEST_CODE = 0xface
    const val APPLICATION_ID = "123456789"
    val testDialogFeature =
        object : DialogFeature {
          override fun getAction(): String = "TEST_ACTION"
          override fun getMinVersion(): Int = 0x7fffffff
          override val name = "TEST_DIALOG"
        }
  }
}
