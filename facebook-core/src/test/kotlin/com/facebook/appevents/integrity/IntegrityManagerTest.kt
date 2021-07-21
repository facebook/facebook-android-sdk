/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.integrity

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.ml.ModelManager
import com.facebook.internal.FetchedAppGateKeepersManager
import com.nhaarman.mockitokotlin2.any
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ModelManager::class, FacebookSdk::class, FetchedAppGateKeepersManager::class)
class IntegrityManagerTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123")

    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    PowerMockito.`when`(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), any(), any()))
        .thenReturn(true)
    IntegrityManager.enable()

    PowerMockito.mockStatic(ModelManager::class.java)
    PowerMockito.`when`(ModelManager.predict(any(), any(), any())).thenAnswer {
      val texts = it.arguments[2] as Array<String>
      val value =
          when (texts[0]) {
            "customer_Address" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_NONE)
            "1 Hacker way" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_ADDRESS)
            "customer_event" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_NONE)
            "event" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_NONE)
            "is_pregnant" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_HEALTH)
            "yes" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_NONE)
            "customer_health" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_NONE)
            "heart attack" -> arrayOf(IntegrityManager.INTEGRITY_TYPE_HEALTH)
            else -> null
          }
      value
    }
  }

  @Test
  fun testAddressDetection() {
    val mockParameters: MutableMap<String, String> =
        hashMapOf("customer_Address" to "1 Hacker way", "customer_event" to "event")

    val jsonObject = JSONObject()
    jsonObject.put("customer_Address", "1 Hacker way")
    val expectedParameters: MutableMap<String, String> =
        hashMapOf("customer_event" to "event", "_onDeviceParams" to jsonObject.toString())

    IntegrityManager.processParameters(mockParameters)

    assertThat(mockParameters.size).isEqualTo(2)
    assertThat(mockParameters).isEqualTo(expectedParameters)
  }

  @Test
  fun testHealthDataFiltering() {
    val mockParameters: MutableMap<String, String> =
        hashMapOf(
            "is_pregnant" to "yes",
            "customer_health" to "heart attack",
            "customer_event" to "event")

    val jsonObject = JSONObject()
    jsonObject.put("is_pregnant", "yes")
    jsonObject.put("customer_health", "heart attack")
    val expectedParameters: MutableMap<String, String> =
        hashMapOf("customer_event" to "event", "_onDeviceParams" to jsonObject.toString())
    IntegrityManager.processParameters(mockParameters)

    assertThat(mockParameters.size).isEqualTo(2)
    assertThat(mockParameters).isEqualTo(expectedParameters)
  }
}
