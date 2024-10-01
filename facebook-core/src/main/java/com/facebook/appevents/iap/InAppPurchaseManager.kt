/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.NONE
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V1
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_V7
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseManager {
    private var specificBillingLibraryVersion: String? = null;
    private const val GOOGLE_BILLINGCLIENT_VERSION = "com.google.android.play.billingclient.version"
    private val enabled = AtomicBoolean(false)

    @JvmStatic
    fun enableAutoLogging() {
        enabled.set(true)
        startTracking()
    }

    @JvmStatic
    fun startTracking() {
        if (!enabled.get()) {
            return
        }
        // Delegate IAP logic to separate handler based on Google Play Billing Library version
        when (val billingClientVersion = getBillingClientVersion()) {
            NONE -> return
            V1 -> InAppPurchaseActivityLifecycleTracker.startIapLogging(V1)
            V2_V4 -> {
                if (isEnabled(FeatureManager.Feature.IapLoggingLib2)) {
                    InAppPurchaseAutoLogger.startIapLogging(
                        getApplicationContext(),
                        billingClientVersion
                    )
                } else {
                    InAppPurchaseActivityLifecycleTracker.startIapLogging(V2_V4)
                }
            }

            V5_V7 -> InAppPurchaseAutoLogger.startIapLogging(
                getApplicationContext(),
                billingClientVersion
            )
        }
    }

    @JvmStatic
    private fun setSpecificBillingLibraryVersion(version: String) {
        specificBillingLibraryVersion = version
    }

    @JvmStatic
    fun getSpecificBillingLibraryVersion(): String? {
        return specificBillingLibraryVersion
    }

    private fun getBillingClientVersion(): InAppPurchaseUtils.BillingClientVersion {
        try {
            val context = getApplicationContext()
            val info =
                context.packageManager.getApplicationInfo(
                    context.packageName, PackageManager.GET_META_DATA
                )
            // If we can't find the package, the billing client wrapper will not be able
            // to fetch any of the necessary methods/classes.
            val version = info.metaData.getString(GOOGLE_BILLINGCLIENT_VERSION)
                ?: return NONE
            val versionArray = version.split(
                ".",
                limit = 3
            )
            if (version.isEmpty()) {
                // Default to newest version
                return V5_V7
            }
            setSpecificBillingLibraryVersion("GPBL.$version")
            val majorVersion =
                versionArray[0].toIntOrNull() ?: return V5_V7
            return if (majorVersion == 1) {
                V1
            } else if (majorVersion < 5) {
                V2_V4
            } else {
                V5_V7
            }
        } catch (e: Exception) {
            // Default to newest version
            return V5_V7
        }
    }
}
