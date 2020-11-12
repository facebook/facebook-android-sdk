package com.facebook.appevents.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.util.common.assertThrows
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

class HashUtilsTest : FacebookPowerMockTestCase() {

  @Test
  fun `test computeChecksum with non-empty file`() {
    val tempFile = File.createTempFile("tempFile.txt", null)
    tempFile.writeText("Hello World!")
    tempFile.deleteOnExit()

    val expectedChecksum = "ed076287532e86365e841e92bfc50d8c"
    val actualChecksum = HashUtils.computeChecksum(tempFile.path)
    assertEquals(expectedChecksum, actualChecksum)
  }

  @Test
  fun `test computeChecksum with empty file`() {
    val tempFile = File.createTempFile("tempFile.txt", null)
    tempFile.deleteOnExit()

    val emptyFileChecksum = "d41d8cd98f00b204e9800998ecf8427e"
    val actualChecksum = HashUtils.computeChecksum(tempFile.path)
    assertEquals(emptyFileChecksum, actualChecksum)
  }

  @Test
  fun `test computeChecksum with missing file throws exception`() {
    assertThrows<FileNotFoundException> { HashUtils.computeChecksum("/does/not/exist.txt") }
  }
}
