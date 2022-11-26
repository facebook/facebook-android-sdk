/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getExecutor
import com.facebook.FacebookSdk.isInitialized
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class InAppPurchaseUtilsTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(isInitialized()).thenReturn(true)
    whenever(getExecutor()).thenReturn(mockExecutor)
  }

  @Test
  fun testGetClass() {
    val myTestClass = InAppPurchaseTestClass(Any())
    assertThat(myTestClass).isNotNull
    val clazz =
        InAppPurchaseUtils.getClass(
            "com.facebook.appevents.iap.InAppPurchaseUtilsTest\$InAppPurchaseTestClass")
    assertThat(clazz).isNotNull
  }

  @Test
  fun testGetMethod() {
    val clazz =
        InAppPurchaseUtils.getClass(
            "com.facebook.appevents.iap.InAppPurchaseUtilsTest\$InAppPurchaseTestClass")
    assertThat(clazz).isNotNull
    val testMethod = clazz?.let { InAppPurchaseUtils.getMethod(it, "inAppPurchaseTestMethod") }
    assertThat(testMethod).isNotNull
  }

  @Test
  fun testInvokeMethod() {
    val myTestClass = InAppPurchaseTestClass(Any())
    val clazz =
        InAppPurchaseUtils.getClass(
            "com.facebook.appevents.iap.InAppPurchaseUtilsTest\$InAppPurchaseTestClass")
    assertThat(clazz).isNotNull
    val testMethod = clazz?.let { InAppPurchaseUtils.getMethod(it, "inAppPurchaseTestMethod") }
    assertThat(testMethod).isNotNull
    val result =
        clazz?.let {
          if (testMethod != null) {
            InAppPurchaseUtils.invokeMethod(it, testMethod, myTestClass)
          } else {
            null
          }
        }
    assertThat(result).isNotNull
  }

  class InAppPurchaseTestClass internal constructor(private val obj: Any) {
    fun inAppPurchaseTestMethod(): String {
      return "Test String"
    }
  }
}
