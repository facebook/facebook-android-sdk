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

package com.facebook.appevents.codeless

import android.app.Activity
import com.facebook.FacebookPowerMockTestCase
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.util.ReflectionHelpers

@PrepareForTest(ViewIndexer::class, CodelessManager::class)
class ViewIndexerTest : FacebookPowerMockTestCase() {

  private lateinit var mockViewIndexer: ViewIndexer
  private lateinit var mockGraphRequest: GraphRequest
  private lateinit var mockGraphResponse: GraphResponse
  private var updateAppIndexingHasBeenCalledTime = 0
  private val currentDigest = "current digest"

  @Before
  fun init() {
    val activity: Activity = mock()
    mockViewIndexer = ViewIndexer(activity)
    mockGraphRequest = mock()
    mockGraphResponse = mock()
    whenCalled(mockGraphRequest.executeAndWait()).thenReturn(mockGraphResponse)

    spy(ViewIndexer::class.java)
    ReflectionHelpers.setStaticField(ViewIndexer::class.java, "instance", mockViewIndexer)

    mockStatic(CodelessManager::class.java)
    updateAppIndexingHasBeenCalledTime = 0
    whenCalled(CodelessManager.updateAppIndexing(any())).then {
      updateAppIndexingHasBeenCalledTime++
    }
  }

  @Test
  fun `processRequest when graph request is null`() {
    mockViewIndexer.processRequest(null, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
    val previousDigest = Whitebox.getInternalState<String>(mockViewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when json object is null`() {
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(null)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
    val previousDigest = Whitebox.getInternalState<String>(mockViewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when success is true`() {
    val jsonObject = JSONObject("{'success': 'true'}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(mockViewIndexer, "previousDigest")
    assertThat(previousDigest).isEqualTo(currentDigest)
  }

  @Test
  fun `processRequest when success is not true`() {
    val jsonObject = JSONObject("{'success': 'false'}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(mockViewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when there is no success in json object`() {
    val jsonObject = JSONObject("{'error': 'user error'}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(mockViewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is true`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': true}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(1, updateAppIndexingHasBeenCalledTime)
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is false`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': false}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(1, updateAppIndexingHasBeenCalledTime)
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is not a boolean`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': 'abc'}")
    whenCalled(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    mockViewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
  }
}
