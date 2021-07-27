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
import com.nhaarman.mockitokotlin2.verify
import java.net.HttpURLConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
