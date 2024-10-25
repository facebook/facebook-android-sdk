/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.codeless.internal.UnityReflection
import com.facebook.appevents.internal.AutomaticAnalyticsLogger
import com.facebook.appevents.internal.Constants
import com.facebook.core.BuildConfig
import com.facebook.internal.FetchedAppGateKeepersManager.queryAppGateKeepers
import com.facebook.internal.InternalSettings.isUnityApp
import com.facebook.internal.SmartLoginOption.Companion.parseOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object FetchedAppSettingsManager {
    private val TAG = FetchedAppSettingsManager::class.java.simpleName
    private const val APP_SETTINGS_PREFS_STORE = "com.facebook.internal.preferences.APP_SETTINGS"
    private const val APP_SETTINGS_PREFS_KEY_FORMAT = "com.facebook.internal.APP_SETTINGS.%s"
    private const val APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING = "supports_implicit_sdk_logging"
    private const val APP_SETTING_NUX_CONTENT = "gdpv4_nux_content"
    private const val APP_SETTING_NUX_ENABLED = "gdpv4_nux_enabled"
    private const val APP_SETTING_DIALOG_CONFIGS = "android_dialog_configs"
    private const val APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES = "android_sdk_error_categories"
    private const val APP_SETTING_APP_EVENTS_SESSION_TIMEOUT = "app_events_session_timeout"
    private const val APP_SETTING_APP_EVENTS_FEATURE_BITMASK = "app_events_feature_bitmask"
    private const val APP_SETTING_APP_EVENTS_EVENT_BINDINGS = "auto_event_mapping_android"
    private const val APP_SETTING_APP_EVENTS_CONFIG = "app_events_config"
    private const val APP_SETTING_RESTRICTIVE_EVENT_FILTER_FIELD = "restrictive_data_filter_params"
    private const val AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD = 1 shl 3

    // The second bit of app_events_feature_bitmask is used for iOS in-app purchase automatic
    // logging, while the fourth bit is used for Android in-app purchase automatic logging.
    private const val IAP_AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD = 1 shl 4
    private const val CODELESS_EVENTS_ENABLED_BITMASK_FIELD = 1 shl 5
    private const val TRACK_UNINSTALL_ENABLED_BITMASK_FIELD = 1 shl 8
    private const val MONITOR_ENABLED_BITMASK_FIELD = 1 shl 14
    private const val APP_SETTING_SMART_LOGIN_OPTIONS = "seamless_login"
    private const val SMART_LOGIN_BOOKMARK_ICON_URL = "smart_login_bookmark_icon_url"
    private const val SMART_LOGIN_MENU_ICON_URL = "smart_login_menu_icon_url"
    private const val SDK_UPDATE_MESSAGE = "sdk_update_message"
    private const val APP_SETTING_APP_EVENTS_AAM_RULE = "aam_rules"
    private const val SUGGESTED_EVENTS_SETTING = "suggested_events_setting"

    private const val PROTECTED_MODE_RULES = "protected_mode_rules"
    private const val STANDARD_PARAMS_KEY = "standard_params"
    private const val MACA_RULES_KEY = "maca_rules"
    private const val BLOCKLIST_EVENTS_KEY = "blocklist_events"
    private const val REDACTED_EVENTS_KEY = "redacted_events"
    private const val SENSITIVE_PARAMS_KEY = "sensitive_params"

    private const val DEDUPE_KEY = "iap_manual_and_auto_log_dedup_keys"
    private const val ANDROID_DEDUPE_KEY = "FBAndroidSDK"
    private const val PROD_DEDUPE_KEY = "prod_keys"
    private const val TEST_DEDUPE_KEY = "test_keys"
    private const val KEY = "key"
    private const val VALUE = "value"

    private const val STD_PARAMS_SCHEMA_KEY = "standard_params_schema"
    private const val STD_PARAMS_BLOCKED_KEY = "standard_params_blocked"


    internal const val AUTO_LOG_APP_EVENTS_DEFAULT_FIELD = "auto_log_app_events_default"
    internal const val AUTO_LOG_APP_EVENT_ENABLED_FIELD = "auto_log_app_events_enabled"

    private val APP_SETTING_FIELDS =
        listOf(
            APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING,
            APP_SETTING_NUX_CONTENT,
            APP_SETTING_NUX_ENABLED,
            APP_SETTING_DIALOG_CONFIGS,
            APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES,
            APP_SETTING_APP_EVENTS_SESSION_TIMEOUT,
            APP_SETTING_APP_EVENTS_FEATURE_BITMASK,
            APP_SETTING_APP_EVENTS_EVENT_BINDINGS,
            APP_SETTING_SMART_LOGIN_OPTIONS,
            SMART_LOGIN_BOOKMARK_ICON_URL,
            SMART_LOGIN_MENU_ICON_URL,
            APP_SETTING_RESTRICTIVE_EVENT_FILTER_FIELD,
            APP_SETTING_APP_EVENTS_AAM_RULE,
            SUGGESTED_EVENTS_SETTING,
            PROTECTED_MODE_RULES,
            AUTO_LOG_APP_EVENTS_DEFAULT_FIELD,
            AUTO_LOG_APP_EVENT_ENABLED_FIELD,
            "${APP_SETTING_APP_EVENTS_CONFIG}.os_version(${android.os.Build.VERSION.RELEASE})"
        )
    private const val APPLICATION_FIELDS = GraphRequest.FIELDS_PARAM
    private val fetchedAppSettings: MutableMap<String, FetchedAppSettings> = ConcurrentHashMap()
    private val loadingState = AtomicReference(FetchAppSettingState.NOT_LOADED)
    private val fetchedAppSettingsCallbacks = ConcurrentLinkedQueue<FetchedAppSettingsCallback>()
    private var printedSDKUpdatedMessage = false
    private var isUnityInit = false
    private var unityEventBindings: JSONArray? = null

    @JvmStatic
    fun loadAppSettingsAsync() {
        val context = FacebookSdk.getApplicationContext()
        val applicationId = FacebookSdk.getApplicationId()
        if (Utility.isNullOrEmpty(applicationId)) {
            loadingState.set(FetchAppSettingState.ERROR)
            pollCallbacks()
            return
        } else if (fetchedAppSettings.containsKey(applicationId)) {
            loadingState.set(FetchAppSettingState.SUCCESS)
            pollCallbacks()
            return
        }
        val canStartLoading =
            loadingState.compareAndSet(
                FetchAppSettingState.NOT_LOADED,
                FetchAppSettingState.LOADING
            ) ||
                    loadingState.compareAndSet(
                        FetchAppSettingState.ERROR,
                        FetchAppSettingState.LOADING
                    )
        if (!canStartLoading) {
            pollCallbacks()
            return
        }
        val settingsKey = String.format(APP_SETTINGS_PREFS_KEY_FORMAT, applicationId)
        FacebookSdk.getExecutor().execute { // See if we had a cached copy and use that immediately.
            val sharedPrefs =
                context.getSharedPreferences(APP_SETTINGS_PREFS_STORE, Context.MODE_PRIVATE)
            val settingsJSONString = sharedPrefs.getString(settingsKey, null)
            var appSettings: FetchedAppSettings? = null
            if (!Utility.isNullOrEmpty(settingsJSONString)) {
                checkNotNull(settingsJSONString)
                var settingsJSON: JSONObject? = null
                try {
                    settingsJSON = JSONObject(settingsJSONString)
                } catch (je: JSONException) {
                    Utility.logd(Utility.LOG_TAG, je)
                }
                if (settingsJSON != null) {
                    appSettings = parseAppSettingsFromJSON(applicationId, settingsJSON)
                }
            }
            val resultJSON = getAppSettingsQueryResponse(applicationId)
            if (resultJSON != null) {
                parseAppSettingsFromJSON(applicationId, resultJSON)
                sharedPrefs.edit().putString(settingsKey, resultJSON.toString()).apply()
            }

            // Print log to notify developers to upgrade SDK when version is too old
            if (appSettings != null) {
                val updateMessage = appSettings.sdkUpdateMessage
                if (!printedSDKUpdatedMessage && updateMessage != null && updateMessage.length > 0) {
                    printedSDKUpdatedMessage = true
                    Log.w(TAG, updateMessage)
                }
            }

            // Fetch GateKeepers
            queryAppGateKeepers(applicationId, true)

            // Start log activate & deactivate app events, in case autoLogAppEvents flag is set
            AutomaticAnalyticsLogger.logActivateAppEvent()

            loadingState.set(
                if (fetchedAppSettings.containsKey(applicationId)) FetchAppSettingState.SUCCESS
                else FetchAppSettingState.ERROR
            )
            pollCallbacks()
        }
    }

    // This call only gets the app settings if they're already fetched
    @JvmStatic
    fun getAppSettingsWithoutQuery(applicationId: String?): FetchedAppSettings? {
        return if (applicationId != null) fetchedAppSettings[applicationId] else null
    }

    /**
     * Run callback with app settings if available. It is possible that app settings take a while to
     * load due to latency or it is requested too early in the application lifecycle.
     *
     * @param callback Callback to be run after app settings are available
     */
    @JvmStatic
    fun getAppSettingsAsync(callback: FetchedAppSettingsCallback) {
        fetchedAppSettingsCallbacks.add(callback)
        loadAppSettingsAsync()
    }

    /**
     * Run all available callbacks and remove them. If app settings are available, run the success
     * callback, error otherwise.
     */
    @Synchronized
    private fun pollCallbacks() {
        val currentState = loadingState.get()
        if (FetchAppSettingState.NOT_LOADED == currentState ||
            FetchAppSettingState.LOADING == currentState
        ) {
            return
        }
        val applicationId = FacebookSdk.getApplicationId()
        val appSettings = fetchedAppSettings[applicationId]
        val handler = Handler(Looper.getMainLooper())
        if (FetchAppSettingState.ERROR == currentState) {
            while (!fetchedAppSettingsCallbacks.isEmpty()) {
                val callback = fetchedAppSettingsCallbacks.poll()
                handler.post { callback.onError() }
            }
            return
        }
        while (!fetchedAppSettingsCallbacks.isEmpty()) {
            val callback = fetchedAppSettingsCallbacks.poll()
            handler.post { callback.onSuccess(appSettings) }
        }
    }

    @JvmStatic
    fun getCachedMigratedAutoLogValuesInAppSettings(): Map<String, Boolean>? {
        val context = FacebookSdk.getApplicationContext()
        val applicationId = FacebookSdk.getApplicationId()
        val settingsKey = String.format(APP_SETTINGS_PREFS_KEY_FORMAT, applicationId)
        val sharedPrefs =
            context.getSharedPreferences(APP_SETTINGS_PREFS_STORE, Context.MODE_PRIVATE)
        val settingsJSONString = sharedPrefs.getString(settingsKey, null)

        if (!Utility.isNullOrEmpty(settingsJSONString)) {
            checkNotNull(settingsJSONString)
            var settingsJSON: JSONObject? = null
            try {
                settingsJSON = JSONObject(settingsJSONString)
            } catch (je: JSONException) {
                Utility.logd(Utility.LOG_TAG, je)
            }
            settingsJSON?.let {
                return parseMigratedAutoLogValues(settingsJSON)
            }
        }
        return null
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread. This call can block for long time if network is not available and network
    // timeout is long.
    @JvmStatic
    fun queryAppSettings(applicationId: String, forceRequery: Boolean): FetchedAppSettings? {
        // Cache the last app checked results.
        if (!forceRequery && fetchedAppSettings.containsKey(applicationId)) {
            return fetchedAppSettings[applicationId]
        }
        val response = getAppSettingsQueryResponse(applicationId) ?: return null
        val fetchedAppSettings = parseAppSettingsFromJSON(applicationId, response)
        if (applicationId == FacebookSdk.getApplicationId()) {
            loadingState.set(FetchAppSettingState.SUCCESS)
            pollCallbacks()
        }
        return fetchedAppSettings
    }

    internal fun parseAppSettingsFromJSON(
        applicationId: String,
        settingsJSON: JSONObject
    ): FetchedAppSettings {
        val errorClassificationJSON =
            settingsJSON.optJSONArray(APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES)
        val errorClassification =
            FacebookRequestErrorClassification.createFromJSON(errorClassificationJSON)
                ?: FacebookRequestErrorClassification.defaultErrorClassification
        val featureBitmask = settingsJSON.optInt(APP_SETTING_APP_EVENTS_FEATURE_BITMASK, 0)
        val automaticLoggingEnabled =
            featureBitmask and AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD != 0
        val inAppPurchaseAutomaticLoggingEnabled =
            featureBitmask and IAP_AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD != 0
        val codelessEventsEnabled = featureBitmask and CODELESS_EVENTS_ENABLED_BITMASK_FIELD != 0
        val trackUninstallEnabled = featureBitmask and TRACK_UNINSTALL_ENABLED_BITMASK_FIELD != 0
        val monitorEnabled = featureBitmask and MONITOR_ENABLED_BITMASK_FIELD != 0
        val eventBindings = settingsJSON.optJSONArray(APP_SETTING_APP_EVENTS_EVENT_BINDINGS)
        unityEventBindings = eventBindings
        if (unityEventBindings != null && isUnityApp) {
            UnityReflection.sendEventMapping(eventBindings?.toString())
        }
        val appEventsConfig = settingsJSON.optJSONObject(
            APP_SETTING_APP_EVENTS_CONFIG
        )
        val result =
            FetchedAppSettings(
                settingsJSON.optBoolean(APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING, false),
                settingsJSON.optString(APP_SETTING_NUX_CONTENT, ""),
                settingsJSON.optBoolean(APP_SETTING_NUX_ENABLED, false),
                settingsJSON.optInt(
                    APP_SETTING_APP_EVENTS_SESSION_TIMEOUT,
                    Constants.getDefaultAppEventsSessionTimeoutInSeconds()
                ),
                parseOptions(settingsJSON.optLong(APP_SETTING_SMART_LOGIN_OPTIONS)),
                parseDialogConfigurations(settingsJSON.optJSONObject(APP_SETTING_DIALOG_CONFIGS)),
                automaticLoggingEnabled,
                errorClassification,
                settingsJSON.optString(SMART_LOGIN_BOOKMARK_ICON_URL),
                settingsJSON.optString(SMART_LOGIN_MENU_ICON_URL),
                inAppPurchaseAutomaticLoggingEnabled,
                codelessEventsEnabled,
                eventBindings,
                settingsJSON.optString(SDK_UPDATE_MESSAGE),
                trackUninstallEnabled,
                monitorEnabled,
                settingsJSON.optString(APP_SETTING_APP_EVENTS_AAM_RULE),
                settingsJSON.optString(SUGGESTED_EVENTS_SETTING),
                settingsJSON.optString(APP_SETTING_RESTRICTIVE_EVENT_FILTER_FIELD),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    STANDARD_PARAMS_KEY
                ),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    MACA_RULES_KEY
                ),
                parseMigratedAutoLogValues(settingsJSON),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    BLOCKLIST_EVENTS_KEY
                ),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    REDACTED_EVENTS_KEY
                ),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    SENSITIVE_PARAMS_KEY
                ),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    STD_PARAMS_SCHEMA_KEY
                ),
                parseProtectedModeRules(
                    settingsJSON.optJSONObject(PROTECTED_MODE_RULES),
                    STD_PARAMS_BLOCKED_KEY
                ),
                parseCurrencyAndValueDedupeParameters(
                    appEventsConfig,
                    AppEventsConstants.EVENT_PARAM_CURRENCY
                ),
                parseCurrencyAndValueDedupeParameters(
                    appEventsConfig,
                    AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM
                ),
                parseDedupeParameters(appEventsConfig),
                parseDedupeParameters(appEventsConfig, getTestValues = true)
            )
        fetchedAppSettings[applicationId] = result
        return result
    }

    @JvmStatic
    fun setIsUnityInit(flag: Boolean) {
        isUnityInit = flag
        if (unityEventBindings != null && isUnityInit) {
            UnityReflection.sendEventMapping(unityEventBindings.toString())
        }
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread. This call can block for long time if network is not available and network
    // timeout is long.
    private fun getAppSettingsQueryResponse(applicationId: String): JSONObject {
        val appSettingsParams = Bundle()
        val appSettingFields = mutableListOf<String>().apply { addAll(APP_SETTING_FIELDS) }
        if (BuildConfig.DEBUG) {
            appSettingFields.add(SDK_UPDATE_MESSAGE)
        }
        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", appSettingFields))
        val request = GraphRequest.newGraphPathRequest(null, "app", null)
        request.setForceApplicationRequest(true)
        request.parameters = appSettingsParams

        return request.executeAndWait().jsonObject ?: JSONObject()
    }

    private fun parseDialogConfigurations(
        dialogConfigResponse: JSONObject?
    ): Map<String, MutableMap<String, FetchedAppSettings.DialogFeatureConfig>> {
        val dialogConfigMap =
            HashMap<String, MutableMap<String, FetchedAppSettings.DialogFeatureConfig>>()
        if (dialogConfigResponse != null) {
            val dialogConfigData = dialogConfigResponse.optJSONArray("data")
            if (dialogConfigData != null) {
                for (i in 0 until dialogConfigData.length()) {
                    val dialogConfig =
                        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
                            dialogConfigData.optJSONObject(i)
                        )
                            ?: continue
                    val dialogName = dialogConfig.dialogName
                    var featureMap = dialogConfigMap[dialogName]
                    if (featureMap == null) {
                        featureMap = HashMap()
                        dialogConfigMap[dialogName] = featureMap
                    }
                    featureMap[dialogConfig.featureName] = dialogConfig
                }
            }
        }
        return dialogConfigMap
    }

    private fun parseProtectedModeRules(
        protectedModeSettings: JSONObject?,
        ruleType: String,
    ): JSONArray? {
        var rule: JSONArray? = null
        if (protectedModeSettings != null) {
            rule = protectedModeSettings.optJSONArray(ruleType)
        }
        return rule
    }

    private fun parseCurrencyAndValueDedupeParameters(
        originalJSON: JSONObject?,
        key: String
    ): List<String>? {
        try {
            var resultList: ArrayList<String>? = null
            val dedupeConfigs = originalJSON?.getJSONArray(DEDUPE_KEY) ?: return null
            for (index in 0 until dedupeConfigs.length()) {
                val config = dedupeConfigs.getJSONObject(index)
                val configKey = config.getString(KEY)
                if (configKey != PROD_DEDUPE_KEY) {
                    continue
                }
                val configValue = config.getJSONArray(VALUE)
                for (keyMappingIndex in 0 until configValue.length()) {
                    val keyMapping = configValue.getJSONObject(keyMappingIndex)
                    val implicitKey = keyMapping.getString(KEY)
                    if (implicitKey != key) {
                        continue
                    }
                    val manualKeyArray = keyMapping.getJSONArray(VALUE)
                    val manualKeys = ArrayList<String>()
                    for (manualKeyIndex in 0 until manualKeyArray.length()) {
                        val manualKeyJSONObject = manualKeyArray.getJSONObject(manualKeyIndex)
                        val manualKey = manualKeyJSONObject.getString(VALUE)
                        manualKeys.add(manualKey)
                    }
                    resultList = ArrayList()
                    resultList.addAll(manualKeys)
                    return resultList
                }
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDedupeParameters(
        originalJSON: JSONObject?,
        getTestValues: Boolean = false
    ): ArrayList<Pair<String, List<String>>>? {
        try {
            var dedupeParameters: ArrayList<Pair<String, List<String>>>? = null
            val dedupeConfigs = originalJSON?.getJSONArray(DEDUPE_KEY) ?: return null
            for (index in 0 until dedupeConfigs.length()) {
                val config = dedupeConfigs.getJSONObject(index)
                val configKey = config.getString(KEY)
                if ((configKey == PROD_DEDUPE_KEY && getTestValues)
                    || (configKey == TEST_DEDUPE_KEY && !getTestValues)
                ) {
                    continue
                }
                val configValue = config.getJSONArray(VALUE)
                for (keyMappingIndex in 0 until configValue.length()) {
                    val keyMapping = configValue.getJSONObject(keyMappingIndex)
                    val implicitKey = keyMapping.getString(KEY)
                    if (implicitKey == AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM ||
                        implicitKey == AppEventsConstants.EVENT_PARAM_CURRENCY
                    ) {
                        continue
                    }
                    val manualKeyArray = keyMapping.getJSONArray(VALUE)
                    val manualKeys = ArrayList<String>()
                    for (manualKeyIndex in 0 until manualKeyArray.length()) {
                        val manualKeyJSONObject = manualKeyArray.getJSONObject(manualKeyIndex)
                        val manualKey = manualKeyJSONObject.getString(VALUE)
                        manualKeys.add(manualKey)
                    }
                    if (dedupeParameters == null) {
                        dedupeParameters = ArrayList()
                    }
                    dedupeParameters.add(Pair(implicitKey, manualKeys))
                }
            }
            return dedupeParameters
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseMigratedAutoLogValues(settingsJSON: JSONObject?): Map<String, Boolean>? {
        if (settingsJSON == null) {
            return null
        }

        var values: MutableMap<String, Boolean> = HashMap()
        if (!settingsJSON?.isNull(AUTO_LOG_APP_EVENTS_DEFAULT_FIELD)) {
            try {
                values[AUTO_LOG_APP_EVENTS_DEFAULT_FIELD] =
                    settingsJSON?.getBoolean(AUTO_LOG_APP_EVENTS_DEFAULT_FIELD)
            } catch (je: JSONException) {
                Utility.logd(Utility.LOG_TAG, je)
            }
        }
        if (!settingsJSON?.isNull(AUTO_LOG_APP_EVENT_ENABLED_FIELD)) {
            try {
                values[AUTO_LOG_APP_EVENT_ENABLED_FIELD] =
                    settingsJSON?.getBoolean(AUTO_LOG_APP_EVENT_ENABLED_FIELD)
            } catch (je: JSONException) {
                Utility.logd(Utility.LOG_TAG, je)
            }
        }

        return if (values.isEmpty()) {
            null
        } else {
            values
        }
    }

    internal enum class FetchAppSettingState {
        NOT_LOADED,
        LOADING,
        SUCCESS,
        ERROR
    }

    interface FetchedAppSettingsCallback {
        fun onSuccess(fetchedAppSettings: FetchedAppSettings?)
        fun onError()
    }
}
