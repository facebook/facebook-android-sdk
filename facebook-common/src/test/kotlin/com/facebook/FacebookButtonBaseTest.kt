/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.facebook.appevents.InternalAppEventsLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

class FacebookButtonBaseTest : FacebookPowerMockTestCase() {
  class TestButton(context: Context, attrs: AttributeSet) :
      FacebookButtonBase(
          context, attrs, 0, 0, BUTTON_CREATED_EVENT_NAME, BUTTON_TAPPED_EVENT_NAME) {
    override val defaultRequestCode: Int = DEFAULT_REQUEST_CODE
    public override val activity: Activity = super.activity

    public override fun setInternalOnClickListener(listener: OnClickListener?) {
      super.setInternalOnClickListener(listener)
    }
  }

  private lateinit var activityController: ActivityController<Activity>
  private lateinit var testAttributeSet: AttributeSet
  private lateinit var testButton: TestButton
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger

  override fun setup() {
    super.setup()
    mockInternalAppEventsLogger = mock()
    val mockInternalAppEventsLoggerCompanion = mock<InternalAppEventsLogger.Companion>()
    whenever(mockInternalAppEventsLoggerCompanion.createInstance(anyOrNull(), anyOrNull()))
        .thenReturn(mockInternalAppEventsLogger)
    Whitebox.setInternalState(
        InternalAppEventsLogger::class.java, "Companion", mockInternalAppEventsLoggerCompanion)

    activityController = Robolectric.buildActivity(Activity::class.java)
    val activity = activityController.get()
    testAttributeSet = Robolectric.buildAttributeSet().build()
    testButton = TestButton(activity, testAttributeSet)
  }

  @Test
  fun `facebook button is clickable and focusable`() {
    assertThat(testButton.isFocusable).isTrue
    assertThat(testButton.isClickable).isTrue
  }

  @Test
  fun `test getting activity will return the host activity`() {
    assertThat(testButton.activity).isEqualTo(activityController.get())
  }

  @Test
  fun `test getting request code will return the default one`() {
    assertThat(testButton.requestCode).isEqualTo(DEFAULT_REQUEST_CODE)
  }

  @Test
  fun `test only the internal onClick listener will be invoked when the button is clicked`() {
    val mockInternalOnClickListener = mock<View.OnClickListener>()
    val mockExternalOnClickListener = mock<View.OnClickListener>()
    testButton.setInternalOnClickListener(mockInternalOnClickListener)
    testButton.setOnClickListener(mockExternalOnClickListener)

    testButton.performClick()

    verify(mockInternalOnClickListener, atLeastOnce()).onClick(testButton)
    verify(mockInternalAppEventsLogger, atLeastOnce()).logEventImplicitly(BUTTON_TAPPED_EVENT_NAME)
    verify(mockExternalOnClickListener, never()).onClick(anyOrNull())
  }

  @Test
  fun `test external onClick listener will be invoked when the button is clicked and the internal listener is not available`() {
    val mockExternalOnClickListener = mock<View.OnClickListener>()
    testButton.setInternalOnClickListener(null)
    testButton.setOnClickListener(mockExternalOnClickListener)

    testButton.performClick()

    verify(mockInternalAppEventsLogger, atLeastOnce()).logEventImplicitly(BUTTON_TAPPED_EVENT_NAME)
    verify(mockExternalOnClickListener, atLeastOnce()).onClick(anyOrNull())
  }

  companion object {
    private const val BUTTON_CREATED_EVENT_NAME = "testAnalyticsButtonCreatedEventName"
    private const val BUTTON_TAPPED_EVENT_NAME = "testAnalyticsButtonTappedEventName"
    private const val DEFAULT_REQUEST_CODE = 42
  }
}
