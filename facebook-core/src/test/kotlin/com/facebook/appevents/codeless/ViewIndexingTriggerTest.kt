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

package com.facebook.appevents.codeless

import android.hardware.SensorEvent
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.powermock.reflect.Whitebox

class ViewIndexingTriggerTest : FacebookPowerMockTestCase() {
  private lateinit var mockOnShakeListener: ViewIndexingTrigger.OnShakeListener
  private lateinit var trigger: ViewIndexingTrigger
  override fun setup() {
    super.setup()
    mockOnShakeListener = mock()
    trigger = ViewIndexingTrigger()
  }

  @Test
  fun `test intense shake event`() {
    trigger.setOnShakeListener(mockOnShakeListener)
    val sensorEvent = mock<SensorEvent>()
    Whitebox.setInternalState(sensorEvent, "values", arrayOf(100f, 200f, 300f).toFloatArray())
    trigger.onSensorChanged(sensorEvent)
    verify(mockOnShakeListener).onShake()
  }

  @Test
  fun `test slight shake event`() {
    trigger.setOnShakeListener(mockOnShakeListener)
    val sensorEvent = mock<SensorEvent>()
    Whitebox.setInternalState(sensorEvent, "values", arrayOf(1e-3f, 1e-2f, 1e-1f).toFloatArray())
    trigger.onSensorChanged(sensorEvent)
    verify(mockOnShakeListener, never()).onShake()
  }

  @Test
  fun `test set on shake listener to null will not crash`() {
    trigger.setOnShakeListener(null)
    trigger.onSensorChanged(mock())
  }
}
