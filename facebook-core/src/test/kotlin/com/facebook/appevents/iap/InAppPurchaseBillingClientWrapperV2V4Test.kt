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
import android.content.Context
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import java.lang.reflect.Method
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import java.lang.reflect.Proxy
import java.lang.reflect.Proxy.newProxyInstance


@PrepareForTest(
    FacebookSdk::class,
    InAppPurchaseUtils::class,
    InAppPurchaseSkuDetailsWrapper::class,
    Proxy::class
)
class InAppPurchaseBillingClientWrapperV2V4Test : FacebookPowerMockTestCase() {
    private val mockExecutor: Executor = FacebookSerialExecutor()
    private lateinit var inAppPurchaseBillingClientWrapperV2V4: InAppPurchaseBillingClientWrapperV2V4
    private lateinit var inAppPurchaseSkuDetailsWrapper: InAppPurchaseSkuDetailsWrapper
    private val exampleClassName =
        "com.facebook.appevents.iap.InAppPurchaseBillingClientWrapperV2V4Test"
    private val METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished"
    private val exampleMethodName = "setup"
    private val exampleResponse = "response"
    private val exampleListener = "com.facebook.appevents.iap.PurchasesUpdatedListener"
    private val purchaseHistoryRecord =
        "{\"productId\":\"coffee\",\"purchaseToken\":\"exampleToken\",\"purchaseTime\":1010101,\"developerPayload\":null}"


    @Before
    override fun setup() {
        super.setup()
        PowerMockito.spy(InAppPurchaseBillingClientWrapperV2V4::class.java)
        PowerMockito.mockStatic(FacebookSdk::class.java)
        PowerMockito.mockStatic(InAppPurchaseSkuDetailsWrapper::class.java)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)


        inAppPurchaseSkuDetailsWrapper = mock()
        inAppPurchaseBillingClientWrapperV2V4 = mock()
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            inAppPurchaseBillingClientWrapperV2V4
        )
        Whitebox.setInternalState(
            InAppPurchaseSkuDetailsWrapper::class.java,
            "instance",
            inAppPurchaseSkuDetailsWrapper
        )
        InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap.clear()
        val listenerClazz =
            Class.forName(exampleListener)

        whenever(FacebookSdk.isInitialized()).thenReturn(true)
        whenever(inAppPurchaseSkuDetailsWrapper.skuDetailsParamsClazz).thenReturn(listenerClazz)
        whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
        whenever(FacebookSdk.getApplicationContext()).thenReturn(mock())
        whenever(getClass(anyOrNull())).thenReturn(listenerClazz)
        whenever(
            getMethod(
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Class.forName(exampleClassName)
                .getMethod(exampleMethodName)
        )
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(exampleResponse)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(0)
    }

    @Test
    fun testHelperClassCanSuccessfullyCreateWrapper() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
        )
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapper =
            InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNotNull()
    }

    @Test
    fun testCantGetClass() {
        whenever(getClass(anyOrNull())).thenReturn(null)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
        )
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapper =
            InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNull()
    }

    @Test
    fun testCantGetMethod() {
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(
            getMethod(
                any(),
                any(),
                any()
            )
        ).thenReturn(
            null
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
        )
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapper =
            InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNull()
    }

    @Test
    fun testHelperClassCanSuccessfullyCreateListenerWrappers() {

        // Test BillingClientStateListenerWrapper
        val billingClientStateListenerWrapper =
            InAppPurchaseBillingClientWrapperV2V4.BillingClientStateListenerWrapper(mock())
        Assertions.assertThat(billingClientStateListenerWrapper).isNotNull

        // Test PurchasesUpdatedListenerWrapper
        val purchasesUpdatedListenerWrapper =
            InAppPurchaseBillingClientWrapperV2V4.PurchasesUpdatedListenerWrapper()
        Assertions.assertThat(purchasesUpdatedListenerWrapper).isNotNull
    }

    @Test
    fun testBillingClientStateListenerWrapper() {
        val proxy: Any = mock()
        var test_var = 1
        val runnable = Runnable {
            test_var = 0
        }
        val billingResult: Any = mock()
        val args = arrayOf(billingResult)

        val mockMethod: Method = mock()
        whenever(mockMethod.name).thenReturn(METHOD_ON_BILLING_SETUP_FINISHED)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(getClass(anyOrNull())).thenReturn(
            Class.forName(exampleClassName)
        )
        whenever(getMethod(anyOrNull(), anyOrNull())).thenReturn(mockMethod)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), eq(billingResult))).thenReturn(
            0
        )
        InAppPurchaseBillingClientWrapperV2V4.BillingClientStateListenerWrapper(runnable)
            .invoke(proxy, mockMethod, args)
        Assertions.assertThat(test_var).isEqualTo(0)

    }

    @Test
    fun testQueryPurchases() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
        )
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapper =
            InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNotNull()
        val purchaseList =
            mutableListOf(JSONObject(purchaseHistoryRecord), JSONObject(purchaseHistoryRecord))
        val purchaseResult: Any = mock()
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any(), eq("inapp"))).thenReturn(
            purchaseResult
        )
        InAppPurchaseBillingClientWrapperV2V4.isServiceConnected.set(true)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(
            purchaseHistoryRecord
        )
        whenever(invokeMethod(anyOrNull(), anyOrNull(), eq(purchaseResult))).thenReturn(
            purchaseList
        )
        PowerMockito.mockStatic(Proxy::class.java)
        whenever(newProxyInstance(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

        val runnable: Runnable = mock()
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(purchaseResult)
        inAppPurchaseBillingClientWrapper?.queryPurchases(
            InAppPurchaseUtils.IAPProductType.INAPP,
            runnable
        )
        val purchaseDetailsMap = InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap
        Assertions.assertThat(purchaseDetailsMap).isNotEmpty
        Assertions.assertThat(purchaseDetailsMap["coffee"].toString())
            .isEqualTo(purchaseHistoryRecord)
    }

    @Test
    fun testQuerySubscriptions() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
        )
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapper =
            InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapper).isNotNull()
        val purchaseList =
            mutableListOf(JSONObject(purchaseHistoryRecord), JSONObject(purchaseHistoryRecord))
        val purchaseResult: Any = mock()
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any(), eq("subs"))).thenReturn(
            purchaseResult
        )
        InAppPurchaseBillingClientWrapperV2V4.isServiceConnected.set(true)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(
            purchaseHistoryRecord
        )
        whenever(invokeMethod(anyOrNull(), anyOrNull(), eq(purchaseResult))).thenReturn(
            purchaseList
        )
        PowerMockito.mockStatic(Proxy::class.java)
        whenever(newProxyInstance(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)

        val runnable: Runnable = mock()
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(purchaseResult)
        inAppPurchaseBillingClientWrapper?.queryPurchases(
            InAppPurchaseUtils.IAPProductType.SUBS,
            runnable
        )
        val purchaseDetailsMap = InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap
        Assertions.assertThat(purchaseDetailsMap).isNotEmpty
        Assertions.assertThat(purchaseDetailsMap["coffee"].toString())
            .isEqualTo(purchaseHistoryRecord)
    }

    @Test
    fun testQueryPurchaseHistory() {
        val runnable: Runnable = mock()
        val purchaseHistoryResponseListenerWrapper =
            inAppPurchaseBillingClientWrapperV2V4.PurchaseHistoryResponseListenerWrapper(
                InAppPurchaseUtils.IAPProductType.INAPP,
                runnable
            )
        Assertions.assertThat(purchaseHistoryResponseListenerWrapper).isNotNull

        val mockList: MutableList<Any> = arrayListOf(purchaseHistoryRecord)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(purchaseHistoryRecord)

        val mockMethod: Method = mock()
        whenever(mockMethod.name).thenReturn("onPurchaseHistoryResponse")
        purchaseHistoryResponseListenerWrapper.invoke(
            mock(), mockMethod, arrayOf(listOf<String>(), mockList)
        )

        val purchaseDetailsMap = InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap
        Assertions.assertThat(purchaseDetailsMap).isNotEmpty
        Assertions.assertThat(purchaseDetailsMap["coffee"].toString())
            .isEqualTo(purchaseHistoryRecord)
    }

    @Test
    fun testQueryPurchaseHistoryWithMissingParameters() {
        val runnable: Runnable = mock()
        val purchaseHistoryResponseListenerWrapper =
            inAppPurchaseBillingClientWrapperV2V4.PurchaseHistoryResponseListenerWrapper(
                InAppPurchaseUtils.IAPProductType.INAPP,
                runnable
            )
        Assertions.assertThat(purchaseHistoryResponseListenerWrapper).isNotNull

        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(purchaseHistoryRecord)

        val mockMethod: Method = mock()
        whenever(mockMethod.name).thenReturn("onPurchaseHistoryResponse")

        // Don't include purchase history record list
        purchaseHistoryResponseListenerWrapper.invoke(
            mock(), mockMethod, arrayOf(listOf<String>())
        )

        val purchaseDetailsMap = InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap
        Assertions.assertThat(purchaseDetailsMap).isEmpty()
    }

    @Test
    fun testQuerySubscriptionHistory() {
        val runnable: Runnable = mock()
        val purchaseHistoryResponseListenerWrapper =
            inAppPurchaseBillingClientWrapperV2V4.PurchaseHistoryResponseListenerWrapper(
                InAppPurchaseUtils.IAPProductType.SUBS,
                runnable
            )
        Assertions.assertThat(purchaseHistoryResponseListenerWrapper).isNotNull

        val mockList: MutableList<Any> = arrayListOf(purchaseHistoryRecord)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(purchaseHistoryRecord)

        val mockMethod: Method = mock()
        whenever(mockMethod.name).thenReturn("onPurchaseHistoryResponse")
        purchaseHistoryResponseListenerWrapper.invoke(
            mock(), mockMethod, arrayOf(listOf<String>(), mockList)
        )

        val purchaseDetailsMap = InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap
        Assertions.assertThat(purchaseDetailsMap).isNotEmpty
        Assertions.assertThat(purchaseDetailsMap["coffee"].toString())
            .isEqualTo(purchaseHistoryRecord)
    }

    @Test
    fun testSkuDetailsResponseListenerWrapper() {
        // Test can successfully create skuDetailsResponseListenerWrapper
        val runnable: Runnable = mock()
        val skuDetailsResponseListenerWrapper =
            inAppPurchaseBillingClientWrapperV2V4.SkuDetailsResponseListenerWrapper(runnable)
        Assertions.assertThat(skuDetailsResponseListenerWrapper).isNotNull

        val skuDetailExample =
            "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$0.99\",\"price_amount_micros\":990000,\"price_currency_code\":\"USD\",\"title\":\"cf (coffeeshop)\",\"description\":\"cf\",\"skuDetailsToken\":\"AEuhp4I4Fby7vHeunJbyRTraiO-Z04Y5GPKRYgZtHVCTfmiIhxHj41Rt7kgywkTtIRxP\"}\n"
        val mockList: MutableList<Any> = arrayListOf(skuDetailExample)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(invokeMethod(anyOrNull(), anyOrNull(), any())).thenReturn(skuDetailExample)

        val mockMethod: Method = mock()
        whenever(mockMethod.name).thenReturn("onSkuDetailsResponse")
        skuDetailsResponseListenerWrapper.invoke(
            mock(), mockMethod, arrayOf(listOf<String>(), mockList)
        )
        val skuDetailsMap = InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap
        Assertions.assertThat(skuDetailsMap).isNotEmpty
    }
}
