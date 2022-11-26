/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
    assertThat(batch.timeout).isEqualTo(10)
  }

  @Test
  fun `test adding duplicate callbacks`() {
    val callback = mock<GraphRequestBatch.Callback>()
    batch.addCallback(callback)
    batch.addCallback(callback)
    assertThat(batch.callbacks.size).isEqualTo(1)
  }

  @Test
  fun `test remove callbacks`() {
    val callback = mock<GraphRequestBatch.Callback>()
    batch.addCallback(callback)
    batch.removeCallback(callback)

    assertThat(batch.callbacks.size).isEqualTo(0)
  }

  @Test
  fun `test constructor with requests`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1, request2)
    assertThat(batch.requests[0]).isEqualTo(request1)
    assertThat(batch.requests[1]).isEqualTo(request2)
  }

  @Test
  fun `test copy constructor`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    val batch1 = GraphRequestBatch(request1, request2)
    val callback1 = mock<GraphRequestBatch.Callback>()
    batch1.addCallback(callback1)
    val batch2 = GraphRequestBatch(batch1)

    assertThat(batch2[0]).isEqualTo(request1)
    assertThat(batch2[1]).isEqualTo(request2)
    assertThat(batch2.callbacks[0]).isEqualTo(callback1)

    // batch 1 won't be changed after batch2 is changed
    batch2.addCallback(mock())
    batch2.add(mock())
    assertThat(batch1.size).isEqualTo(2)
    assertThat(batch1.callbacks.size).isEqualTo(1)
  }

  @Test
  fun `test execute and wait calls GraphRequest`() {
    batch.executeAndWait()
    verify(mockGraphRequestCompanion).executeBatchAndWait(batch)
  }

  @Test
  fun `test execute async calls GraphRequest`() {
    batch.executeAsync()
    verify(mockGraphRequestCompanion).executeBatchAsync(batch)
  }

  @Test
  fun `test GraphRequest from the batch`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1, request2)
    val popRequest = batch.removeAt(0)
    assertThat(popRequest).isEqualTo(request1)
    assertThat(batch.remove(request2)).isTrue
    assertThat(batch.remove(request1)).isFalse
  }

  @Test
  fun `test clear`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1, request2)
    batch.clear()
    assertThat(batch.size).isEqualTo(0)
  }

  @Test
  fun `test add to specific position`() {
    val request1 = mock<GraphRequest>()
    val request2 = mock<GraphRequest>()
    batch = GraphRequestBatch(request1)
    batch.add(0, request2)
    assertThat(batch[0]).isEqualTo(request2)
    assertThat(batch[1]).isEqualTo(request1)
  }
}
