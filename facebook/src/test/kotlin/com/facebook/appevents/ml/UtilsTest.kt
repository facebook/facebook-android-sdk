package com.facebook.appevents.ml

import com.facebook.FacebookPowerMockTestCase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest : FacebookPowerMockTestCase() {
  @Test
  fun `test normalizeString`() {
    assertEquals("a b c", Utils.normalizeString(" a  b    c "))
    assertEquals("", Utils.normalizeString(""))
    assertEquals("", Utils.normalizeString(" "))
    assertEquals("a b c", Utils.normalizeString("\n\n\n\na b c \t\t"))
    assertEquals("abcd b c", Utils.normalizeString("\n\n\n\nabcd\r\rb    c \t\t"))
  }

  @Test
  fun `test vectorize with larger maxLen`() {
    val actual = Utils.vectorize("Hello", 10)
    val expected = intArrayOf(72, 101, 108, 108, 111, 0, 0, 0, 0, 0)
    assertArrayEquals(expected, actual)
  }

  @Test
  fun `test vectorize with smaller maxLen`() {
    val actual = Utils.vectorize("Hello", 3)
    val expected = intArrayOf(72, 101, 108)
    assertArrayEquals(expected, actual)
  }
}
