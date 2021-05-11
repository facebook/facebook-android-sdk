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
import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InAppPurchaseAutoLogger {
  private static final String BILLING_CLIENT_PURCHASE_NAME =
      "com.android.billingclient.api.Purchase";

  public static void startIapLogging(Context context) {
    // check if the app has IAP with Billing Lib
    if (InAppPurchaseUtils.getClass(BILLING_CLIENT_PURCHASE_NAME) == null) {
      return;
    }
    final InAppPurchaseBillingClientWrapper billingClientWrapper =
        InAppPurchaseBillingClientWrapper.getOrCreateInstance(context);
    if (billingClientWrapper == null) {
      return;
    }

    if (InAppPurchaseBillingClientWrapper.isServiceConnected.get()) {
      if (InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()) {
        billingClientWrapper.queryPurchaseHistory(
            "inapp",
            new Runnable() {
              @Override
              public void run() {
                logPurchase();
              }
            });
      } else {
        billingClientWrapper.queryPurchase(
            "inapp",
            new Runnable() {
              @Override
              public void run() {
                logPurchase();
              }
            });
      }
    }
  }

  private static void logPurchase() {
    InAppPurchaseLoggerManager.filterPurchaseLogging(
        InAppPurchaseBillingClientWrapper.purchaseDetailsMap,
        InAppPurchaseBillingClientWrapper.skuDetailsMap);
    InAppPurchaseBillingClientWrapper.purchaseDetailsMap.clear();
  }
}
