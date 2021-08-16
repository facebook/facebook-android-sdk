package com.facebook.appevents.ml

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.ml.TensorTestUtils.createModelFile
import com.facebook.appevents.ml.TensorTestUtils.createTestTensor
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class UtilsTest : FacebookPowerMockTestCase() {
  private lateinit var utilsTestFileDir: File

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    utilsTestFileDir = File(UUID.randomUUID().toString())
    utilsTestFileDir.mkdirs()
    val mockContext = PowerMockito.mock(Context::class.java)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenever(mockContext.filesDir).thenReturn(utilsTestFileDir)
  }

  @After
  fun teardown() {
    utilsTestFileDir.deleteRecursively()
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

  @Test
  fun `test parseModelWeights`() {
    val conv1Weights = createTestTensor(intArrayOf(32, 20))
    val conv1Bias = createTestTensor(intArrayOf(20))

    val model = hashMapOf("conv1.weights" to conv1Weights, "conv1.bias" to conv1Bias)
    val modelFile = createModelFile(model, utilsTestFileDir)
    val parsedWeights = Utils.parseModelWeights(modelFile)
    checkNotNull(parsedWeights)
    assertThat(parsedWeights.keys).isEqualTo(model.keys)
    for (key in parsedWeights.keys) {
      assertThat(parsedWeights[key]?.data).isEqualTo(model[key]?.data)
    }
  }

  @Test
  fun `test parse illegal model weights`() {
    val modelFile = File(utilsTestFileDir, "testIllegalModelFile")
    val fileOutputStream = FileOutputStream(modelFile)
    fileOutputStream.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 8))
    fileOutputStream.close()
    val parsedWeights = Utils.parseModelWeights(modelFile)
    assertThat(parsedWeights).isNull()
  }
}
