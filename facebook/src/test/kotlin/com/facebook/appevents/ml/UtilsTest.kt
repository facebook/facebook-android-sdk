package com.facebook.appevents.ml

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.File
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class UtilsTest : FacebookPowerMockTestCase() {
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    val utilsTestFileDir = File(UUID.randomUUID().toString())
    val mockContext = PowerMockito.mock(Context::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    PowerMockito.`when`(mockContext.filesDir).thenReturn(utilsTestFileDir)
    utilsTestFileDir.deleteOnExit()
  }

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
  fun `test vectorize with non-ascii characters`() {
    val actual = Utils.vectorize("αβγ㍻", 9)
    val expected = intArrayOf(0xCE, 0xB1, 0xCE, 0xB2, 0xCE, 0xB3, 0xE3, 0x8D, 0xBB)
    assertArrayEquals(expected, actual)
  }

  @Test
  fun `test vectorize with smaller maxLen`() {
    val actual = Utils.vectorize("Hello", 3)
    val expected = intArrayOf(72, 101, 108)
    assertArrayEquals(expected, actual)
  }

  @Test
  fun `test get ml dir create the path`() {
    val mlDir = Utils.getMlDir()
    checkNotNull(mlDir)
    assertThat(mlDir).exists().isDirectory
    mlDir.deleteRecursively()
  }
}
