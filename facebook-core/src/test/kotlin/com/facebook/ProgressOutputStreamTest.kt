/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private const val MAX_PROGRESS = 10L
  }

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
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
