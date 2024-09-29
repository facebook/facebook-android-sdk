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
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_BILLING_CLIENT
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_BILLING_CLIENT_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_BILLING_CLIENT_STATE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_BILLING_RESULT
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PRODUCT_DETAILS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASES_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASES_UPDATED_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE_HISTORY_RECORD
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASES_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_BUILD
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ENABLE_PENDING_PURCHASES
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_ORIGINAL_JSON
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_RESPONSE_CODE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_NEW_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SERVICE_DISCONNECTED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SETUP_FINISHED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_PRODUCT_DETAILS_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_PURCHASE_HISTORY_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_QUERY_PURCHASES_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PRODUCT_DETAILS_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PURCHASES_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PURCHASE_HISTORY_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_ID
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_LIST
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_TYPE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_START_CONNECTION
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_TO_STRING
import com.facebook.appevents.iap.InAppPurchaseConstants.PACKAGE_NAME
import com.facebook.appevents.iap.InAppPurchaseConstants.PRODUCT_ID
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InAppPurchaseBillingClientWrapperV5V7
private constructor(
    private val packageName: String,
    override val billingClient: Any,
    private val billingClientClazz: Class<*>,
    private val purchaseClazz: Class<*>,
    private val productDetailsClazz: Class<*>,
    private val purchaseHistoryRecordClazz: Class<*>,
    private val queryProductDetailsParamsProductClazz: Class<*>,
    private val billingResultClazz: Class<*>,

    private val queryProductDetailsParamsClazz: Class<*>,
    private val queryPurchaseHistoryParamsClazz: Class<*>,
    private val queryPurchasesParamsClazz: Class<*>,

    private val queryProductDetailsParamsBuilderClazz: Class<*>,
    private val queryPurchaseHistoryParamsBuilderClazz: Class<*>,
    private val queryPurchasesParamsBuilderClazz: Class<*>,
    private val queryProductDetailsParamsProductBuilderClazz: Class<*>,

    private val billingClientStateListenerClazz: Class<*>,
    private val productDetailsResponseListenerClazz: Class<*>,
    private val purchasesResponseListenerClazz: Class<*>,
    private val purchaseHistoryResponseListenerClazz: Class<*>,

    private val queryPurchasesAsyncMethod: Method,
    private val queryPurchasesParamsNewBuilderMethod: Method,
    private val queryPurchasesParamsBuilderBuildMethod: Method,
    private val queryPurchasesParamsBuilderSetProductTypeMethod: Method,
    private val purchaseGetOriginalJsonMethod: Method,

    private val queryPurchaseHistoryAsyncMethod: Method,
    private val queryPurchaseHistoryParamsNewBuilderMethod: Method,
    private val queryPurchaseHistoryParamsBuilderBuildMethod: Method,
    private val queryPurchaseHistoryParamsBuilderSetProductTypeMethod: Method,
    private val purchaseHistoryRecordGetOriginalJsonMethod: Method,

    private val queryProductDetailsAsyncMethod: Method,
    private val queryProductDetailsParamsNewBuilderMethod: Method,
    private val queryProductDetailsParamsBuilderBuildMethod: Method,
    private val queryProductDetailsParamsBuilderSetProductListMethod: Method,
    private val queryProductDetailsParamsProductNewBuilderMethod: Method,
    private val queryProductDetailsParamsProductBuilderBuildMethod: Method,
    private val queryProductDetailsParamsProductBuilderSetProductIdMethod: Method,
    private val queryProductDetailsParamsProductBuilderSetProductTypeMethod: Method,
    private val productDetailsToStringMethod: Method,

    private val billingClientStartConnectionMethod: Method,
    private val billingResultGetResponseCodeMethod: Method
) : InAppPurchaseBillingClientWrapper {

    inner class ListenerWrapper(private var wrapperArgs: Array<Any>?) : InvocationHandler {
        override fun invoke(proxy: Any, m: Method, listenerArgs: Array<Any>?): Any? {
            when (m.name) {
                METHOD_ON_QUERY_PURCHASES_RESPONSE -> onQueryPurchasesResponse(
                    wrapperArgs,
                    listenerArgs
                )

                METHOD_ON_PURCHASE_HISTORY_RESPONSE -> onPurchaseHistoryResponse(
                    wrapperArgs,
                    listenerArgs
                )

                METHOD_ON_PRODUCT_DETAILS_RESPONSE -> onProductDetailsResponse(
                    wrapperArgs,
                    listenerArgs
                )

                METHOD_ON_BILLING_SETUP_FINISHED -> onBillingSetupFinished(
                    wrapperArgs,
                    listenerArgs
                )

                METHOD_ON_BILLING_SERVICE_DISCONNECTED -> onBillingServiceDisconnected(
                    wrapperArgs, listenerArgs
                )
            }
            return null
        }
    }


    private fun getQueryPurchasesParams(productType: InAppPurchaseUtils.IAPProductType): Any? {
        // 1. newBuilder()
        var builder: Any? =
            invokeMethod(queryPurchasesParamsClazz, queryPurchasesParamsNewBuilderMethod, null)
                ?: return null

        // 2. setProductType(productType)
        builder = invokeMethod(
            queryPurchasesParamsBuilderClazz,
            queryPurchasesParamsBuilderSetProductTypeMethod,
            builder,
            productType.type
        )

        // 3. build()
        return invokeMethod(
            queryPurchasesParamsBuilderClazz,
            queryPurchasesParamsBuilderBuildMethod,
            builder
        )
    }

    private fun getQueryPurchaseHistoryParams(productType: InAppPurchaseUtils.IAPProductType): Any? {
        // 1. newBuilder()
        var builder: Any? = invokeMethod(
            queryPurchaseHistoryParamsClazz,
            queryPurchaseHistoryParamsNewBuilderMethod,
            null
        )

        // 2. setProductType(productType)
        builder = invokeMethod(
            queryPurchaseHistoryParamsBuilderClazz,
            queryPurchaseHistoryParamsBuilderSetProductTypeMethod,
            builder,
            productType.type
        )

        // 3. build()
        return invokeMethod(
            queryPurchaseHistoryParamsBuilderClazz,
            queryPurchaseHistoryParamsBuilderBuildMethod,
            builder
        )
    }

    private fun getQueryProductDetailsParams(
        productType: InAppPurchaseUtils.IAPProductType,
        productIds: List<String?>
    ): Any? {
        if (productIds.isEmpty()) {
            return null
        }

        val productList = ArrayList<Any>()
        for (productId in productIds) {
            // 1. Product.newBuilder()
            var productBuilder = invokeMethod(
                queryProductDetailsParamsProductClazz,
                queryProductDetailsParamsProductNewBuilderMethod,
                null
            )

            // productBuilder.setProductId(productId)
            productBuilder = invokeMethod(
                queryProductDetailsParamsProductBuilderClazz,
                queryProductDetailsParamsProductBuilderSetProductIdMethod,
                productBuilder,
                productId
            )

            // productBuilder.setProductType(productType)
            productBuilder = invokeMethod(
                queryProductDetailsParamsProductBuilderClazz,
                queryProductDetailsParamsProductBuilderSetProductTypeMethod,
                productBuilder,
                productType.type
            )

            // productBuilder.build()
            val product = invokeMethod(
                queryProductDetailsParamsProductBuilderClazz,
                queryProductDetailsParamsProductBuilderBuildMethod,
                productBuilder
            )

            if (product != null) {
                productList.add(product)
            }
        }

        // 1, QueryProductDetailsParams.newBuilder()
        var paramBuilder = invokeMethod(
            queryProductDetailsParamsClazz,
            queryProductDetailsParamsNewBuilderMethod,
            null
        )

        // 2. paramBuilder.setProductList(productList)
        paramBuilder = invokeMethod(
            queryProductDetailsParamsBuilderClazz,
            queryProductDetailsParamsBuilderSetProductListMethod,
            paramBuilder,
            productList
        )

        // 3. paramBuilder.build()
        return invokeMethod(
            queryProductDetailsParamsBuilderClazz,
            queryProductDetailsParamsBuilderBuildMethod,
            paramBuilder
        )
    }

    override fun queryPurchases(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable
    ) {
        val runnableQuery = Runnable {
            val listenerObj = Proxy.newProxyInstance(
                purchasesResponseListenerClazz.classLoader,
                arrayOf(purchasesResponseListenerClazz),
                ListenerWrapper(arrayOf(productType, completionHandler))
            )
            invokeMethod(
                billingClientClazz,
                queryPurchasesAsyncMethod,
                billingClient,
                getQueryPurchasesParams(productType),
                listenerObj
            )
        }
        executeServiceRequest(runnableQuery)
    }

    override fun queryPurchaseHistory(
        productType: InAppPurchaseUtils.IAPProductType, completionHandler: Runnable
    ) {
        val runnableQuery = Runnable {
            val listenerObj = Proxy.newProxyInstance(
                purchaseHistoryResponseListenerClazz.classLoader,
                arrayOf(purchaseHistoryResponseListenerClazz),
                ListenerWrapper(arrayOf(productType, completionHandler))
            )
            invokeMethod(
                billingClientClazz,
                queryPurchaseHistoryAsyncMethod,
                billingClient,
                getQueryPurchaseHistoryParams(productType),
                listenerObj
            )
        }
        executeServiceRequest(runnableQuery)
    }

    private fun queryProductDetailsAsync(
        productType: InAppPurchaseUtils.IAPProductType,
        productIds: List<String>,
        completionHandler: Runnable
    ) {
        val runnableQuery = Runnable {
            val listenerObj = Proxy.newProxyInstance(
                productDetailsResponseListenerClazz.classLoader,
                arrayOf(productDetailsResponseListenerClazz),
                ListenerWrapper(arrayOf(completionHandler))
            )
            val queryProductDetailsParams = getQueryProductDetailsParams(productType, productIds)
            if (queryProductDetailsParams != null) {
                invokeMethod(
                    billingClientClazz,
                    queryProductDetailsAsyncMethod,
                    billingClient,
                    queryProductDetailsParams,
                    listenerObj
                )
            }
        }
        executeServiceRequest(runnableQuery)
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected.get()) {
            runnable.run()
        } else {
            startConnection(runnable)
        }
    }

    private fun startConnection(runnable: Runnable) {
        val listenerObj = Proxy.newProxyInstance(
            billingClientStateListenerClazz.classLoader,
            arrayOf(billingClientStateListenerClazz),
            ListenerWrapper(arrayOf(runnable))
        )
        invokeMethod(
            billingClientClazz,
            billingClientStartConnectionMethod,
            billingClient,
            listenerObj
        )
    }

    fun getOriginalJson(productDetailsString: String): String? {
        val jsonStringRegex = """jsonString='(.*?)'""".toRegex()
        val matchResult = jsonStringRegex.find(productDetailsString)
        return matchResult?.groupValues?.getOrNull(1)
    }

    private fun onQueryPurchasesResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val productType = wrapperArgs?.getOrNull(0)
        if (productType == null || productType !is InAppPurchaseUtils.IAPProductType) {
            return
        }
        val completionHandler = wrapperArgs.getOrNull(1)
        if (completionHandler !is Runnable) {
            return
        }
        val purchaseList = listenerArgs?.getOrNull(1)
        if (purchaseList == null || purchaseList !is List<*>) {
            return
        }
        val productIds = mutableListOf<String>()
        for (purchase in purchaseList) {
            val purchaseJsonStr =
                invokeMethod(
                    purchaseClazz,
                    purchaseGetOriginalJsonMethod,
                    purchase
                ) as? String ?: continue
            val purchaseJson = JSONObject(purchaseJsonStr)
            if (purchaseJson.has(PRODUCT_ID)) {
                val productId = purchaseJson.getString(PRODUCT_ID)
                if (productId !in productDetailsMap) {
                    productIds.add(productId)
                }
                if (productType == InAppPurchaseUtils.IAPProductType.INAPP) {
                    iapPurchaseDetailsMap[productId] = purchaseJson
                } else {
                    subsPurchaseDetailsMap[productId] = purchaseJson
                }
            }
        }
        if (productIds.isNotEmpty()) {
            queryProductDetailsAsync(productType, productIds, completionHandler)
        } else {
            // If productIds is empty we have all of the product info we need and can execute our completion handler
            completionHandler.run()
        }
    }


    private fun onPurchaseHistoryResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val productType = wrapperArgs?.getOrNull(0)
        if (productType == null || productType !is InAppPurchaseUtils.IAPProductType) {
            return
        }
        val completionHandler = wrapperArgs.getOrNull(1)
        if (completionHandler !is Runnable) {
            return
        }
        val purchaseHistoryRecordList = listenerArgs?.getOrNull(1)

        if (purchaseHistoryRecordList == null || purchaseHistoryRecordList !is List<*>) {
            return
        }
        val productIds = mutableListOf<String>()
        for (purchaseHistoryRecord in purchaseHistoryRecordList) {
            try {
                val purchaseHistoryRecordJsonStr = invokeMethod(
                    purchaseHistoryRecordClazz,
                    purchaseHistoryRecordGetOriginalJsonMethod,
                    purchaseHistoryRecord
                ) as? String ?: continue
                val purchaseHistoryRecordJson = JSONObject(purchaseHistoryRecordJsonStr)
                val packageName = packageName
                purchaseHistoryRecordJson.put(PACKAGE_NAME, packageName)
                if (purchaseHistoryRecordJson.has(PRODUCT_ID)) {
                    val productId = purchaseHistoryRecordJson.getString(PRODUCT_ID)
                    if (productId !in productDetailsMap) {
                        productIds.add(productId)
                    }
                    if (productType == InAppPurchaseUtils.IAPProductType.INAPP) {
                        iapPurchaseDetailsMap[productId] = purchaseHistoryRecordJson
                    } else {
                        subsPurchaseDetailsMap[productId] = purchaseHistoryRecordJson
                    }
                }
            } catch (e: Exception) {
                /* swallow */
            }
        }
        if (productIds.isNotEmpty()) {
            queryProductDetailsAsync(productType, productIds, completionHandler)
        } else {
            // If productIds is empty we have all of the product info we need and can execute our completion handler
            completionHandler.run()
        }
    }

    private fun onProductDetailsResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val completionHandler = wrapperArgs?.getOrNull(0)
        val productDetailsList = listenerArgs?.getOrNull(1)

        if (productDetailsList == null || productDetailsList !is List<*>) {
            return
        }
        for (productDetails in productDetailsList) {
            try {
                val productDetailStr = invokeMethod(
                    productDetailsClazz,
                    productDetailsToStringMethod,
                    productDetails
                ) as? String ?: continue
                val productDetailsJsonStr = getOriginalJson(productDetailStr) ?: continue
                val productDetailJson = JSONObject(productDetailsJsonStr)
                if (productDetailJson.has(PRODUCT_ID)) {
                    val productId = productDetailJson.getString(PRODUCT_ID)
                    productDetailsMap[productId] = productDetailJson
                }
            } catch (e: Exception) {
                /* swallow */
            }
        }
        if (completionHandler != null && completionHandler is Runnable) {
            completionHandler.run()
        }
    }

    private fun onBillingSetupFinished(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        if (listenerArgs.isNullOrEmpty()) {
            return
        }
        val billingResult = listenerArgs[0]
        val responseCode =
            invokeMethod(billingResultClazz, billingResultGetResponseCodeMethod, billingResult)
        if (responseCode == 0) {
            isServiceConnected.set(true)
            if (!wrapperArgs.isNullOrEmpty() && wrapperArgs[0] is Runnable) {
                val runnable: Runnable? = wrapperArgs[0] as Runnable?
                runnable?.run()
            }
        }
    }

    private fun onBillingServiceDisconnected(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        isServiceConnected.set(false)
    }

    companion object : InvocationHandler {
        private val TAG = InAppPurchaseBillingClientWrapperV5V7::class.java.canonicalName
        val isServiceConnected = AtomicBoolean(false)
        private var instance: InAppPurchaseBillingClientWrapperV5V7? = null

        // Use ConcurrentHashMap because purchase values may be updated in different threads
        val iapPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val subsPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val productDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()

        @Synchronized
        @JvmStatic
        fun getOrCreateInstance(context: Context): InAppPurchaseBillingClientWrapperV5V7? {
            return instance ?: createInstance(context)
        }

        private fun createInstance(context: Context): InAppPurchaseBillingClientWrapperV5V7? {
            // Get classes
            val billingClientClazz = getClass(CLASSNAME_BILLING_CLIENT)
            val purchaseClazz = getClass(CLASSNAME_PURCHASE)
            val productDetailsClazz = getClass(CLASSNAME_PRODUCT_DETAILS)
            val purchaseHistoryRecordClazz = getClass(CLASSNAME_PURCHASE_HISTORY_RECORD)
            val queryProductDetailsParamsProductClazz = getClass(
                CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT
            )
            val billingResultClazz = getClass(CLASSNAME_BILLING_RESULT)

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
            val purchaseHistoryResponseListenerClazz = getClass(
                CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER
            )

            if (billingClientClazz == null ||
                purchaseClazz == null ||
                productDetailsClazz == null ||
                purchaseHistoryRecordClazz == null ||
                queryProductDetailsParamsProductClazz == null ||
                billingResultClazz == null ||

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
                purchasesResponseListenerClazz == null ||
                purchaseHistoryResponseListenerClazz == null

            ) {
                Log.w(
                    TAG,
                    "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }

            // Get methods: Query purchases
            val queryPurchasesAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_PURCHASES_ASYNC,
                    queryPurchasesParamsClazz,
                    purchasesResponseListenerClazz
                )
            val queryPurchasesParamsNewBuilderMethod =
                getMethod(queryPurchasesParamsClazz, METHOD_NEW_BUILDER)
            val queryPurchasesParamsBuilderBuildMethod =
                getMethod(queryPurchasesParamsBuilderClazz, METHOD_BUILD)
            val queryPurchasesParamsBuilderSetProductTypeMethod =
                getMethod(
                    queryPurchasesParamsBuilderClazz,
                    METHOD_SET_PRODUCT_TYPE,
                    String::class.java
                )
            val purchaseGetOriginalJsonMethod = getMethod(purchaseClazz, METHOD_GET_ORIGINAL_JSON)

            // Get methods: Query purchase history
            val queryPurchaseHistoryAsyncMethod = getMethod(
                billingClientClazz,
                METHOD_QUERY_PURCHASE_HISTORY_ASYNC,
                queryPurchaseHistoryParamsClazz,
                purchaseHistoryResponseListenerClazz
            )
            val queryPurchaseHistoryParamsNewBuilderMethod =
                getMethod(queryPurchaseHistoryParamsClazz, METHOD_NEW_BUILDER)
            val queryPurchaseHistoryParamsBuilderBuildMethod =
                getMethod(queryPurchaseHistoryParamsBuilderClazz, METHOD_BUILD)
            val queryPurchaseHistoryParamsBuilderSetProductTypeMethod =
                getMethod(
                    queryPurchaseHistoryParamsBuilderClazz,
                    METHOD_SET_PRODUCT_TYPE,
                    String::class.java
                )
            val purchaseHistoryRecordGetOriginalJsonMethod =
                getMethod(purchaseHistoryRecordClazz, METHOD_GET_ORIGINAL_JSON)

            // Get methods: Query product details
            val queryProductDetailsAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_PRODUCT_DETAILS_ASYNC,
                    queryProductDetailsParamsClazz,
                    productDetailsResponseListenerClazz
                )
            val queryProductDetailsParamsNewBuilderMethod =
                getMethod(queryProductDetailsParamsClazz, METHOD_NEW_BUILDER)
            val queryProductDetailsParamsBuilderBuildMethod =
                getMethod(queryProductDetailsParamsBuilderClazz, METHOD_BUILD)
            val queryProductDetailsParamsBuilderSetProductListMethod =
                getMethod(
                    queryProductDetailsParamsBuilderClazz,
                    METHOD_SET_PRODUCT_LIST,
                    List::class.java
                )
            val queryProductDetailsParamsProductNewBuilderMethod =
                getMethod(queryProductDetailsParamsProductClazz, METHOD_NEW_BUILDER)
            val queryProductDetailsParamsProductBuilderBuildMethod =
                getMethod(queryProductDetailsParamsProductBuilderClazz, METHOD_BUILD)
            val queryProductDetailsParamsProductBuilderSetProductIdMethod =
                getMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    METHOD_SET_PRODUCT_ID,
                    String::class.java
                )
            val queryProductDetailsParamsProductBuilderSetProductTypeMethod =
                getMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    METHOD_SET_PRODUCT_TYPE,
                    String::class.java
                )
            val productDetailsToStringMethod = getMethod(productDetailsClazz, METHOD_TO_STRING)

            // Get methods: Start billing client connection
            val billingClientStartConnectionMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_START_CONNECTION,
                    billingClientStateListenerClazz
                )

            val billingResultGetResponseCodeMethod =
                getMethod(billingResultClazz, METHOD_GET_RESPONSE_CODE)

            if (
                queryPurchasesAsyncMethod == null ||
                queryPurchasesParamsNewBuilderMethod == null ||
                queryPurchasesParamsBuilderBuildMethod == null ||
                queryPurchasesParamsBuilderSetProductTypeMethod == null ||
                purchaseGetOriginalJsonMethod == null ||

                queryPurchaseHistoryAsyncMethod == null ||
                queryPurchaseHistoryParamsNewBuilderMethod == null ||
                queryPurchaseHistoryParamsBuilderBuildMethod == null ||
                queryPurchaseHistoryParamsBuilderSetProductTypeMethod == null ||
                purchaseHistoryRecordGetOriginalJsonMethod == null ||

                queryProductDetailsAsyncMethod == null ||
                queryProductDetailsParamsNewBuilderMethod == null ||
                queryProductDetailsParamsBuilderBuildMethod == null ||
                queryProductDetailsParamsBuilderSetProductListMethod == null ||
                queryProductDetailsParamsProductNewBuilderMethod == null ||
                queryProductDetailsParamsProductBuilderBuildMethod == null ||
                queryProductDetailsParamsProductBuilderSetProductIdMethod == null ||
                queryProductDetailsParamsProductBuilderSetProductTypeMethod == null ||
                productDetailsToStringMethod == null ||

                billingClientStartConnectionMethod == null ||
                billingResultGetResponseCodeMethod == null
            ) {
                Log.w(
                    TAG,
                    "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }

            val billingClient = createBillingClient(
                context,
                billingClientClazz,
                billingClientBuilderClazz,
                purchasesUpdatedListenerClazz
            )
            if (billingClient == null) {
                Log.w(
                    TAG,
                    "Failed to build a Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }
            instance = InAppPurchaseBillingClientWrapperV5V7(
                context.packageName,
                billingClient,
                billingClientClazz,
                purchaseClazz,
                productDetailsClazz,
                purchaseHistoryRecordClazz,
                queryProductDetailsParamsProductClazz,
                billingResultClazz,

                queryProductDetailsParamsClazz,
                queryPurchaseHistoryParamsClazz,
                queryPurchasesParamsClazz,

                queryProductDetailsParamsBuilderClazz,
                queryPurchaseHistoryParamsBuilderClazz,
                queryPurchasesParamsBuilderClazz,
                queryProductDetailsParamsProductBuilderClazz,

                billingClientStateListenerClazz,
                productDetailsResponseListenerClazz,
                purchasesResponseListenerClazz,
                purchaseHistoryResponseListenerClazz,

                queryPurchasesAsyncMethod,
                queryPurchasesParamsNewBuilderMethod,
                queryPurchasesParamsBuilderBuildMethod,
                queryPurchasesParamsBuilderSetProductTypeMethod,
                purchaseGetOriginalJsonMethod,

                queryPurchaseHistoryAsyncMethod,
                queryPurchaseHistoryParamsNewBuilderMethod,
                queryPurchaseHistoryParamsBuilderBuildMethod,
                queryPurchaseHistoryParamsBuilderSetProductTypeMethod,
                purchaseHistoryRecordGetOriginalJsonMethod,

                queryProductDetailsAsyncMethod,
                queryProductDetailsParamsNewBuilderMethod,
                queryProductDetailsParamsBuilderBuildMethod,
                queryProductDetailsParamsBuilderSetProductListMethod,
                queryProductDetailsParamsProductNewBuilderMethod,
                queryProductDetailsParamsProductBuilderBuildMethod,
                queryProductDetailsParamsProductBuilderSetProductIdMethod,
                queryProductDetailsParamsProductBuilderSetProductTypeMethod,
                productDetailsToStringMethod,

                billingClientStartConnectionMethod,
                billingResultGetResponseCodeMethod
            )
            return instance
        }

        private fun createBillingClient(
            context: Context,
            billingClientClazz: Class<*>,
            billingClientBuilderClazz: Class<*>,
            purchasesUpdatedListenerClazz: Class<*>
        ): Any? {
            val billingClientNewBuilderMethod =
                getMethod(billingClientClazz, METHOD_NEW_BUILDER, Context::class.java)
            val billingClientBuilderSetListenerMethod =
                getMethod(
                    billingClientBuilderClazz,
                    METHOD_SET_LISTENER,
                    purchasesUpdatedListenerClazz
                )
            val billingClientBuilderEnablePendingPurchasesMethod =
                getMethod(billingClientBuilderClazz, METHOD_ENABLE_PENDING_PURCHASES)
            val billingClientBuilderBuildMethod = getMethod(billingClientBuilderClazz, METHOD_BUILD)

            if (billingClientBuilderBuildMethod == null ||
                billingClientBuilderSetListenerMethod == null ||
                billingClientNewBuilderMethod == null ||
                billingClientBuilderEnablePendingPurchasesMethod == null
            ) {
                return null
            }

            // 1. newBuilder(context)
            var builder: Any? =
                invokeMethod(billingClientClazz, billingClientNewBuilderMethod, null, context)

            // 2. setListener(listener)
            val listenerObj =
                Proxy.newProxyInstance(
                    purchasesUpdatedListenerClazz.classLoader,
                    arrayOf(purchasesUpdatedListenerClazz),
                    this
                )
            builder = invokeMethod(
                billingClientBuilderClazz,
                billingClientBuilderSetListenerMethod,
                builder,
                listenerObj
            )
            if (builder == null) {
                return null
            }

            // 3. enablePendingPurchases()
            builder = invokeMethod(
                billingClientBuilderClazz,
                billingClientBuilderEnablePendingPurchasesMethod,
                builder
            )

            // 4. build()
            return invokeMethod(billingClientBuilderClazz, billingClientBuilderBuildMethod, builder)
        }

        // This serves as the PurchasesUpdatedListener.
        // Because we are not launching any purchases, we need not implement this.
        override fun invoke(proxy: Any, m: Method, args: Array<Any>?): Any? = null
    }
}
