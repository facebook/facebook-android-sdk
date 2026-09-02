/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
  fun `test computeChecksum preserves leading zeros in digest`() {
    // MD5("a") = 0cc175b9c0f1b6a831c399e269772661 (one leading zero nibble) and
    // MD5("jk8ssl") = 0000000018e6137ac2caab16074784a6 (eight leading zero nibbles).
    // A digest with leading zeros must still render as a full 32-character hex string.
    val singleZero = File.createTempFile("tempFile.txt", null)
    singleZero.writeText("a")
    singleZero.deleteOnExit()
    assertEquals("0cc175b9c0f1b6a831c399e269772661", HashUtils.computeChecksum(singleZero.path))

    val manyZeros = File.createTempFile("tempFile.txt", null)
    manyZeros.writeText("jk8ssl")
    manyZeros.deleteOnExit()
    assertEquals("0000000018e6137ac2caab16074784a6", HashUtils.computeChecksum(manyZeros.path))
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
