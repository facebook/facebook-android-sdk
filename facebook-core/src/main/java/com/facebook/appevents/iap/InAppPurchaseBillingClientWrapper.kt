/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.iap

import android.content.Context
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
class InAppPurchaseBillingClientWrapper
private constructor(
    private val context: Context,
    private val billingClient: Any,
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
) {
  private val historyPurchaseSet: MutableSet<String?> = CopyOnWriteArraySet()
  fun queryPurchaseHistory(skuType: String, queryPurchaseHistoryRunnable: Runnable) {
    queryPurchaseHistoryAsync(skuType) {
      querySkuDetailsAsync(IN_APP, ArrayList(historyPurchaseSet), queryPurchaseHistoryRunnable)
    }
  }

  fun queryPurchase(skuType: String, querySkuRunnable: Runnable) {
    // TODO (T67568885): support subs
    val purchaseResult =
        invokeMethod(billingClientClazz, queryPurchasesMethod, billingClient, IN_APP)
    val purchaseObjects =
        invokeMethod(purchaseResultClazz, getPurchaseListMethod, purchaseResult) as? List<*>
            ?: return
    try {
      val skuIDs: MutableList<String?> = arrayListOf()
      for (purchaseObject in purchaseObjects) {
        val purchaseJsonStr =
            invokeMethod(purchaseClazz, getOriginalJsonMethod, purchaseObject) as? String
                ?: continue
        val purchaseJson = JSONObject(purchaseJsonStr)
        if (purchaseJson.has(PRODUCT_ID)) {
          val skuID = purchaseJson.getString(PRODUCT_ID)
          skuIDs.add(skuID)
          purchaseDetailsMap[skuID] = purchaseJson
        }
      }
      querySkuDetailsAsync(skuType, skuIDs, querySkuRunnable)
    } catch (je: JSONException) {
      /* swallow */
    }
  }

  private fun querySkuDetailsAsync(skuType: String, skuIDs: List<String?>, runnable: Runnable) {
    val listenerObj =
        Proxy.newProxyInstance(
            skuDetailsResponseListenerClazz.classLoader,
            arrayOf(skuDetailsResponseListenerClazz),
            SkuDetailsResponseListenerWrapper(runnable))
    val skuDetailsParams = inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs)
    invokeMethod(
        billingClientClazz,
        querySkuDetailsAsyncMethod,
        billingClient,
        skuDetailsParams,
        listenerObj)
  }

  private fun queryPurchaseHistoryAsync(skuType: String, runnable: Runnable) {
    val listenerObj =
        Proxy.newProxyInstance(
            purchaseHistoryResponseListenerClazz.classLoader,
            arrayOf(purchaseHistoryResponseListenerClazz),
            PurchaseHistoryResponseListenerWrapper(runnable))
    invokeMethod(
        billingClientClazz, queryPurchaseHistoryAsyncMethod, billingClient, skuType, listenerObj)
  }

  private fun startConnection() {
    val listenerClazz = getClass(CLASSNAME_BILLING_CLIENT_STATE_LISTENER) ?: return
    val method = getMethod(billingClientClazz, METHOD_START_CONNECTION, listenerClazz) ?: return
    val listenerObj =
        Proxy.newProxyInstance(
            listenerClazz.classLoader, arrayOf(listenerClazz), BillingClientStateListenerWrapper())
    invokeMethod(billingClientClazz, method, billingClient, listenerObj)
  }

  @AutoHandleExceptions
  internal class BillingClientStateListenerWrapper : InvocationHandler {
    override fun invoke(proxy: Any, m: Method, args: Array<Any>?): Any? {
      if (m.name == METHOD_ON_BILLING_SETUP_FINISHED) {
        isServiceConnected.set(true)
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
  internal inner class PurchaseHistoryResponseListenerWrapper(var runnable: Runnable) :
      InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
      if (method.name == METHOD_ON_PURCHASE_HISTORY_RESPONSE) {
        val purchaseHistoryRecordListObject = args?.get(1)
        if (purchaseHistoryRecordListObject != null && purchaseHistoryRecordListObject is List<*>) {
          getPurchaseHistoryRecord(purchaseHistoryRecordListObject)
        }
      }
      return null
    }

    private fun getPurchaseHistoryRecord(purchaseHistoryRecordList: List<*>) {
      for (purchaseHistoryObject in purchaseHistoryRecordList) {
        try {
          val purchaseHistoryJsonRaw =
              invokeMethod(
                  purchaseHistoryRecordClazz,
                  getOriginalJsonPurchaseHistoryMethod,
                  purchaseHistoryObject) as?
                  String
                  ?: continue
          val purchaseHistoryJson = JSONObject(purchaseHistoryJsonRaw)
          val packageName = context.packageName
          purchaseHistoryJson.put(PACKAGE_NAME, packageName)
          if (purchaseHistoryJson.has(PRODUCT_ID)) {
            val skuID = purchaseHistoryJson.getString(PRODUCT_ID)
            historyPurchaseSet.add(skuID)
            purchaseDetailsMap[skuID] = purchaseHistoryJson
          }
        } catch (e: Exception) {
          /* swallow */
        }
      }
      runnable.run()
    }
  }

  @AutoHandleExceptions
  internal inner class SkuDetailsResponseListenerWrapper(var runnable: Runnable) :
      InvocationHandler {
    override fun invoke(proxy: Any, m: Method, args: Array<Any>?): Any? {
      if (m.name == METHOD_ON_SKU_DETAILS_RESPONSE) {
        val skuDetailsObj = args?.get(1)
        if (skuDetailsObj != null && skuDetailsObj is List<*>) {
          parseSkuDetails(skuDetailsObj)
        }
      }
      return null
    }

    fun parseSkuDetails(skuDetailsObjectList: List<*>) {
      for (skuDetail in skuDetailsObjectList) {
        try {
          val skuDetailJson =
              invokeMethod(skuDetailsClazz, getOriginalJsonSkuMethod, skuDetail) as? String
                  ?: continue
          val skuJson = JSONObject(skuDetailJson)
          if (skuJson.has(PRODUCT_ID)) {
            val skuID = skuJson.getString(PRODUCT_ID)
            skuDetailsMap[skuID] = skuJson
          }
        } catch (e: Exception) {
          /* swallow */
        }
      }
      runnable.run()
    }
  }

  companion object {
    private val initialized = AtomicBoolean(false)
    private var instance: InAppPurchaseBillingClientWrapper? = null
    val isServiceConnected = AtomicBoolean(false)

    // Use ConcurrentHashMap because purchase values may be updated in different threads
    val purchaseDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
    val skuDetailsMap: MutableMap<String, JSONObject> = ConcurrentHashMap()
    private const val IN_APP = "inapp"
    private const val PRODUCT_ID = "productId"
    private const val PACKAGE_NAME = "packageName"

    // Class names
    private const val CLASSNAME_BILLING_CLIENT = "com.android.billingclient.api.BillingClient"
    private const val CLASSNAME_PURCHASE = "com.android.billingclient.api.Purchase"
    private const val CLASSNAME_PURCHASES_RESULT =
        "com.android.billingclient.api.Purchase\$PurchasesResult"
    private const val CLASSNAME_SKU_DETAILS = "com.android.billingclient.api.SkuDetails"
    private const val CLASSNAME_PURCHASE_HISTORY_RECORD =
        "com.android.billingclient.api.PurchaseHistoryRecord"
    private const val CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER =
        "com.android.billingclient.api.SkuDetailsResponseListener"
    private const val CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER =
        "com.android.billingclient.api.PurchaseHistoryResponseListener"
    private const val CLASSNAME_BILLING_CLIENT_BUILDER =
        "com.android.billingclient.api.BillingClient\$Builder"
    private const val CLASSNAME_PURCHASE_UPDATED_LISTENER =
        "com.android.billingclient.api.PurchasesUpdatedListener"
    private const val CLASSNAME_BILLING_CLIENT_STATE_LISTENER =
        "com.android.billingclient.api.BillingClientStateListener"

    // Method names
    private const val METHOD_QUERY_PURCHASES = "queryPurchases"
    private const val METHOD_GET_PURCHASE_LIST = "getPurchasesList"
    private const val METHOD_GET_ORIGINAL_JSON = "getOriginalJson"
    private const val METHOD_QUERY_SKU_DETAILS_ASYNC = "querySkuDetailsAsync"
    private const val METHOD_QUERY_PURCHASE_HISTORY_ASYNC = "queryPurchaseHistoryAsync"
    private const val METHOD_NEW_BUILDER = "newBuilder"
    private const val METHOD_ENABLE_PENDING_PURCHASES = "enablePendingPurchases"
    private const val METHOD_SET_LISTENER = "setListener"
    private const val METHOD_BUILD = "build"
    private const val METHOD_START_CONNECTION = "startConnection"
    private const val METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished"
    private const val METHOD_ON_BILLING_SERVICE_DISCONNECTED = "onBillingServiceDisconnected"
    private const val METHOD_ON_PURCHASE_HISTORY_RESPONSE = "onPurchaseHistoryResponse"
    private const val METHOD_ON_SKU_DETAILS_RESPONSE = "onSkuDetailsResponse"
    @Synchronized
    @JvmStatic
    fun getOrCreateInstance(context: Context): InAppPurchaseBillingClientWrapper? {
      if (initialized.get()) {
        return instance
      }
      createInstance(context)
      initialized.set(true)
      return instance
    }

    private fun createInstance(context: Context) {
      val inAppPurchaseSkuDetailsWrapper = getOrCreateInstance() ?: return
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
          purchaseHistoryResponseListenerClazz == null) {
        return
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
              skuDetailsResponseListenerClazz)
      val queryPurchaseHistoryAsyncMethod =
          getMethod(
              billingClientClazz,
              METHOD_QUERY_PURCHASE_HISTORY_ASYNC,
              String::class.java,
              purchaseHistoryResponseListenerClazz)
      if (queryPurchasesMethod == null ||
          getPurchaseListMethod == null ||
          getOriginalJsonMethod == null ||
          getOriginalJsonSkuMethod == null ||
          getOriginalJsonPurchaseHistoryMethod == null ||
          querySkuDetailsAsyncMethod == null ||
          queryPurchaseHistoryAsyncMethod == null) {
        return
      }
      val billingClient = createBillingClient(context, billingClientClazz) ?: return
      instance =
          InAppPurchaseBillingClientWrapper(
              context,
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
              inAppPurchaseSkuDetailsWrapper)
      (instance as InAppPurchaseBillingClientWrapper).startConnection()
    }

    private fun createBillingClient(context: Context?, billingClientClazz: Class<*>): Any? {
      // 0. pre-check
      val builderClazz = getClass(CLASSNAME_BILLING_CLIENT_BUILDER)
      val listenerClazz = getClass(CLASSNAME_PURCHASE_UPDATED_LISTENER)
      if (builderClazz == null || listenerClazz == null) {
        return null
      }
      val newBuilderMethod = getMethod(billingClientClazz, METHOD_NEW_BUILDER, Context::class.java)
      val enablePendingPurchasesMethod = getMethod(builderClazz, METHOD_ENABLE_PENDING_PURCHASES)
      val setListenerMethod = getMethod(builderClazz, METHOD_SET_LISTENER, listenerClazz)
      val buildMethod = getMethod(builderClazz, METHOD_BUILD)
      if (newBuilderMethod == null ||
          enablePendingPurchasesMethod == null ||
          setListenerMethod == null ||
          buildMethod == null) {
        return null
      }

      // 1. newBuilder(context)
      var builder: Any? =
          invokeMethod(billingClientClazz, newBuilderMethod, null, context) ?: return null

      // 2. setListener(listener)
      val listenerObj =
          Proxy.newProxyInstance(
              listenerClazz.classLoader, arrayOf(listenerClazz), PurchasesUpdatedListenerWrapper())
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
