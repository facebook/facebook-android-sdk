/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookTestCase
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AggregateExceptionTest : FacebookTestCase() {

  @Test
  fun `test AggregateException pass the first throwable as cause`() {
    val throwables = listOf<Throwable>(RuntimeException("first"), RuntimeException("second"))
    val exception = AggregateException("unknown", throwables)
    assertThat(exception.cause?.message).isEqualTo("first")
  }

  @Test
  fun `test null as throwables`() {
    val exception = AggregateException("unknown", null)
    assertThat(exception.cause).isNull()
  }

  @Test
  fun `test null as throwables elements`() {
    val exception = AggregateException("unknown", listOf(null))
    assertThat(exception.cause).isNull()
    val testBuffer = ByteArrayOutputStream()
    val testPrintStream = PrintStream(testBuffer)
    exception.printStackTrace(testPrintStream)
  }

  @Test
  fun `test all throwables are printed`() {
    val throwables = listOf<Throwable>(RuntimeException("first%"), RuntimeException("second^"))
    val exception = AggregateException("unknown", throwables)
    val testBuffer = ByteArrayOutputStream()
    val testPrintStream = PrintStream(testBuffer)
    exception.printStackTrace(testPrintStream)
    testPrintStream.close()
    val printedData = testBuffer.toString()
    assertThat(printedData).contains("first%")
    assertThat(printedData).contains("second^")
  }
}
