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

package com.facebook.appevents.ml

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.MockSharedPreference
import com.facebook.internal.FeatureManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FeatureManager::class)
class ModelManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val MOCK_APP_ID = "123456987"

    // Created based on the parsing code in ModelManager.parseRawJsonObject()
    private const val MOCK_MODEL_JSON_AS_TEXT =
        """
    {
      "data": [{
        "version_id": "mock_version",
        "use_case": "mock_use_case",      
        "thresholds": ["mock_threshold_1", "mock_threshold_2"],
        "asset_uri": "mock_asset_uri"
      }]
    }
    """

    // The parsing code transforms the model JSON into this expected format
    //  - Top level key is the use case value (data key is removed)
    //  - All whitespace is removed (not sure if this is side effect is important or not)
    private val EXPECTED_MODEL_JSON_AS_TEXT =
        """
    {"mock_use_case":{"version_id":"mock_version","use_case":"mock_use_case","thresholds":["mock_threshold_1","mock_threshold_2"],"asset_uri":"mock_asset_uri"}}
    """.trim()

    // Mirror these constant keys so we can make sure they don't get accidentally changed
    private const val MODEL_ASSET_STORE_KEY = "com.facebook.internal.MODEL_STORE"
    private const val CACHE_KEY_MODEL_REQUEST_TIMESTAMP = "model_request_timestamp"
    private const val CACHE_KEY_MODELS = "models"
  }
  @Mock private lateinit var mockApplicationContext: Context
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  private lateinit var mockSharedPreferences: MockSharedPreference

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getExecutor()).thenReturn(FacebookSerialExecutor())
    whenever(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    mockSharedPreferences = MockSharedPreference()
    whenever(mockApplicationContext.getSharedPreferences(anyString(), any()))
        .thenReturn(mockSharedPreferences)
    mockGraphRequestCompanion = mock()
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    PowerMockito.mockStatic(FeatureManager::class.java)
  }

  @Test
  fun `test enable() checks ModelRequest feature`() {
    var didCheck = false
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)).thenAnswer {
      didCheck = true
      return@thenAnswer true
    }
    whenever(mockGraphRequestCompanion.newGraphPathRequest(any(), any(), any())).thenReturn(mock())
    ModelManager.enable()
    assertThat(didCheck).isTrue
  }

  @Test
  fun `test enable() fetches model assets with graph request`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)).thenReturn(false)
    var capturedPath: String? = null
    val mockGraphRequest: GraphRequest = mock()
    whenever(mockGraphRequestCompanion.newGraphPathRequest(eq(null), any(), eq(null))).thenAnswer {
      capturedPath = it.arguments[1].toString()
      mockGraphRequest
    }
    whenever(mockGraphRequest.executeAndWait()).thenReturn(mock())

    ModelManager.enable()
    checkNotNull(capturedPath)
    assertThat(capturedPath).contains("model_asset")
  }

  @Test
  fun `test enable() checks model store in shared preferences`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)).thenReturn(false)
    whenever(mockGraphRequestCompanion.newGraphPathRequest(any(), any(), any())).thenReturn(mock())
    whenever(
            mockApplicationContext.getSharedPreferences(
                MODEL_ASSET_STORE_KEY, Context.MODE_PRIVATE))
        .thenReturn(mock())
    ModelManager.enable()
    verify(mockApplicationContext).getSharedPreferences(MODEL_ASSET_STORE_KEY, Context.MODE_PRIVATE)
  }

  @Test
  fun `test getRuleFile() retrieves appropriate taskhandler rule file`() {
    // Rather than mocking this private class field, I'd prefer to have it be populated with
    // some meaningful values, and then test how it behaves.  However, these unit tests do not
    // currently have a way to fill the ModelManager with realistic configuration data.
    val mockTaskHandlers: ConcurrentHashMap<Any, Any> = mock()
    Whitebox.setInternalState(ModelManager::class.java, "taskHandlers", mockTaskHandlers)
    val integrityFile = File("MTML_INTEGRITY_DETECT")
    val mockTaskHandler: ModelManager.TaskHandler = mock()
    whenever(mockTaskHandlers[ModelManager.Task.MTML_INTEGRITY_DETECT.toUseCase()])
        .thenReturn(mockTaskHandler)
    whenever(mockTaskHandler.ruleFile).thenReturn(integrityFile)
    val resultFile = ModelManager.getRuleFile(ModelManager.Task.MTML_INTEGRITY_DETECT)
    assertThat(resultFile).isEqualTo(integrityFile)
  }

  private fun setupMocksForModelFetch(modelRequestTimestamp: Long) {
    mockSharedPreferences
        .edit()
        .putLong(CACHE_KEY_MODEL_REQUEST_TIMESTAMP, modelRequestTimestamp)
        // Return some JSON, as though the model cache contained something
        .putString(CACHE_KEY_MODELS, MOCK_MODEL_JSON_AS_TEXT)
        .apply()

    // Force ModelManager to exercise its cache logic
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)).thenReturn(true)

    val mockGraphRequest: GraphRequest = mock()
    whenever(mockGraphRequestCompanion.newGraphPathRequest(eq(null), any(), eq(null))).thenAnswer {
      mockGraphRequest
    }

    // Mock the GraphResponse to return JSON so that fetchModels() can complete successfully
    val mockGraphResponse: GraphResponse = mock()
    val mockModelJson = JSONObject(MOCK_MODEL_JSON_AS_TEXT)
    whenever(mockGraphResponse.getJSONObject()).thenReturn(mockModelJson)
    whenever(mockGraphRequest.executeAndWait()).thenReturn(mockGraphResponse)
  }

  private fun assertThatModelWasFetchedAndCached() {
    assertThat(mockSharedPreferences.getString(CACHE_KEY_MODELS, null))
        .isEqualTo(EXPECTED_MODEL_JSON_AS_TEXT)
    assertThat(mockSharedPreferences.getLong(CACHE_KEY_MODEL_REQUEST_TIMESTAMP, 0L))
        .isGreaterThan(0L)
  }

  @Test
  fun `test enable() fetches model assets when cache request timestamp is zero`() {
    // Use a timestamp of zero, as though the cache was empty
    setupMocksForModelFetch(0L)
    ModelManager.enable()
    assertThatModelWasFetchedAndCached()
  }

  @Test
  fun `test enable() fetches model assets when cache is expired`() {
    // Set the cache timestamp to be 1 millisecond older than the expiration interval
    val justExpiredTimestamp: Long = (ModelManager.MODEL_REQUEST_INTERVAL_MILLISECONDS + 1).toLong()
    assertThat(justExpiredTimestamp).isEqualTo(259200001)

    setupMocksForModelFetch(justExpiredTimestamp)
    ModelManager.enable()
    assertThatModelWasFetchedAndCached()
    // Make sure we don't still have the expired model cache timestamp saved
    assertThat(mockSharedPreferences.getLong(CACHE_KEY_MODEL_REQUEST_TIMESTAMP, 0L))
        .isNotEqualTo(justExpiredTimestamp)
  }

  @Test
  fun `test enable() does not fetch model assets when cache is present`() {
    // This recent cache time should result in the cached model getting used
    val recentlyCachedTime = System.currentTimeMillis() - 1000
    setupMocksForModelFetch(recentlyCachedTime)
    ModelManager.enable()
    assertThat(mockSharedPreferences.getString(CACHE_KEY_MODELS, null))
        .isEqualTo(MOCK_MODEL_JSON_AS_TEXT)
    assertThat(mockSharedPreferences.getLong(CACHE_KEY_MODEL_REQUEST_TIMESTAMP, 0L))
        .isEqualTo(recentlyCachedTime)
  }
}
