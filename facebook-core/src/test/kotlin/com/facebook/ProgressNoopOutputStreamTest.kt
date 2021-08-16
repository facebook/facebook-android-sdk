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
package com.facebook

import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ProgressNoopOutputStreamTest : FacebookPowerMockTestCase() {
  private lateinit var stream: ProgressNoopOutputStream
  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    stream = ProgressNoopOutputStream(null)
    val request = PowerMockito.mock(GraphRequest::class.java)
    stream.setCurrentRequest(request)
  }

  @After
  fun after() {
    stream.close()
  }

  @Test
  fun testSetup() {
    Assert.assertEquals(0, stream.maxProgress)
    Assert.assertTrue(stream.getProgressMap().isEmpty())
  }

  @Test
  fun testWriting() {
    Assert.assertEquals(0, stream.maxProgress)
    stream.write(0)
    Assert.assertEquals(1, stream.maxProgress)
    val buf = ByteArray(8)
    stream.write(buf)
    Assert.assertEquals(9, stream.maxProgress)
    stream.write(buf, 2, 2)
    Assert.assertEquals(11, stream.maxProgress)
    stream.addProgress(16)
    Assert.assertEquals(27, stream.maxProgress)
  }
}
