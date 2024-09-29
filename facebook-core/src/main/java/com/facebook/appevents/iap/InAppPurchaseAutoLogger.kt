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
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.SUBS
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
        var billingClientWrapper: InAppPurchaseBillingClientWrapper? = null
        if (billingClientVersion == V2_V4) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(context)
        } else if (billingClientVersion == V5_Plus) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV5V7.getOrCreateInstance(context)
        }
        if (billingClientWrapper == null) {
            failedToCreateWrapper.set(true)
            return
        }
        billingClientWrapper.queryPurchaseHistory(INAPP) {
            billingClientWrapper.queryPurchaseHistory(SUBS) {
                logPurchase(billingClientVersion, context.packageName)
            }
        }
    }

    private fun logPurchase(
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion,
        packageName: String
    ) {
        if (billingClientVersion == V2_V4) {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                false,
                packageName
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                true,
                packageName
            )
            InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap.clear()
        } else {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                false,
                packageName
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                true,
                packageName
            )
            InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap.clear()
        }
    }
}
