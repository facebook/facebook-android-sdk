/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.facebook.AccessToken
import com.facebook.FacebookSdk
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.Executor

/**
 * com.facebook.appevents.InternalAppEventsLogger is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is unsupported, and they may
 * be modified or removed without warning at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InternalAppEventsLogger internal constructor(private val loggerImpl: AppEventsLoggerImpl) {
    constructor(context: Context?) : this(AppEventsLoggerImpl(context, null, null))
    constructor(
        context: Context?,
        applicationId: String?
    ) : this(AppEventsLoggerImpl(context, applicationId, null))

    constructor(
        activityName: String,
        applicationId: String?,
        accessToken: AccessToken?
    ) : this(AppEventsLoggerImpl(activityName, applicationId, accessToken))

    fun logEvent(eventName: String?, parameters: Bundle?) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEvent(eventName, parameters)
        }
    }

    fun logEvent(eventName: String?, valueToSum: Double, parameters: Bundle?) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEvent(eventName, valueToSum, parameters)
        }
    }

    fun logPurchaseImplicitly(
        purchaseAmount: BigDecimal?,
        currency: Currency?,
        parameters: Bundle?,
        operationalData: OperationalData? = null
    ) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logPurchaseImplicitly(purchaseAmount, currency, parameters, operationalData)
        }
    }

    fun logEventFromSE(eventName: String?, buttonText: String?) {
        loggerImpl.logEventFromSE(eventName, buttonText)
    }

    fun logEventImplicitly(
        eventName: String?,
        purchaseAmount: BigDecimal?,
        currency: Currency?,
        parameters: Bundle?,
        operationalData: OperationalData? = null
    ) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEventImplicitly(
                eventName,
                purchaseAmount,
                currency,
                parameters,
                operationalData
            )
        }
    }

    fun logEventImplicitly(eventName: String?) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEventImplicitly(eventName, null, null)
        }
    }

    fun logEventImplicitly(eventName: String?, valueToSum: Double?, parameters: Bundle?) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEventImplicitly(eventName, valueToSum, parameters)
        }
    }

    fun logEventImplicitly(eventName: String?, parameters: Bundle?) {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEventImplicitly(eventName, null, parameters)
        }
    }

    fun logChangedSettingsEvent(parameters: Bundle) {
        val previousAutoLog = parameters.getInt("previous") and 0x02 != 0
        val eventName = "fb_sdk_settings_changed"
        if (previousAutoLog || FacebookSdk.getAutoLogAppEventsEnabled()) {
            loggerImpl.logEventImplicitly(eventName, null, parameters)
        }
    }

    fun flush() {
        loggerImpl.flush()
    }

    companion object {
        @JvmStatic
        fun getFlushBehavior(): AppEventsLogger.FlushBehavior {
            return AppEventsLoggerImpl.getFlushBehavior()
        }

        @JvmStatic
        fun getAnalyticsExecutor(): Executor {
            return AppEventsLoggerImpl.getAnalyticsExecutor()
        }

        @JvmStatic
        fun getPushNotificationsRegistrationId(): String? {
            return AppEventsLoggerImpl.getPushNotificationsRegistrationId()
        }

        @JvmStatic
        fun setUserData(userData: Bundle?) {
            UserDataStore.setUserDataAndHash(userData)
        }

        @RestrictTo(RestrictTo.Scope.GROUP_ID)
        @JvmStatic
        fun setInternalUserData(ud: Map<String, String>) {
            UserDataStore.setInternalUd(ud)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmStatic
        @JvmOverloads
        fun createInstance(
            context: Context?,
            applicationId: String? = null
        ): InternalAppEventsLogger =
            InternalAppEventsLogger(context, applicationId)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmStatic
        fun createInstance(
            activityName: String,
            applicationId: String?,
            accessToken: AccessToken?
        ): InternalAppEventsLogger =
            InternalAppEventsLogger(activityName, applicationId, accessToken)
    }
}
