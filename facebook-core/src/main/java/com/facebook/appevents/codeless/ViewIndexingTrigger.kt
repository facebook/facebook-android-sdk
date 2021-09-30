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
