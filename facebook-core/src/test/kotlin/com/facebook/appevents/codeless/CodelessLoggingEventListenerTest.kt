/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.codeless.internal.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CodelessLoggingEventListenerTest : FacebookPowerMockTestCase() {
  private lateinit var expectedParameters: Bundle
  private val priceString = "price: $1,234.567. Good deal!"
  private val priceValueToSum = 1_234.567

  @Before
  fun init() {
    // reset expected parameters
    expectedParameters = Bundle()
    expectedParameters.putString(Constants.IS_CODELESS_EVENT_KEY, "1")
  }

  @Test
  fun `updateParameters with _valueToSum`() {
    val parameters = Bundle()
    parameters.putString(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, priceString)

    CodelessLoggingEventListener.updateParameters(parameters)

    expectedParameters.putDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, priceValueToSum)

    assertEquals(
        expectedParameters.get(Constants.IS_CODELESS_EVENT_KEY),
        parameters.get(Constants.IS_CODELESS_EVENT_KEY))
    assertEquals(
        expectedParameters.get(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM),
        parameters.get(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM))
  }

  @Test
  fun `updateParameters without _valueToSum`() {
    val parameters = Bundle()
    CodelessLoggingEventListener.updateParameters(parameters)

    assertEquals(
        expectedParameters.get(Constants.IS_CODELESS_EVENT_KEY),
        parameters.get(Constants.IS_CODELESS_EVENT_KEY))
    assertNull(expectedParameters.get(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM))
  }
}
