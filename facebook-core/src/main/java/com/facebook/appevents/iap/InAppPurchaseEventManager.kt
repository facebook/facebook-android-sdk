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
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseEventManager {
  private val methodMap: HashMap<String, Method> = hashMapOf()
  private val classMap: HashMap<String, Class<*>> = hashMapOf()
  private const val CACHE_CLEAR_TIME_LIMIT_SEC = 7 * 24 * 60 * 60 // 7 days

  // Sku detail cache setting
  private const val SKU_DETAIL_EXPIRE_TIME_SEC = 12 * 60 * 60 // 12 h

  // Purchase types
  private const val SUBSCRIPTION = "subs"
  private const val INAPP = "inapp"

  // Purchase setting
  private const val PURCHASE_EXPIRE_TIME_SEC = 24 * 60 * 60 // 24 h
  private const val PURCHASE_STOP_QUERY_TIME_SEC = 20 * 60 // 20 min
  private const val MAX_QUERY_PURCHASE_NUM = 30

  // Class names
  private const val IN_APP_BILLING_SERVICE_STUB =
      "com.android.vending.billing.IInAppBillingService\$Stub"
  private const val IN_APP_BILLING_SERVICE = "com.android.vending.billing.IInAppBillingService"

  // Method names
  private const val AS_INTERFACE = "asInterface"
  private const val GET_SKU_DETAILS = "getSkuDetails"
  private const val GET_PURCHASES = "getPurchases"
  private const val GET_PURCHASE_HISTORY = "getPurchaseHistory"
  private const val IS_BILLING_SUPPORTED = "isBillingSupported"

  // Other names
  private const val ITEM_ID_LIST = "ITEM_ID_LIST"
  private const val RESPONSE_CODE = "RESPONSE_CODE"
  private const val DETAILS_LIST = "DETAILS_LIST"
  private const val INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
  private const val INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"
  private const val LAST_CLEARED_TIME = "LAST_CLEARED_TIME"
  private val PACKAGE_NAME = getApplicationContext().packageName
  private const val SKU_DETAILS_STORE = "com.facebook.internal.SKU_DETAILS"
  private const val PURCHASE_INAPP_STORE = "com.facebook.internal.PURCHASE"
  private val skuDetailSharedPrefs =
      getApplicationContext().getSharedPreferences(SKU_DETAILS_STORE, Context.MODE_PRIVATE)
  private val purchaseInappSharedPrefs =
      getApplicationContext().getSharedPreferences(PURCHASE_INAPP_STORE, Context.MODE_PRIVATE)

  @JvmStatic
  fun asInterface(context: Context, service: IBinder?): Any? {
    val args = arrayOf<Any?>(service)
    return invokeMethod(context, IN_APP_BILLING_SERVICE_STUB, AS_INTERFACE, null, args)
  }

  @JvmStatic
  fun getSkuDetails(
      context: Context,
      skuList: ArrayList<String>,
      inAppBillingObj: Any?,
      isSubscription: Boolean
  ): Map<String, String> {
    val skuDetailsMap = readSkuDetailsFromCache(skuList)
    val unresolvedSkuList = arrayListOf<String>()
    for (sku in skuList) {
      if (!skuDetailsMap.containsKey(sku)) {
        unresolvedSkuList.add(sku)
      }
    }
    skuDetailsMap.putAll(
        getSkuDetailsFromGoogle(context, unresolvedSkuList, inAppBillingObj, isSubscription))
    return skuDetailsMap
  }

  private fun getSkuDetailsFromGoogle(
      context: Context,
      skuList: ArrayList<String>,
      inAppBillingObj: Any?,
      isSubscription: Boolean
  ): Map<String, String> {
    val skuDetailsMap: MutableMap<String, String> = mutableMapOf()
    if (inAppBillingObj == null || skuList.isEmpty()) {
      return skuDetailsMap
    }
    val querySkus = Bundle()
    querySkus.putStringArrayList(ITEM_ID_LIST, skuList)
    val args = arrayOf(3, PACKAGE_NAME, if (isSubscription) SUBSCRIPTION else INAPP, querySkus)
    val result =
        invokeMethod(context, IN_APP_BILLING_SERVICE, GET_SKU_DETAILS, inAppBillingObj, args)
    if (result != null) {
      val bundle = result as Bundle
      val response = bundle.getInt(RESPONSE_CODE)
      if (response == 0) {
        val skuDetailsList = bundle.getStringArrayList(DETAILS_LIST)
        if (skuDetailsList != null && skuList.size == skuDetailsList.size) {
          for (i in skuList.indices) {
            skuDetailsMap[skuList[i]] = skuDetailsList[i]
          }
        }
        writeSkuDetailsToCache(skuDetailsMap)
      }
    }
    return skuDetailsMap
  }

  private fun readSkuDetailsFromCache(skuList: ArrayList<String>): MutableMap<String, String> {
    val skuDetailsMap: MutableMap<String, String> = mutableMapOf()
    val nowSec = System.currentTimeMillis() / 1000L
    for (sku in skuList) {
      val rawString = skuDetailSharedPrefs.getString(sku, null)
      if (rawString != null) {
        val splitted = rawString.split(";", limit = 2)
        val timeSec = splitted[0].toLong()
        if (nowSec - timeSec < SKU_DETAIL_EXPIRE_TIME_SEC) {
          skuDetailsMap[sku] = splitted[1]
        }
      }
    }
    return skuDetailsMap
  }

  private fun writeSkuDetailsToCache(skuDetailsMap: Map<String, String>) {
    val nowSec = System.currentTimeMillis() / 1000L
    val editor = skuDetailSharedPrefs.edit()
    for ((key, value) in skuDetailsMap) {
      editor.putString(key, "$nowSec;$value")
    }
    editor.apply()
  }

  private fun isBillingSupported(context: Context, inAppBillingObj: Any?, type: String): Boolean {
    if (inAppBillingObj == null) {
      return false
    }
    val args = arrayOf<Any?>(3, PACKAGE_NAME, type)
    val result =
        invokeMethod(context, IN_APP_BILLING_SERVICE, IS_BILLING_SUPPORTED, inAppBillingObj, args)
    return result != null && result as Int == 0
  }

  @JvmStatic
  fun getPurchasesInapp(context: Context, inAppBillingObj: Any?): ArrayList<String> {
    return filterPurchases(getPurchases(context, inAppBillingObj, INAPP))
  }

  @JvmStatic
  fun getPurchasesSubs(context: Context, inAppBillingObj: Any?): ArrayList<String> {
    return filterPurchases(getPurchases(context, inAppBillingObj, SUBSCRIPTION))
  }

  private fun getPurchases(
      context: Context,
      inAppBillingObj: Any?,
      type: String
  ): ArrayList<String> {
    val purchases = arrayListOf<String>()
    if (inAppBillingObj == null) {
      return purchases
    }
    if (isBillingSupported(context, inAppBillingObj, type)) {
      var continuationToken: String? = null
      var queriedPurchaseNum = 0
      do {
        val args = arrayOf<Any?>(3, PACKAGE_NAME, type, continuationToken)
        val result =
            invokeMethod(context, IN_APP_BILLING_SERVICE, GET_PURCHASES, inAppBillingObj, args)
        continuationToken = null
        if (result != null) {
          val purchaseDetails = result as Bundle
          val response = purchaseDetails.getInt(RESPONSE_CODE)
          if (response == 0) {
            val details = purchaseDetails.getStringArrayList(INAPP_PURCHASE_DATA_LIST)
            if (details != null) {
              queriedPurchaseNum += details.size
              purchases.addAll(details)
              continuationToken = purchaseDetails.getString(INAPP_CONTINUATION_TOKEN)
            } else {
              break
            }
          }
        }
      } while (queriedPurchaseNum < MAX_QUERY_PURCHASE_NUM && continuationToken != null)
    }
    return purchases
  }

  fun hasFreeTrialPeirod(skuDetail: String): Boolean {
    try {
      val skuDetailsJson = JSONObject(skuDetail)
      val freeTrialPeriod = skuDetailsJson.optString("freeTrialPeriod")
      return freeTrialPeriod != null && freeTrialPeriod.isNotEmpty()
    } catch (e: JSONException) {
      /*no op*/
    }
    return false
  }

  @JvmStatic
  fun getPurchaseHistoryInapp(context: Context, inAppBillingObj: Any?): ArrayList<String> {
    var purchases = ArrayList<String>()
    if (inAppBillingObj == null) {
      return purchases
    }
    val iapClass = getClass(context, IN_APP_BILLING_SERVICE) ?: return purchases
    getMethod(iapClass, GET_PURCHASE_HISTORY) ?: return purchases
    purchases = getPurchaseHistory(context, inAppBillingObj, INAPP)
    return filterPurchases(purchases)
  }

  private fun getPurchaseHistory(
      context: Context,
      inAppBillingObj: Any,
      type: String
  ): ArrayList<String> {
    val purchases = arrayListOf<String>()
    if (isBillingSupported(context, inAppBillingObj, type)) {
      var continuationToken: String? = null
      var queriedPurchaseNum = 0
      var reachTimeLimit = false
      do {
        val args = arrayOf(6, PACKAGE_NAME, type, continuationToken, Bundle())
        continuationToken = null
        val result =
            invokeMethod(
                context, IN_APP_BILLING_SERVICE, GET_PURCHASE_HISTORY, inAppBillingObj, args)
        if (result != null) {
          val nowSec = System.currentTimeMillis() / 1000L
          val purchaseDetails = result as Bundle
          val response = purchaseDetails.getInt(RESPONSE_CODE)
          if (response == 0) {
            val details = purchaseDetails.getStringArrayList(INAPP_PURCHASE_DATA_LIST) ?: continue
            for (detail in details) {
              try {
                val detailJson = JSONObject(detail)
                val purchaseTimeSec = detailJson.getLong("purchaseTime") / 1000L
                if (nowSec - purchaseTimeSec > PURCHASE_STOP_QUERY_TIME_SEC) {
                  reachTimeLimit = true
                  break
                } else {
                  purchases.add(detail)
                  queriedPurchaseNum++
                }
              } catch (e: JSONException) {
                /*no op*/
              }
            }
            continuationToken = purchaseDetails.getString(INAPP_CONTINUATION_TOKEN)
          }
        }
      } while (queriedPurchaseNum < MAX_QUERY_PURCHASE_NUM &&
          continuationToken != null &&
          !reachTimeLimit)
    }
    return purchases
  }

  private fun filterPurchases(purchases: ArrayList<String>): ArrayList<String> {
    val filteredPurchase = ArrayList<String>()
    val editor = purchaseInappSharedPrefs.edit()
    val nowSec = System.currentTimeMillis() / 1000L
    for (purchase in purchases) {
      try {
        val purchaseJson = JSONObject(purchase)
        val sku = purchaseJson.getString("productId")
        val purchaseTimeMillis = purchaseJson.getLong("purchaseTime")
        val purchaseToken = purchaseJson.getString("purchaseToken")
        if (nowSec - purchaseTimeMillis / 1000L > PURCHASE_EXPIRE_TIME_SEC) {
          continue
        }
        val historyPurchaseToken = purchaseInappSharedPrefs.getString(sku, "")
        if (historyPurchaseToken == purchaseToken) {
          continue
        }
        editor.putString(sku, purchaseToken) // write new purchase into cache
        filteredPurchase.add(purchase)
      } catch (e: JSONException) {
        /*no op*/
      }
    }
    editor.apply()
    return filteredPurchase
  }

  private fun getMethod(classObj: Class<*>, methodName: String): Method? {
    var method = methodMap[methodName]
    if (method != null) {
      return method
    }
    try {
      var paramTypes: Array<Class<*>>? = null
      when (methodName) {
        AS_INTERFACE -> paramTypes = arrayOf(IBinder::class.java)
        GET_SKU_DETAILS ->
            paramTypes =
                arrayOf(Integer.TYPE, String::class.java, String::class.java, Bundle::class.java)
        IS_BILLING_SUPPORTED ->
            paramTypes = arrayOf(Integer.TYPE, String::class.java, String::class.java)
        GET_PURCHASES ->
            paramTypes =
                arrayOf(Integer.TYPE, String::class.java, String::class.java, String::class.java)
        GET_PURCHASE_HISTORY ->
            paramTypes =
                arrayOf(
                    Integer.TYPE,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Bundle::class.java)
      }
      val parameterTypes = paramTypes
      method =
          if (parameterTypes == null) classObj.getDeclaredMethod(methodName, null)
          else classObj.getDeclaredMethod(methodName, *parameterTypes)
      methodMap[methodName] = method
    } catch (e: NoSuchMethodException) {
      /*no op*/
    }
    return method
  }

  private fun getClass(context: Context, className: String): Class<*>? {
    var classObj = classMap[className]
    if (classObj != null) {
      return classObj
    }
    try {
      classObj = context.classLoader.loadClass(className)
      classMap[className] = classObj
    } catch (e: ClassNotFoundException) {
      /*no op*/
    }
    return classObj
  }

  private fun invokeMethod(
      context: Context,
      className: String,
      methodName: String,
      obj: Any?,
      args: Array<Any?>
  ): Any? {
    var obj = obj
    val classObj = getClass(context, className) ?: return null
    val methodObj = getMethod(classObj, methodName) ?: return null
    if (obj != null) {
      obj = classObj.cast(obj)
    }
    try {
      return methodObj.invoke(obj, *args)
    } catch (e: IllegalAccessException) {
      /* swallow */
    } catch (e: InvocationTargetException) {
      /* swallow */
    }
    return null
  }

  @JvmStatic
  fun clearSkuDetailsCache() {
    val nowSec = System.currentTimeMillis() / 1000L

    // Sku details cache
    val lastClearedTimeSec = skuDetailSharedPrefs.getLong(LAST_CLEARED_TIME, 0)
    if (lastClearedTimeSec == 0L) {
      skuDetailSharedPrefs.edit().putLong(LAST_CLEARED_TIME, nowSec).apply()
    } else if (nowSec - lastClearedTimeSec > CACHE_CLEAR_TIME_LIMIT_SEC) {
      skuDetailSharedPrefs.edit().clear().putLong(LAST_CLEARED_TIME, nowSec).apply()
    }
  }
}
