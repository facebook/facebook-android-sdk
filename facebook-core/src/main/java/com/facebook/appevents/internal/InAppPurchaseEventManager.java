/**
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

package com.facebook.appevents.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.facebook.internal.Utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class InAppPurchaseEventManager {
    private static final HashMap<String, Method> methodMap =
            new HashMap<>();
    private static final HashMap<String, Class<?>> classMap =
            new HashMap<>();
    private static final String TAG = InAppPurchaseEventManager.class.getCanonicalName();
    private static final String GET_INTERFACE_METHOD = "iap_get_interface";
    private static final String GET_SKU_DETAILS_METHOD = "iap_get_sku_details";
    private static final String IN_APP_BILLING_SERVICE_STUB =
            "com.android.vending.billing.IInAppBillingService$Stub";
    private static final String IN_APP_BILLING_SERVICE =
            "com.android.vending.billing.IInAppBillingService";
    private static final String ITEM_ID_LIST = "ITEM_ID_LIST";
    private static final String RESPONSE_CODE = "RESPONSE_CODE";
    private static final String DETAILS_LIST = "DETAILS_LIST";

    public static Object getServiceInterface(Context context, IBinder service) {
        try {
            Method getInterfaceMethod = methodMap.get(GET_INTERFACE_METHOD);
            if (getInterfaceMethod ==  null) {
                Class<?> iapClass = context.getClassLoader().loadClass(IN_APP_BILLING_SERVICE_STUB);
                Class[] paramTypes = new Class[1];
                paramTypes[0] = IBinder.class;
                getInterfaceMethod = iapClass.getDeclaredMethod("asInterface", paramTypes);
                methodMap.put(GET_INTERFACE_METHOD, getInterfaceMethod);
            }

            Object[] args = new Object[1];
            args[0] = service;
            Utility.logd(TAG, "In-app billing service connected");
            return getInterfaceMethod.invoke(null, args);
        }
        catch (ClassNotFoundException e) {
            Log.e(
                    TAG,
                    IN_APP_BILLING_SERVICE_STUB + " is not available, please add " +
                            IN_APP_BILLING_SERVICE + " to the project.",
                    e);
        }
        catch (NoSuchMethodException e) {
            Log.e(TAG, IN_APP_BILLING_SERVICE_STUB + ".asInterface method not found", e);
        }
        catch (IllegalAccessException e) {
            Log.e(
                    TAG, "Illegal access to method " + IN_APP_BILLING_SERVICE_STUB + ".asInterface",
                    e);
        }
        catch (InvocationTargetException e) {
            Log.e(TAG, "Invocation target exception in " + IN_APP_BILLING_SERVICE_STUB +
                    ".asInterface", e);
        }
        return null;
    }

    public static String getPurchaseDetails(Context context, String sku, Object inAppBillingObj) {
        if (inAppBillingObj == null || sku == "") {
            return "";
        }
        try {
            Method getSkuDetailsMethod = methodMap.get(GET_SKU_DETAILS_METHOD);
            Class<?> iapClass = classMap.get(IN_APP_BILLING_SERVICE);
            if (getSkuDetailsMethod == null || iapClass == null) {
                iapClass = context.getClassLoader().loadClass(IN_APP_BILLING_SERVICE);
                Class[] paramTypes = new Class[4];
                paramTypes[0] = Integer.TYPE;
                paramTypes[1] = String.class;
                paramTypes[2] = String.class;
                paramTypes[3] = Bundle.class;
                getSkuDetailsMethod = iapClass.getDeclaredMethod("getSkuDetails", paramTypes);
                methodMap.put(GET_SKU_DETAILS_METHOD, getSkuDetailsMethod);
                classMap.put(IN_APP_BILLING_SERVICE, iapClass);
            }

            ArrayList<String> skuList = new ArrayList<>();
            skuList.add(sku);
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList(ITEM_ID_LIST, skuList);
            Object localObj = iapClass.cast(inAppBillingObj);
            Object[] args = new Object[4];
            args[0] = Integer.valueOf(3);
            args[1] = context.getPackageName();
            args[2] = "inapp";
            args[3] = querySkus;
            Bundle skuDetails = (Bundle) getSkuDetailsMethod.invoke(localObj, args);

            int response = skuDetails.getInt(RESPONSE_CODE);
            if (response == 0) {
                ArrayList<String> details = skuDetails.getStringArrayList(DETAILS_LIST);
                String detail = details.size() < 1 ? "" : details.get(0);
                return detail;
            }
        }
        catch (ClassNotFoundException e) {
            Log.e(
                    TAG,
                    IN_APP_BILLING_SERVICE + " is not available, please add " +
                            IN_APP_BILLING_SERVICE + " to the project, and import the " +
                            "IInAppBillingService.aidl file into this package",
                    e);
        }
        catch (NoSuchMethodException e) {
            Log.e(TAG, IN_APP_BILLING_SERVICE + ".getSkuDetails method is not available", e);
        }
        catch (InvocationTargetException e) {
            Log.e(TAG,
                    "Invocation target exception in " + IN_APP_BILLING_SERVICE + ".getSkuDetails",
                    e);
        }
        catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access to method " + IN_APP_BILLING_SERVICE + ".getSkuDetails", e);
        }
        return "";
    }
}
