/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import com.facebook.appevents.iap.InAppPurchaseSkuDetailsWrapper.Companion.getOrCreateInstance
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InAppPurchaseBillingClientWrapperV5Plus
private constructor(
    private val context: Context,
    private val billingClientClazz: Class<*>,
    private val purchaseClazz: Class<*>,
    private val productDetailsClazz: Class<*>,
    private val purchaseHistoryRecordClazz: Class<*>,
    private val queryProductDetailsParamsProductClazz: Class<*>,
    private val queryProductDetailsParamsClazz: Class<*>,
    private val queryPurchaseHistoryParamsClazz: Class<*>,
    private val queryPurchasesParamsClazz: Class<*>,
    private val queryProductDetailsParamsBuilderClazz: Class<*>,
    private val queryPurchaseHistoryParamsBuilderClazz: Class<*>,
    private val queryPurchasesParamsBuilderClazz: Class<*>,
    private val queryProductDetailsParamsProductBuilderClazz: Class<*>,
    private val billingClientBuilderClazz: Class<*>,
    private val purchasesUpdatedListenerClazz: Class<*>,
    private val billingClientStateListenerClazz: Class<*>,
    private val productDetailsResponseListenerClazz: Class<*>,
    private val purchasesResponseListenerClazz: Class<*>,
) {

    companion object {
        private val TAG = InAppPurchaseBillingClientWrapperV5Plus::class.java.canonicalName
        private var instance: InAppPurchaseBillingClientWrapperV5Plus? = null

        // Class names
        private const val CLASSNAME_BILLING_CLIENT = "com.android.billingclient.api.BillingClient"
        private const val CLASSNAME_PURCHASE = "com.android.billingclient.api.Purchase"
        private const val CLASSNAME_PRODUCT_DETAILS = "com.android.billingclient.api.ProductDetails"
        private const val CLASSNAME_PURCHASE_HISTORY_RECORD =
            "com.android.billingclient.api.PurchaseHistoryRecord"
        private const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT =
            "com.android.billingclient.api.QueryProductDetailsParams\$Product"

        // Class names: Params
        private const val CLASSNAME_PENDING_PURCHASES_PARAMS =
            "com.android.billingclient.api.PendingPurchasesParams"
        private const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS =
            "com.android.billingclient.api.QueryProductDetailsParams"
        private const val CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS =
            "com.android.billingclient.api.QueryPurchaseHistoryParams"
        private const val CLASSNAME_QUERY_PURCHASES_PARAMS =
            "com.android.billingclient.api.QueryPurchasesParams"

        // Class names: Builders
        private const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER =
            "com.android.billingclient.api.QueryProductDetailsParams\$Builder"
        private const val CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS_BUILDER =
            "com.android.billingclient.api.QueryPurchaseHistoryParams\$Builder"
        private const val CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER =
            "com.android.billingclient.api.QueryPurchasesParams\$Builder"
        private const val CLASSNAME_PENDING_PURCHASES_PARAMS_BUILDER =
            "com.android.billingclient.api.PendingPurchasesParams\$Builder"
        private const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER =
            "com.android.billingclient.api.QueryProductDetailsParams\$Product\$Builder"
        private const val CLASSNAME_BILLING_CLIENT_BUILDER =
            "com.android.billingclient.api.BillingClient\$Builder"

        // Class names: Listeners
        private const val CLASSNAME_PURCHASES_UPDATED_LISTENER =
            "com.android.billingclient.api.PurchasesUpdatedListener"
        private const val CLASSNAME_BILLING_CLIENT_STATE_LISTENER =
            "com.android.billingclient.api.BillingClientStateListener"
        private const val CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER =
            "com.android.billingclient.api.ProductDetailsResponseListener"
        private const val CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER =
            "com.android.billingclient.api.PurchaseHistoryResponseListener"
        private const val CLASSNAME_PURCHASES_RESPONSE_LISTENER =
            "com.android.billingclient.api.PurchasesResponseListener"


        @Synchronized
        @JvmStatic
        fun getOrCreateInstance(context: Context): InAppPurchaseBillingClientWrapperV5Plus? {
            if (instance != null) {
                return instance
            }
            createInstance(context)
            return instance
        }

        private fun createInstance(context: Context) {
            // Get classes
            val billingClientClazz = getClass(CLASSNAME_BILLING_CLIENT)
            val purchaseClazz = getClass(CLASSNAME_PURCHASE)
            val productDetailsClazz = getClass(CLASSNAME_PRODUCT_DETAILS)
            val purchaseHistoryRecordClazz = getClass(CLASSNAME_PURCHASE_HISTORY_RECORD)
            val queryProductDetailsParamsProductClazz = getClass(
                CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT
            )

            // Get classes: Params
            val queryProductDetailsParamsClazz = getClass(CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS)
            val queryPurchaseHistoryParamsClazz = getClass(CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS)
            val queryPurchasesParamsClazz = getClass(CLASSNAME_QUERY_PURCHASES_PARAMS)

            // Get classes: Builders
            val queryProductDetailsParamsBuilderClazz = getClass(
                CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER
            )
            val queryPurchaseHistoryParamsBuilderClazz = getClass(
                CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS_BUILDER
            )
            val queryPurchasesParamsBuilderClazz =
                getClass(CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER)
            val queryProductDetailsParamsProductBuilderClazz = getClass(
                CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER
            )
            val billingClientBuilderClazz = getClass(CLASSNAME_BILLING_CLIENT_BUILDER)

            // Get classes: Listeners
            val purchasesUpdatedListenerClazz = getClass(CLASSNAME_PURCHASES_UPDATED_LISTENER)
            val billingClientStateListenerClazz = getClass(CLASSNAME_BILLING_CLIENT_STATE_LISTENER)
            val productDetailsResponseListenerClazz = getClass(
                CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER
            )
            val purchasesResponseListenerClazz = getClass(CLASSNAME_PURCHASES_RESPONSE_LISTENER)

            if (billingClientClazz == null ||
                purchaseClazz == null ||
                productDetailsClazz == null ||
                purchaseHistoryRecordClazz == null ||
                queryProductDetailsParamsProductClazz == null ||
                queryProductDetailsParamsClazz == null ||
                queryPurchaseHistoryParamsClazz == null ||
                queryPurchasesParamsClazz == null ||
                queryProductDetailsParamsBuilderClazz == null ||
                queryPurchaseHistoryParamsBuilderClazz == null ||
                queryPurchasesParamsBuilderClazz == null ||
                queryProductDetailsParamsProductBuilderClazz == null ||
                billingClientBuilderClazz == null ||
                purchasesUpdatedListenerClazz == null ||
                billingClientStateListenerClazz == null ||
                productDetailsResponseListenerClazz == null ||
                purchasesResponseListenerClazz == null

            ) {
                Log.w(
                    TAG,
                    "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return
            }

            // TODO: Get methods

            // TODO: Create a billing client

            instance = InAppPurchaseBillingClientWrapperV5Plus(
                context,
                billingClientClazz,
                purchaseClazz,
                productDetailsClazz,
                purchaseHistoryRecordClazz,
                queryProductDetailsParamsProductClazz,
                queryProductDetailsParamsClazz,
                queryPurchaseHistoryParamsClazz,
                queryPurchasesParamsClazz,
                queryProductDetailsParamsBuilderClazz,
                queryPurchaseHistoryParamsBuilderClazz,
                queryPurchasesParamsBuilderClazz,
                queryProductDetailsParamsProductBuilderClazz,
                billingClientBuilderClazz,
                purchasesUpdatedListenerClazz,
                billingClientStateListenerClazz,
                productDetailsResponseListenerClazz,
                purchasesResponseListenerClazz
            )
        }
    }
}
