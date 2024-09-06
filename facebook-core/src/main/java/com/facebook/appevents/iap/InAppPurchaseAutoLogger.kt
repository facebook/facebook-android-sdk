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
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseAutoLogger {
    val failedToCreateWrapper = AtomicBoolean(false)

    @JvmStatic
    fun startIapLogging(
        context: Context,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion
    ) {
        // Check if we have previously tried and failed to create a billing client wrapper
        if (failedToCreateWrapper.get()) {
            return
        }

        if (billingClientVersion == V2_V4) {
            val billingClientWrapper =
                InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(context)
            if (billingClientWrapper == null) {
                failedToCreateWrapper.set(true)
                return
            }
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
                InAppPurchaseBillingClientWrapperV5V7.getOrCreateInstance(context)
            if (billingClientWrapper == null) {
                failedToCreateWrapper.set(true)
                return
            }
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
                InAppPurchaseBillingClientWrapperV2V4.purchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap
            )
            InAppPurchaseBillingClientWrapperV2V4.purchaseDetailsMap.clear()
        } else {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.purchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap
            )
            InAppPurchaseBillingClientWrapperV5V7.purchaseDetailsMap.clear()
        }
    }
}
