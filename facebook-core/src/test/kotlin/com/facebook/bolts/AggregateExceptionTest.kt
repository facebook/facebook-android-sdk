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
