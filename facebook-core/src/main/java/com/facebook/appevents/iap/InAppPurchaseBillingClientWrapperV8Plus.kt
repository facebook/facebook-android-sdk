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
        val runnableQuery = Runnable {
            val queryPurchasesParams = getQueryPurchasesParams(productType)
            if (queryPurchasesParams == null) {
                Log.w(TAG, "Failed to build GPBL 8 query purchases parameters")
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
        executeServiceRequest(runnableQuery)
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
        completionHandler: Runnable,
    ) {
        val runnableQuery = Runnable {
            val queryProductDetailsParams = getQueryProductDetailsParams(productType, productIds)
            if (queryProductDetailsParams == null) {
                Log.w(TAG, "Failed to build GPBL 8 query product details parameters")
                return@Runnable
            }
            val listener =
                Proxy.newProxyInstance(
                    productDetailsResponseListenerClazz.classLoader,
                    arrayOf(productDetailsResponseListenerClazz),
                    ListenerWrapper(arrayOf(completionHandler)),
                )
            invokeMethod(
                billingClientClazz,
                queryProductDetailsAsyncMethod,
                billingClient,
                queryProductDetailsParams,
                listener,
            )
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
        val listener =
            Proxy.newProxyInstance(
                billingClientStateListenerClazz.classLoader,
                arrayOf(billingClientStateListenerClazz),
                ListenerWrapper(arrayOf(runnable)),
            )
        invokeMethod(
            billingClientClazz,
            billingClientStartConnectionMethod,
            billingClient,
            listener,
        )
    }

    fun getOriginalJson(productDetailsString: String): String? {
        val jsonStringRegex = """jsonString='(.*?)'""".toRegex()
        return jsonStringRegex.find(productDetailsString)?.groupValues?.getOrNull(1)
    }

    private fun onQueryPurchasesResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val completionHandler = wrapperArgs?.getOrNull(1) as? Runnable ?: return
        val productType = wrapperArgs.getOrNull(0) as? InAppPurchaseUtils.IAPProductType
        val purchaseList = listenerArgs?.getOrNull(1) as? List<*>
        if (productType == null || purchaseList == null) {
            Log.w(TAG, "Failed to read GPBL 8 purchases response")
            return
        }

        val productIds = mutableListOf<String>()
        for (purchase in purchaseList) {
            try {
                val purchaseJsonString =
                    invokeMethod(purchaseClazz, purchaseGetOriginalJsonMethod, purchase) as? String
                        ?: continue
                val purchaseJson = JSONObject(purchaseJsonString)
                if (!purchaseJson.has(PRODUCT_ID)) {
                    continue
                }
                val productId = purchaseJson.getString(PRODUCT_ID)
                if (productId !in productDetailsMap) {
                    productIds.add(productId)
                }
                if (productType == InAppPurchaseUtils.IAPProductType.INAPP) {
                    iapPurchaseDetailsMap[productId] = purchaseJson
                } else {
                    subsPurchaseDetailsMap[productId] = purchaseJson
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to parse a GPBL 8 purchase", exception)
            }
        }
        if (productIds.isNotEmpty()) {
            queryProductDetailsAsync(productType, productIds, completionHandler)
        } else {
            completionHandler.run()
        }
    }

    private fun onProductDetailsResponse(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val completionHandler = wrapperArgs?.getOrNull(0) as? Runnable ?: return
        val rawResult =
            listenerArgs?.getOrNull(1)
                ?: run {
                    Log.w(TAG, "GPBL 8 product details response is missing")
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
                    productDetailsMap[productDetailsJson.getString(PRODUCT_ID)] = productDetailsJson
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to parse GPBL 8 product details", exception)
            }
        }
        completionHandler.run()
    }

    private fun onBillingSetupFinished(wrapperArgs: Array<Any>?, listenerArgs: Array<Any>?) {
        val billingResult = listenerArgs?.getOrNull(0) ?: return
        val responseCode =
            invokeMethod(billingResultClazz, billingResultGetResponseCodeMethod, billingResult)
        if (responseCode == 0) {
            isServiceConnected.set(true)
            (wrapperArgs?.getOrNull(0) as? Runnable)?.run()
        } else {
            Log.w(TAG, "GPBL 8 billing setup failed with response code $responseCode")
        }
    }

    private fun onBillingServiceDisconnected() {
        isServiceConnected.set(false)
    }

    companion object : InvocationHandler {
        private val TAG = InAppPurchaseBillingClientWrapperV8Plus::class.java.canonicalName
        private const val METHOD_GET_PRODUCT_DETAILS_LIST = "getProductDetailsList"
        val isServiceConnected = AtomicBoolean(false)
        private var instance: InAppPurchaseBillingClientWrapperV8Plus? = null
        private val lock = Any()

        val iapPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val subsPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val productDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()

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

        override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? = null

        private const val WRAPPER_CREATION_ERROR =
            "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
        private const val WRAPPER_BUILD_ERROR =
            "Failed to build a Google Play billing library wrapper for in-app purchase auto-logging"
    }
}
