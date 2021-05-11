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

package com.facebook.appevents.iap;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.json.JSONObject;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InAppPurchaseLoggerManager {

  private static SharedPreferences sharedPreferences;
  private static final Set<String> cachedPurchaseSet = new CopyOnWriteArraySet<>();
  private static final Map<String, Long> cachedPurchaseMap = new ConcurrentHashMap<>();

  private static final String PURCHASE_TIME = "purchaseTime";
  private static final String PRODUCT_DETAILS_STORE = "com.facebook.internal.iap.PRODUCT_DETAILS";
  private static final String LAST_CLEARED_TIME = "LAST_CLEARED_TIME";
  private static final String PURCHASE_DETAILS_SET = "PURCHASE_DETAILS_SET";
  private static final String LAST_QUERY_PURCHASE_HISTORY_TIME = "LAST_QUERY_PURCHASE_HISTORY_TIME";

  private static final int CACHE_CLEAR_TIME_LIMIT_SEC = 7 * 24 * 60 * 60; // 7 days
  private static final int PURCHASE_IN_CACHE_INTERVAL = 24 * 60 * 60; // 1 day

  private static void readPurchaseCache() {
    // clear cached purchases logged by lib 1
    SharedPreferences cachedSkuSharedPref =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences("com.facebook.internal.SKU_DETAILS", Context.MODE_PRIVATE);
    SharedPreferences cachedPurchaseSharedPref =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences("com.facebook.internal.PURCHASE", Context.MODE_PRIVATE);
    if (cachedSkuSharedPref.contains("LAST_CLEARED_TIME")) {
      cachedSkuSharedPref.edit().clear().apply();
      cachedPurchaseSharedPref.edit().clear().apply();
    }

    sharedPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(PRODUCT_DETAILS_STORE, Context.MODE_PRIVATE);
    cachedPurchaseSet.addAll(
        sharedPreferences.getStringSet(PURCHASE_DETAILS_SET, new HashSet<String>()));

    // Construct purchase de-dup map.
    for (String purchaseHistory : cachedPurchaseSet) {
      String[] splitPurchase = purchaseHistory.split(";", 2);
      cachedPurchaseMap.put(splitPurchase[0], Long.parseLong(splitPurchase[1]));
    }

    // Clean up cache every 7 days, and only keep recent 1 day purchases
    clearOutdatedProductInfoInCache();
  }

  public static void filterPurchaseLogging(
      Map<String, JSONObject> purchaseDetailsMap, Map<String, JSONObject> skuDetailsMap) {
    readPurchaseCache();

    Map<String, String> loggingReadyMap =
        new HashMap<>(
            constructLoggingReadyMap(cacheDeDupPurchase(purchaseDetailsMap), skuDetailsMap));
    logPurchases(loggingReadyMap);
  }

  static void logPurchases(Map<String, String> purchaseDetailsMap) {
    for (Map.Entry<String, String> pair : purchaseDetailsMap.entrySet()) {
      String purchaseDetails = pair.getKey();
      String skuDetails = pair.getValue();
      if (purchaseDetails != null && skuDetails != null) {
        AutomaticAnalyticsLogger.logPurchase(purchaseDetails, skuDetails, false);
      }
    }
  }

  static Map<String, JSONObject> cacheDeDupPurchase(Map<String, JSONObject> purchaseDetailsMap) {
    long nowSec = System.currentTimeMillis() / 1000L;
    Map<String, JSONObject> tempPurchaseDetailsMap = new HashMap<>(purchaseDetailsMap);
    for (Map.Entry<String, JSONObject> entry : tempPurchaseDetailsMap.entrySet()) {
      try {
        JSONObject purchaseJson = entry.getValue();
        if (purchaseJson.has("purchaseToken")) {
          String purchaseToken = purchaseJson.getString("purchaseToken");
          if (cachedPurchaseMap.containsKey(purchaseToken)) {
            purchaseDetailsMap.remove(entry.getKey());
          } else {
            cachedPurchaseSet.add(purchaseToken + ';' + nowSec);
          }
        }
      } catch (Exception e) {
        /* swallow */
      }
    }

    sharedPreferences.edit().putStringSet(PURCHASE_DETAILS_SET, cachedPurchaseSet).apply();
    return new HashMap<>(purchaseDetailsMap);
  }

  private static void clearOutdatedProductInfoInCache() {
    long nowSec = System.currentTimeMillis() / 1000L;
    long lastClearedTimeSec = sharedPreferences.getLong(LAST_CLEARED_TIME, 0);

    if (lastClearedTimeSec == 0) {
      sharedPreferences.edit().putLong(LAST_CLEARED_TIME, nowSec).apply();
    } else if ((nowSec - lastClearedTimeSec) > CACHE_CLEAR_TIME_LIMIT_SEC) {
      Map<String, Long> tempPurchaseMap = new HashMap<>(cachedPurchaseMap);
      for (Map.Entry<String, Long> historyPurchase : tempPurchaseMap.entrySet()) {
        String purchaseToken = historyPurchase.getKey();
        Long historyPurchaseTime = historyPurchase.getValue();
        if (nowSec - historyPurchaseTime > PURCHASE_IN_CACHE_INTERVAL) {
          cachedPurchaseSet.remove(purchaseToken + ";" + historyPurchaseTime);
          cachedPurchaseMap.remove(purchaseToken);
        }
      }
      sharedPreferences
          .edit()
          .putStringSet(PURCHASE_DETAILS_SET, cachedPurchaseSet)
          .putLong(LAST_CLEARED_TIME, nowSec)
          .apply();
    }
  }

  public static boolean eligibleQueryPurchaseHistory() {
    readPurchaseCache();
    long nowSec = System.currentTimeMillis() / 1000L;
    long lastQueryPurchaseHistoryTime =
        sharedPreferences.getLong(LAST_QUERY_PURCHASE_HISTORY_TIME, 0);
    if (lastQueryPurchaseHistoryTime != 0
        && nowSec - lastQueryPurchaseHistoryTime < PURCHASE_IN_CACHE_INTERVAL) {
      return false;
    }
    sharedPreferences.edit().putLong(LAST_QUERY_PURCHASE_HISTORY_TIME, nowSec).apply();
    return true;
  }

  static Map<String, String> constructLoggingReadyMap(
      Map<String, JSONObject> purchaseDetailsMap, Map<String, JSONObject> skuDetailsMap) {
    long nowSec = System.currentTimeMillis() / 1000L;
    Map<String, String> purchaseResultMap = new HashMap<>();
    for (Map.Entry<String, JSONObject> pair : purchaseDetailsMap.entrySet()) {
      JSONObject skuDetail = skuDetailsMap.get(pair.getKey());
      JSONObject purchaseDetail = pair.getValue();
      if (purchaseDetail != null && purchaseDetail.has(PURCHASE_TIME)) {
        try {
          long purchaseTime = purchaseDetail.getLong(PURCHASE_TIME);
          // Purchase is too old (more than 24h) to log
          if (nowSec - purchaseTime / 1000L > PURCHASE_IN_CACHE_INTERVAL) {
            continue;
          }
          if (skuDetail != null) {
            purchaseResultMap.put(purchaseDetail.toString(), skuDetail.toString());
          }
        } catch (Exception e) {
          /* swallow */
        }
      }
    }
    return purchaseResultMap;
  }
}
