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
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PENDING_PURCHASES_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PENDING_PURCHASES_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PRODUCT_DETAILS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASES_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASES_UPDATED_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASES_PARAMS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_BUILD
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ENABLE_ONE_TIME_PRODUCTS
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ENABLE_PENDING_PURCHASES
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_ORIGINAL_JSON
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_RESPONSE_CODE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_NEW_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SERVICE_DISCONNECTED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SETUP_FINISHED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_PRODUCT_DETAILS_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_PURCHASES_UPDATED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_QUERY_PURCHASES_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PRODUCT_DETAILS_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PURCHASES_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_ID
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_LIST
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_PRODUCT_TYPE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_START_CONNECTION
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_TO_STRING
import com.facebook.appevents.iap.InAppPurchaseConstants.PRODUCT_ID
import com.facebook.appevents.iap.InAppPurchaseConstants.PRODUCT_IDS
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
class InAppPurchaseBillingClientWrapperV8Plus
private constructor(
    override val billingClient: Any,
    private val billingClientClazz: Class<*>,
    private val purchaseClazz: Class<*>,
    private val productDetailsClazz: Class<*>,
    private val queryProductDetailsParamsProductClazz: Class<*>,
    private val billingResultClazz: Class<*>,
    private val queryProductDetailsParamsClazz: Class<*>,
    private val queryPurchasesParamsClazz: Class<*>,
    private val queryProductDetailsParamsBuilderClazz: Class<*>,
    private val queryPurchasesParamsBuilderClazz: Class<*>,
    private val queryProductDetailsParamsProductBuilderClazz: Class<*>,
    private val billingClientStateListenerClazz: Class<*>,
    private val productDetailsResponseListenerClazz: Class<*>,
    private val purchasesResponseListenerClazz: Class<*>,
    private val queryPurchasesAsyncMethod: Method,
    private val queryPurchasesParamsNewBuilderMethod: Method,
    private val queryPurchasesParamsBuilderBuildMethod: Method,
    private val queryPurchasesParamsBuilderSetProductTypeMethod: Method,
    private val purchaseGetOriginalJsonMethod: Method,
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
    private val billingResultGetResponseCodeMethod: Method,
) : InAppPurchaseBillingClientWrapper {

    internal fun interface QueryResultCallback {
        fun onComplete(success: Boolean)
    }

    internal fun interface PurchaseQueryResultCallback {
        fun onComplete(result: PurchaseQueryResult)
    }

    internal data class PurchaseQueryResult(
        val productType: InAppPurchaseUtils.IAPProductType,
        val succeeded: Boolean,
        val purchaseDetails: Map<String, JSONObject> = emptyMap(),
        val productDetails: Map<String, JSONObject> = emptyMap(),
    )

    inner class ListenerWrapper(private val wrapperArgs: Array<Any>?) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, listenerArgs: Array<Any>?): Any? {
            when (method.name) {
                METHOD_ON_QUERY_PURCHASES_RESPONSE ->
                    onQueryPurchasesResponse(wrapperArgs, listenerArgs)

                METHOD_ON_PRODUCT_DETAILS_RESPONSE ->
                    onProductDetailsResponse(wrapperArgs, listenerArgs)

                METHOD_ON_BILLING_SETUP_FINISHED ->
                    onBillingSetupFinished(wrapperArgs, listenerArgs)

                METHOD_ON_BILLING_SERVICE_DISCONNECTED -> onBillingServiceDisconnected()
            }
            return null
        }
    }

    private fun getQueryPurchasesParams(productType: InAppPurchaseUtils.IAPProductType): Any? {
        var builder =
            invokeMethod(queryPurchasesParamsClazz, queryPurchasesParamsNewBuilderMethod, null)
                ?: return null
        builder =
            invokeMethod(
                queryPurchasesParamsBuilderClazz,
                queryPurchasesParamsBuilderSetProductTypeMethod,
                builder,
                productType.type,
            ) ?: return null
        return invokeMethod(
            queryPurchasesParamsBuilderClazz,
            queryPurchasesParamsBuilderBuildMethod,
            builder,
        )
    }

    private fun getQueryProductDetailsParams(
        productType: InAppPurchaseUtils.IAPProductType,
        productIds: List<String>,
    ): Any? {
        if (productIds.isEmpty()) {
            return null
        }

        val productList = ArrayList<Any>()
        for (productId in productIds) {
            var productBuilder =
                invokeMethod(
                    queryProductDetailsParamsProductClazz,
                    queryProductDetailsParamsProductNewBuilderMethod,
                    null,
                ) ?: continue
            productBuilder =
                invokeMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    queryProductDetailsParamsProductBuilderSetProductIdMethod,
                    productBuilder,
                    productId,
                ) ?: continue
            productBuilder =
                invokeMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    queryProductDetailsParamsProductBuilderSetProductTypeMethod,
                    productBuilder,
                    productType.type,
                ) ?: continue
            val product =
                invokeMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    queryProductDetailsParamsProductBuilderBuildMethod,
                    productBuilder,
                )
            if (product != null) {
                productList.add(product)
            }
        }
        if (productList.isEmpty()) {
            return null
        }

        var paramsBuilder =
            invokeMethod(
                queryProductDetailsParamsClazz,
                queryProductDetailsParamsNewBuilderMethod,
                null,
            ) ?: return null
        paramsBuilder =
            invokeMethod(
                queryProductDetailsParamsBuilderClazz,
                queryProductDetailsParamsBuilderSetProductListMethod,
                paramsBuilder,
                productList,
            ) ?: return null
        return invokeMethod(
            queryProductDetailsParamsBuilderClazz,
            queryProductDetailsParamsBuilderBuildMethod,
            paramsBuilder,
        )
    }

    override fun queryPurchases(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable,
    ) {
        queryPurchasesWithResult(productType) { result ->
            publishQueryResult(result)
            completionHandler.run()
        }
    }

    internal fun queryPurchasesWithResult(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: PurchaseQueryResultCallback,
    ) {
        val runnableQuery = Runnable {
            val queryPurchasesParams = getQueryPurchasesParams(productType)
            if (queryPurchasesParams == null) {
                Log.w(TAG, "Failed to build GPBL 8 query purchases parameters")
                completionHandler.onComplete(PurchaseQueryResult(productType, false))
                return@Runnable
            }
            val listener =
                Proxy.newProxyInstance(
                    purchasesResponseListenerClazz.classLoader,
                    arrayOf(purchasesResponseListenerClazz),
                    ListenerWrapper(arrayOf(productType, completionHandler)),
                )
            invokeMethod(
                billingClientClazz,
                queryPurchasesAsyncMethod,
                billingClient,
                queryPurchasesParams,
                listener,
            )
        }
        executeServiceRequest(
            runnableQuery,
            Runnable { completionHandler.onComplete(PurchaseQueryResult(productType, false)) },
        )
    }

    override fun queryPurchaseHistory(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable,
    ) {
        // GPBL 8 removed queryPurchaseHistoryAsync.
        completionHandler.run()
    }

    private fun queryProductDetailsAsync(
        productType: InAppPurchaseUtils.IAPProductType,
        productIds: List<String>,
        productDetailsTarget: MutableMap<String, JSONObject>,
        completionHandler: QueryResultCallback,
    ) {
        val runnableQuery = Runnable {
            val queryProductDetailsParams = getQueryProductDetailsParams(productType, productIds)
            if (queryProductDetailsParams == null) {
                Log.w(TAG, "Failed to build GPBL 8 query product details parameters")
                completionHandler.onComplete(false)
                return@Runnable
            }
            val listener =
                Proxy.newProxyInstance(
                    productDetailsResponseListenerClazz.classLoader,
                    arrayOf(productDetailsResponseListenerClazz),
                    ListenerWrapper(
                        arrayOf(productIds, productDetailsTarget, completionHandler)
                    ),
                )
            invokeMethod(
                billingClientClazz,
                queryProductDetailsAsyncMethod,
                billingClient,
                queryProductDetailsParams,
                listener,
            )
        }
        executeServiceRequest(runnableQuery, Runnable { completionHandler.onComplete(false) })
    }

    private fun executeServiceRequest(runnable: Runnable, failureHandler: Runnable) {
        if (isServiceConnected.get()) {
            runnable.run()
        } else {
            startConnection(runnable, failureHandler)
        }
    }

    private fun startConnection(runnable: Runnable, failureHandler: Runnable) {
        val listener =
            Proxy.newProxyInstance(
                billingClientStateListenerClazz.classLoader,
                arrayOf(billingClientStateListenerClazz),
                ListenerWrapper(arrayOf(runnable, failureHandler)),
            )
        invokeMethod(
            billingClientClazz,
            billingClientStartConnectionMethod,
            billingClient,
            listener,
        )
    }

    fun getOriginalJson(productDetailsString: String): String? {
        // ProductDetails does not expose its original JSON. GPBL 8 currently wraps it between
        // these two delimiters in toString(); return null if that representation changes.
        val start = productDetailsString.indexOf(PRODUCT_DETAILS_JSON_PREFIX)
        if (start < 0) {
            return null
        }
        val jsonStart = start + PRODUCT_DETAILS_JSON_PREFIX.length
        val jsonEnd = productDetailsString.lastIndexOf(PRODUCT_DETAILS_JSON_SUFFIX)
        return if (jsonEnd >= jsonStart) productDetailsString.substring(jsonStart, jsonEnd)
        else null
    }

    private fun onQueryPurchasesResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val completionHandler =
            wrapperArgs?.getOrNull(1) as? PurchaseQueryResultCallback ?: return
        val productType = wrapperArgs.getOrNull(0) as? InAppPurchaseUtils.IAPProductType
        if (productType == null) {
            Log.w(TAG, "Failed to read GPBL 8 purchases product type")
            return
        }
        val purchaseList = listenerArgs?.getOrNull(1) as? List<*>
        if (!hasSuccessfulBillingResult(listenerArgs, "purchases query")) {
            completionHandler.onComplete(PurchaseQueryResult(productType, false))
            return
        }
        if (purchaseList == null) {
            Log.w(TAG, "Failed to read GPBL 8 purchases response")
            completionHandler.onComplete(PurchaseQueryResult(productType, false))
            return
        }

        val purchaseJsons = mutableListOf<JSONObject>()
        for (purchase in purchaseList) {
            try {
                val purchaseJsonString =
                    invokeMethod(purchaseClazz, purchaseGetOriginalJsonMethod, purchase) as? String
                if (purchaseJsonString == null) {
                    continue
                }
                purchaseJsons.add(JSONObject(purchaseJsonString))
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to parse a GPBL 8 purchase", exception)
            }
        }
        val purchaseDetails = mutableMapOf<String, JSONObject>()
        val queryProductDetails = mutableMapOf<String, JSONObject>()
        processPurchaseJsons(
            productType,
            purchaseJsons,
            purchaseDetails,
            queryProductDetails,
        ) {
            // A successful purchases response completes the scan. ProductDetails and individual
            // payload failures remain retryable, but must not keep first-launch mode enabled.
            completionHandler.onComplete(
                PurchaseQueryResult(
                    productType,
                    true,
                    purchaseDetails.toMap(),
                    queryProductDetails.toMap(),
                )
            )
        }
    }

    internal fun processPurchaseJsons(
        productType: InAppPurchaseUtils.IAPProductType,
        purchaseJsons: List<JSONObject>,
        purchaseDetailsTarget: MutableMap<String, JSONObject>,
        productDetailsTarget: MutableMap<String, JSONObject>,
        completionHandler: QueryResultCallback,
    ) {
        val missingProductIds = linkedSetOf<String>()
        var mappedEveryPurchase = true
        for (purchaseJson in purchaseJsons) {
            val productIds = getProductIds(purchaseJson)
            if (productIds.isEmpty()) {
                mappedEveryPurchase = false
                Log.w(TAG, "GPBL 8 purchase is missing product identifiers")
                continue
            }
            for (productId in productIds) {
                val normalizedPurchase =
                    JSONObject(purchaseJson.toString()).put(PRODUCT_ID, productId)
                val cachedProductDetails = productDetailsMap[productId]
                if (cachedProductDetails == null) {
                    missingProductIds.add(productId)
                } else {
                    productDetailsTarget[productId] = cachedProductDetails
                }
                purchaseDetailsTarget[productId] = normalizedPurchase
            }
        }
        if (missingProductIds.isNotEmpty()) {
            queryProductDetailsAsync(
                productType,
                missingProductIds.toList(),
                productDetailsTarget,
            ) { completionHandler.onComplete(mappedEveryPurchase && it) }
        } else {
            completionHandler.onComplete(mappedEveryPurchase)
        }
    }

    private fun getProductIds(purchaseJson: JSONObject): List<String> {
        val productIds = mutableListOf<String>()
        val productIdArray = purchaseJson.optJSONArray(PRODUCT_IDS)
        if (productIdArray != null) {
            for (index in 0 until productIdArray.length()) {
                productIdArray.optString(index).takeIf { it.isNotEmpty() }?.let(productIds::add)
            }
        }
        if (productIds.isEmpty()) {
            purchaseJson.optString(PRODUCT_ID).takeIf { it.isNotEmpty() }?.let(productIds::add)
        }
        return productIds.distinct()
    }

    private fun onProductDetailsResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val requestedProductIds = wrapperArgs?.getOrNull(0) as? List<*> ?: return
        @Suppress("UNCHECKED_CAST")
        val productDetailsTarget =
            wrapperArgs.getOrNull(1) as? MutableMap<String, JSONObject> ?: return
        val completionHandler = wrapperArgs.getOrNull(2) as? QueryResultCallback ?: return
        if (!hasSuccessfulBillingResult(listenerArgs, "product details query")) {
            completionHandler.onComplete(false)
            return
        }
        val rawResult =
            listenerArgs?.getOrNull(1)
                ?: run {
                    Log.w(TAG, "GPBL 8 product details response is missing")
                    completionHandler.onComplete(false)
                    return
                }
        val productDetailsList =
            when (rawResult) {
                is List<*> -> rawResult
                else -> {
                    try {
                        val getProductDetailsList =
                            rawResult.javaClass.getMethod(METHOD_GET_PRODUCT_DETAILS_LIST)
                        getProductDetailsList.invoke(rawResult) as? List<*>
                    } catch (exception: Exception) {
                        Log.w(TAG, "Failed to read GPBL 8 product details response", exception)
                        null
                    }
                }
            }

        if (productDetailsList == null) {
            completionHandler.onComplete(false)
            return
        }
        for (productDetails in productDetailsList) {
            try {
                val productDetailsString =
                    invokeMethod(
                        productDetailsClazz,
                        productDetailsToStringMethod,
                        productDetails,
                    )
                        as? String ?: continue
                val productDetailsJsonString = getOriginalJson(productDetailsString) ?: continue
                val productDetailsJson = JSONObject(productDetailsJsonString)
                if (productDetailsJson.has(PRODUCT_ID)) {
                    productDetailsTarget[productDetailsJson.getString(PRODUCT_ID)] =
                        productDetailsJson
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to parse GPBL 8 product details", exception)
            }
        }
        completionHandler.onComplete(
            requestedProductIds.filterIsInstance<String>().all(productDetailsTarget::containsKey)
        )
    }

    private fun hasSuccessfulBillingResult(listenerArgs: Array<Any>?, operation: String): Boolean {
        val billingResult = listenerArgs?.getOrNull(0)
        val responseCode = billingResult?.let {
            invokeMethod(billingResultClazz, billingResultGetResponseCodeMethod, it) as? Int
        }
        if (responseCode != BILLING_RESPONSE_OK) {
            Log.w(TAG, "GPBL 8 $operation failed with response code $responseCode")
            return false
        }
        return true
    }

    private fun onBillingSetupFinished(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val failureHandler = wrapperArgs?.getOrNull(1) as? Runnable
        val billingResult =
            listenerArgs?.getOrNull(0)
                ?: run {
                    Log.w(TAG, "GPBL 8 billing setup response is missing")
                    failureHandler?.run()
                    return
                }
        val responseCode =
            invokeMethod(billingResultClazz, billingResultGetResponseCodeMethod, billingResult)
        if (responseCode == BILLING_RESPONSE_OK) {
            isServiceConnected.set(true)
            (wrapperArgs?.getOrNull(0) as? Runnable)?.run()
        } else {
            Log.w(TAG, "GPBL 8 billing setup failed with response code $responseCode")
            failureHandler?.run()
        }
    }

    private fun onBillingServiceDisconnected() {
        isServiceConnected.set(false)
    }

    companion object : InvocationHandler {
        private val TAG = InAppPurchaseBillingClientWrapperV8Plus::class.java.canonicalName
        private const val BILLING_RESPONSE_OK = 0
        private const val METHOD_GET_PRODUCT_DETAILS_LIST = "getProductDetailsList"
        private const val PRODUCT_DETAILS_JSON_PREFIX = "jsonString='"
        private const val PRODUCT_DETAILS_JSON_SUFFIX = "', parsedJson="
        val isServiceConnected = AtomicBoolean(false)
        @Volatile private var instance: InAppPurchaseBillingClientWrapperV8Plus? = null
        private val lock = Any()

        @Volatile internal var purchasesUpdatedHandler: ((Int, List<JSONObject>) -> Unit)? = null

        val iapPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val subsPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val productDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()

        internal fun publishQueryResult(result: PurchaseQueryResult) {
            productDetailsMap.putAll(result.productDetails)
            val purchaseDetailsTarget =
                if (result.productType == InAppPurchaseUtils.IAPProductType.INAPP) {
                    iapPurchaseDetailsMap
                } else {
                    subsPurchaseDetailsMap
                }
            purchaseDetailsTarget.putAll(result.purchaseDetails)
        }

        @JvmStatic
        fun getOrCreateInstance(context: Context): InAppPurchaseBillingClientWrapperV8Plus? {
            synchronized(lock) {
                return instance ?: createInstance(context)
            }
        }

        private fun createInstance(context: Context): InAppPurchaseBillingClientWrapperV8Plus? {
            val billingClientClazz = getClass(CLASSNAME_BILLING_CLIENT)
            val purchaseClazz = getClass(CLASSNAME_PURCHASE)
            val productDetailsClazz = getClass(CLASSNAME_PRODUCT_DETAILS)
            val queryProductDetailsParamsProductClazz =
                getClass(CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT)
            val billingResultClazz = getClass(CLASSNAME_BILLING_RESULT)

            val queryProductDetailsParamsClazz = getClass(CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS)
            val queryPurchasesParamsClazz = getClass(CLASSNAME_QUERY_PURCHASES_PARAMS)
            val pendingPurchasesParamsClazz = getClass(CLASSNAME_PENDING_PURCHASES_PARAMS)

            val queryProductDetailsParamsBuilderClazz =
                getClass(CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER)
            val queryPurchasesParamsBuilderClazz =
                getClass(CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER)
            val queryProductDetailsParamsProductBuilderClazz =
                getClass(CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER)
            val billingClientBuilderClazz = getClass(CLASSNAME_BILLING_CLIENT_BUILDER)
            val pendingPurchasesParamsBuilderClazz =
                getClass(CLASSNAME_PENDING_PURCHASES_PARAMS_BUILDER)

            val purchasesUpdatedListenerClazz = getClass(CLASSNAME_PURCHASES_UPDATED_LISTENER)
            val billingClientStateListenerClazz = getClass(CLASSNAME_BILLING_CLIENT_STATE_LISTENER)
            val productDetailsResponseListenerClazz =
                getClass(CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER)
            val purchasesResponseListenerClazz = getClass(CLASSNAME_PURCHASES_RESPONSE_LISTENER)

            if (
                billingClientClazz == null ||
                    purchaseClazz == null ||
                    productDetailsClazz == null ||
                    queryProductDetailsParamsProductClazz == null ||
                    billingResultClazz == null ||
                    queryProductDetailsParamsClazz == null ||
                    queryPurchasesParamsClazz == null ||
                    pendingPurchasesParamsClazz == null ||
                    queryProductDetailsParamsBuilderClazz == null ||
                    queryPurchasesParamsBuilderClazz == null ||
                    queryProductDetailsParamsProductBuilderClazz == null ||
                    billingClientBuilderClazz == null ||
                    pendingPurchasesParamsBuilderClazz == null ||
                    purchasesUpdatedListenerClazz == null ||
                    billingClientStateListenerClazz == null ||
                    productDetailsResponseListenerClazz == null ||
                    purchasesResponseListenerClazz == null
            ) {
                Log.w(TAG, WRAPPER_CREATION_ERROR)
                return null
            }

            val queryPurchasesAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_PURCHASES_ASYNC,
                    queryPurchasesParamsClazz,
                    purchasesResponseListenerClazz,
                )
            val queryPurchasesParamsNewBuilderMethod =
                getMethod(queryPurchasesParamsClazz, METHOD_NEW_BUILDER)
            val queryPurchasesParamsBuilderBuildMethod =
                getMethod(queryPurchasesParamsBuilderClazz, METHOD_BUILD)
            val queryPurchasesParamsBuilderSetProductTypeMethod =
                getMethod(
                    queryPurchasesParamsBuilderClazz,
                    METHOD_SET_PRODUCT_TYPE,
                    String::class.java,
                )
            val purchaseGetOriginalJsonMethod = getMethod(purchaseClazz, METHOD_GET_ORIGINAL_JSON)

            val queryProductDetailsAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_PRODUCT_DETAILS_ASYNC,
                    queryProductDetailsParamsClazz,
                    productDetailsResponseListenerClazz,
                )
            val queryProductDetailsParamsNewBuilderMethod =
                getMethod(queryProductDetailsParamsClazz, METHOD_NEW_BUILDER)
            val queryProductDetailsParamsBuilderBuildMethod =
                getMethod(queryProductDetailsParamsBuilderClazz, METHOD_BUILD)
            val queryProductDetailsParamsBuilderSetProductListMethod =
                getMethod(
                    queryProductDetailsParamsBuilderClazz,
                    METHOD_SET_PRODUCT_LIST,
                    List::class.java,
                )
            val queryProductDetailsParamsProductNewBuilderMethod =
                getMethod(queryProductDetailsParamsProductClazz, METHOD_NEW_BUILDER)
            val queryProductDetailsParamsProductBuilderBuildMethod =
                getMethod(queryProductDetailsParamsProductBuilderClazz, METHOD_BUILD)
            val queryProductDetailsParamsProductBuilderSetProductIdMethod =
                getMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    METHOD_SET_PRODUCT_ID,
                    String::class.java,
                )
            val queryProductDetailsParamsProductBuilderSetProductTypeMethod =
                getMethod(
                    queryProductDetailsParamsProductBuilderClazz,
                    METHOD_SET_PRODUCT_TYPE,
                    String::class.java,
                )
            val productDetailsToStringMethod = getMethod(productDetailsClazz, METHOD_TO_STRING)

            val billingClientStartConnectionMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_START_CONNECTION,
                    billingClientStateListenerClazz,
                )
            val billingResultGetResponseCodeMethod =
                getMethod(billingResultClazz, METHOD_GET_RESPONSE_CODE)

            if (
                queryPurchasesAsyncMethod == null ||
                    queryPurchasesParamsNewBuilderMethod == null ||
                    queryPurchasesParamsBuilderBuildMethod == null ||
                    queryPurchasesParamsBuilderSetProductTypeMethod == null ||
                    purchaseGetOriginalJsonMethod == null ||
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
                Log.w(TAG, WRAPPER_CREATION_ERROR)
                return null
            }

            val billingClient =
                createBillingClient(
                    context,
                    billingClientClazz,
                    billingClientBuilderClazz,
                    purchasesUpdatedListenerClazz,
                    pendingPurchasesParamsClazz,
                    pendingPurchasesParamsBuilderClazz,
                )
            if (billingClient == null) {
                Log.w(TAG, WRAPPER_BUILD_ERROR)
                return null
            }

            instance =
                InAppPurchaseBillingClientWrapperV8Plus(
                    billingClient,
                    billingClientClazz,
                    purchaseClazz,
                    productDetailsClazz,
                    queryProductDetailsParamsProductClazz,
                    billingResultClazz,
                    queryProductDetailsParamsClazz,
                    queryPurchasesParamsClazz,
                    queryProductDetailsParamsBuilderClazz,
                    queryPurchasesParamsBuilderClazz,
                    queryProductDetailsParamsProductBuilderClazz,
                    billingClientStateListenerClazz,
                    productDetailsResponseListenerClazz,
                    purchasesResponseListenerClazz,
                    queryPurchasesAsyncMethod,
                    queryPurchasesParamsNewBuilderMethod,
                    queryPurchasesParamsBuilderBuildMethod,
                    queryPurchasesParamsBuilderSetProductTypeMethod,
                    purchaseGetOriginalJsonMethod,
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
                    billingResultGetResponseCodeMethod,
                )
            return instance
        }

        private fun createBillingClient(
            context: Context,
            billingClientClazz: Class<*>,
            billingClientBuilderClazz: Class<*>,
            purchasesUpdatedListenerClazz: Class<*>,
            pendingPurchasesParamsClazz: Class<*>,
            pendingPurchasesParamsBuilderClazz: Class<*>,
        ): Any? {
            val billingClientNewBuilderMethod =
                getMethod(billingClientClazz, METHOD_NEW_BUILDER, Context::class.java)
            val billingClientBuilderSetListenerMethod =
                getMethod(
                    billingClientBuilderClazz,
                    METHOD_SET_LISTENER,
                    purchasesUpdatedListenerClazz,
                )
            val billingClientBuilderEnablePendingPurchasesMethod =
                getMethod(
                    billingClientBuilderClazz,
                    METHOD_ENABLE_PENDING_PURCHASES,
                    pendingPurchasesParamsClazz,
                )
            val billingClientBuilderBuildMethod = getMethod(billingClientBuilderClazz, METHOD_BUILD)
            val pendingPurchasesParamsNewBuilderMethod =
                getMethod(pendingPurchasesParamsClazz, METHOD_NEW_BUILDER)
            val pendingPurchasesParamsBuilderEnableOneTimeProductsMethod =
                getMethod(
                    pendingPurchasesParamsBuilderClazz,
                    METHOD_ENABLE_ONE_TIME_PRODUCTS,
                )
            val pendingPurchasesParamsBuilderBuildMethod =
                getMethod(pendingPurchasesParamsBuilderClazz, METHOD_BUILD)

            if (
                billingClientNewBuilderMethod == null ||
                    billingClientBuilderSetListenerMethod == null ||
                    billingClientBuilderEnablePendingPurchasesMethod == null ||
                    billingClientBuilderBuildMethod == null ||
                    pendingPurchasesParamsNewBuilderMethod == null ||
                    pendingPurchasesParamsBuilderEnableOneTimeProductsMethod == null ||
                    pendingPurchasesParamsBuilderBuildMethod == null
            ) {
                return null
            }

            var billingClientBuilder =
                invokeMethod(billingClientClazz, billingClientNewBuilderMethod, null, context)
                    ?: return null
            val purchasesUpdatedListener =
                Proxy.newProxyInstance(
                    purchasesUpdatedListenerClazz.classLoader,
                    arrayOf(purchasesUpdatedListenerClazz),
                    this,
                )
            billingClientBuilder =
                invokeMethod(
                    billingClientBuilderClazz,
                    billingClientBuilderSetListenerMethod,
                    billingClientBuilder,
                    purchasesUpdatedListener,
                ) ?: return null

            var pendingPurchasesParamsBuilder =
                invokeMethod(
                    pendingPurchasesParamsClazz,
                    pendingPurchasesParamsNewBuilderMethod,
                    null,
                ) ?: return null
            pendingPurchasesParamsBuilder =
                invokeMethod(
                    pendingPurchasesParamsBuilderClazz,
                    pendingPurchasesParamsBuilderEnableOneTimeProductsMethod,
                    pendingPurchasesParamsBuilder,
                ) ?: return null
            val pendingPurchasesParams =
                invokeMethod(
                    pendingPurchasesParamsBuilderClazz,
                    pendingPurchasesParamsBuilderBuildMethod,
                    pendingPurchasesParamsBuilder,
                ) ?: return null
            billingClientBuilder =
                invokeMethod(
                    billingClientBuilderClazz,
                    billingClientBuilderEnablePendingPurchasesMethod,
                    billingClientBuilder,
                    pendingPurchasesParams,
                ) ?: return null
            return invokeMethod(
                billingClientBuilderClazz,
                billingClientBuilderBuildMethod,
                billingClientBuilder,
            )
        }

        override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
            if (method.name == METHOD_ON_PURCHASES_UPDATED) {
                handlePurchasesUpdated(args)
            }
            return null
        }

        private fun handlePurchasesUpdated(args: Array<Any>?) {
            val wrapper = instance ?: return
            val billingResult = args?.getOrNull(0) ?: return
            val purchases = args.getOrNull(1) as? List<*> ?: return
            val responseCode =
                invokeMethod(
                    wrapper.billingResultClazz,
                    wrapper.billingResultGetResponseCodeMethod,
                    billingResult,
                )
                    as? Int ?: return
            val purchaseJsons = mutableListOf<JSONObject>()
            for (purchase in purchases) {
                try {
                    val originalJson =
                        invokeMethod(
                            wrapper.purchaseClazz,
                            wrapper.purchaseGetOriginalJsonMethod,
                            purchase,
                        )
                            as? String ?: continue
                    purchaseJsons.add(JSONObject(originalJson))
                } catch (exception: Exception) {
                    Log.w(TAG, "Failed to parse a GPBL 8 realtime purchase update", exception)
                }
            }
            purchasesUpdatedHandler?.invoke(responseCode, purchaseJsons)
        }

        private const val WRAPPER_CREATION_ERROR =
            "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
        private const val WRAPPER_BUILD_ERROR =
            "Failed to build a Google Play billing library wrapper for in-app purchase auto-logging"
    }
}
