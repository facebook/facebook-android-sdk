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
package com.facebook.appevents.iap

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getExecutor
import com.facebook.FacebookSdk.isInitialized
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
