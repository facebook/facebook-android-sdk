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

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ProgressOutputStreamTest : FacebookPowerMockTestCase() {
  private lateinit var r1: GraphRequest
  private lateinit var r2: GraphRequest
  private lateinit var progressMap: MutableMap<GraphRequest, RequestProgress>
  private lateinit var requests: GraphRequestBatch
  private lateinit var stream: ProgressOutputStream

  companion object {
    private const val MAX_PROGRESS = 10.toLong()
  }

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    r1 = GraphRequest(null, "4")
    r2 = GraphRequest(null, "4")
    progressMap = hashMapOf(r1 to RequestProgress(null, r1), r2 to RequestProgress(null, r2))
    progressMap[r1]?.addToMax(5)
    progressMap[r2]?.addToMax(5)
    requests = GraphRequestBatch(r1, r2)
    val backing = ByteArrayOutputStream()
    stream = ProgressOutputStream(backing, requests, progressMap, MAX_PROGRESS)
  }

  @After
  fun after() {
    stream.close()
  }

  @Test
  fun `test setup stream`() {
    Assert.assertEquals(0, stream.batchProgress)
    Assert.assertEquals(MAX_PROGRESS, stream.maxProgress)
    for (p in progressMap.values) {
      Assert.assertEquals(0, p.progress)
      Assert.assertEquals(5, p.maxProgress)
    }
  }

  @Test
  fun `test writing to stream`() {
    try {
      Assert.assertEquals(0, stream.batchProgress)
      stream.setCurrentRequest(r1)
      stream.write(0)
      Assert.assertEquals(1, stream.batchProgress)
      val buf = ByteArray(4)
      stream.write(buf)
      Assert.assertEquals(5, stream.batchProgress)
      stream.setCurrentRequest(r2)
      stream.write(buf, 2, 2)
      stream.write(buf, 1, 3)
      Assert.assertEquals(MAX_PROGRESS, stream.batchProgress)
      Assert.assertEquals(stream.maxProgress, stream.batchProgress)
      val progress1 = checkNotNull(progressMap[r1])
      val progress2 = checkNotNull(progressMap[r2])
      Assert.assertEquals(progress1.maxProgress, progress1.progress)
      Assert.assertEquals(progress2.maxProgress, progress2.progress)
    } catch (ex: Exception) {
      Assert.fail(ex.message)
    }
  }

  @Test
  fun `test close does all clean work`() {
    val mockProgress1 = mock<RequestProgress>()
    val mockProgress2 = mock<RequestProgress>()
    val progressMap = hashMapOf(r1 to mockProgress1, r2 to mockProgress2)
    val mockOutputStream = mock<OutputStream>()
    val stream = ProgressOutputStream(mockOutputStream, requests, progressMap, MAX_PROGRESS)
    stream.close()
    verify(mockOutputStream).close()
    verify(mockProgress1).reportProgress()
    verify(mockProgress2).reportProgress()
  }
}
