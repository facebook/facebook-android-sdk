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

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import java.lang.reflect.Method
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class, InAppPurchaseBillingClientWrapper::class, InAppPurchaseUtils::class)
class InAppPurchaseBillingClientWrapperTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  private lateinit var inAppPurchaseBillingClientWrapper: InAppPurchaseBillingClientWrapper

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mock())

    PowerMockito.spy(InAppPurchaseBillingClientWrapper::class.java)
    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "initialized", AtomicBoolean(true))
    inAppPurchaseBillingClientWrapper = mock()
    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java,
        "instance",
        inAppPurchaseBillingClientWrapper)
  }

  @Test
  fun testHelperClassCanSuccessfullyCreateWrapper() {
    // Test InAppPurchaseBillingClientWrapper
    Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNotNull

    // Test BillingClientStateListenerWrapper
    val billingClientStateListenerWrapper =
        InAppPurchaseBillingClientWrapper.BillingClientStateListenerWrapper()
    Assertions.assertThat(billingClientStateListenerWrapper).isNotNull

    // Test PurchasesUpdatedListenerWrapper
    val purchasesUpdatedListenerWrapper =
        InAppPurchaseBillingClientWrapper.PurchasesUpdatedListenerWrapper()
    Assertions.assertThat(purchasesUpdatedListenerWrapper).isNotNull
  }

  @Test
  fun testBillingClientWrapper() {
    val runnable: Runnable = mock()
    val purchaseHistoryResponseListenerWrapper =
        inAppPurchaseBillingClientWrapper.PurchaseHistoryResponseListenerWrapper(runnable)
    Assertions.assertThat(purchaseHistoryResponseListenerWrapper).isNotNull

    Whitebox.setInternalState(
        inAppPurchaseBillingClientWrapper, "historyPurchaseSet", HashSet<Any>())

    val purchaseHistoryRecord =
        "{\"productId\":\"coffee\",\"purchaseToken\":\"aedeglbgcjhjcjnabndchooe.AO-J1Oydf8j_hBxWxvsAvKHLC1h8Kw6YPDtGERpjCWDKSB0Hd6asHyo5E_NjbPg1u1hW5rW-s4go3d0D_DjFstxDA6zn9H_85ReDVbQBdgb2VAAyTX39jcM\",\"purchaseTime\":1614677061238,\"developerPayload\":null}\n"
    val mockList: MutableList<Any> = arrayListOf(purchaseHistoryRecord)
    PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
    whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(purchaseHistoryRecord)
    val mockContext: Context = mock()
    Whitebox.setInternalState(inAppPurchaseBillingClientWrapper, "context", mockContext)
    whenever(mockContext.packageName).thenReturn("value")

    val mockMethod: Method = mock()
    whenever(mockMethod.name).thenReturn("onPurchaseHistoryResponse")
    purchaseHistoryResponseListenerWrapper.invoke(
        mock(), mockMethod, arrayOf(listOf<String>(), mockList))

    val purchaseDetailsMap = InAppPurchaseBillingClientWrapper.purchaseDetailsMap
    Assertions.assertThat(purchaseDetailsMap).isNotEmpty
  }

  @Test
  fun testSkuDetailsResponseListenerWrapper() {
    // Test can successfully create skuDetailsResponseListenerWrapper
    val runnable: Runnable = mock()
    val skuDetailsResponseListenerWrapper =
        inAppPurchaseBillingClientWrapper.SkuDetailsResponseListenerWrapper(runnable)
    Assertions.assertThat(skuDetailsResponseListenerWrapper).isNotNull

    val skuDetailExample =
        "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$0.99\",\"price_amount_micros\":990000,\"price_currency_code\":\"USD\",\"title\":\"cf (coffeeshop)\",\"description\":\"cf\",\"skuDetailsToken\":\"AEuhp4I4Fby7vHeunJbyRTraiO-Z04Y5GPKRYgZtHVCTfmiIhxHj41Rt7kgywkTtIRxP\"}\n"
    val mockList: MutableList<Any> = arrayListOf(skuDetailExample)
    PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
    whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(skuDetailExample)

    val mockMethod: Method = mock()
    whenever(mockMethod.name).thenReturn("onSkuDetailsResponse")
    skuDetailsResponseListenerWrapper.invoke(
        mock(), mockMethod, arrayOf(listOf<String>(), mockList))
    val skuDetailsMap = InAppPurchaseBillingClientWrapper.skuDetailsMap
    Assertions.assertThat(skuDetailsMap).isNotEmpty
  }
}
