/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.UserSettingsManager
import com.facebook.appevents.AppEventQueue.add
import com.facebook.appevents.AppEventQueue.flush
import com.facebook.appevents.AppEventQueue.getKeySet
import com.facebook.appevents.AppEventQueue.persistToDisk
import com.facebook.appevents.gps.ara.GpsAraTriggersManager
import com.facebook.appevents.gps.pa.PACustomAudienceClient
import com.facebook.appevents.iap.InAppPurchase
import com.facebook.appevents.iap.InAppPurchaseDedupeConfig
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.integrity.BannedParamManager.processFilterBannedParams
import com.facebook.appevents.integrity.BlocklistEventsManager.isInBlocklist
import com.facebook.appevents.integrity.MACARuleMatchingManager
import com.facebook.appevents.integrity.ProtectedModeManager
import com.facebook.appevents.integrity.ProtectedModeManager.processParametersForProtectedMode
import com.facebook.appevents.integrity.SensitiveParamsManager.processFilterSensitiveParams
import com.facebook.appevents.integrity.StdParamsEnforcementManager.processFilterParamSchemaBlocking
import com.facebook.appevents.internal.ActivityLifecycleTracker.getCurrentSessionGuid
import com.facebook.appevents.internal.ActivityLifecycleTracker.isInBackground
import com.facebook.appevents.internal.ActivityLifecycleTracker.startTracking
import com.facebook.appevents.internal.AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled
import com.facebook.appevents.internal.Constants
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager.isOnDeviceProcessingEnabled
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager.sendCustomEventAsync
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.FetchedAppGateKeepersManager.getGateKeeperForKey
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.InstallReferrerUtil
import com.facebook.internal.InstallReferrerUtil.tryUpdateReferrerInfo
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Utility.getActivityName
import com.facebook.internal.Utility.getMetadataApplicationId
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.logd
import com.facebook.internal.Utility.stringsEqualOrEmpty
import com.facebook.internal.Validate.sdkInitialized
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Currency
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@AutoHandleExceptions
internal class AppEventsLoggerImpl
internal constructor(activityName: String, applicationId: String?, accessToken: AccessToken?) {

    internal constructor(
        context: Context?,
        applicationId: String?,
        accessToken: AccessToken?
    ) : this(getActivityName(context), applicationId, accessToken)

    // Instance member variables
    private val contextName: String
    private var accessTokenAppId: AccessTokenAppIdPair

    fun logEvent(eventName: String?) {
        logEvent(eventName, null)
    }

    fun logEvent(eventName: String?, parameters: Bundle? = null) {
        logEvent(eventName, null, parameters, false, getCurrentSessionGuid())
    }

    fun logEvent(eventName: String?, valueToSum: Double) {
        logEvent(eventName, valueToSum, null)
    }

    fun logEvent(eventName: String?, valueToSum: Double, parameters: Bundle?) {
        logEvent(eventName, valueToSum, parameters, false, getCurrentSessionGuid())
    }

    fun logEventFromSE(eventName: String?, buttonText: String?) {
        val parameters = Bundle()
        parameters.putString("_is_suggested_event", "1")
        parameters.putString("_button_text", buttonText)
        logEvent(eventName, parameters)
    }

    fun logPurchase(purchaseAmount: BigDecimal?, currency: Currency?) {
        logPurchase(purchaseAmount, currency, null)
    }

    fun logPurchase(purchaseAmount: BigDecimal?, currency: Currency?, parameters: Bundle? = null) {
        logPurchase(purchaseAmount, currency, parameters, false)
    }

    fun logPurchaseImplicitly(
        purchaseAmount: BigDecimal?,
        currency: Currency?,
        parameters: Bundle?,
        operationalData: OperationalData? = null
    ) {
        logPurchase(purchaseAmount, currency, parameters, true, operationalData)
    }

    fun logPurchase(
        purchaseAmount: BigDecimal?,
        currency: Currency?,
        parameters: Bundle?,
        isImplicitlyLogged: Boolean,
        operationalData: OperationalData? = null
    ) {
        var parameters = parameters
        if (purchaseAmount == null) {
            notifyDeveloperError("purchaseAmount cannot be null")
            return
        } else if (currency == null) {
            notifyDeveloperError("currency cannot be null")
            return
        }
        if (parameters == null) {
            parameters = Bundle()
        }
        parameters.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency.currencyCode)
        logEvent(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            purchaseAmount.toDouble(),
            parameters,
            isImplicitlyLogged,
            getCurrentSessionGuid(),
            operationalData
        )
        eagerFlush()
    }

    fun logPushNotificationOpen(payload: Bundle, action: String?) {
        var campaignId: String? = null
        try {
            val payloadString = payload.getString(PUSH_PAYLOAD_KEY)
            if (isNullOrEmpty(payloadString)) {
                return // Ignore the payload if no fb push payload is present.
            }
            val facebookPayload = JSONObject(payloadString)
            campaignId = facebookPayload.getString(PUSH_PAYLOAD_CAMPAIGN_KEY)
        } catch (je: JSONException) {
            // ignore
        }
        if (campaignId == null) {
            log(
                LoggingBehavior.DEVELOPER_ERRORS,
                TAG,
                "Malformed payload specified for logging a push notification open."
            )
            return
        }
        val parameters = Bundle()
        parameters.putString(APP_EVENT_PUSH_PARAMETER_CAMPAIGN, campaignId)
        if (action != null) {
            parameters.putString(APP_EVENT_PUSH_PARAMETER_ACTION, action)
        }
        logEvent(APP_EVENT_NAME_PUSH_OPENED, parameters)
    }

    fun logProductItem(
        itemID: String?,
        availability: AppEventsLogger.ProductAvailability?,
        condition: AppEventsLogger.ProductCondition?,
        description: String?,
        imageLink: String?,
        link: String?,
        title: String?,
        priceAmount: BigDecimal?,
        currency: Currency?,
        gtin: String?,
        mpn: String?,
        brand: String?,
        parameters: Bundle?
    ) {
        var parameters = parameters
        if (itemID == null) {
            notifyDeveloperError("itemID cannot be null")
            return
        } else if (availability == null) {
            notifyDeveloperError("availability cannot be null")
            return
        } else if (condition == null) {
            notifyDeveloperError("condition cannot be null")
            return
        } else if (description == null) {
            notifyDeveloperError("description cannot be null")
            return
        } else if (imageLink == null) {
            notifyDeveloperError("imageLink cannot be null")
            return
        } else if (link == null) {
            notifyDeveloperError("link cannot be null")
            return
        } else if (title == null) {
            notifyDeveloperError("title cannot be null")
            return
        } else if (priceAmount == null) {
            notifyDeveloperError("priceAmount cannot be null")
            return
        } else if (currency == null) {
            notifyDeveloperError("currency cannot be null")
            return
        } else if (gtin == null && mpn == null && brand == null) {
            notifyDeveloperError("Either gtin, mpn or brand is required")
            return
        }
        if (parameters == null) {
            parameters = Bundle()
        }
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_ITEM_ID, itemID)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_AVAILABILITY, availability.name)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_CONDITION, condition.name)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, description)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, imageLink)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, link)
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, title)
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
            priceAmount.setScale(3, BigDecimal.ROUND_HALF_UP).toString()
        )
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY, currency.currencyCode)
        if (gtin != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_GTIN, gtin)
        }
        if (mpn != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_MPN, mpn)
        }
        if (brand != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_BRAND, brand)
        }
        logEvent(AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE, parameters)
        eagerFlush()
    }

    fun flush() {
        flush(FlushReason.EXPLICIT)
    }

    fun isValidForAccessToken(accessToken: AccessToken): Boolean {
        val other = AccessTokenAppIdPair(accessToken)
        return accessTokenAppId == other
    }

    fun logSdkEvent(eventName: String, valueToSum: Double?, parameters: Bundle?) {
        if (!eventName.startsWith(ACCOUNT_KIT_EVENT_NAME_PREFIX)) {
            Log.e(
                TAG,
                "logSdkEvent is deprecated and only supports account kit for legacy, " +
                        "please use logEvent instead"
            )
            return
        }
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            logEvent(eventName, valueToSum, parameters, true, getCurrentSessionGuid())
        }
    }

    /**
     * Returns the app ID this logger was configured to log to.
     *
     * @return the Facebook app ID
     */
    val applicationId: String
        get() = accessTokenAppId.applicationId

    fun logEventImplicitly(eventName: String?, valueToSum: Double?, parameters: Bundle?) {
        logEvent(eventName, valueToSum, parameters, true, getCurrentSessionGuid())
    }

    fun logEventImplicitly(
        eventName: String?,
        purchaseAmount: BigDecimal?,
        currency: Currency?,
        parameters: Bundle?,
        operationalData: OperationalData? = null
    ) {
        var parameters = parameters
        if (purchaseAmount == null || currency == null) {
            logd(TAG, "purchaseAmount and currency cannot be null")
            return
        }
        if (parameters == null) {
            parameters = Bundle()
        }
        parameters.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency.currencyCode)
        logEvent(
            eventName,
            purchaseAmount.toDouble(),
            parameters,
            true,
            getCurrentSessionGuid(),
            operationalData
        )
    }

    fun logEvent(
        eventName: String?,
        valueToSum: Double?,
        parameters: Bundle?,
        isImplicitlyLogged: Boolean,
        currentSessionId: UUID?,
        operationalData: OperationalData? = null
    ) {
        if (eventName.isNullOrEmpty()) {
            return
        }

        var modifiedParameters = parameters
        var modifiedOperationalData = operationalData
        // Attempt implicit x manual purchase/subscription dedupe
        if (!isImplicitlyLogged &&
            isImplicitPurchaseLoggingEnabled() &&
            (eventName == AppEventsConstants.EVENT_NAME_PURCHASED ||
                    eventName == AppEventsConstants.EVENT_NAME_SUBSCRIBE
                    || eventName == AppEventsConstants.EVENT_NAME_START_TRIAL)
        ) {
            Log.w(
                TAG,
                "You are logging purchase events while auto-logging of in-app purchase is " +
                        "enabled in the SDK. Make sure you don't log duplicate events"
            )
            if ((isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe) &&
                        eventName == AppEventsConstants.EVENT_NAME_PURCHASED) ||
                (isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe) &&
                        (eventName == AppEventsConstants.EVENT_NAME_SUBSCRIBE ||
                                eventName == AppEventsConstants.EVENT_NAME_START_TRIAL))
            ) {
                val purchaseAmount =
                    InAppPurchaseDedupeConfig.getValueOfManualEvent(valueToSum, parameters)
                val currency = InAppPurchaseDedupeConfig.getCurrencyOfManualEvent(parameters)
                if (purchaseAmount != null && currency != null) {
                    val purchase =
                        InAppPurchase(
                            eventName,
                            purchaseAmount.toDouble(),
                            currency
                        )
                    val dedupeParameters = InAppPurchaseManager.performDedupe(
                        listOf(purchase),
                        System.currentTimeMillis(),
                        false,
                        listOf(Pair(parameters, operationalData)),
                    )
                    val (newParameters, newOperationalData) = InAppPurchaseDedupeConfig.addDedupeParameters(
                        dedupeParameters,
                        modifiedParameters,
                        operationalData
                    )
                    modifiedParameters = newParameters
                    modifiedOperationalData = newOperationalData
                }
            }
        }

        // Kill events if kill-switch is enabled
        if (getGateKeeperForKey(APP_EVENTS_KILLSWITCH, FacebookSdk.getApplicationId(), false)) {
            log(
                LoggingBehavior.APP_EVENTS,
                "AppEvents",
                "KillSwitch is enabled and fail to log app event: %s",
                eventName
            )
            return
        }

        // return earlier if the event name is in blocklist
        if (isInBlocklist(eventName)) {
            return
        }

        val (newParameters, newOperationalData) = addImplicitPurchaseParameters(
            modifiedParameters,
            modifiedOperationalData,
            isImplicitlyLogged
        )
        modifiedParameters = newParameters
        modifiedOperationalData = newOperationalData

        try {
            if (!ProtectedModeManager.protectedModeIsApplied(modifiedParameters)) {
                processFilterSensitiveParams(modifiedParameters, eventName)
            }
            processFilterBannedParams(modifiedParameters)
            MACARuleMatchingManager.processParameters(modifiedParameters, eventName)
            processFilterParamSchemaBlocking(modifiedParameters)
            processParametersForProtectedMode(modifiedParameters)
            val event =
                AppEvent(
                    contextName,
                    eventName,
                    valueToSum,
                    modifiedParameters,
                    isImplicitlyLogged,
                    isInBackground(),
                    currentSessionId,
                    modifiedOperationalData
                )
            logEvent(event, accessTokenAppId)
        } catch (jsonException: JSONException) {
            // If any of the above failed, just consider this an illegal event.
            log(
                LoggingBehavior.APP_EVENTS,
                "AppEvents",
                "JSON encoding for app event failed: '%s'",
                jsonException.toString()
            )
        } catch (e: FacebookException) {
            // If any of the above failed, just consider this an illegal event.
            log(LoggingBehavior.APP_EVENTS, "AppEvents", "Invalid app event: %s", e.toString())
        }
    }

    companion object {
        // Constants
        private val TAG =
            AppEventsLoggerImpl::class.java.canonicalName
                ?: "com.facebook.appevents.AppEventsLoggerImpl"
        private const val APP_SUPPORTS_ATTRIBUTION_ID_RECHECK_PERIOD_IN_SECONDS = 60 * 60 * 24
        private const val PUSH_PAYLOAD_KEY = "fb_push_payload"
        private const val PUSH_PAYLOAD_CAMPAIGN_KEY = "campaign"
        private const val APP_EVENT_NAME_PUSH_OPENED = "fb_mobile_push_opened"
        private const val APP_EVENT_PUSH_PARAMETER_CAMPAIGN = "fb_push_campaign"
        private const val APP_EVENT_PUSH_PARAMETER_ACTION = "fb_push_action"
        private const val ACCOUNT_KIT_EVENT_NAME_PREFIX = "fb_ak"
        private var backgroundExecutor: ScheduledThreadPoolExecutor? = null
        private var flushBehaviorField = AppEventsLogger.FlushBehavior.AUTO

        @JvmStatic
        fun getFlushBehavior(): AppEventsLogger.FlushBehavior {
            synchronized(staticLock) {
                return flushBehaviorField
            }
        }

        @JvmStatic
        fun setFlushBehavior(flushBehavior: AppEventsLogger.FlushBehavior) {
            synchronized(staticLock) { flushBehaviorField = flushBehavior }
        }

        private val staticLock = Any()
        private var anonymousAppDeviceGUID: String? = null
        private var isActivateAppEventRequested = false

        // Log implicit push token event and flush logger immediately
        private var pushNotificationsRegistrationIdField: String? = null

        @JvmStatic
        fun getPushNotificationsRegistrationId(): String? {
            synchronized(staticLock) {
                return pushNotificationsRegistrationIdField
            }
        }

        @JvmStatic
        fun setPushNotificationsRegistrationId(registrationId: String?) {
            synchronized(staticLock) {
                if (!stringsEqualOrEmpty(pushNotificationsRegistrationIdField, registrationId)) {
                    pushNotificationsRegistrationIdField = registrationId
                    val logger =
                        AppEventsLoggerImpl(FacebookSdk.getApplicationContext(), null, null)
                    // Log implicit push token event and flush logger immediately
                    logger.logEvent(AppEventsConstants.EVENT_NAME_PUSH_TOKEN_OBTAINED)
                    if (getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
                        logger.flush()
                    }
                }
            }
        }

        private const val APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences"
        const val APP_EVENTS_KILLSWITCH = "app_events_killswitch"

        @JvmStatic
        fun activateApp(application: Application, applicationId: String?) {
            var applicationId = applicationId
            if (!FacebookSdk.isInitialized()) {
                throw FacebookException(
                    "The Facebook sdk must be initialized before calling " + "activateApp"
                )
            }
            AnalyticsUserIDStore.initStore()
            UserDataStore.initStore()
            if (applicationId == null) {
                applicationId = FacebookSdk.getApplicationId()
            }

            // activateApp supersedes publishInstall in the public API, so we need to explicitly invoke
            // it, since the server can't reliably infer install state for all conditions of an app
            // activate.
            FacebookSdk.publishInstallAsync(application, applicationId)

            // Will do nothing in case AutoLogAppEventsEnabled is true, as we already started the
            // tracking as part of sdkInitialize() flow
            startTracking(application, applicationId)

            if (isEnabled(FeatureManager.Feature.GPSPACAProcessing)) {
                PACustomAudienceClient.joinCustomAudience(applicationId, "fb_mobile_app_install")
            }
        }

        @JvmStatic
        fun addImplicitPurchaseParameters(
            params: Bundle?,
            operationalData: OperationalData?,
            isImplicitlyLogged: Boolean
        ): Pair<Bundle?, OperationalData?> {
            var modifiedParams = params
            var modifiedOperationalData = operationalData
            var modifiedParamsAndData: Pair<Bundle?, OperationalData?>
            val isImplicitPurchaseEnabled = if (isImplicitPurchaseLoggingEnabled()) {
                "1"
            } else {
                "0"
            }
            modifiedParamsAndData = OperationalData.addParameterAndReturn(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
                isImplicitPurchaseEnabled,
                modifiedParams,
                modifiedOperationalData
            )
            val productId = OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.IAP_PRODUCT_ID,
                modifiedParams,
                modifiedOperationalData
            ) as? String
            if (!isImplicitlyLogged && modifiedParams?.getString(AppEventsConstants.EVENT_PARAM_CONTENT_ID) == null && productId != null) {
                modifiedParamsAndData = OperationalData.addParameterAndReturn(
                    OperationalDataEnum.IAPParameters,
                    AppEventsConstants.EVENT_PARAM_CONTENT_ID,
                    productId,
                    modifiedParams,
                    modifiedOperationalData
                )
                modifiedParams = modifiedParamsAndData.first
                modifiedOperationalData = modifiedParamsAndData.second
                modifiedParamsAndData = OperationalData.addParameterAndReturn(
                    OperationalDataEnum.IAPParameters,
                    Constants.ANDROID_DYNAMIC_ADS_CONTENT_ID,
                    "client_manual",
                    modifiedParams,
                    modifiedOperationalData
                )
            }

            modifiedParams = modifiedParamsAndData.first
            modifiedOperationalData = modifiedParamsAndData.second

            val isAutoLoggingEnabled = if (UserSettingsManager.getAutoLogAppEventsEnabled()) {
                "1"
            } else {
                "0"
            }
            modifiedParamsAndData = OperationalData.addParameterAndReturn(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
                isAutoLoggingEnabled,
                modifiedParams,
                modifiedOperationalData
            )
            modifiedParams = modifiedParamsAndData.first
            modifiedOperationalData = modifiedParamsAndData.second
            return Pair(modifiedParams, modifiedOperationalData)
        }

        @JvmStatic
        fun functionDEPRECATED(extraMsg: String) {
            Log.w(TAG, "This function is deprecated. $extraMsg")
        }

        @JvmStatic
        fun initializeLib(context: Context, applicationId: String?) {
            if (!FacebookSdk.getAutoLogAppEventsEnabled()) {
                return
            }
            val logger = AppEventsLoggerImpl(context, applicationId, null)
            checkNotNull(backgroundExecutor).execute {
                val params = Bundle()
                val classes =
                    arrayOf( // internal SDK Libraries
                        "com.facebook.core.Core",
                        "com.facebook.login.Login",
                        "com.facebook.share.Share",
                        "com.facebook.places.Places",
                        "com.facebook.messenger.Messenger",
                        "com.facebook.applinks.AppLinks",
                        "com.facebook.marketing.Marketing",
                        "com.facebook.gamingservices.GamingServices",
                        "com.facebook.all.All", // external SDK Libraries
                        "com.android.billingclient.api.BillingClient",
                        "com.android.vending.billing.IInAppBillingService"
                    )
                val keys =
                    arrayOf( // internal SDK Libraries
                        "core_lib_included",
                        "login_lib_included",
                        "share_lib_included",
                        "places_lib_included",
                        "messenger_lib_included",
                        "applinks_lib_included",
                        "marketing_lib_included",
                        "gamingservices_lib_included",
                        "all_lib_included", // external SDK Libraries
                        "billing_client_lib_included",
                        "billing_service_lib_included"
                    )
                if (classes.size != keys.size) {
                    throw FacebookException("Number of class names and key names should match")
                }
                var bitmask = 0
                for (i in classes.indices) {
                    val className = classes[i]
                    val keyName = keys[i]
                    try {
                        Class.forName(className)
                        params.putInt(keyName, 1)
                        bitmask = bitmask or (1 shl i)
                    } catch (ignored: ClassNotFoundException) {
                        /* no op */
                    }
                }
                val preferences =
                    context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
                val previousBitmask = preferences.getInt("kitsBitmask", 0)
                if (previousBitmask != bitmask) {
                    preferences.edit().putInt("kitsBitmask", bitmask).apply()
                    logger.logEventImplicitly(AnalyticsEvents.EVENT_SDK_INITIALIZE, null, params)
                }
            }
        }

        @JvmStatic
        fun onContextStop() {
            // TODO: (v4) add onContextStop() to samples that use the logger.
            persistToDisk()
        }

        // will make async connection to try retrieve data. First time we might return null
        // instead of actual result.
        // Should not be a problem as subsequent calls will have the data if it exists.
        @JvmStatic
        fun getInstallReferrer(): String? {
            tryUpdateReferrerInfo(
                object : InstallReferrerUtil.Callback {
                    // will make async connection to try retrieve data. First time we might return null
                    // instead of actual result.
                    // Should not be a problem as subsequent calls will have the data if it exists.
                    override fun onReceiveReferrerUrl(s: String?) {
                        setInstallReferrer(s)
                    }
                })
            val ctx = FacebookSdk.getApplicationContext()
            val preferences = ctx.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
            return preferences.getString("install_referrer", null)
        }

        @JvmStatic
        fun setInstallReferrer(referrer: String?) {
            val ctx = FacebookSdk.getApplicationContext()
            val preferences = ctx.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
            if (referrer != null) {
                preferences.edit().putString("install_referrer", referrer).apply()
            }
        }

        @JvmStatic
        fun augmentWebView(webView: WebView, context: Context?) {
            val parts = Build.VERSION.RELEASE.split(".").toTypedArray()
            val majorRelease = if (parts.isNotEmpty()) parts[0].toInt() else 0
            val minorRelease = if (parts.size > 1) parts[1].toInt() else 0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ||
                majorRelease < 4 ||
                majorRelease == 4 && minorRelease <= 1
            ) {
                log(
                    LoggingBehavior.DEVELOPER_ERRORS,
                    TAG,
                    "augmentWebView is only available for Android SDK version >= 17 on devices " +
                            "running Android >= 4.2"
                )
                return
            }
            webView.addJavascriptInterface(
                FacebookSDKJSInterface(context), "fbmq_" + FacebookSdk.getApplicationId()
            )
        }

        private fun initializeTimersIfNeeded() {
            synchronized(staticLock) {
                if (backgroundExecutor != null) {
                    return
                }
                // Having single runner thread enforces ordered execution of tasks,
                // which matters in some cases e.g. making sure user id is set before
                // trying to update user properties for a given id
                backgroundExecutor = ScheduledThreadPoolExecutor(1)
            }
            val attributionRecheckRunnable = Runnable {
                val applicationIds: MutableSet<String> = HashSet()
                for (accessTokenAppId in getKeySet()) {
                    applicationIds.add(accessTokenAppId.applicationId)
                }
                for (applicationId in applicationIds) {
                    queryAppSettings(applicationId, true)
                }
            }
            checkNotNull(backgroundExecutor)
                .scheduleAtFixedRate(
                    attributionRecheckRunnable,
                    0,
                    APP_SUPPORTS_ATTRIBUTION_ID_RECHECK_PERIOD_IN_SECONDS.toLong(),
                    TimeUnit.SECONDS
                )
        }

        private fun logEvent(event: AppEvent, accessTokenAppId: AccessTokenAppIdPair) {
            add(accessTokenAppId, event)
            if (isEnabled(FeatureManager.Feature.OnDevicePostInstallEventProcessing) &&
                isOnDeviceProcessingEnabled()
            ) {
                sendCustomEventAsync(accessTokenAppId.applicationId, event)
            }
            if (isEnabled(FeatureManager.Feature.GPSARATriggers)) {
                GpsAraTriggersManager.registerTriggerAsync(accessTokenAppId.applicationId, event)
            }
            if (isEnabled(FeatureManager.Feature.GPSPACAProcessing)) {
                PACustomAudienceClient.joinCustomAudience(accessTokenAppId.applicationId, event)
            }

            // Make sure Activated_App is always before other app events
            if (!event.getIsImplicit() && !isActivateAppEventRequested) {
                if (event.name == AppEventsConstants.EVENT_NAME_ACTIVATED_APP) {
                    isActivateAppEventRequested = true
                } else {
                    log(
                        LoggingBehavior.APP_EVENTS,
                        "AppEvents",
                        "Warning: Please call AppEventsLogger.activateApp(...)" +
                                "from the long-lived activity's onResume() method" +
                                "before logging other app events."
                    )
                }
            }
        }

        fun eagerFlush() {
            if (getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
                flush(FlushReason.EAGER_FLUSHING_EVENT)
            }
        }

        /**
         * Invoke this method, rather than throwing an Exception, for situations where user/server input
         * might reasonably cause this to occur, and thus don't want an exception thrown at production
         * time, but do want logging notification.
         */
        private fun notifyDeveloperError(message: String) {
            log(LoggingBehavior.DEVELOPER_ERRORS, "AppEvents", message)
        }

        @JvmStatic
        fun getAnalyticsExecutor(): Executor {
            if (backgroundExecutor == null) {
                initializeTimersIfNeeded()
            }
            return checkNotNull(backgroundExecutor)
        }

        @JvmStatic
        fun getAnonymousAppDeviceGUID(context: Context): String {
            if (anonymousAppDeviceGUID == null) {
                synchronized(staticLock) {
                    if (anonymousAppDeviceGUID == null) {
                        val preferences =
                            context.getSharedPreferences(
                                APP_EVENT_PREFERENCES,
                                Context.MODE_PRIVATE
                            )
                        anonymousAppDeviceGUID =
                            preferences.getString("anonymousAppDeviceGUID", null)
                        if (anonymousAppDeviceGUID == null) {
                            // Arbitrarily prepend XZ to distinguish from device supplied identifiers.
                            anonymousAppDeviceGUID = "XZ" + UUID.randomUUID().toString()
                            context
                                .getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
                                .edit()
                                .putString("anonymousAppDeviceGUID", anonymousAppDeviceGUID)
                                .apply()
                        }
                    }
                }
            }
            return checkNotNull(anonymousAppDeviceGUID)
        }
    }

    init {
        var applicationId = applicationId
        var accessToken = accessToken
        sdkInitialized()
        contextName = activityName
        if (accessToken == null) {
            accessToken = getCurrentAccessToken()
        }

        // If we have a session and the appId passed is null or matches the session's app ID:
        if (accessToken != null &&
            !accessToken.isExpired &&
            (applicationId == null || applicationId == accessToken.applicationId)
        ) {
            accessTokenAppId = AccessTokenAppIdPair(accessToken)
        } else {
            // If no app ID passed, get it from the manifest:
            if (applicationId == null) {
                applicationId = getMetadataApplicationId(FacebookSdk.getApplicationContext())
            }
            accessTokenAppId = AccessTokenAppIdPair(null, checkNotNull(applicationId))
        }
        initializeTimersIfNeeded()
    }
}
