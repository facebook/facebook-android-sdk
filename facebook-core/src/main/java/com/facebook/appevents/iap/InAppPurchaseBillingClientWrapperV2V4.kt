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
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASES_RESULT
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE_HISTORY_RECORD
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_PURCHASE_UPDATED_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_SKU_DETAILS
import com.facebook.appevents.iap.InAppPurchaseConstants.CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_BUILD
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ENABLE_PENDING_PURCHASES
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_ORIGINAL_JSON
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_PURCHASE_LIST
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_GET_RESPONSE_CODE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_NEW_BUILDER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SERVICE_DISCONNECTED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_BILLING_SETUP_FINISHED
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_PURCHASE_HISTORY_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_ON_SKU_DETAILS_RESPONSE
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PURCHASES
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_PURCHASE_HISTORY_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_QUERY_SKU_DETAILS_ASYNC
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_SET_LISTENER
import com.facebook.appevents.iap.InAppPurchaseConstants.METHOD_START_CONNECTION
import com.facebook.appevents.iap.InAppPurchaseConstants.PACKAGE_NAME
import com.facebook.appevents.iap.InAppPurchaseConstants.PRODUCT_ID
import com.facebook.appevents.iap.InAppPurchaseSkuDetailsWrapper.Companion.getOrCreateInstance
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InAppPurchaseBillingClientWrapperV2V4
private constructor(
    private val packageName: String,
    override val billingClient: Any,
    private val billingClientClazz: Class<*>,
    private val purchaseResultClazz: Class<*>,
    private val purchaseClazz: Class<*>,
    private val skuDetailsClazz: Class<*>,
    private val purchaseHistoryRecordClazz: Class<*>,
    private val skuDetailsResponseListenerClazz: Class<*>,
    private val purchaseHistoryResponseListenerClazz: Class<*>,
    private val queryPurchasesMethod: Method,
    private val getPurchaseListMethod: Method,
    private val getOriginalJsonMethod: Method,
    private val getOriginalJsonSkuMethod: Method,
    private val getOriginalJsonPurchaseHistoryMethod: Method,
    private val querySkuDetailsAsyncMethod: Method,
    private val queryPurchaseHistoryAsyncMethod: Method,
    private val inAppPurchaseSkuDetailsWrapper: InAppPurchaseSkuDetailsWrapper
) : InAppPurchaseBillingClientWrapper {

    override fun queryPurchases(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable
    ) {
        val queryPurchaseRunnable = Runnable {
            val purchaseResult =
                invokeMethod(
                    billingClientClazz,
                    queryPurchasesMethod,
                    billingClient,
                    productType.type
                )
            val purchaseObjects =
                invokeMethod(purchaseResultClazz, getPurchaseListMethod, purchaseResult) as? List<*>
            try {
                val skuIDs: MutableList<String?> = arrayListOf()
                if (purchaseObjects == null) {
                    return@Runnable
                }
                for (purchaseObject in purchaseObjects) {
                    val purchaseJsonStr =
                        invokeMethod(
                            purchaseClazz,
                            getOriginalJsonMethod,
                            purchaseObject
                        ) as? String ?: continue
                    val purchaseJson = JSONObject(purchaseJsonStr)
                    if (!purchaseJson.has(PRODUCT_ID)) {
                        continue
                    }
                    val skuID = purchaseJson.getString(PRODUCT_ID)
                    skuIDs.add(skuID)
                    if (productType == InAppPurchaseUtils.IAPProductType.INAPP) {
                        iapPurchaseDetailsMap[skuID] = purchaseJson
                    } else {
                        subsPurchaseDetailsMap[skuID] = purchaseJson
                    }
                }
                querySkuDetailsAsync(productType, skuIDs, completionHandler)
            } catch (je: JSONException) {
                /* swallow */
            }
        }
        executeServiceRequest(queryPurchaseRunnable)
    }

    private fun querySkuDetailsAsync(
        skuType: InAppPurchaseUtils.IAPProductType,
        skuIDs: List<String?>,
        completionHandler: Runnable
    ) {
        val querySkuDetailAsyncRunnable = Runnable {
            val listenerObj =
                Proxy.newProxyInstance(
                    skuDetailsResponseListenerClazz.classLoader,
                    arrayOf(skuDetailsResponseListenerClazz),
                    SkuDetailsResponseListenerWrapper(completionHandler)
                )
            val skuDetailsParams =
                inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs)
            invokeMethod(
                billingClientClazz,
                querySkuDetailsAsyncMethod,
                billingClient,
                skuDetailsParams,
                listenerObj
            )
        }
        executeServiceRequest(querySkuDetailAsyncRunnable)
    }

    override fun queryPurchaseHistory(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable
    ) {
        val queryPurchaseHistoryAsyncRunnable = Runnable {
            val listenerObj =
                Proxy.newProxyInstance(
                    purchaseHistoryResponseListenerClazz.classLoader,
                    arrayOf(purchaseHistoryResponseListenerClazz),
                    PurchaseHistoryResponseListenerWrapper(productType, completionHandler)
                )
            invokeMethod(
                billingClientClazz,
                queryPurchaseHistoryAsyncMethod,
                billingClient,
                productType.type,
                listenerObj
            )
        }
        executeServiceRequest(queryPurchaseHistoryAsyncRunnable)
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected.get()) {
            runnable.run()
        } else {
            startConnection(runnable)
        }
    }

    private fun startConnection(runnable: Runnable?) {
        val listenerClazz = getClass(CLASSNAME_BILLING_CLIENT_STATE_LISTENER) ?: return
        val method = getMethod(billingClientClazz, METHOD_START_CONNECTION, listenerClazz) ?: return
        val listenerObj =
            Proxy.newProxyInstance(
                listenerClazz.classLoader,
                arrayOf(listenerClazz),
                BillingClientStateListenerWrapper(runnable)
            )
        invokeMethod(billingClientClazz, method, billingClient, listenerObj)
    }

    @AutoHandleExceptions
    internal class BillingClientStateListenerWrapper(private val runnable: Runnable?) :
        InvocationHandler {
        override fun invoke(proxy: Any, m: Method, args: Array<Any>?): Any? {
            if (m.name == METHOD_ON_BILLING_SETUP_FINISHED) {
                val billingResult = args?.getOrNull(0)
                val billingResultClazz = getClass(CLASSNAME_BILLING_RESULT) ?: return null
                val billingResultGetResponseCodeMethod =
                    getMethod(
                        billingResultClazz, METHOD_GET_RESPONSE_CODE
                    ) ?: return null
                val responseCode =
                    invokeMethod(
                        billingResultClazz,
                        billingResultGetResponseCodeMethod,
                        billingResult
                    )
                if (responseCode == 0) {
                    isServiceConnected.set(true)
                    runnable?.run()
                }
            } else if (m.name.endsWith(METHOD_ON_BILLING_SERVICE_DISCONNECTED)) {
                isServiceConnected.set(false)
            }
            return null
        }
    }

    @AutoHandleExceptions
    internal class PurchasesUpdatedListenerWrapper : InvocationHandler {
        // dummy function, no need to implement onPurchasesUpdated
        override fun invoke(proxy: Any, m: Method, args: Array<Any>?): Any? = null
    }

    @AutoHandleExceptions
    internal inner class PurchaseHistoryResponseListenerWrapper(
        private var skuType: InAppPurchaseUtils.IAPProductType,
        private var completionHandler: Runnable
    ) :
        InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<Any>?) {
            if (method.name != METHOD_ON_PURCHASE_HISTORY_RESPONSE) {
                return
            }
            val purchaseHistoryRecordList = args?.getOrNull(1)
            if (purchaseHistoryRecordList == null || purchaseHistoryRecordList !is List<*>) {
                return
            }
            val skuIDs = mutableListOf<String>()
            for (purchaseHistoryObject in purchaseHistoryRecordList) {
                try {
                    val purchaseHistoryJsonRaw =
                        invokeMethod(
                            purchaseHistoryRecordClazz,
                            getOriginalJsonPurchaseHistoryMethod,
                            purchaseHistoryObject
                        ) as? String ?: continue
                    val purchaseHistoryJson = JSONObject(purchaseHistoryJsonRaw)
                    val packageName = packageName
                    purchaseHistoryJson.put(PACKAGE_NAME, packageName)
                    if (!purchaseHistoryJson.has(PRODUCT_ID)) {
                        continue
                    }
                    val skuID = purchaseHistoryJson.getString(PRODUCT_ID)
                    skuIDs.add(skuID)
                    if (skuType == InAppPurchaseUtils.IAPProductType.INAPP) {
                        iapPurchaseDetailsMap[skuID] = purchaseHistoryJson
                    } else {
                        subsPurchaseDetailsMap[skuID] = purchaseHistoryJson
                    }
                } catch (e: Exception) {
                    /* swallow */
                }
            }
            if (skuIDs.isNotEmpty()) {
                querySkuDetailsAsync(skuType, skuIDs, completionHandler)
            } else {
                // If skuIDs is empty we have all of the product info we need and can execute our completion handler
                completionHandler.run()
            }

        }
    }

    @AutoHandleExceptions
    internal inner class SkuDetailsResponseListenerWrapper(private var completionHandler: Runnable) :
        InvocationHandler {
        override fun invoke(proxy: Any, m: Method, args: Array<Any>?) {
            if (m.name != METHOD_ON_SKU_DETAILS_RESPONSE) {
                return
            }
            val skuDetailsList = args?.getOrNull(1)
            if (skuDetailsList == null || skuDetailsList !is List<*>) {
                return
            }
            for (skuDetail in skuDetailsList) {
                try {
                    val skuDetailJson =
                        invokeMethod(
                            skuDetailsClazz,
                            getOriginalJsonSkuMethod,
                            skuDetail
                        ) as? String ?: continue
                    val skuJson = JSONObject(skuDetailJson)
                    if (!skuJson.has(PRODUCT_ID)) {
                        continue
                    }
                    val skuID = skuJson.getString(PRODUCT_ID)
                    skuDetailsMap[skuID] = skuJson
                } catch (e: Exception) {
                    /* swallow */
                }
            }
            completionHandler.run()
        }
    }

    companion object {
        private val TAG = InAppPurchaseBillingClientWrapperV2V4::class.java.canonicalName
        private var instance: InAppPurchaseBillingClientWrapperV2V4? = null
        val isServiceConnected = AtomicBoolean(false)

        // Use ConcurrentHashMap because purchase values may be updated in different threads
        val iapPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val subsPurchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
        val skuDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()


        @Synchronized
        @JvmStatic
        fun getOrCreateInstance(context: Context): InAppPurchaseBillingClientWrapperV2V4? {
            return instance ?: createInstance(context)
        }

        private fun createInstance(context: Context): InAppPurchaseBillingClientWrapperV2V4? {
            val inAppPurchaseSkuDetailsWrapper = getOrCreateInstance() ?: return null
            val billingClientClazz = getClass(CLASSNAME_BILLING_CLIENT)
            val purchaseClazz = getClass(CLASSNAME_PURCHASE)
            val purchaseResultClazz = getClass(CLASSNAME_PURCHASES_RESULT)
            val skuDetailsClazz = getClass(CLASSNAME_SKU_DETAILS)
            val purchaseHistoryRecordClazz = getClass(CLASSNAME_PURCHASE_HISTORY_RECORD)
            val skuDetailsResponseListenerClazz = getClass(CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER)
            val purchaseHistoryResponseListenerClazz =
                getClass(CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER)
            if (billingClientClazz == null ||
                purchaseResultClazz == null ||
                purchaseClazz == null ||
                skuDetailsClazz == null ||
                skuDetailsResponseListenerClazz == null ||
                purchaseHistoryRecordClazz == null ||
                purchaseHistoryResponseListenerClazz == null
            ) {
                Log.w(
                    TAG,
                    "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }
            val queryPurchasesMethod =
                getMethod(billingClientClazz, METHOD_QUERY_PURCHASES, String::class.java)
            val getPurchaseListMethod = getMethod(purchaseResultClazz, METHOD_GET_PURCHASE_LIST)
            val getOriginalJsonMethod = getMethod(purchaseClazz, METHOD_GET_ORIGINAL_JSON)
            val getOriginalJsonSkuMethod = getMethod(skuDetailsClazz, METHOD_GET_ORIGINAL_JSON)
            val getOriginalJsonPurchaseHistoryMethod =
                getMethod(purchaseHistoryRecordClazz, METHOD_GET_ORIGINAL_JSON)
            val querySkuDetailsAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_SKU_DETAILS_ASYNC,
                    inAppPurchaseSkuDetailsWrapper.skuDetailsParamsClazz,
                    skuDetailsResponseListenerClazz
                )
            val queryPurchaseHistoryAsyncMethod =
                getMethod(
                    billingClientClazz,
                    METHOD_QUERY_PURCHASE_HISTORY_ASYNC,
                    String::class.java,
                    purchaseHistoryResponseListenerClazz
                )
            if (queryPurchasesMethod == null ||
                getPurchaseListMethod == null ||
                getOriginalJsonMethod == null ||
                getOriginalJsonSkuMethod == null ||
                getOriginalJsonPurchaseHistoryMethod == null ||
                querySkuDetailsAsyncMethod == null ||
                queryPurchaseHistoryAsyncMethod == null
            ) {
                Log.w(
                    TAG,
                    "Failed to create Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }
            val billingClient = createBillingClient(context, billingClientClazz)
            if (billingClient == null) {
                Log.w(
                    TAG,
                    "Failed to build a Google Play billing library wrapper for in-app purchase auto-logging"
                )
                return null
            }
            instance =
                InAppPurchaseBillingClientWrapperV2V4(
                    context.packageName,
                    billingClient,
                    billingClientClazz,
                    purchaseResultClazz,
                    purchaseClazz,
                    skuDetailsClazz,
                    purchaseHistoryRecordClazz,
                    skuDetailsResponseListenerClazz,
                    purchaseHistoryResponseListenerClazz,
                    queryPurchasesMethod,
                    getPurchaseListMethod,
                    getOriginalJsonMethod,
                    getOriginalJsonSkuMethod,
                    getOriginalJsonPurchaseHistoryMethod,
                    querySkuDetailsAsyncMethod,
                    queryPurchaseHistoryAsyncMethod,
                    inAppPurchaseSkuDetailsWrapper
                )
            return instance
        }

        private fun createBillingClient(context: Context?, billingClientClazz: Class<*>): Any? {
            // 0. pre-check
            val builderClazz = getClass(CLASSNAME_BILLING_CLIENT_BUILDER)
            val listenerClazz = getClass(CLASSNAME_PURCHASE_UPDATED_LISTENER)
            if (builderClazz == null || listenerClazz == null) {
                return null
            }
            val newBuilderMethod =
                getMethod(billingClientClazz, METHOD_NEW_BUILDER, Context::class.java)
            val enablePendingPurchasesMethod =
                getMethod(builderClazz, METHOD_ENABLE_PENDING_PURCHASES)
            val setListenerMethod = getMethod(builderClazz, METHOD_SET_LISTENER, listenerClazz)
            val buildMethod = getMethod(builderClazz, METHOD_BUILD)
            if (newBuilderMethod == null ||
                enablePendingPurchasesMethod == null ||
                setListenerMethod == null ||
                buildMethod == null
            ) {
                return null
            }

            // 1. newBuilder(context)
            var builder: Any? =
                invokeMethod(billingClientClazz, newBuilderMethod, null, context) ?: return null

            // 2. setListener(listener)
            val listenerObj =
                Proxy.newProxyInstance(
                    listenerClazz.classLoader,
                    arrayOf(listenerClazz),
                    PurchasesUpdatedListenerWrapper()
                )
            builder = invokeMethod(builderClazz, setListenerMethod, builder, listenerObj)
            if (builder == null) {
                return null
            }

            // 3. enablePendingPurchases() or  4. build()
            builder = invokeMethod(builderClazz, enablePendingPurchasesMethod, builder)
            return if (builder == null) {
                null
            } else invokeMethod(builderClazz, buildMethod, builder)
        }
    }
}
