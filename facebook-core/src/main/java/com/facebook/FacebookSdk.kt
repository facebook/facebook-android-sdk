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

package com.facebook

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.appevents.AppEventsLogger
import com.facebook.appevents.AppEventsManager
import com.facebook.appevents.internal.ActivityLifecycleTracker.startTracking
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager
import com.facebook.core.BuildConfig
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import com.facebook.internal.BoltsMeasurementEventListener
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppSettingsManager.loadAppSettingsAsync
import com.facebook.internal.LockOnGetVariable
import com.facebook.internal.NativeProtocol.updateAllAvailableProtocolVersionsAsync
import com.facebook.internal.ServerProtocol.getDefaultAPIVersion
import com.facebook.internal.Utility
import com.facebook.internal.Validate
import com.facebook.internal.instrument.InstrumentManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashSet
import kotlin.collections.Set
import kotlin.collections.hashSetOf
import kotlin.collections.isEmpty
import kotlin.collections.toList
import kotlin.concurrent.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** This class allows some customization of Facebook SDK behavior. */
object FacebookSdk {
  private val TAG = FacebookSdk::class.java.canonicalName
  private val loggingBehaviors = hashSetOf(LoggingBehavior.DEVELOPER_ERRORS)
  private const val DEFAULT_CALLBACK_REQUEST_CODE_OFFSET = 0xface
  private var executor: Executor? = null
  @Volatile private var applicationId: String? = null
  @Volatile private var applicationName: String? = null
  @Volatile private var appClientToken: String? = null
  @Volatile private var codelessDebugLogEnabled: Boolean? = null
  private var onProgressThreshold = AtomicLong(65536)
  @Volatile private var isDebugEnabledField = BuildConfig.DEBUG
  private var isLegacyTokenUpgradeSupported = false
  private lateinit var cacheDir: LockOnGetVariable<File>
  private lateinit var applicationContext: Context
  private var callbackRequestCodeOffset = DEFAULT_CALLBACK_REQUEST_CODE_OFFSET
  private val LOCK = ReentrantLock()
  private var graphApiVersion = getDefaultAPIVersion()
  private const val MAX_REQUEST_CODE_RANGE = 100
  private const val ATTRIBUTION_PREFERENCES = "com.facebook.sdk.attributionTracking"
  private const val PUBLISH_ACTIVITY_PATH = "%s/activities"
  const val CALLBACK_OFFSET_CHANGED_AFTER_INIT =
      "The callback request code offset can't be updated once the SDK is initialized. " +
          "Call FacebookSdk.setCallbackRequestCodeOffset inside your Application.onCreate method"
  const val CALLBACK_OFFSET_NEGATIVE = "The callback request code offset can't be negative."

  /** The key for AppEvent perfernece setting. */
  const val APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences"

  /** The key for the data processing options preference setting. */
  const val DATA_PROCESSING_OPTIONS_PREFERENCES = "com.facebook.sdk.DataProcessingOptions"

  /** The key for the application ID in the Android manifest. */
  const val APPLICATION_ID_PROPERTY = "com.facebook.sdk.ApplicationId"

  /** The key for the application name in the Android manifest. */
  const val APPLICATION_NAME_PROPERTY = "com.facebook.sdk.ApplicationName"

  /** The key for the client token in the Android manifest. */
  const val CLIENT_TOKEN_PROPERTY = "com.facebook.sdk.ClientToken"

  /** The key for the web dialog theme in the Android manifest. */
  const val WEB_DIALOG_THEME = "com.facebook.sdk.WebDialogTheme"

  /** The key for the auto init SDK in the Android manifest. */
  const val AUTO_INIT_ENABLED_PROPERTY = "com.facebook.sdk.AutoInitEnabled"

  /** The key for the auto logging app events in the Android manifest. */
  const val AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY = "com.facebook.sdk.AutoLogAppEventsEnabled"

  /** The key for the auto log codeless in the Android manifest. */
  const val CODELESS_DEBUG_LOG_ENABLED_PROPERTY = "com.facebook.sdk.CodelessDebugLogEnabled"

  /** The key for the advertiserID collection in the Android manifest. */
  const val ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY =
      "com.facebook.sdk.AdvertiserIDCollectionEnabled"

  /** The key for the callback off set in the Android manifest. */
  const val CALLBACK_OFFSET_PROPERTY = "com.facebook.sdk.CallbackOffset"

  /** The key for the monitor enable in the Android manifest. */
  const val MONITOR_ENABLED_PROPERTY = "com.facebook.sdk.MonitorEnabled"

  /** The key for modes in data processing options. */
  const val DATA_PROCESSION_OPTIONS = "data_processing_options"

  /** The key for country in data processing options. */
  const val DATA_PROCESSION_OPTIONS_COUNTRY = "data_processing_options_country"

  /** The key for state in data processing options. */
  const val DATA_PROCESSION_OPTIONS_STATE = "data_processing_options_state"
  @JvmField var hasCustomTabsPrefetching = false
  @JvmField var ignoreAppSwitchToLoggedOut = false
  @JvmField var bypassAppSwitch = false
  const val INSTAGRAM = "instagram"
  const val GAMING = "gaming"
  const val FACEBOOK_COM = "facebook.com"
  const val FB_GG = "fb.gg"
  const val INSTAGRAM_COM = "instagram.com"
  private val sdkInitialized = AtomicBoolean(false)
  @Volatile private var instagramDomain = INSTAGRAM_COM
  @Volatile private var facebookDomain = FACEBOOK_COM
  private var graphRequestCreator: GraphRequestCreator =
      GraphRequestCreator { accessToken, publishUrl, publishParams, callback ->
    GraphRequest.newPostRequest(accessToken, publishUrl, publishParams, callback)
  }
  private var isFullyInitialized = false

  /**
   * Returns the Executor used by the SDK for non-AsyncTask background work.
   *
   * By default this uses AsyncTask Executor via reflection if the API level is high enough.
   * Otherwise this creates a new Executor with defaults similar to those used in AsyncTask.
   *
   * @return an Executor used by the SDK. This will never be null.
   */
  @JvmStatic
  fun getExecutor(): Executor {
    LOCK.withLock {
      if (executor == null) {
        executor = AsyncTask.THREAD_POOL_EXECUTOR
      }
    }
    return checkNotNull(executor)
  }

  /**
   * Sets the Executor used by the SDK for non-AsyncTask background work.
   *
   * @param executor the Executor to use; must not be null.
   */
  @JvmStatic
  fun setExecutor(executor: Executor) {
    LOCK.withLock { this.executor = executor }
  }

  /** Gets the threshold used to report progress on requests. */
  @JvmStatic
  fun getOnProgressThreshold(): Long {
    Validate.sdkInitialized()
    return onProgressThreshold.get()
  }

  /**
   * Sets the threshold used to report progress on requests. Note that the value will be read when
   * the request is started and cannot be changed during a request (or batch) execution.
   *
   * @param threshold The number of bytes progressed to force a callback.
   */
  @JvmStatic
  fun setOnProgressThreshold(threshold: Long) {
    onProgressThreshold.set(threshold)
  }

  /** Indicates if we are in debug mode. */
  @JvmStatic fun isDebugEnabled(): Boolean = isDebugEnabledField

  /**
   * Used to enable or disable logging, and other debug features. Defaults to BuildConfig.DEBUG.
   *
   * @param enabled Debug features (like logging) are enabled if true, disabled if false.
   */
  @JvmStatic
  fun setIsDebugEnabled(enabled: Boolean) {
    isDebugEnabledField = enabled
  }

  /**
   * Indicates if the SDK should fallback and read the legacy token. This is turned off by default
   * for performance.
   *
   * @return if the legacy token upgrade is supported.
   */
  @JvmStatic fun isLegacyTokenUpgradeSupported(): Boolean = isLegacyTokenUpgradeSupported

  /**
   * Setter for legacy token upgrade.
   *
   * @param supported True if upgrade should be supported.
   */
  @JvmStatic
  fun setLegacyTokenUpgradeSupported(supported: Boolean) {
    isLegacyTokenUpgradeSupported = supported
  }

  /**
   * Returns the Graph API version to use when making Graph requests. This defaults to the latest
   * Graph API version at the time when the Facebook SDK is shipped.
   *
   * @return the Graph API version to use.
   */
  @JvmStatic
  fun getGraphApiVersion(): String {
    Utility.logd(TAG, String.format("getGraphApiVersion: %s", graphApiVersion))
    return graphApiVersion
  }

  /**
   * Sets the Graph API version to use when making Graph requests. This defaults to the latest Graph
   * API version at the time when the Facebook SDK is shipped.
   *
   * @param graphApiVersion the Graph API version, it should be of the form
   * ServerProtocol.getDefaultAPIVersion()
   */
  @JvmStatic
  fun setGraphApiVersion(graphApiVersion: String) {
    if (!BuildConfig.DEBUG) {
      Log.w(TAG, "WARNING: Calling setGraphApiVersion from non-DEBUG code.")
    }
    if (!Utility.isNullOrEmpty(graphApiVersion) && FacebookSdk.graphApiVersion != graphApiVersion) {
      FacebookSdk.graphApiVersion = graphApiVersion
    }
  }

  /**
   * Indicates whether the Facebook SDK has been fully initialized.
   *
   * Facebook SDK won't work before fully initialized.
   *
   * @return true if fully initialized, false if not
   */
  @Synchronized
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @JvmStatic
  fun isFullyInitialized(): Boolean = isFullyInitialized

  /**
   * Gets the base Facebook domain to use when making Web Requests; in production code this will
   * always be "facebook.com".
   *
   * This is required for PlatformDialogs which always need to use facebook.com
   *
   * @return the Facebook Domain
   */
  @JvmStatic fun getFacebookDomain(): String = facebookDomain

  /**
   * Gets the base Instagram domain to use when making Web Requests; in production code this will
   * always be "instagram.com".
   *
   * @return the Instagram Domain
   */
  @JvmStatic fun getInstagramDomain(): String = instagramDomain

  /**
   * Sets the base Facebook domain to use when making Web requests. This defaults to "facebook.com",
   * but may be overridden to, e.g., "beta.facebook.com" to direct requests at a different domain.
   * This method should never be called from production code.
   *
   * Updating this will also affect getGraphDomain calls. Setting "beta.facebook.com" will return
   * beta.fb.gg if using the gaming domain for example.
   *
   * @param facebookDomain the base domain to use instead of "facebook.com"
   */
  @JvmStatic
  fun setFacebookDomain(facebookDomain: String) {
    if (!BuildConfig.DEBUG) {
      Log.w(TAG, "WARNING: Calling setFacebookDomain from non-DEBUG code.")
    }
    this.facebookDomain = facebookDomain
  }

  /**
   * This function initializes the Facebook SDK. This function is called automatically on app start
   * up if the proper entries are listed in the AndroidManifest, such as the facebook app id. This
   * method can be called manually if needed. The behavior of Facebook SDK functions are
   * undetermined if this function is not called. It should be called as early as possible. As part
   * of SDK initialization basic auto logging of app events will occur, this can be controlled via
   * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
   *
   * @param applicationContext The application context
   * @param callbackRequestCodeOffset The request code offset that Facebook activities will be
   * called with. Please do not use the range between the value you set and another 100 entries
   * after it in your other requests. @Deprecated [sdkInitialize] and [AppEventsLogger.activateApp]
   * are called automatically on application start. Automatic event logging from 'activateApp' can
   * be controlled via the 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting. The
   * callbackRequestCodeOffset can be set in the AndroidManifest as a meta data entry with the name
   * [CALLBACK_OFFSET_PROPERTY].
   */
  @Deprecated("")
  @Synchronized
  @JvmStatic
  fun sdkInitialize(applicationContext: Context, callbackRequestCodeOffset: Int) {
    sdkInitialize(applicationContext, callbackRequestCodeOffset, null)
  }

  /**
   * This function initializes the Facebook SDK. This function is called automatically on app start
   * up if the proper entries are listed in the AndroidManifest, such as the facebook app id. This
   * method can be called manually if needed. The behavior of Facebook SDK functions are
   * undetermined if this function is not called. It should be called as early as possible. As part
   * of SDK initialization basic auto logging of app events will occur, this can be controlled via
   * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
   *
   * @param applicationContext The application context
   * @param callbackRequestCodeOffset The request code offset that Facebook activities will be
   * called with. Please do not use the range between the value you set and another 100 entries
   * after it in your other requests.
   * @param callback A callback called when initialize finishes. This will be called even if the sdk
   * is already initialized. @Deprecated [sdkInitialize] and [AppEventsLogger.activateApp] are
   * called automatically on application start. Automatic event logging from 'activateApp' can be
   * controlled via the 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting. The
   * callbackRequestCodeOffset can be set in the AndroidManifest as a meta data entry with the name
   * [CALLBACK_OFFSET_PROPERTY].
   */
  @Deprecated("")
  @Synchronized
  @JvmStatic
  fun sdkInitialize(
      applicationContext: Context,
      callbackRequestCodeOffset: Int,
      callback: InitializeCallback?
  ) {
    if (sdkInitialized.get() &&
        callbackRequestCodeOffset != FacebookSdk.callbackRequestCodeOffset) {
      throw FacebookException(CALLBACK_OFFSET_CHANGED_AFTER_INIT)
    }
    if (callbackRequestCodeOffset < 0) {
      throw FacebookException(CALLBACK_OFFSET_NEGATIVE)
    }
    FacebookSdk.callbackRequestCodeOffset = callbackRequestCodeOffset
    sdkInitialize(applicationContext, callback)
  }

  /**
   * This function initializes the Facebook SDK. This function is called automatically on app start
   * up if the proper entries are listed in the AndroidManifest, such as the facebook app id. This
   * method can be called manually if needed. The behavior of Facebook SDK functions are
   * undetermined if this function is not called. It should be called as early as possible. As part
   * of SDK initialization basic auto logging of app events will occur, this can be controlled via
   * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
   *
   * @param applicationContext The application context @Deprecated [sdkInitialize] and
   * [AppEventsLogger.activateApp] are called automatically on application start. Automatic event
   * logging from 'activateApp' can be controlled via the 'com.facebook.sdk.AutoLogAppEventsEnabled'
   * manifest setting.
   */
  @Deprecated("")
  @Synchronized
  @JvmStatic
  fun sdkInitialize(applicationContext: Context) {
    sdkInitialize(applicationContext, null)
  }

  /**
   * This function initializes the Facebook SDK. This function is called automatically on app start
   * up if the proper entries are listed in the AndroidManifest, such as the facebook app id. This
   * method can bee called manually if needed. The behavior of Facebook SDK functions are
   * undetermined if this function is not called. It should be called as early as possible. As part
   * of SDK initialization basic auto logging of app events will occur, this can be controlled via
   * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
   *
   * @param applicationContext The application context
   * @param callback A callback called when initialize finishes. This will be called even if the sdk
   * is already initialized. @Deprecated [sdkInitialize] and [AppEventsLogger.activateApp] are
   * called automatically on application start. Automatic event logging from 'activateApp' can be
   * controlled via the 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting.
   */
  @Deprecated("")
  @Synchronized
  @JvmStatic
  fun sdkInitialize(applicationContext: Context, callback: InitializeCallback?) {
    if (sdkInitialized.get()) {
      callback?.onInitialized()
      return
    }

    // Don't throw for these validations here, just log an error. We'll throw when we actually
    // need them
    Validate.hasFacebookActivity(applicationContext, false)
    Validate.hasInternetPermissions(applicationContext, false)
    FacebookSdk.applicationContext = applicationContext.applicationContext

    // Make sure anon_id doesn't get overridden
    AppEventsLogger.getAnonymousAppDeviceGUID(applicationContext)

    // Make sure we've loaded default settings if we haven't already.
    loadDefaultsFromMetadata(FacebookSdk.applicationContext)

    // We should have an application id by now if not throw
    if (Utility.isNullOrEmpty(applicationId)) {
      throw FacebookException(
          "A valid Facebook app id must be set in the " +
              "AndroidManifest.xml or set by calling FacebookSdk.setApplicationId " +
              "before initializing the sdk.")
    }

    // Set sdkInitialized to true now so the bellow async tasks don't throw not initialized
    // exceptions.
    sdkInitialized.set(true)

    // Set sdkFullyInitialzed if auto init enabled.
    if (getAutoInitEnabled()) {
      fullyInitialize()
    }

    // Register ActivityLifecycleTracker callbacks now, so will log activate app event properly
    if (FacebookSdk.applicationContext is Application &&
        UserSettingsManager.getAutoLogAppEventsEnabled()) {
      startTracking(FacebookSdk.applicationContext as Application, applicationId)
    }

    // Load app settings from network so that dialog configs are available
    loadAppSettingsAsync()

    // Fetch available protocol versions from the apps on the device
    updateAllAvailableProtocolVersionsAsync()
    BoltsMeasurementEventListener.getInstance(FacebookSdk.applicationContext)
    cacheDir = LockOnGetVariable<File> { FacebookSdk.applicationContext.cacheDir }
    FeatureManager.checkFeature(FeatureManager.Feature.Instrument) { enabled ->
      if (enabled) {
        InstrumentManager.start()
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.AppEvents) { enabled ->
      if (enabled) {
        AppEventsManager.start()
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.ChromeCustomTabsPrefetching) { enabled ->
      if (enabled) {
        hasCustomTabsPrefetching = true
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.IgnoreAppSwitchToLoggedOut) { enabled ->
      if (enabled) {
        ignoreAppSwitchToLoggedOut = true
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.BypassAppSwitch) { enabled ->
      if (enabled) {
        bypassAppSwitch = true
      }
    }
    val futureTask =
        FutureTask<Void> {
          AccessTokenManager.getInstance().loadCurrentAccessToken()
          ProfileManager.getInstance().loadCurrentProfile()
          if (AccessToken.isCurrentAccessTokenActive() && Profile.getCurrentProfile() == null) {
            // Access token and profile went out of sync due to a network or caching
            // issue, retry
            Profile.fetchProfileForCurrentAccessToken()
          }
          callback?.onInitialized()
          AppEventsLogger.initializeLib(getApplicationContext(), applicationId)
          UserSettingsManager.logIfAutoAppLinkEnabled()

          // Flush any app events that might have been persisted during last run.
          AppEventsLogger.newLogger(getApplicationContext().applicationContext).flush()
          null
        }
    getExecutor().execute(futureTask)
  }

  /**
   * Indicates whether the Facebook SDK has been initialized.
   *
   * @return true if initialized, false if not
   */
  @JvmStatic fun isInitialized(): Boolean = sdkInitialized.get()

  /** Mark Facebook SDK fully initialized to make it works as expected. */
  @JvmStatic
  fun fullyInitialize() {
    isFullyInitialized = true
  }

  /**
   * Certain logging behaviors are available for debugging beyond those that should be enabled in
   * production.
   *
   * Returns the types of extended logging that are currently enabled.
   *
   * @return a set containing enabled logging behaviors
   */
  @JvmStatic
  fun getLoggingBehaviors(): Set<LoggingBehavior> {
    synchronized(loggingBehaviors) {
      return Collections.unmodifiableSet(HashSet(loggingBehaviors))
    }
  }

  /**
   * Certain logging behaviors are available for debugging beyond those that should be enabled in
   * production.
   *
   * Enables a particular extended logging in the SDK.
   *
   * @param behavior The LoggingBehavior to enable
   */
  @JvmStatic
  fun addLoggingBehavior(behavior: LoggingBehavior) {
    synchronized(loggingBehaviors) {
      loggingBehaviors.add(behavior)
      updateGraphDebugBehavior()
    }
  }

  /**
   * Certain logging behaviors are available for debugging beyond those that should be enabled in
   * production.
   *
   * Disables a particular extended logging behavior in the SDK.
   *
   * @param behavior The LoggingBehavior to disable
   */
  @JvmStatic
  fun removeLoggingBehavior(behavior: LoggingBehavior) {
    synchronized(loggingBehaviors) { loggingBehaviors.remove(behavior) }
  }

  /**
   * Certain logging behaviors are available for debugging beyond those that should be enabled in
   * production.
   *
   * Disables all extended logging behaviors.
   */
  @JvmStatic
  fun clearLoggingBehaviors() {
    synchronized(loggingBehaviors) { loggingBehaviors.clear() }
  }

  /**
   * Certain logging behaviors are available for debugging beyond those that should be enabled in
   * production.
   *
   * Checks if a particular extended logging behavior is enabled.
   *
   * @param behavior The LoggingBehavior to check
   * @return whether behavior is enabled
   */
  @JvmStatic
  fun isLoggingBehaviorEnabled(behavior: LoggingBehavior): Boolean {
    synchronized(loggingBehaviors) {
      return isDebugEnabled() && loggingBehaviors.contains(behavior)
    }
  }

  private fun updateGraphDebugBehavior() {
    if (loggingBehaviors.contains(LoggingBehavior.GRAPH_API_DEBUG_INFO) &&
        !loggingBehaviors.contains(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
      loggingBehaviors.add(LoggingBehavior.GRAPH_API_DEBUG_WARNING)
    }
  }

  /**
   * Gets the base Facebook domain to use when making Graph API requests; in production code this
   * will normally be "facebook.com". However certain Access Tokens are meant to be used with other
   * domains. Currently gaming -> fb.gg
   *
   * This checks the current Access Token (if any) and returns the correct domain to use.
   *
   * @return the Graph API Domain
   */
  @JvmStatic
  fun getGraphDomain(): String {
    val currentToken = AccessToken.getCurrentAccessToken()
    var tokenGraphDomain: String? = null
    if (currentToken != null) {
      tokenGraphDomain = currentToken.graphDomain
    }
    return Utility.getGraphDomainFromTokenDomain(tokenGraphDomain)
  }

  /**
   * The getter for the context of the current application.
   *
   * @return The context of the current application.
   */
  @JvmStatic
  fun getApplicationContext(): Context {
    Validate.sdkInitialized()
    return applicationContext
  }

  /**
   * This method is public in order to be used by app events, please don't use directly.
   *
   * @param context The application context.
   * @param applicationId The application id.
   */
  @AutoHandleExceptions
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @JvmStatic
  fun publishInstallAsync(context: Context, applicationId: String) {
    // grab the application context ahead of time, since we will return to the caller
    // immediately.
    val applicationContext = context.applicationContext
    getExecutor().execute { publishInstallAndWaitForResponse(applicationContext, applicationId) }
    if (FeatureManager.isEnabled(FeatureManager.Feature.OnDeviceEventProcessing) &&
        OnDeviceProcessingManager.isOnDeviceProcessingEnabled()) {
      OnDeviceProcessingManager.sendInstallEventAsync(applicationId, ATTRIBUTION_PREFERENCES)
    }
  }

  @AutoHandleExceptions
  private fun publishInstallAndWaitForResponse(context: Context, applicationId: String) {
    try {
      val identifiers = getAttributionIdentifiers(context)
      val preferences = context.getSharedPreferences(ATTRIBUTION_PREFERENCES, Context.MODE_PRIVATE)
      val pingKey = applicationId + "ping"
      var lastPing = preferences.getLong(pingKey, 0)
      val publishParams =
          try {
            AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
                AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
                identifiers,
                AppEventsLogger.getAnonymousAppDeviceGUID(context),
                getLimitEventAndDataUsage(context),
                context)
          } catch (e: JSONException) {
            throw FacebookException("An error occurred while publishing install.", e)
          }
      val publishUrl = String.format(PUBLISH_ACTIVITY_PATH, applicationId)
      val publishRequest =
          graphRequestCreator.createPostRequest(null, publishUrl, publishParams, null)
      if (lastPing == 0L) {
        // send install event only if have not sent before
        val publishResponse = publishRequest.executeAndWait()
        if (publishResponse.error == null) {
          // denote success since there is no error in response of the post.
          val editor = preferences.edit()
          lastPing = System.currentTimeMillis()
          editor.putLong(pingKey, lastPing)
          editor.apply()
        }
      }
    } catch (e: Exception) {
      // if there was an error, fall through to the failure case.
      Utility.logd("Facebook-publish", e)
    }
  }

  /**
   * Returns the current version of the Facebook SDK for Android as a string.
   *
   * @return the current version of the SDK
   */
  @JvmStatic
  fun getSdkVersion(): String {
    return FacebookSdkVersion.BUILD
  }

  /**
   * Returns whether data such as those generated through AppEventsLogger and sent to Facebook
   * should be restricted from being used for purposes other than analytics and conversions, such as
   * targeting ads to this user. Defaults to false. This value is stored on the device and persists
   * across app launches.
   *
   * @param context Used to read the value.
   */
  @JvmStatic
  fun getLimitEventAndDataUsage(context: Context): Boolean {
    Validate.sdkInitialized()
    val preferences = context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
    return preferences.getBoolean("limitEventUsage", false)
  }

  /**
   * Sets whether data such as those generated through AppEventsLogger and sent to Facebook should
   * be restricted from being used for purposes other than analytics and conversions, such as
   * targeting ads to this user. Defaults to false. This value is stored on the device and persists
   * across app launches. Changes to this setting will apply to app events currently queued to be
   * flushed.
   *
   * @param context Used to persist this value across app runs.
   */
  @JvmStatic
  fun setLimitEventAndDataUsage(context: Context, limitEventUsage: Boolean) {
    context
        .getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("limitEventUsage", limitEventUsage)
        .apply()
  }

  @JvmStatic
  internal fun loadDefaultsFromMetadata(context: Context?) {
    if (context == null) {
      return
    }
    val ai =
        try {
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
          return
        }
    if (ai.metaData == null) {
      return
    }
    if (applicationId == null) {
      val appId = ai.metaData[APPLICATION_ID_PROPERTY]
      if (appId is String) {
        val appIdString = appId
        if (appIdString.toLowerCase(Locale.ROOT).startsWith("fb")) {
          applicationId = appIdString.substring(2)
        } else {
          applicationId = appIdString
        }
      } else if (appId is Number) {
        throw FacebookException(
            "App Ids cannot be directly placed in the manifest." +
                "They must be prefixed by 'fb' or be placed in the string resource file.")
      }
    }
    if (applicationName == null) {
      applicationName = ai.metaData.getString(APPLICATION_NAME_PROPERTY)
    }
    if (appClientToken == null) {
      appClientToken = ai.metaData.getString(CLIENT_TOKEN_PROPERTY)
    }
    if (callbackRequestCodeOffset == DEFAULT_CALLBACK_REQUEST_CODE_OFFSET) {
      callbackRequestCodeOffset =
          ai.metaData.getInt(CALLBACK_OFFSET_PROPERTY, DEFAULT_CALLBACK_REQUEST_CODE_OFFSET)
    }
    if (codelessDebugLogEnabled == null) {
      codelessDebugLogEnabled = ai.metaData.getBoolean(CODELESS_DEBUG_LOG_ENABLED_PROPERTY, false)
    }
  }

  /**
   * Internal call please don't use directly.
   *
   * @param context The application context.
   * @return The application signature.
   */
  @AutoHandleExceptions
  @JvmStatic
  fun getApplicationSignature(context: Context?): String? {
    Validate.sdkInitialized()
    if (context == null) {
      return null
    }
    val packageManager = context.packageManager ?: return null
    val packageName = context.packageName
    val packageInfo =
        try {
          packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
          return null
        }
    val signatures = packageInfo.signatures
    if (signatures == null || signatures.isEmpty()) {
      return null
    }
    val md =
        try {
          MessageDigest.getInstance("SHA-1")
        } catch (e: NoSuchAlgorithmException) {
          return null
        }
    md.update(packageInfo.signatures[0].toByteArray())
    return Base64.encodeToString(md.digest(), Base64.URL_SAFE or Base64.NO_PADDING)
  }

  /**
   * Gets the Facebook application ID for the current app. This should only be called after the SDK
   * has been initialized by calling FacebookSdk.sdkInitialize().
   *
   * @return the application ID
   */
  @JvmStatic
  fun getApplicationId(): String {
    Validate.sdkInitialized()
    return this.applicationId
        ?: throw FacebookException(
            "A valid Facebook app id must be set in the " +
                "AndroidManifest.xml or set by calling FacebookSdk.setApplicationId " +
                "before initializing the sdk.")
  }

  /**
   * Sets the Facebook application ID for the current app.
   *
   * @param applicationId the application ID
   */
  @JvmStatic
  fun setApplicationId(applicationId: String) {
    Validate.notEmpty(applicationId, "applicationId")
    FacebookSdk.applicationId = applicationId
  }

  /**
   * Gets the Facebook application name of the current app. This should only be called after the SDK
   * has been initialized by calling FacebookSdk.sdkInitialize().
   *
   * @return the application name
   */
  @JvmStatic
  fun getApplicationName(): String? {
    Validate.sdkInitialized()
    return applicationName
  }

  /**
   * Sets the Facebook application name for the current app.
   *
   * @param applicationName the application name
   */
  @JvmStatic
  fun setApplicationName(applicationName: String?) {
    FacebookSdk.applicationName = applicationName
  }

  /**
   * Gets the client token for the current app. This will be null unless explicitly set or unless
   * loadDefaultsFromMetadata has been called.
   *
   * @return the client token
   */
  @JvmStatic
  fun getClientToken(): String {
    Validate.sdkInitialized()
    return appClientToken
        ?: throw FacebookException(
            "A valid Facebook client token must be set in the " +
                "AndroidManifest.xml or set by calling FacebookSdk.setClientToken " +
                "before initializing the sdk.")
  }

  /**
   * Sets the Facebook client token for the current app.
   *
   * @param clientToken the client token
   */
  @JvmStatic
  fun setClientToken(clientToken: String?) {
    appClientToken = clientToken
  }

  /** @return the auto init SDK flag for the application */
  @JvmStatic fun getAutoInitEnabled(): Boolean = UserSettingsManager.getAutoInitEnabled()

  /**
   * Sets the auto init SDK flag for the application
   *
   * @param flag true or false
   *
   * When flag is false, SDK is not fully initialized.
   */
  @JvmStatic
  fun setAutoInitEnabled(flag: Boolean) {
    UserSettingsManager.setAutoInitEnabled(flag)
    if (flag) {
      fullyInitialize()
    }
  }

  /**
   * Gets the flag used by [com.facebook.appevents.AppEventsLogger]
   *
   * @return the auto logging events flag for the application
   */
  @JvmStatic
  fun getAutoLogAppEventsEnabled(): Boolean = UserSettingsManager.getAutoLogAppEventsEnabled()

  /**
   * Sets the auto logging events flag for the application [com.facebook.appevents.AppEventsLogger]
   *
   * @param flag true or false
   *
   * When flag is false, events will not be logged, see
   * [com.facebook.appevents.internal.AutomaticAnalyticsLogger]
   */
  @JvmStatic
  fun setAutoLogAppEventsEnabled(flag: Boolean) {
    UserSettingsManager.setAutoLogAppEventsEnabled(flag)
    if (flag) {
      val application = getApplicationContext() as Application
      startTracking(application, getApplicationId())
    }
  }

  /** @return the codeless debug flag for the application */
  @JvmStatic
  fun getCodelessDebugLogEnabled(): Boolean {
    Validate.sdkInitialized()
    return codelessDebugLogEnabled ?: false
  }

  /** @return the codeless enabled flag for the application */
  @JvmStatic fun getCodelessSetupEnabled(): Boolean = UserSettingsManager.getCodelessSetupEnabled()

  /** @return the advertiserID collection flag for the application */
  @JvmStatic
  fun getAdvertiserIDCollectionEnabled(): Boolean =
      UserSettingsManager.getAdvertiserIDCollectionEnabled()

  /**
   * Sets the advertiserID collection flag for the application
   *
   * @param flag true or false
   */
  @JvmStatic
  fun setAdvertiserIDCollectionEnabled(flag: Boolean) {
    UserSettingsManager.setAdvertiserIDCollectionEnabled(flag)
  }

  /**
   * Sets the codeless debug flag for the application
   *
   * @param flag true or false
   */
  @JvmStatic
  fun setCodelessDebugLogEnabled(flag: Boolean) {
    codelessDebugLogEnabled = flag
  }

  /**
   * Gets the flag of Monitor Feature
   *
   * @return the monitor flag to indicate if it has been turn on
   */
  @JvmStatic fun getMonitorEnabled(): Boolean = UserSettingsManager.getMonitorEnabled()

  /**
   * Sets the monitor flag for the application
   *
   * @param flag true or false
   */
  @JvmStatic
  fun setMonitorEnabled(flag: Boolean) {
    UserSettingsManager.setMonitorEnabled(flag)
  }

  /** Sets data processing options */
  @AutoHandleExceptions
  @JvmStatic
  fun setDataProcessingOptions(options: Array<String>?) {
    setDataProcessingOptions(options, 0, 0)
  }

  /** Sets data processing options */
  @AutoHandleExceptions
  @JvmStatic
  fun setDataProcessingOptions(options: Array<String>?, country: Int, state: Int) {
    val optionsNotNull = options ?: arrayOf()
    try {
      val dataProcessingOptions = JSONObject()
      val array = JSONArray(optionsNotNull.toList())
      dataProcessingOptions.put(DATA_PROCESSION_OPTIONS, array)
      dataProcessingOptions.put(DATA_PROCESSION_OPTIONS_COUNTRY, country)
      dataProcessingOptions.put(DATA_PROCESSION_OPTIONS_STATE, state)
      applicationContext
          .getSharedPreferences(DATA_PROCESSING_OPTIONS_PREFERENCES, Context.MODE_PRIVATE)
          .edit()
          .putString(DATA_PROCESSION_OPTIONS, dataProcessingOptions.toString())
          .apply()
    } catch (e: JSONException) {}
  }

  /**
   * Gets the cache directory to use for caching responses, etc. The default will be the value
   * returned by Context.getCacheDir() when the SDK was initialized, but it can be overridden.
   *
   * @return the cache directory
   */
  @JvmStatic
  fun getCacheDir(): File? {
    Validate.sdkInitialized()
    return cacheDir.value
  }

  /**
   * Sets the cache directory to use for caching responses, etc.
   *
   * @param cacheDir the cache directory
   */
  @JvmStatic
  fun setCacheDir(cacheDir: File) {
    FacebookSdk.cacheDir = LockOnGetVariable(cacheDir)
  }

  /**
   * Getter for the callback request code offset. The request codes starting at this offset and the
   * next 100 values are used by the Facebook SDK.
   *
   * @return The callback request code offset.
   */
  @JvmStatic
  fun getCallbackRequestCodeOffset(): Int {
    Validate.sdkInitialized()
    return callbackRequestCodeOffset
  }

  /**
   * Returns true if the request code is within the range used by Facebook SDK requests. This does
   * not include request codes that you explicitly set on the dialogs, buttons or LoginManager. The
   * range of request codes that the SDK uses starts at the callbackRequestCodeOffset and continues
   * for the next 100 values.
   *
   * @param requestCode the request code to check.
   * @return true if the request code is within the range used by the Facebook SDK.
   */
  @JvmStatic
  fun isFacebookRequestCode(requestCode: Int): Boolean {
    return (requestCode >= callbackRequestCodeOffset &&
        requestCode < callbackRequestCodeOffset + MAX_REQUEST_CODE_RANGE)
  }

  /** Callback passed to the sdkInitialize function. */
  fun interface InitializeCallback {
    /** Called when the sdk has been initialized. */
    fun onInitialized()
  }

  /** Abstraction for better testability. */
  @VisibleForTesting
  internal fun interface GraphRequestCreator {
    fun createPostRequest(
        accessToken: AccessToken?,
        publishUrl: String?,
        publishParams: JSONObject?,
        callback: GraphRequest.Callback?
    ): GraphRequest
  }

  @VisibleForTesting
  @JvmStatic
  internal fun setGraphRequestCreator(graphRequestCreator: GraphRequestCreator) {
    FacebookSdk.graphRequestCreator = graphRequestCreator
  }
}
