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
import java.lang.reflect.Proxy
import java.lang.reflect.Proxy.newProxyInstance

@PrepareForTest(FacebookSdk::class, InAppPurchaseUtils::class, Proxy::class)
class InAppPurchaseBillingClientWrapperV5PlusTest : FacebookPowerMockTestCase() {
    private val exampleClassName =
        "com.facebook.appevents.iap.InAppPurchaseBillingClientWrapperV5PlusTest"
    private val exampleListener = "com.facebook.appevents.iap.PurchasesUpdatedListener"
    private val exampleMethodName = "setup"
    private val exampleResponse = "response"
    private val purchaseJsonStr = "{\"productId\":\"product_1\"}"
    private val purchaseHistoryRecordJsonStr = "{\"productId\":\"product_2\"}"
    private val METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished"
    private val METHOD_ON_BILLING_SERVICE_DISCONNECTED = "onBillingServiceDisconnected"
    private val METHOD_ON_QUERY_PURCHASES_RESPONSE = "onQueryPurchasesResponse"
    private val METHOD_ON_PURCHASE_HISTORY_RESPONSE = "onPurchaseHistoryResponse"

    @Before
    override fun setup() {
        InAppPurchaseBillingClientWrapperV5Plus.instance = null
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
    }

    @Test
    fun testHelperClassCanSuccessfullyCreateWrapper() {
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNotNull()
    }

    @Test
    fun testCantGetClass() {
        whenever(getClass(anyOrNull())).thenReturn(null)
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
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
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNull()
    }

    @Test
    fun testBillingServiceConnectedSuccessfully() {
        val billingResult: Any = mock()
        val proxy: Any = mock()
        val args = arrayOf(billingResult)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(0)
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)

        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SETUP_FINISHED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5Plus.isServiceConnected.get())
            .isTrue()
    }

    @Test
    fun testBillingServiceConnectedUnsuccessfully() {
        val billingResult: Any = mock()
        val proxy: Any = mock()
        val args = arrayOf(billingResult)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(1)
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SETUP_FINISHED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5Plus.isServiceConnected.get())
            .isFalse()
    }

    @Test
    fun testBillingServiceDisconnected() {
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        val billingResult: Any = mock()
        val proxy: Any = mock()
        val args = arrayOf(billingResult)
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_BILLING_SERVICE_DISCONNECTED),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5Plus.isServiceConnected.get())
            .isFalse()

    }

    @Test
    fun testQueryPurchasesAsync() {
        val billingResult: Any = mock()
        val proxy: Any = mock()
        val purchase: Any = mock()
        val purchaseList: List<*> = listOf(purchase)

        val args = arrayOf(billingResult, purchaseList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchase),
            )
        ).thenReturn(purchaseJsonStr)
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_QUERY_PURCHASES_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5Plus.purchaseDetailsMap["product_1"].toString())
            .isEqualTo(JSONObject(purchaseJsonStr).toString())
    }

    @Test
    fun testQueryPurchaseHistoryAsync() {
        val billingResult: Any = mock()
        val proxy: Any = mock()
        val purchaseHistoryRecord: Any = mock()
        val purchaseHistoryRecordList: List<*> = listOf(purchaseHistoryRecord)

        val args = arrayOf(billingResult, purchaseHistoryRecordList)
        whenever(
            invokeMethod(
                anyOrNull(),
                anyOrNull(),
                eq(purchaseHistoryRecord),
            )
        ).thenReturn(purchaseHistoryRecordJsonStr)
        val mockContext: Context = mock()
        val inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)
        inAppPurchaseBillingClientWrapperV5Plus?.ListenerWrapper(null)?.invoke(
            proxy,
            Class.forName(exampleClassName)
                .getMethod(METHOD_ON_PURCHASE_HISTORY_RESPONSE),
            args
        )
        Assertions.assertThat(InAppPurchaseBillingClientWrapperV5Plus.purchaseDetailsMap["product_2"].toString())
            .isEqualTo(JSONObject(purchaseHistoryRecordJsonStr).toString())
    }


    fun onBillingSetupFinished() {}
    fun onBillingServiceDisconnected() {}
    fun onQueryPurchasesResponse() {}
    fun onPurchaseHistoryResponse() {}
}
