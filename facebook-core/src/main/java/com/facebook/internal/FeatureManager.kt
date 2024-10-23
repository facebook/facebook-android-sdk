/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureManager {
    private const val FEATURE_MANAGER_STORE = "com.facebook.internal.FEATURE_MANAGER"
    private val featureMapping: MutableMap<Feature, Array<String>> = hashMapOf()

    @JvmStatic
    fun checkFeature(feature: Feature, callback: Callback) {
        FetchedAppGateKeepersManager.loadAppGateKeepersAsync(
            object : FetchedAppGateKeepersManager.Callback {
                override fun onCompleted() {
                    callback.onCompleted(isEnabled(feature))
                }
            })
    }

    @JvmStatic
    fun isEnabled(feature: Feature): Boolean {
        if (Feature.Unknown == feature) {
            return false
        }
        if (Feature.Core == feature) {
            return true
        }
        val version =
            FacebookSdk.getApplicationContext()
                .getSharedPreferences(FEATURE_MANAGER_STORE, Context.MODE_PRIVATE)
                .getString(feature.toKey(), null)
        if (version != null && version == FacebookSdk.getSdkVersion()) {
            return false
        }
        val parent = feature.parent
        return if (parent == feature) {
            getGKStatus(feature)
        } else {
            isEnabled(parent) && getGKStatus(feature)
        }
    }

    @JvmStatic
    fun disableFeature(feature: Feature) {
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(FEATURE_MANAGER_STORE, Context.MODE_PRIVATE)
            .edit()
            .putString(feature.toKey(), FacebookSdk.getSdkVersion())
            .apply()
    }

    @JvmStatic
    fun getFeature(className: String): Feature {
        initializeFeatureMapping()
        for ((key, value) in featureMapping) {
            for (v in value) {
                if (className.startsWith(v)) {
                    return key
                }
            }
        }
        return Feature.Unknown
    }

    @Synchronized
    private fun initializeFeatureMapping() {
        if (!featureMapping.isEmpty()) {
            return
        }
        featureMapping[Feature.AAM] = arrayOf("com.facebook.appevents.aam.")
        featureMapping[Feature.CodelessEvents] = arrayOf("com.facebook.appevents.codeless.")
        featureMapping[Feature.CloudBridge] = arrayOf("com.facebook.appevents.cloudbridge.")
        featureMapping[Feature.ErrorReport] =
            arrayOf("com.facebook.internal.instrument.errorreport.")
        featureMapping[Feature.AnrReport] = arrayOf("com.facebook.internal.instrument.anrreport.")
        featureMapping[Feature.PrivacyProtection] = arrayOf("com.facebook.appevents.ml.")
        featureMapping[Feature.SuggestedEvents] = arrayOf("com.facebook.appevents.suggestedevents.")
        featureMapping[Feature.RestrictiveDataFiltering] =
            arrayOf("com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager")
        featureMapping[Feature.IntelligentIntegrity] =
            arrayOf("com.facebook.appevents.integrity.IntegrityManager")
        featureMapping[Feature.ProtectedMode] =
            arrayOf("com.facebook.appevents.integrity.ProtectedModeManager")
        featureMapping[Feature.MACARuleMatching] =
            arrayOf("com.facebook.appevents.integrity.MACARuleMatchingManager")
        featureMapping[Feature.BlocklistEvents] =
            arrayOf("com.facebook.appevents.integrity.BlocklistEventsManager")
        featureMapping[Feature.FilterRedactedEvents] =
            arrayOf("com.facebook.appevents.integrity.RedactedEventsManager")
        featureMapping[Feature.FilterSensitiveParams] =
            arrayOf("com.facebook.appevents.integrity.SensitiveParamsManager")
        featureMapping[Feature.EventDeactivation] =
            arrayOf("com.facebook.appevents.eventdeactivation.")
        featureMapping[Feature.OnDeviceEventProcessing] =
            arrayOf("com.facebook.appevents.ondeviceprocessing.")
        featureMapping[Feature.IapLogging] = arrayOf("com.facebook.appevents.iap.")
        featureMapping[Feature.Monitoring] = arrayOf("com.facebook.internal.logging.monitor")
    }

    private fun getGKStatus(feature: Feature): Boolean {
        val defaultStatus = defaultStatus(feature)
        return FetchedAppGateKeepersManager.getGateKeeperForKey(
            feature.toKey(), FacebookSdk.getApplicationId(), defaultStatus
        )
    }

    private fun defaultStatus(feature: Feature): Boolean {
        return when (feature) {
            Feature.RestrictiveDataFiltering,
            Feature.Instrument,
            Feature.CrashReport,
            Feature.CrashShield,
            Feature.ThreadCheck,
            Feature.ErrorReport,
            Feature.AnrReport,
            Feature.AAM,
            Feature.CloudBridge,
            Feature.PrivacyProtection,
            Feature.SuggestedEvents,
            Feature.IntelligentIntegrity,
            Feature.ModelRequest,
            Feature.EventDeactivation,
            Feature.OnDeviceEventProcessing,
            Feature.OnDevicePostInstallEventProcessing,
            Feature.IapLogging,
            Feature.IapLoggingLib2,
            Feature.IapLoggingLib5To7,
            Feature.AndroidManualImplicitPurchaseDedupe,
            Feature.AndroidManualImplicitSubsDedupe,
            Feature.BannedParamFiltering,
            Feature.ProtectedMode,
            Feature.StdParamEnforcement,
            Feature.MACARuleMatching,
            Feature.BlocklistEvents,
            Feature.FilterRedactedEvents,
            Feature.FilterSensitiveParams,
            Feature.ChromeCustomTabsPrefetching,
            Feature.Monitoring,
            Feature.IgnoreAppSwitchToLoggedOut,
            Feature.BypassAppSwitch -> false

            else -> true
        }
    }

    /**
     * Feature enum Defines features in SDK
     *
     * Sample: AppEvents = 0x00010000, ^ ^ ^ ^ | | | | kit | | | feature | | sub-feature |
     * sub-sub-feature 1st byte: kit 2nd byte: feature 3rd byte: sub-feature 4th byte: sub-sub-feature
     */
    enum class Feature(private val code: Int) {
        Unknown(-1),

        // Features in CoreKit
        /** Essential of CoreKit */
        Core(0x00000000),
        AppEvents(0x00010000),
        CodelessEvents(0x00010100),
        CloudBridge(0x00010800),
        RestrictiveDataFiltering(0x00010200),
        AAM(0x00010300),
        PrivacyProtection(0x00010400),
        SuggestedEvents(0x00010401),
        IntelligentIntegrity(0x00010402),
        ModelRequest(0x00010403),
        ProtectedMode(0x00010404), /* filter out the params which are not supported legally */
        MACARuleMatching(0x00010405),
        BlocklistEvents(0x00010406), /* drop the events in the blocklist */
        FilterRedactedEvents(0x00010407), /* replace the event name via the redaction string */
        FilterSensitiveParams(0x00010408), /* filter out the sensitive params */
        StdParamEnforcement(0x01010409),
        BannedParamFiltering(0x0101040a),
        EventDeactivation(0x00010500),
        OnDeviceEventProcessing(0x00010600),
        OnDevicePostInstallEventProcessing(0x00010601),
        IapLogging(0x00010700),
        IapLoggingLib2(0x00010701),
        IapLoggingLib5To7(0x0010702),
        AndroidManualImplicitPurchaseDedupe(0x0010703),
        AndroidManualImplicitSubsDedupe(0x0010704),
        Instrument(0x00020000),
        CrashReport(0x00020100),
        CrashShield(0x00020101),
        ThreadCheck(0x00020102),
        ErrorReport(0x00020200),
        AnrReport(0x00020300),
        Monitoring(0x00030000),
        ServiceUpdateCompliance(0x00030100),
        Megatron(0x00040000),
        Elora(0x00050000),
        // Features in LoginKit
        /** Essential of LoginKit */
        Login(0x01000000),
        ChromeCustomTabsPrefetching(0x01010000),
        IgnoreAppSwitchToLoggedOut(0x01020000),
        BypassAppSwitch(0x01030000),
        // Features in ShareKit
        /** Essential of ShareKit */
        Share(0x02000000);

        override fun toString(): String =
            when (this) {
                Core -> "CoreKit"
                AppEvents -> "AppEvents"
                CodelessEvents -> "CodelessEvents"
                RestrictiveDataFiltering -> "RestrictiveDataFiltering"
                Instrument -> "Instrument"
                CrashReport -> "CrashReport"
                CrashShield -> "CrashShield"
                ThreadCheck -> "ThreadCheck"
                ErrorReport -> "ErrorReport"
                AnrReport -> "AnrReport"
                AAM -> "AAM"
                CloudBridge -> "AppEventsCloudbridge"
                PrivacyProtection -> "PrivacyProtection"
                SuggestedEvents -> "SuggestedEvents"
                IntelligentIntegrity -> "IntelligentIntegrity"
                StdParamEnforcement -> "StdParamEnforcement"
                ProtectedMode -> "ProtectedMode"
                BannedParamFiltering -> "BannedParamFiltering"
                MACARuleMatching -> "MACARuleMatching"
                BlocklistEvents -> "BlocklistEvents"
                FilterRedactedEvents -> "FilterRedactedEvents"
                FilterSensitiveParams -> "FilterSensitiveParams"
                ModelRequest -> "ModelRequest"
                EventDeactivation -> "EventDeactivation"
                OnDeviceEventProcessing -> "OnDeviceEventProcessing"
                OnDevicePostInstallEventProcessing -> "OnDevicePostInstallEventProcessing"
                IapLogging -> "IAPLogging"
                IapLoggingLib2 -> "IAPLoggingLib2"
                IapLoggingLib5To7 -> "IAPLoggingLib5To7"
                AndroidManualImplicitPurchaseDedupe -> "AndroidManualImplicitPurchaseDedupe"
                AndroidManualImplicitSubsDedupe -> "AndroidManualImplicitSubsDedupe"
                Monitoring -> "Monitoring"
                Megatron -> "Megatron"
                Elora -> "Elora"
                ServiceUpdateCompliance -> "ServiceUpdateCompliance"
                Login -> "LoginKit"
                ChromeCustomTabsPrefetching -> "ChromeCustomTabsPrefetching"
                IgnoreAppSwitchToLoggedOut -> "IgnoreAppSwitchToLoggedOut"
                BypassAppSwitch -> "BypassAppSwitch"
                Share -> "ShareKit"
                else -> "unknown"
            }

        fun toKey(): String {
            return "FBSDKFeature$this"
        }

        val parent: Feature
            get() =
                when {
                    this.code and 0xFF > 0 -> {
                        fromInt(this.code and -0x100)
                    }

                    this.code and 0xFF00 > 0 -> {
                        fromInt(this.code and -0x10000)
                    }

                    this.code and 0xFF0000 > 0 -> {
                        fromInt(this.code and -0x1000000)
                    }

                    else -> {
                        fromInt(0)
                    }
                }

        companion object {
            fun fromInt(code: Int): Feature {
                for (feature in values()) {
                    if (feature.code == code) {
                        return feature
                    }
                }
                return Unknown
            }
        }
    }

    /**
     * Callback for fetching feature status. Method [FeatureManager.checkFeature]} will call
     * GateKeeper manager to load the latest GKs first and then run the callback function.
     */
    fun interface Callback {
        /** The method that will be called when the feature status request completes. */
        fun onCompleted(enabled: Boolean)
    }
}
