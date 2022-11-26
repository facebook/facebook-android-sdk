/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ml

import kotlin.math.min

class MTensor(private var shape: IntArray) {
  private var capacity: Int = getCapacity(shape)
  var data: FloatArray = FloatArray(capacity)
    private set
  val shapeSize: Int
    get() = shape.size

  fun getShape(i: Int) = shape[i]

  /**
   * Reshape the shape
   *
   * @param shape to reshape
   */
  fun reshape(shape: IntArray) {
    this.shape = shape
    val newCapacity = getCapacity(shape)
    val newData = FloatArray(newCapacity)
    System.arraycopy(data, 0, newData, 0, min(capacity, newCapacity))
    data = newData
    capacity = newCapacity
  }

  companion object {
    private fun getCapacity(shape: IntArray) = shape.reduce { acc, i -> acc * i }
  }
}
