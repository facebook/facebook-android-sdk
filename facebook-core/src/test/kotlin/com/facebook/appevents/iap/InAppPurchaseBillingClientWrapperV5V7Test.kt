/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
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

@PrepareForTest(FacebookSdk::class, InAppPurchaseUtils::class, Proxy::class)
class InAppPurchaseBillingClientWrapperV5V7Test : FacebookPowerMockTestCase() {
    private val exampleClassName =
        "com.facebook.appevents.iap.InAppPurchaseBillingClientWrapperV5V7Test"
    private val exampleListener = "com.facebook.appevents.iap.PurchasesUpdatedListener"
    private val exampleMethodName = "setup"
    private val exampleResponse = "response"
    private val purchaseJsonStr = "{\"productId\":\"product_1\"}"
    private val purchaseHistoryRecordJsonStr =
        "{\"productId\":\"product_2\",\"packageName\":\"examplePackageName\"}"
    private val METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished"
    private val METHOD_ON_BILLING_SERVICE_DISCONNECTED = "onBillingServiceDisconnected"
    private val METHOD_ON_QUERY_PURCHASES_RESPONSE = "onQueryPurchasesResponse"
    private val METHOD_ON_PURCHASE_HISTORY_RESPONSE = "onPurchaseHistoryResponse"
    private val METHOD_ON_PRODUCT_DETAILS_RESPONSE = "onProductDetailsResponse"
    private val badProductDetailStr = "/"
    private val billingResult: Any = mock()
    private val proxy: Any = mock()
    private lateinit var runnable: Runnable
    private val purchase: Any = mock()
    private val purchaseHistoryRecord: Any = mock()
    private val productDetail: Any = mock()

    // These strings have been adapted from real ProductDetails objects returned by Google
    private val productDetailStr =
        """ProductDetails{jsonString='{"productId":"exampleProductId","type":"inapp","title":"exampleTitle","name":"exampleName","description":"exampleDescription","localizedIn":["en-US"],"skuDetailsToken":"exampleToken=","oneTimePurchaseOfferDetails":{"priceAmountMicros":12000000,"priceCurrencyCode":"USD","formattedPrice":"$12.00","offerIdToken":"exampleOfferIdToken=="}}', parsedJson={"productId":"exampleProductId","type":"inapp","title":"exampleTitle","name":"exampleName","description":"exampleDescription","localizedIn":["en-US"],"skuDetailsToken":"exampleToken=","oneTimePurchaseOfferDetails":{"priceAmountMicros":12000000,"priceCurrencyCode":"USD","formattedPrice":"$12.00","offerIdToken":"exampleOfferIdToken=="}}, productId='exampleProductId', productType='inapp', title='exampleTitle', productDetailsToken='exampleToken=', subscriptionOfferDetails=null}"""
    private val productDetailsJsonStr =
        """{"productId":"exampleProductId","type":"inapp","title":"exampleTitle","name":"exampleName","description":"exampleDescription","localizedIn":["en-US"],"skuDetailsToken":"exampleToken=","oneTimePurchaseOfferDetails":{"priceAmountMicros":12000000,"priceCurrencyCode":"USD","formattedPrice":"${'$'}12.00","offerIdToken":"exampleOfferIdToken=="}}"""

    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(0)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(exampleResponse)
        val listenerClazz =
            Class.forName(exampleListener)
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

        PowerMockito.mockStatic(Proxy::class.java)
        whenever(
            newProxyInstance(
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(exampleResponse)
        InAppPurchaseBillingClientWrapperV5V7.productDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV5V7.isServiceConnected.set(false)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5V7::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV5V7
        )
        runnable = Runnable {
            return@Runnable
        }
    }

    private fun getWrapperWithMockedContext(): InAppPurchaseBillingClientWrapperV5V7? {
        val mockContext: Context = mock()
        whenever(mockContext.packageName).thenReturn("examplePackageName")
        return InAppPurchaseBillingClientWrapperV5V7.getOrCreateInstance(mockContext)
    }

    @Test
    fun testHelperClassCanSuccessfullyCreateWrapper() {
        val inAppPurchaseBillingClientWrapperV5Plus = getWrapperWithMockedContext()
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNotNull()
    }

    @Test
    fun testCantGetClass() {
        whenever(getClass(anyOrNull())).thenReturn(null)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNull()
    }

    @Test
    fun testCantGetMethod() {
        whenever(
            getMethod(
                any(),
                any(),
                any()
            )
        ).thenReturn(
            null
        )
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNull()
    }

    @Test
    fun testBillingServiceConnectedSuccessfully() {
        val args = arrayOf(billingResult)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(0)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()

        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SETUP_FINISHED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.isServiceConnected.get())
            .isTrue()
    }

    @Test
    fun testBillingServiceConnectedUnsuccessfully() {
        val args = arrayOf(billingResult)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(1)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SETUP_FINISHED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.isServiceConnected.get())
            .isFalse()
    }

    @Test
    fun testBillingServiceDisconnected() {
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        val args = arrayOf(billingResult)
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SERVICE_DISCONNECTED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.isServiceConnected.get())
            .isFalse()

    }

    @Test
    fun testQueryPurchasesAsync() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.INAPP
        val wrapperArgs = arrayOf(productType, runnable)
        val purchaseList: List<*> = listOf(purchase)

        val args = arrayOf(billingResult, purchaseList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchase),
            )
        ).thenReturn(purchaseJsonStr)

        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_QUERY_PURCHASES_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap["product_1"].toString())
            .isEqualTo(JSONObject(purchaseJsonStr).toString())
    }


    @Test
    fun testQueryPurchasesMissingArgument() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.INAPP

        // Missing completion handler
        val wrapperArgs = arrayOf(productType)
        val purchaseList: List<*> = listOf(purchase)

        val args = arrayOf(billingResult, purchaseList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchase),
            )
        ).thenReturn(purchaseJsonStr)

        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_QUERY_PURCHASES_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.size)
            .isEqualTo(0)

    }

    @Test
    fun testQuerySubscriptionsAsync() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.SUBS
        val wrapperArgs = arrayOf(productType, runnable)
        val purchaseList: List<*> = listOf(purchase)
        val args = arrayOf(billingResult, purchaseList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchase),
            )
        ).thenReturn(purchaseJsonStr)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy, Class.forName(exampleClassName)
                .getMethod(METHOD_ON_QUERY_PURCHASES_RESPONSE), args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap["product_1"].toString())
            .isEqualTo(JSONObject(purchaseJsonStr).toString())
    }


    @Test
    fun testQueryPurchaseHistoryAsync() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.INAPP
        val wrapperArgs = arrayOf(productType, runnable)
        val purchaseHistoryRecordList: List<*> = listOf(purchaseHistoryRecord)

        val args = arrayOf(billingResult, purchaseHistoryRecordList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchaseHistoryRecord),
            )
        ).thenReturn(purchaseHistoryRecordJsonStr)

        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PURCHASE_HISTORY_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap["product_2"].toString())
            .isEqualTo(JSONObject(purchaseHistoryRecordJsonStr).toString())
    }

    @Test
    fun testQueryPurchaseHistoryMissingParameters() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.INAPP

        // Missing completion handler
        val wrapperArgs = arrayOf(productType)
        val purchaseHistoryRecordList: List<*> = listOf(purchaseHistoryRecord)

        val args = arrayOf(billingResult, purchaseHistoryRecordList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchaseHistoryRecord),
            )
        ).thenReturn(purchaseHistoryRecordJsonStr)

        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PURCHASE_HISTORY_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.size)
            .isEqualTo(0)
    }

    @Test
    fun testQuerySubscriptionHistoryAsync() {
        val productType: Any = InAppPurchaseUtils.IAPProductType.SUBS
        val wrapperArgs = arrayOf(productType, runnable)
        val purchaseHistoryRecordList: List<*> = listOf(purchaseHistoryRecord)
        val args = arrayOf(billingResult, purchaseHistoryRecordList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchaseHistoryRecord),
            )
        ).thenReturn(purchaseHistoryRecordJsonStr)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(wrapperArgs)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PURCHASE_HISTORY_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap["product_2"].toString())
            .isEqualTo(JSONObject(purchaseHistoryRecordJsonStr).toString())
    }

    @Test
    fun testGetOriginalJson() {
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        val result = inAppPurchaseBillingClientWrapperV5Plus?.getOriginalJson(productDetailStr)
        Assertions.assertThat(result).isEqualTo(productDetailsJsonStr)
    }

    @Test
    fun testUnsuccessfullyGetOriginalJson() {
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        val result = inAppPurchaseBillingClientWrapperV5Plus?.getOriginalJson(badProductDetailStr)
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun testQueryProductDetailsAsync() {
        val productDetailsList: List<*> = listOf(productDetail)
        val args = arrayOf(billingResult, productDetailsList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(productDetail),
            )
        ).thenReturn(productDetailStr)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PRODUCT_DETAILS_RESPONSE),
            args
        )
        Assertions.assertThat(
            InAppPurchaseBillingClientWrapperV5V7.productDetailsMap.containsKey(
                "exampleProductId"
            )
        ).isTrue()
    }

    @Test
    fun testQueryProductDetailsAsyncWithBadProductDetailsJSON() {
        val productDetailsList: List<*> = listOf(productDetail)
        val args = arrayOf(billingResult, productDetailsList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(productDetail),
            )
        ).thenReturn(badProductDetailStr)
        val inAppPurchaseBillingClientWrapperV5Plus =
            getWrapperWithMockedContext()
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PRODUCT_DETAILS_RESPONSE),
            args
        )
        Assertions.assertThat(
            InAppPurchaseBillingClientWrapperV5V7.productDetailsMap.containsKey(
                "exampleProductId"
            )
        ).isFalse()
    }


    fun onBillingSetupFinished() {}
    fun onBillingServiceDisconnected() {}
    fun onQueryPurchasesResponse() {}
    fun onPurchaseHistoryResponse() {}
    fun onProductDetailsResponse() {}
}
