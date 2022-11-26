/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.hardware.SensorEvent
import com.facebook.FacebookPowerMockTestCase
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
