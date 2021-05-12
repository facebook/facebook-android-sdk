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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.lang.IllegalArgumentException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.reflect.Whitebox

class GraphRequestBatchTest : FacebookPowerMockTestCase() {
  private lateinit var batch: GraphRequestBatch
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  @Before
  override fun setup() {
    super.setup()
    mockGraphRequestCompanion = mock()
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    batch = GraphRequestBatch()
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test set timeout with invalid value`() {
    batch.timeout = -1
  }

  @Test
  fun `test set timeout with valid value`() {
    batch.timeout = 10
    Assert.assertEquals(10, batch.timeout)
  }

  @Test
  fun `test adding duplicate callbacks`() {
    val callback = mock<GraphRequestBatch.Callback>()
    batch.addCallback(callback)
    batch.addCallback(callback)

    Assert.assertEquals(1, batch.callbacks.size)
  }

  @Test
  fun `test constructor with requests`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1, request2)
    Assert.assertEquals(request1, batch.requests[0])
    Assert.assertEquals(request2, batch.requests[1])
  }

  @Test
  fun `test execute and wait calls GraphRequest`() {
    val mockGraphResponses = mock<List<GraphResponse>>()
    whenever(mockGraphRequestCompanion.executeBatchAndWait(batch)).thenReturn(mockGraphResponses)
    val responses = batch.executeAndWait()
    Assert.assertEquals(mockGraphResponses, responses)
  }

  @Test
  fun `test execute async calls GraphRequest`() {
    val mockGraphRequestAsyncTask = mock<GraphRequestAsyncTask>()
    whenever(mockGraphRequestCompanion.executeBatchAsync(batch))
        .thenReturn(mockGraphRequestAsyncTask)
    val asyncTask = batch.executeAsync()
    Assert.assertEquals(mockGraphRequestAsyncTask, asyncTask)
  }

  @Test
  fun `test remove GraphRequest from the batch`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1, request2)
    val popRequest = batch.removeAt(0)
    Assert.assertEquals(request1, popRequest)
    Assert.assertTrue(batch.remove(request2))
    Assert.assertFalse(batch.remove(request1))
  }
}
