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
        var completed = false
        val productDetails = ProductDetailsStub(PRODUCT_DETAILS_STRING)
        val queryResult = QueryProductDetailsResultStub(listOf(productDetails))

        wrapper
            .ListenerWrapper(arrayOf<Any>(Runnable { completed = true }))
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onProductDetailsResponse"),
                arrayOf(Any(), queryResult),
            )

        assertThat(completed).isTrue()
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap)
            .containsKey("exampleProductId")
    }

    @Test
    fun `product details callback fails closed when GPBL response cannot be read`() {
        var completed = false

        wrapper
            .ListenerWrapper(arrayOf<Any>(Runnable { completed = true }))
            .invoke(
                Any(),
                ListenerMethods::class.java.getMethod("onProductDetailsResponse"),
                arrayOf(Any(), Any()),
            )

        assertThat(completed).isFalse()
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap).isEmpty()
    }

    @Test
    fun `original json is extracted from product details string`() {
        assertThat(wrapper.getOriginalJson(PRODUCT_DETAILS_STRING))
            .isEqualTo("{\"productId\":\"exampleProductId\"}")
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
            fixtureClass,
            ProductDetailsStub::class.java,
            fixtureClass,
            fixtureClass,
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
            noOpMethod,
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
            noOpMethod,
        ) as InAppPurchaseBillingClientWrapperV8Plus
    }

    class ReflectionFixture {
        fun noOp(): Any = Any()
    }

    class ProductDetailsStub(private val value: String) {
        override fun toString(): String = value
    }

    class QueryProductDetailsResultStub(private val productDetailsList: List<ProductDetailsStub>) {
        fun getProductDetailsList(): List<ProductDetailsStub> = productDetailsList
    }

    class ListenerMethods {
        fun onProductDetailsResponse() = Unit
    }

    companion object {
        private const val PRODUCT_DETAILS_STRING =
            "ProductDetails{jsonString='{\"productId\":\"exampleProductId\"}', parsedJson={}}"
    }
}
