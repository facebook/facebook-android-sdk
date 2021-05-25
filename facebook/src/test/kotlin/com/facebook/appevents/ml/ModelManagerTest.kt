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
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.internal.FeatureManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FeatureManager::class)
class ModelManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val MOCK_APP_ID = "123456987"
  }
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  private lateinit var mockContext: Context

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getExecutor()).thenReturn(FacebookSerialExecutor())
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID)
    mockGraphRequestCompanion = mock()
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    PowerMockito.mockStatic(FeatureManager::class.java)
  }

  @Test
  fun `test enable() checks ModelRequest feature`() {
    var didCheck = false
    PowerMockito.`when`(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)).thenAnswer {
      didCheck = true
      return@thenAnswer true
    }
    whenever(mockGraphRequestCompanion.newGraphPathRequest(any(), any(), any())).thenReturn(mock())
    ModelManager.enable()
    assertThat(didCheck).isTrue
  }

  @Test
  fun `test enable() fetches model assets with graph request`() {
    PowerMockito.`when`(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest))
        .thenReturn(false)
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
    assertThat(capturedPath).contains(MOCK_APP_ID)
    verify(mockGraphRequest).setSkipClientToken(true)
  }

  @Test
  fun `test enable() checks model store in shared preferences`() {
    PowerMockito.`when`(FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest))
        .thenReturn(false)
    whenever(mockGraphRequestCompanion.newGraphPathRequest(any(), any(), any())).thenReturn(mock())
    mockContext = mock()
    val modelStoreKey = "com.facebook.internal.MODEL_STORE"
    whenever(mockContext.getSharedPreferences(modelStoreKey, Context.MODE_PRIVATE))
        .thenReturn(mock())
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    ModelManager.enable()
    verify(mockContext).getSharedPreferences(modelStoreKey, Context.MODE_PRIVATE)
  }
}
