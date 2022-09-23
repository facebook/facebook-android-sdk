/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.ml.ModelManager
import com.facebook.internal.FetchedAppGateKeepersManager
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ModelManager::class, FacebookSdk::class, FetchedAppGateKeepersManager::class)
class IntegrityManagerTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123")

    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    whenever(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), any(), any())).thenReturn(true)
    IntegrityManager.enable()

    PowerMockito.mockStatic(ModelManager::class.java)
    whenever(ModelManager.predict(any(), any(), any())).thenAnswer {
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
