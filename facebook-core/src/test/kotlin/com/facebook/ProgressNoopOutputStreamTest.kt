/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
    assertThat(stream.getProgressMap().isEmpty()).isTrue
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
