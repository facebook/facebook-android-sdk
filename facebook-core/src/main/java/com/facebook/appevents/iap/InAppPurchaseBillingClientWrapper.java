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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InAppPurchaseBillingClientWrapper {
  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static @Nullable InAppPurchaseBillingClientWrapper mInstance = null;

  public static final AtomicBoolean isServiceConnected = new AtomicBoolean(false);
  private final Context context;
  private final Object billingClient;
  private final InAppPurchaseSkuDetailsWrapper inAppPurchaseSkuDetailsWrapper;

  private final Class<?> billingClientClazz;
  private final Class<?> purchaseResultClazz;
  private final Class<?> purchaseClazz;
  private final Class<?> skuDetailsClazz;
  private final Class<?> PurchaseHistoryRecordClazz;
  private final Class<?> skuDetailsResponseListenerClazz;
  private final Class<?> purchaseHistoryResponseListenerClazz;
  private final Method queryPurchasesMethod;
  private final Method getPurchaseListMethod;
  private final Method getOriginalJsonMethod;
  private final Method getOriginalJsonSkuMethod;
  private final Method getOriginalJsonPurchaseHistoryMethod;
  private final Method querySkuDetailsAsyncMethod;
  private final Method queryPurchaseHistoryAsyncMethod;

  // Use ConcurrentHashMap because purchase values may be updated in different threads
  public static final Map<String, JSONObject> purchaseDetailsMap = new ConcurrentHashMap<>();
  public static final Map<String, JSONObject> skuDetailsMap = new ConcurrentHashMap<>();
  private final Set<String> historyPurchaseSet = new CopyOnWriteArraySet<>();

  private static final String IN_APP = "inapp";
  private static final String PRODUCT_ID = "productId";
  private static final String PACKAGE_NAME = "packageName";

  // Class names
  private static final String CLASSNAME_BILLING_CLIENT =
      "com.android.billingclient.api.BillingClient";
  private static final String CLASSNAME_PURCHASE = "com.android.billingclient.api.Purchase";
  private static final String CLASSNAME_PURCHASES_RESULT =
      "com.android.billingclient.api.Purchase$PurchasesResult";
  private static final String CLASSNAME_SKU_DETAILS = "com.android.billingclient.api.SkuDetails";
  private static final String CLASSNAME_PURCHASE_HISTORY_RECORD =
      "com.android.billingclient.api.PurchaseHistoryRecord";
  private static final String CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER =
      "com.android.billingclient.api.SkuDetailsResponseListener";
  private static final String CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER =
      "com.android.billingclient.api.PurchaseHistoryResponseListener";
  private static final String CLASSNAME_BILLING_CLIENT_BUILDER =
      "com.android.billingclient.api.BillingClient$Builder";
  private static final String CLASSNAME_PURCHASE_UPDATED_LISTENER =
      "com.android.billingclient.api.PurchasesUpdatedListener";
  private static final String CLASSNAME_BILLING_CLIENT_STATE_LISTENER =
      "com.android.billingclient.api.BillingClientStateListener";

  // Method names
  private static final String METHOD_QUERY_PURCHASES = "queryPurchases";
  private static final String METHOD_GET_PURCHASE_LIST = "getPurchasesList";
  private static final String METHOD_GET_ORIGINAL_JSON = "getOriginalJson";
  private static final String METHOD_QUERY_SKU_DETAILS_ASYNC = "querySkuDetailsAsync";
  private static final String METHOD_QUERY_PURCHASE_HISTORY_ASYNC = "queryPurchaseHistoryAsync";
  private static final String METHOD_NEW_BUILDER = "newBuilder";
  private static final String METHOD_ENABLE_PENDING_PURCHASES = "enablePendingPurchases";
  private static final String METHOD_SET_LISTENER = "setListener";
  private static final String METHOD_BUILD = "build";
  private static final String METHOD_START_CONNECTION = "startConnection";
  private static final String METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished";
  private static final String METHOD_ON_BILLING_SERVICE_DISCONNECTED =
      "onBillingServiceDisconnected";
  private static final String METHOD_ON_PURCHASE_HISTORY_RESPONSE = "onPurchaseHistoryResponse";
  private static final String METHOD_ON_SKU_DETAILS_RESPONSE = "onSkuDetailsResponse";

  private InAppPurchaseBillingClientWrapper(
      Context context,
      Object billingClient,
      Class<?> billingClientClazz,
      Class<?> purchaseResultClazz,
      Class<?> purchaseClazz,
      Class<?> skuDetailsClazz,
      Class<?> PurchaseHistoryRecordClazz,
      Class<?> skuDetailsResponseListenerClazz,
      Class<?> purchaseHistoryResponseListenerClazz,
      Method queryPurchasesMethod,
      Method getPurchaseListsMethod,
      Method getOriginalJsonMethod,
      Method getOriginalJsonSkuMethod,
      Method getOriginalJsonPurchaseHistoryMethod,
      Method querySkuDetailsAsyncMethod,
      Method queryPurchaseHistoryAsyncMethod,
      InAppPurchaseSkuDetailsWrapper inAppPurchaseSkuDetailsWrapper) {
    this.context = context;
    this.billingClient = billingClient;
    this.billingClientClazz = billingClientClazz;
    this.purchaseResultClazz = purchaseResultClazz;
    this.purchaseClazz = purchaseClazz;
    this.skuDetailsClazz = skuDetailsClazz;
    this.PurchaseHistoryRecordClazz = PurchaseHistoryRecordClazz;
    this.skuDetailsResponseListenerClazz = skuDetailsResponseListenerClazz;
    this.purchaseHistoryResponseListenerClazz = purchaseHistoryResponseListenerClazz;
    this.queryPurchasesMethod = queryPurchasesMethod;
    this.getPurchaseListMethod = getPurchaseListsMethod;
    this.getOriginalJsonMethod = getOriginalJsonMethod;
    this.getOriginalJsonSkuMethod = getOriginalJsonSkuMethod;
    this.getOriginalJsonPurchaseHistoryMethod = getOriginalJsonPurchaseHistoryMethod;
    this.querySkuDetailsAsyncMethod = querySkuDetailsAsyncMethod;
    this.queryPurchaseHistoryAsyncMethod = queryPurchaseHistoryAsyncMethod;
    this.inAppPurchaseSkuDetailsWrapper = inAppPurchaseSkuDetailsWrapper;
  }

  @Nullable
  public static synchronized InAppPurchaseBillingClientWrapper getOrCreateInstance(
      Context context) {
    if (initialized.get()) {
      return mInstance;
    }
    createInstance(context);
    initialized.set(true);
    return mInstance;
  }

  private static void createInstance(Context context) {
    InAppPurchaseSkuDetailsWrapper inAppPurchaseSkuDetailsWrapper =
        InAppPurchaseSkuDetailsWrapper.getOrCreateInstance();
    if (inAppPurchaseSkuDetailsWrapper == null) {
      return;
    }

    Class<?> billingClientClazz = InAppPurchaseUtils.getClass(CLASSNAME_BILLING_CLIENT);
    Class<?> purchaseClazz = InAppPurchaseUtils.getClass(CLASSNAME_PURCHASE);
    Class<?> purchaseResultClazz = InAppPurchaseUtils.getClass(CLASSNAME_PURCHASES_RESULT);
    Class<?> skuDetailsClazz = InAppPurchaseUtils.getClass(CLASSNAME_SKU_DETAILS);
    Class<?> PurchaseHistoryRecordClazz =
        InAppPurchaseUtils.getClass(CLASSNAME_PURCHASE_HISTORY_RECORD);

    Class<?> skuDetailsResponseListenerClazz =
        InAppPurchaseUtils.getClass(CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER);
    Class<?> purchaseHistoryResponseListenerClazz =
        InAppPurchaseUtils.getClass(CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER);

    if (billingClientClazz == null
        || purchaseResultClazz == null
        || purchaseClazz == null
        || skuDetailsClazz == null
        || skuDetailsResponseListenerClazz == null
        || PurchaseHistoryRecordClazz == null
        || purchaseHistoryResponseListenerClazz == null) {
      return;
    }

    Method queryPurchasesMethod =
        InAppPurchaseUtils.getMethod(billingClientClazz, METHOD_QUERY_PURCHASES, String.class);
    Method getPurchaseListMethod =
        InAppPurchaseUtils.getMethod(purchaseResultClazz, METHOD_GET_PURCHASE_LIST);
    Method getOriginalJsonMethod =
        InAppPurchaseUtils.getMethod(purchaseClazz, METHOD_GET_ORIGINAL_JSON);
    Method getOriginalJsonSkuMethod =
        InAppPurchaseUtils.getMethod(skuDetailsClazz, METHOD_GET_ORIGINAL_JSON);
    Method getOriginalJsonPurchaseHistoryMethod =
        InAppPurchaseUtils.getMethod(PurchaseHistoryRecordClazz, METHOD_GET_ORIGINAL_JSON);
    Method querySkuDetailsAsyncMethod =
        InAppPurchaseUtils.getMethod(
            billingClientClazz,
            METHOD_QUERY_SKU_DETAILS_ASYNC,
            inAppPurchaseSkuDetailsWrapper.getSkuDetailsParamsClazz(),
            skuDetailsResponseListenerClazz);
    Method queryPurchaseHistoryAsyncMethod =
        InAppPurchaseUtils.getMethod(
            billingClientClazz,
            METHOD_QUERY_PURCHASE_HISTORY_ASYNC,
            String.class,
            purchaseHistoryResponseListenerClazz);
    if (queryPurchasesMethod == null
        || getPurchaseListMethod == null
        || getOriginalJsonMethod == null
        || getOriginalJsonSkuMethod == null
        || getOriginalJsonPurchaseHistoryMethod == null
        || querySkuDetailsAsyncMethod == null
        || queryPurchaseHistoryAsyncMethod == null) {
      return;
    }

    Object billingClient = createBillingClient(context, billingClientClazz);
    if (billingClient == null) {
      return;
    }

    mInstance =
        new InAppPurchaseBillingClientWrapper(
            context,
            billingClient,
            billingClientClazz,
            purchaseResultClazz,
            purchaseClazz,
            skuDetailsClazz,
            PurchaseHistoryRecordClazz,
            skuDetailsResponseListenerClazz,
            purchaseHistoryResponseListenerClazz,
            queryPurchasesMethod,
            getPurchaseListMethod,
            getOriginalJsonMethod,
            getOriginalJsonSkuMethod,
            getOriginalJsonPurchaseHistoryMethod,
            querySkuDetailsAsyncMethod,
            queryPurchaseHistoryAsyncMethod,
            inAppPurchaseSkuDetailsWrapper);
    mInstance.startConnection();
  }

  static Object createBillingClient(Context context, Class<?> billingClientClazz) {
    // 0. pre-check
    Class<?> builderClazz = InAppPurchaseUtils.getClass(CLASSNAME_BILLING_CLIENT_BUILDER);
    Class<?> listenerClazz = InAppPurchaseUtils.getClass(CLASSNAME_PURCHASE_UPDATED_LISTENER);
    if (builderClazz == null || listenerClazz == null) {
      return null;
    }

    Method newBuilderMethod =
        InAppPurchaseUtils.getMethod(billingClientClazz, METHOD_NEW_BUILDER, Context.class);
    Method enablePendingPurchasesMethod =
        InAppPurchaseUtils.getMethod(builderClazz, METHOD_ENABLE_PENDING_PURCHASES);
    Method setListenerMethod =
        InAppPurchaseUtils.getMethod(builderClazz, METHOD_SET_LISTENER, listenerClazz);
    Method buildMethod = InAppPurchaseUtils.getMethod(builderClazz, METHOD_BUILD);
    if (newBuilderMethod == null
        || enablePendingPurchasesMethod == null
        || setListenerMethod == null
        || buildMethod == null) {
      return null;
    }

    // 1. newBuilder(context)
    Object builder =
        InAppPurchaseUtils.invokeMethod(billingClientClazz, newBuilderMethod, null, context);
    if (builder == null) {
      return null;
    }

    // 2. setListener(listener)
    Object listenerObj =
        Proxy.newProxyInstance(
            listenerClazz.getClassLoader(),
            new Class[] {listenerClazz},
            new PurchasesUpdatedListenerWrapper());
    builder =
        InAppPurchaseUtils.invokeMethod(builderClazz, setListenerMethod, builder, listenerObj);
    if (builder == null) {
      return null;
    }

    // 3. enablePendingPurchases()
    builder = InAppPurchaseUtils.invokeMethod(builderClazz, enablePendingPurchasesMethod, builder);
    if (builder == null) {
      return null;
    }

    // 4. build()
    return InAppPurchaseUtils.invokeMethod(builderClazz, buildMethod, builder);
  }

  public void queryPurchaseHistory(String skuType, final Runnable queryPurchaseHistoryRunnable) {
    queryPurchaseHistoryAsync(
        skuType,
        new Runnable() {
          @Override
          public void run() {
            querySkuDetailsAsync(
                IN_APP, new ArrayList<>(historyPurchaseSet), queryPurchaseHistoryRunnable);
          }
        });
  }

  public void queryPurchase(String skuType, final Runnable querySkuRunnable) {
    // TODO (T67568885): support subs
    Object purchaseResult =
        InAppPurchaseUtils.invokeMethod(
            billingClientClazz, queryPurchasesMethod, billingClient, IN_APP);
    Object purchaseObjects =
        InAppPurchaseUtils.invokeMethod(purchaseResultClazz, getPurchaseListMethod, purchaseResult);
    if (!(purchaseObjects instanceof List)) {
      return;
    }

    try {
      List<String> skuIDs = new ArrayList<>();
      for (Object purchaseObject : (List<?>) purchaseObjects) {
        Object purchaseJsonStr =
            InAppPurchaseUtils.invokeMethod(purchaseClazz, getOriginalJsonMethod, purchaseObject);
        if (!(purchaseJsonStr instanceof String)) {
          continue;
        }
        JSONObject purchaseJson = new JSONObject((String) purchaseJsonStr);
        if (purchaseJson.has(PRODUCT_ID)) {
          String skuID = purchaseJson.getString(PRODUCT_ID);
          skuIDs.add(skuID);
          purchaseDetailsMap.put(skuID, purchaseJson);
        }
      }

      querySkuDetailsAsync(skuType, skuIDs, querySkuRunnable);
    } catch (JSONException je) {
      /* swallow */
    }
  }

  private void querySkuDetailsAsync(String skuType, List<String> skuIDs, Runnable runnable) {
    Object listenerObj =
        Proxy.newProxyInstance(
            skuDetailsResponseListenerClazz.getClassLoader(),
            new Class[] {skuDetailsResponseListenerClazz},
            new SkuDetailsResponseListenerWrapper(runnable));
    Object skuDetailsParams = inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs);
    InAppPurchaseUtils.invokeMethod(
        billingClientClazz,
        querySkuDetailsAsyncMethod,
        billingClient,
        skuDetailsParams,
        listenerObj);
  }

  private void queryPurchaseHistoryAsync(String skuType, Runnable runnable) {
    Object listenerObj =
        Proxy.newProxyInstance(
            purchaseHistoryResponseListenerClazz.getClassLoader(),
            new Class[] {purchaseHistoryResponseListenerClazz},
            new PurchaseHistoryResponseListenerWrapper(runnable));
    InAppPurchaseUtils.invokeMethod(
        billingClientClazz, queryPurchaseHistoryAsyncMethod, billingClient, skuType, listenerObj);
  }

  private void startConnection() {
    Class<?> listenerClazz = InAppPurchaseUtils.getClass(CLASSNAME_BILLING_CLIENT_STATE_LISTENER);
    if (listenerClazz == null) {
      return;
    }
    Method method =
        InAppPurchaseUtils.getMethod(billingClientClazz, METHOD_START_CONNECTION, listenerClazz);
    if (method == null) {
      return;
    }

    Object listenerObj =
        Proxy.newProxyInstance(
            listenerClazz.getClassLoader(),
            new Class[] {listenerClazz},
            new BillingClientStateListenerWrapper());
    InAppPurchaseUtils.invokeMethod(billingClientClazz, method, billingClient, listenerObj);
  }

  static class BillingClientStateListenerWrapper implements java.lang.reflect.InvocationHandler {
    public BillingClientStateListenerWrapper() {}

    @Override
    @Nullable
    public Object invoke(Object proxy, Method m, Object[] args) {
      if (m.getName().equals(METHOD_ON_BILLING_SETUP_FINISHED)) {
        isServiceConnected.set(true);
      } else if (m.getName().endsWith(METHOD_ON_BILLING_SERVICE_DISCONNECTED)) {
        isServiceConnected.set(false);
      }
      return null;
    }
  }

  static class PurchasesUpdatedListenerWrapper implements java.lang.reflect.InvocationHandler {
    public PurchasesUpdatedListenerWrapper() {}

    // dummy function, no need to implement onPurchasesUpdated
    @Override
    @Nullable
    public Object invoke(Object proxy, Method m, Object[] args) {
      return null;
    }
  }

  class PurchaseHistoryResponseListenerWrapper implements java.lang.reflect.InvocationHandler {
    Runnable runnable;

    public PurchaseHistoryResponseListenerWrapper(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) {
      if (method.getName().equals(METHOD_ON_PURCHASE_HISTORY_RESPONSE)) {
        Object purchaseHistoryRecordListObject = args[1];
        if (purchaseHistoryRecordListObject instanceof List<?>) {
          getPurchaseHistoryRecord((List<?>) purchaseHistoryRecordListObject);
        }
      }
      return null;
    }

    private void getPurchaseHistoryRecord(List<?> purchaseHistoryRecordList) {
      for (Object purchaseHistoryObject : purchaseHistoryRecordList) {
        try {
          Object purchaseHistoryJsonRaw =
              InAppPurchaseUtils.invokeMethod(
                  PurchaseHistoryRecordClazz,
                  getOriginalJsonPurchaseHistoryMethod,
                  purchaseHistoryObject);
          if (!(purchaseHistoryJsonRaw instanceof String)) {
            continue;
          }
          JSONObject purchaseHistoryJson = new JSONObject((String) purchaseHistoryJsonRaw);

          String packageName = context.getPackageName();
          purchaseHistoryJson.put(PACKAGE_NAME, packageName);

          if (purchaseHistoryJson.has(PRODUCT_ID)) {
            String skuID = purchaseHistoryJson.getString(PRODUCT_ID);
            historyPurchaseSet.add(skuID);
            purchaseDetailsMap.put(skuID, purchaseHistoryJson);
          }
        } catch (Exception e) {
          /* swallow */
        }
      }
      runnable.run();
    }
  }

  class SkuDetailsResponseListenerWrapper implements java.lang.reflect.InvocationHandler {
    Runnable runnable;

    public SkuDetailsResponseListenerWrapper(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method m, Object[] args) {
      if (m.getName().equals(METHOD_ON_SKU_DETAILS_RESPONSE)) {
        Object skuDetailsObj = args[1];
        if (skuDetailsObj instanceof List<?>) {
          parseSkuDetails((List<?>) skuDetailsObj);
        }
      }
      return null;
    }

    void parseSkuDetails(List<?> skuDetailsObjectList) {
      for (Object skuDetail : skuDetailsObjectList) {
        try {
          Object skuDetailJson =
              InAppPurchaseUtils.invokeMethod(skuDetailsClazz, getOriginalJsonSkuMethod, skuDetail);
          if (!(skuDetailJson instanceof String)) {
            continue;
          }
          JSONObject skuJson = new JSONObject((String) skuDetailJson);
          if (skuJson.has(PRODUCT_ID)) {
            String skuID = skuJson.getString(PRODUCT_ID);
            skuDetailsMap.put(skuID, skuJson);
          }
        } catch (Exception e) {
          /* swallow */
        }
      }
      runnable.run();
    }
  }
}
