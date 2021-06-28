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
package com.facebook.appevents

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
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
class InternalAppEventsLogger
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
constructor(private val loggerImpl: AppEventsLoggerImpl) {
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

  fun logPurchaseImplicitly(purchaseAmount: BigDecimal?, currency: Currency?, parameters: Bundle?) {
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      loggerImpl.logPurchaseImplicitly(purchaseAmount, currency, parameters)
    }
  }

  fun logEventFromSE(eventName: String?, buttonText: String?) {
    loggerImpl.logEventFromSE(eventName, buttonText)
  }

  fun logEventImplicitly(
      eventName: String?,
      purchaseAmount: BigDecimal?,
      currency: Currency?,
      parameters: Bundle?
  ) {
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      loggerImpl.logEventImplicitly(eventName, purchaseAmount, currency, parameters)
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
    fun setInternalUserData(ud: Map<String?, String?>?) {
      UserDataStore.setInternalUd(ud)
    }
  }
}
