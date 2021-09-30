package com.facebook.appevents.ml

import com.facebook.FacebookPowerMockTestCase
import com.facebook.util.common.assertThrows
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.powermock.reflect.Whitebox

class MTensorTest : FacebookPowerMockTestCase() {

  @Test
  fun `test reshape() and getData`() {
    // Capacity: 24 (2 * 3 * 4)
    val tensor = MTensor(intArrayOf(2, 3, 4))
    val data =
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
    Whitebox.setInternalState(tensor, "data", data)

    // Reshape to capacity of 16 (2 * 2 * 4)
    tensor.reshape(intArrayOf(2, 2, 4))
    var expectedData =
        floatArrayOf(1f, 3f, 5f, 7f, 5f, 7f, 9f, 11f, 9f, 11f, 13f, 15f, 13f, 15f, 17f, 19f)
    assertArrayEquals(tensor.data, expectedData, 0.0001f)
    assertEquals(Whitebox.getInternalState<Any>(tensor, "capacity"), 16)

    // Reshape to capacity of 24 (2 * 3 * 4)
    tensor.reshape(intArrayOf(2, 3, 4))
    expectedData =
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
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f)
    assertArrayEquals(tensor.data, expectedData, 0.0001f)
    assertEquals(Whitebox.getInternalState<Any>(tensor, "capacity"), 24)
  }

  @Test
  fun `test resizing to large capacity can overflow`() {
    val tensor = MTensor(intArrayOf(1, 2, 3))
    val data = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
    Whitebox.setInternalState(tensor, "data", data)

    // Capacity becomes -6 due to int owerflow (2 * 3 * Int.MAX_VALUE)
    assertThrows<NegativeArraySizeException> { tensor.reshape(intArrayOf(2, 3, Int.MAX_VALUE)) }
  }

  @Test
  fun `test getShape`() {
    val tensor = MTensor(intArrayOf(2, 3, 4))
    assertEquals(2, tensor.getShape(0))
    assertEquals(3, tensor.getShape(1))
    assertEquals(4, tensor.getShape(2))
  }

  @Test
  fun `test getShapeSize`() {
    val tensor = MTensor(intArrayOf(2, 3, 4))
    assertEquals(3, tensor.shapeSize)
  }
}
