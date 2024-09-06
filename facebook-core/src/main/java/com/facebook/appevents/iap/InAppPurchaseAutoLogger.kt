/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_Plus
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.INAPP
import android.content.Context
import androidx.annotation.RestrictTo
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseAutoLogger {
    private const val BILLING_CLIENT_PURCHASE_NAME = "com.android.billingclient.api.Purchase"

    @JvmStatic
    fun startIapLogging(
        context: Context,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion
    ) {
        // check if the app has IAP with Billing Lib
        if (InAppPurchaseUtils.getClass(BILLING_CLIENT_PURCHASE_NAME) == null) {
            return
        }

        if (billingClientVersion == V2_V4) {
            val billingClientWrapper =
                InAppPurchaseBillingClientWrapper.getOrCreateInstance(context) ?: return
            if (InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()) {
                billingClientWrapper.queryPurchaseHistory(INAPP) {
                    logPurchase(V2_V4)
                }
            } else {
                billingClientWrapper.queryPurchase(INAPP) {
                    logPurchase(V2_V4)
                }
            }
        } else if (billingClientVersion == V5_Plus) {
            val billingClientWrapper =
                InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(context) ?: return
            if (InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()) {
                billingClientWrapper.queryPurchaseHistoryAsync(INAPP) { logPurchase(V5_Plus) }
            } else {
                billingClientWrapper.queryPurchasesAsync(INAPP) { logPurchase(V5_Plus) }
            }
        }
    }

    private fun logPurchase(billingClientVersion: InAppPurchaseUtils.BillingClientVersion) {
        if (billingClientVersion == V2_V4) {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapper.purchaseDetailsMap,
                InAppPurchaseBillingClientWrapper.skuDetailsMap
            )
            InAppPurchaseBillingClientWrapper.purchaseDetailsMap.clear()
        } else {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5Plus.purchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5Plus.productDetailsMap
            )
            InAppPurchaseBillingClientWrapperV5Plus.purchaseDetailsMap.clear()
        }
    }
}
