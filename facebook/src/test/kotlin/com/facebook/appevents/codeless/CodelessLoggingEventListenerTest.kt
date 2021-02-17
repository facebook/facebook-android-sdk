/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved. <p> You are hereby granted a
 * non-exclusive, worldwide, royalty-free license to use, copy, modify, and distribute this software
 * in source code or binary form for use in connection with the web services and APIs provided by
 * Facebook. <p> As with any software that integrates with the Facebook platform, your use of this
 * software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
 * OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
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
  private val priceValueToSum = 1234.567

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
