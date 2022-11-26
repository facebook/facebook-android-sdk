/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import java.net.HttpURLConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.powermock.reflect.Whitebox

class GraphRequestAsyncTaskTest : FacebookPowerMockTestCase() {
  @Test
  fun `test creating async tasks with requests`() {
    val r1 = mock<GraphRequest>()
    val r2 = mock<GraphRequest>()
    val r3 = mock<GraphRequest>()
    val task = GraphRequestAsyncTask(r1, r2, r3)
    assertThat(task.requests[0]).isEqualTo(r1)
    assertThat(task.requests[1]).isEqualTo(r2)
    assertThat(task.requests[2]).isEqualTo(r3)
  }

  @Test
  fun `test creating async task with request batch and executing`() {
    val mockBatch = mock<GraphRequestBatch>()
    val task = GraphRequestAsyncTask(mockBatch)
    task.doInBackground()
    verify(mockBatch).executeAndWait()
  }

  @Test
  fun `test onPreExecute set handler for the requests`() {
    val task = GraphRequestAsyncTask(mock<GraphRequest>())
    task.onPreExecute()
    assertThat(task.requests.callbackHandler).isNotNull
  }

  @Test
  fun `test executing requests with existing connection`() {
    val mockGraphRequestCompanion = mock<GraphRequest.Companion>()
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    val connection = mock<HttpURLConnection>()
    val task = GraphRequestAsyncTask(connection, mock<GraphRequest>())
    task.doInBackground()
    verify(mockGraphRequestCompanion).executeConnectionAndWait(connection, task.requests)
  }
}
