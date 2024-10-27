/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_V7
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.INAPP
import android.content.Context
import androidx.annotation.RestrictTo
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.getIsFirstAppLaunch
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.setAppHasBeenLaunched
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.SUBS
import com.facebook.appevents.integrity.ProtectedModeManager
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
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
        } else if (billingClientVersion == V5_V7) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV5V7.getOrCreateInstance(context)
        }
        if (billingClientWrapper == null) {
            failedToCreateWrapper.set(true)
            return
        }

        if (isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)
            && (!ProtectedModeManager.isEnabled() || billingClientVersion == V2_V4)
        ) {
            billingClientWrapper.queryPurchaseHistory(INAPP) {
                billingClientWrapper.queryPurchaseHistory(SUBS) {
                    logPurchase(billingClientVersion, context.packageName)
                }
            }
        } else {
            billingClientWrapper.queryPurchaseHistory(INAPP) {
                logPurchase(billingClientVersion, context.packageName)
            }
        }
    }

    private fun logPurchase(
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion,
        packageName: String
    ) {
        val isFirstAppLaunch = getIsFirstAppLaunch()
        if (billingClientVersion == V2_V4) {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                false,
                packageName,
                billingClientVersion,
                isFirstAppLaunch
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                true,
                packageName,
                billingClientVersion,
                isFirstAppLaunch
            )
            InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap.clear()
        } else {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                false,
                packageName,
                billingClientVersion,
                isFirstAppLaunch
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                true,
                packageName,
                billingClientVersion,
                isFirstAppLaunch
            )
            InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap.clear()
        }
        if (isFirstAppLaunch) {
            setAppHasBeenLaunched()
        }
    }
}
