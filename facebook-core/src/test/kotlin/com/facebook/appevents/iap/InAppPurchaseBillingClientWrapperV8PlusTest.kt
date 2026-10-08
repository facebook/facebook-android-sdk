/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class InAppPurchaseBillingClientWrapperV8PlusTest : FacebookTestCase() {
    private lateinit var wrapper: InAppPurchaseBillingClientWrapperV8Plus

    @Before
    fun initializeWrapper() {
        InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV8Plus.subsPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV8Plus.isServiceConnected.set(false)
        InAppPurchaseBillingClientWrapperV8Plus.purchasesUpdatedHandler = null
        setSingletonInstance(null)
        wrapper = createWrapper()
    }

    @Test
    fun `query purchase history completes without querying removed GPBL API`() {
        var completed = false

        wrapper.queryPurchaseHistory(InAppPurchaseUtils.IAPProductType.INAPP) { completed = true }

        assertThat(completed).isTrue()
    }

    @Test
    fun `product details callback reads GPBL 8 result wrapper`() {
        var succeeded: Boolean? = null
        val productDetails = ProductDetailsStub(PRODUCT_DETAILS_STRING)
        val queryResult = QueryProductDetailsResultStub(listOf(productDetails))
        val productDetailsTarget = mutableMapOf<String, JSONObject>()

        wrapper
            .ListenerWrapper(
                arrayOf<Any>(
                    listOf("exampleProductId"),
                    productDetailsTarget,
                    InAppPurchaseBillingClientWrapperV8Plus.QueryResultCallback { succeeded = it },
                )
            )
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onProductDetailsResponse"),
                arrayOf(BillingResultStub(0), queryResult),
            )

        assertThat(succeeded).isTrue()
        assertThat(productDetailsTarget).containsKey("exampleProductId")
    }

    @Test
    fun `product details callback fails closed when GPBL response cannot be read`() {
        var succeeded: Boolean? = null
        val productDetailsTarget = mutableMapOf<String, JSONObject>()

        wrapper
            .ListenerWrapper(
                arrayOf<Any>(
                    listOf("exampleProductId"),
                    productDetailsTarget,
                    InAppPurchaseBillingClientWrapperV8Plus.QueryResultCallback { succeeded = it },
                )
            )
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onProductDetailsResponse"),
                arrayOf(BillingResultStub(0), Any()),
            )

        assertThat(succeeded).isFalse()
        assertThat(productDetailsTarget).isEmpty()
    }

    @Test
    fun `product details callback reports non-OK BillingResult`() {
        var succeeded: Boolean? = null
        val productDetailsTarget = mutableMapOf<String, JSONObject>()

        wrapper
            .ListenerWrapper(
                arrayOf<Any>(
                    listOf("exampleProductId"),
                    productDetailsTarget,
                    InAppPurchaseBillingClientWrapperV8Plus.QueryResultCallback { succeeded = it },
                )
            )
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onProductDetailsResponse"),
                arrayOf(BillingResultStub(5), QueryProductDetailsResultStub(emptyList())),
            )

        assertThat(succeeded).isFalse()
    }

    @Test
    fun `successful purchases query completes when a purchase cannot be mapped`() {
        var succeeded: Boolean? = null

        wrapper
            .ListenerWrapper(
                arrayOf<Any>(
                    InAppPurchaseUtils.IAPProductType.INAPP,
                    InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback {
                        succeeded = it.succeeded
                    },
                )
            )
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onQueryPurchasesResponse"),
                arrayOf(BillingResultStub(0), listOf(PurchaseStub("not-json"))),
            )

        assertThat(succeeded).isTrue()
    }

    @Test
    fun `original json is extracted from product details string`() {
        assertThat(wrapper.getOriginalJson(PRODUCT_DETAILS_STRING))
            .isEqualTo("{\"productId\":\"exampleProductId\"}")
    }

    @Test
    fun `original json extraction preserves apostrophes`() {
        val productDetailsString =
            "ProductDetails{jsonString='{\"productId\":\"exampleProductId\",\"title\":\"Kid's Pack\"}', parsedJson={}}"

        assertThat(wrapper.getOriginalJson(productDetailsString))
            .isEqualTo("{\"productId\":\"exampleProductId\",\"title\":\"Kid's Pack\"}")
    }

    @Test
    fun `original json extraction rejects an unknown product details layout`() {
        val productDetailsString =
            "ProductDetails{jsonString='{\"productId\":\"exampleProductId\"}', otherValue='x'}"

        assertThat(wrapper.getOriginalJson(productDetailsString) == null).isTrue()
    }

    @Test
    fun `purchase productIds are normalized for logging`() {
        InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap["productA"] = JSONObject()
        InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap["productB"] = JSONObject()
        val purchaseDetailsTarget = mutableMapOf<String, JSONObject>()
        val productDetailsTarget = mutableMapOf<String, JSONObject>()
        var succeeded: Boolean? = null

        wrapper.processPurchaseJsons(
            InAppPurchaseUtils.IAPProductType.INAPP,
            listOf(JSONObject("{\"productIds\":[\"productA\",\"productB\"],\"purchaseState\":0}")),
            purchaseDetailsTarget,
            productDetailsTarget,
        ) {
            succeeded = it
        }

        assertThat(succeeded).isTrue()
        assertThat(purchaseDetailsTarget.keys).isEqualTo(setOf("productA", "productB"))
        assertThat(purchaseDetailsTarget["productB"]?.getString("productId"))
            .isEqualTo("productB")
        assertThat(productDetailsTarget.keys).containsExactlyInAnyOrder("productA", "productB")
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap).isEmpty()

        InAppPurchaseBillingClientWrapperV8Plus.publishQueryResult(
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult(
                InAppPurchaseUtils.IAPProductType.INAPP,
                true,
                purchaseDetailsTarget,
                productDetailsTarget,
            )
        )

        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap.keys)
            .containsExactlyInAnyOrder("productA", "productB")
    }

    @Test
    fun `realtime listener forwards successful purchase updates`() {
        setSingletonInstance(wrapper)
        var forwardedCode: Int? = null
        var forwardedPurchases: List<JSONObject>? = null
        InAppPurchaseBillingClientWrapperV8Plus.purchasesUpdatedHandler = { code, purchases ->
            forwardedCode = code
            forwardedPurchases = purchases
        }

        InAppPurchaseBillingClientWrapperV8Plus.invoke(
            Any(),
            ListenerMethods::class.java.getMethod("onPurchasesUpdated"),
            arrayOf(
                BillingResultStub(0),
                listOf(PurchaseStub("{\"productId\":\"exampleProductId\",\"purchaseState\":0}")),
            ),
        )

        assertThat(forwardedCode).isEqualTo(0)
        assertThat(forwardedPurchases?.size).isEqualTo(1)
        assertThat(forwardedPurchases?.first()?.getString("productId"))
            .isEqualTo("exampleProductId")
    }

    @Test
    fun `GPBL 8 dependency exposes reflected API contract`() {
        val billingResultClass = Class.forName("com.android.billingclient.api.BillingResult")
        val queryResultClass =
            Class.forName("com.android.billingclient.api.QueryProductDetailsResult")
        val listenerClass =
            Class.forName("com.android.billingclient.api.ProductDetailsResponseListener")
        val pendingPurchasesParamsClass =
            Class.forName("com.android.billingclient.api.PendingPurchasesParams")

        assertThat(queryResultClass.getMethod("getProductDetailsList") != null).isTrue()
        assertThat(
                listenerClass.getMethod(
                    "onProductDetailsResponse",
                    billingResultClass,
                    queryResultClass,
                ) != null
            )
            .isTrue()
        assertThat(
                pendingPurchasesParamsClass
                    .getMethod("newBuilder")
                    .returnType
                    .getMethod("enableOneTimeProducts") != null
            )
            .isTrue()
    }

    private fun createWrapper(): InAppPurchaseBillingClientWrapperV8Plus {
        val fixture = ReflectionFixture()
        val fixtureClass = ReflectionFixture::class.java
        val noOpMethod = fixtureClass.getMethod("noOp")
        val constructor =
            InAppPurchaseBillingClientWrapperV8Plus::class.java.declaredConstructors.first {
                it.parameterCount == 30
            }
        constructor.isAccessible = true
        return constructor.newInstance(
            fixture,
            fixtureClass,
            PurchaseStub::class.java,
            ProductDetailsStub::class.java,
            fixtureClass,
            BillingResultStub::class.java,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            fixtureClass,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            PurchaseStub::class.java.getMethod("getOriginalJson"),
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            noOpMethod,
            ProductDetailsStub::class.java.getMethod("toString"),
            noOpMethod,
            BillingResultStub::class.java.getMethod("getResponseCode"),
        ) as InAppPurchaseBillingClientWrapperV8Plus
    }

    private fun setSingletonInstance(instance: InAppPurchaseBillingClientWrapperV8Plus?) {
        val instanceField =
            InAppPurchaseBillingClientWrapperV8Plus::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, instance)
    }

    class ReflectionFixture {
        fun noOp(): Any = Any()
    }

    class ProductDetailsStub(private val value: String) {
        override fun toString(): String = value
    }

    class BillingResultStub(private val responseCode: Int) {
        fun getResponseCode(): Int = responseCode
    }

    class PurchaseStub(private val originalJson: String) {
        fun getOriginalJson(): String = originalJson
    }

    class QueryProductDetailsResultStub(private val productDetailsList: List<ProductDetailsStub>) {
        fun getProductDetailsList(): List<ProductDetailsStub> = productDetailsList
    }

    class ListenerMethods {
        fun onProductDetailsResponse() = Unit

        fun onPurchasesUpdated() = Unit

        fun onQueryPurchasesResponse() = Unit
    }

    companion object {
        private const val PRODUCT_DETAILS_STRING =
            "ProductDetails{jsonString='{\"productId\":\"exampleProductId\"}', parsedJson={}}"
    }
}
