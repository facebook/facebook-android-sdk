/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import kotlin.math.sqrt

@AutoHandleExceptions
internal class ViewIndexingTrigger : SensorEventListener {
  private var onShakeListener: OnShakeListener? = null
  fun setOnShakeListener(listener: OnShakeListener?) {
    onShakeListener = listener
  }

  fun interface OnShakeListener {
    fun onShake()
  }

  override fun onSensorChanged(event: SensorEvent) {
    onShakeListener?.let {
      val x = event.values[0]
      val y = event.values[1]
      val z = event.values[2]
      val gX = (x / SensorManager.GRAVITY_EARTH).toDouble()
      val gY = (y / SensorManager.GRAVITY_EARTH).toDouble()
      val gZ = (z / SensorManager.GRAVITY_EARTH).toDouble()

      // gForce will be close to 1 when there is no movement.
      val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
      if (gForce > SHAKE_THRESHOLD_GRAVITY) {
        it.onShake()
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    // no op
  }

  companion object {
    private const val SHAKE_THRESHOLD_GRAVITY = 2.3
  }
}
