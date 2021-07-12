/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.ml

import com.facebook.appevents.ml.TensorTestUtils.setTensorData
import org.junit.Assert
import org.junit.Test

class OperatorTest {
  @Test
  fun `test addmv`() {
    val x = MTensor(intArrayOf(2, 3, 4))
    setTensorData(
        x,
        floatArrayOf(
            0f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            11f,
            12f,
            13f,
            14f,
            15f,
            16f,
            17f,
            18f,
            19f,
            20f,
            21f,
            22f,
            23f))
    val bias = MTensor(intArrayOf(4))
    setTensorData(bias, floatArrayOf(1f, 2f, 3f, 4f))
    Operator.addmv(x, bias)
    val expectedData =
        floatArrayOf(
            1f,
            3f,
            5f,
            7f,
            5f,
            7f,
            9f,
            11f,
            9f,
            11f,
            13f,
            15f,
            13f,
            15f,
            17f,
            19f,
            17f,
            19f,
            21f,
            23f,
            21f,
            23f,
            25f,
            27f)
    Assert.assertArrayEquals(expectedData, x.data, 0.0001f)
  }

  @Test
  fun `test mul`() {
    val x = MTensor(intArrayOf(2, 4))
    setTensorData(x, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
    val weight = MTensor(intArrayOf(4, 2))
    setTensorData(weight, floatArrayOf(1f, 0f, 0f, 2f, -1f, 0f, 0f, 2f))
    val y = Operator.mul(x, weight)
    val expectedData = floatArrayOf(-2f, 8f, -2f, 24.000001f)
    Assert.assertArrayEquals(expectedData, y.data, 0.0001f)
  }

  @Test
  fun `test relu`() {
    val x = MTensor(intArrayOf(2, 4))
    setTensorData(x, floatArrayOf(0f, -1f, 2f, -3f, -4f, 5f, -6f, 7f))
    Operator.relu(x)
    val expectedData = floatArrayOf(0f, 0f, 2f, 0f, 0f, 5f, 0f, 7f)
    Assert.assertArrayEquals(expectedData, x.data, 0.0001f)
  }

  @Test
  fun `test flatten`() {
    val x = MTensor(intArrayOf(2, 3, 4))
    setTensorData(
        x,
        floatArrayOf(
            0f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            11f,
            12f,
            13f,
            14f,
            15f,
            16f,
            17f,
            18f,
            19f,
            20f,
            21f,
            22f,
            23f))
    Operator.flatten(x, 1)
    val expectedShape = intArrayOf(2, 12)
    val expectedData =
        floatArrayOf(
            0f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            11f,
            12f,
            13f,
            14f,
            15f,
            16f,
            17f,
            18f,
            19f,
            20f,
            21f,
            22f,
            23f)
    Assert.assertArrayEquals(expectedData, x.data, 0.0001f)
    expectedShape.forEachIndexed { index: Int, i: Int -> Assert.assertEquals(i, x.getShape(index)) }
    Assert.assertEquals(expectedShape.size, x.shapeSize)
  }

  @Test
  fun `test concatenate`() {
    val x1 = MTensor(intArrayOf(2, 4))
    setTensorData(x1, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
    val x2 = MTensor(intArrayOf(2, 3))
    setTensorData(x2, floatArrayOf(16f, 17f, 18f, 20f, 21f, 22f))
    val x3 = MTensor(intArrayOf(2, 2))
    setTensorData(x3, floatArrayOf(4f, 5f, 6f, 7f))
    val y = Operator.concatenate(arrayOf(x1, x2, x3))
    val expectedShape = intArrayOf(2, 9)
    val expectedData =
        floatArrayOf(0f, 1f, 2f, 3f, 16f, 17f, 18f, 4f, 5f, 4f, 5f, 6f, 7f, 20f, 21f, 22f, 6f, 7f)
    Assert.assertArrayEquals(expectedData, y.data, 0.0001f)
    expectedShape.forEachIndexed { index: Int, i: Int -> Assert.assertEquals(i, y.getShape(index)) }
    Assert.assertEquals(expectedShape.size, y.shapeSize)
  }

  @Test
  fun `test softmax`() {
    val x = MTensor(intArrayOf(2, 4))
    setTensorData(x, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
    Operator.softmax(x)
    val expectedData =
        floatArrayOf(
            0.03205860f,
            0.08714432f,
            0.23688284f,
            0.6439143f,
            0.03205860f,
            0.08714432f,
            0.23688284f,
            0.6439143f)
    Assert.assertArrayEquals(expectedData, x.data, 0.00000001f)
  }

  @Test
  fun `test dense`() {
    val x = MTensor(intArrayOf(2, 4))
    setTensorData(x, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
    val weight = MTensor(intArrayOf(4, 2))
    setTensorData(weight, floatArrayOf(1f, 0f, 0f, 2f, -1f, 0f, 0f, 2f))
    val bias = MTensor(intArrayOf(2))
    setTensorData(bias, floatArrayOf(1f, -1f))
    val y = Operator.dense(x, weight, bias)
    val expectedData = floatArrayOf(-1f, 7f, -1f, 23f)
    Assert.assertArrayEquals(expectedData, y.data, 0.0001f)
  }

  @Test
  fun `test transpose 2D`() {
    val input = MTensor(intArrayOf(3, 4))
    setTensorData(input, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f))
    val output = Operator.transpose2D(input)
    val expectedData = floatArrayOf(0f, 4f, 8f, 1f, 5f, 9f, 2f, 6f, 10f, 3f, 7f, 11f)
    Assert.assertArrayEquals(expectedData, output.data, 0.0001f)
  }

  @Test
  fun `test transpose 3D`() {
    val input = MTensor(intArrayOf(2, 3, 4))
    setTensorData(
        input,
        floatArrayOf(
            0f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            11f,
            12f,
            13f,
            14f,
            15f,
            16f,
            17f,
            18f,
            19f,
            20f,
            21f,
            22f,
            23f))
    val output = Operator.transpose3D(input)
    val expectedData =
        floatArrayOf(
            0f,
            12f,
            4f,
            16f,
            8f,
            20f,
            1f,
            13f,
            5f,
            17f,
            9f,
            21f,
            2f,
            14f,
            6f,
            18f,
            10f,
            22f,
            3f,
            15f,
            7f,
            19f,
            11f,
            23f)
    Assert.assertArrayEquals(expectedData, output.data, 0.0001f)
  }

  @Test
  fun `test conv 1D`() {
    val input = MTensor(intArrayOf(1, 5, 3))
    setTensorData(input, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 9f, 8f, 7f, 5f, 8f, 1f, 5f, 3f, 0f))
    val weight = MTensor(intArrayOf(3, 3, 4))
    setTensorData(
        weight,
        floatArrayOf(
            -1f,
            3f,
            0f,
            1f,
            5f,
            -7f,
            5f,
            7f,
            -9f,
            9f,
            2f,
            3f,
            2f,
            4f,
            5f,
            6f,
            6f,
            8f,
            9f,
            4f,
            10f,
            -10f,
            5f,
            6f,
            1f,
            0f,
            5f,
            6f,
            2f,
            5f,
            9f,
            4f,
            9f,
            10f,
            5f,
            6f))
    val output = Operator.conv1D(input, weight)
    val expectedData =
        floatArrayOf(168f, 122f, 263f, 232f, 133f, 111f, 291f, 253f, 47f, 123f, 208f, 196f)
    Assert.assertArrayEquals(expectedData, output.data, 0.0001f)
  }

  @Test
  fun `test max pool 1D`() {
    val input = MTensor(intArrayOf(2, 2, 3))
    setTensorData(input, floatArrayOf(-1f, 2f, 3f, 4f, -5f, 6f, 7f, -8f, 9f, -10f, 11f, 12f))
    val output = Operator.maxPool1D(input, 2)
    val expectedData = floatArrayOf(4f, 2f, 6f, 7f, 11f, 12f)
    Assert.assertArrayEquals(expectedData, output.data, 0.0001f)
  }
}
