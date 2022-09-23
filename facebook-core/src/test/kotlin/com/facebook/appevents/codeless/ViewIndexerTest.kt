/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.app.Activity
import com.facebook.FacebookPowerMockTestCase
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(CodelessManager::class)
class ViewIndexerTest : FacebookPowerMockTestCase() {

  private lateinit var viewIndexer: ViewIndexer
  private lateinit var mockGraphRequest: GraphRequest
  private lateinit var mockGraphResponse: GraphResponse
  private var updateAppIndexingHasBeenCalledTime = 0
  private val currentDigest = "current digest"

  @Before
  fun init() {
    val activity: Activity = mock()
    viewIndexer = ViewIndexer(activity)
    mockGraphRequest = mock()
    mockGraphResponse = mock()
    whenever(mockGraphRequest.executeAndWait()).thenReturn(mockGraphResponse)

    mockStatic(CodelessManager::class.java)
    updateAppIndexingHasBeenCalledTime = 0
    whenever(CodelessManager.updateAppIndexing(any())).then { updateAppIndexingHasBeenCalledTime++ }
  }

  @Test
  fun `processRequest when graph request is null`() {
    viewIndexer.processRequest(null, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
    val previousDigest = Whitebox.getInternalState<String>(viewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when json object is null`() {
    whenever(mockGraphResponse.getJSONObject()).thenReturn(null)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
    val previousDigest = Whitebox.getInternalState<String>(viewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when success is true`() {
    val jsonObject = JSONObject("{'success': 'true'}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(viewIndexer, "previousDigest")
    assertThat(previousDigest).isEqualTo(currentDigest)
  }

  @Test
  fun `processRequest when success is not true`() {
    val jsonObject = JSONObject("{'success': 'false'}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(viewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when there is no success in json object`() {
    val jsonObject = JSONObject("{'error': 'user error'}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    val previousDigest = Whitebox.getInternalState<String>(viewIndexer, "previousDigest")
    assertThat(previousDigest).isNull()
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is true`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': true}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(1, updateAppIndexingHasBeenCalledTime)
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is false`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': false}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(1, updateAppIndexingHasBeenCalledTime)
  }

  @Test
  fun `processRequest when is_app_indexing_enabled is not a boolean`() {
    val jsonObject = JSONObject("{'is_app_indexing_enabled': 'abc'}")
    whenever(mockGraphResponse.getJSONObject()).thenReturn(jsonObject)
    viewIndexer.processRequest(mockGraphRequest, currentDigest)
    assertEquals(0, updateAppIndexingHasBeenCalledTime)
  }
}
