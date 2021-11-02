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
package com.facebook.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import com.facebook.CallbackManager
import com.facebook.CustomTabMainActivity
import com.facebook.FacebookActivity
import com.facebook.FacebookException
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.CustomTabUtils.getChromePackage
import com.facebook.internal.CustomTabUtils.getDefaultRedirectURI
import com.facebook.internal.FetchedAppSettings.Companion.getDialogFeatureConfig
import com.facebook.internal.NativeProtocol.createBundleForException
import com.facebook.internal.NativeProtocol.createPlatformActivityIntent
import com.facebook.internal.NativeProtocol.getLatestAvailableProtocolVersionForAction
import com.facebook.internal.NativeProtocol.getLatestKnownVersion
import com.facebook.internal.NativeProtocol.isVersionCompatibleWithBucketedIntent
import com.facebook.internal.NativeProtocol.setupProtocolRequestIntent
import com.facebook.internal.ServerProtocol.getDialogAuthority
import com.facebook.internal.ServerProtocol.getQueryParamsForPlatformActivityIntentWebFallback
import com.facebook.internal.Utility.buildUri
import com.facebook.internal.Validate.hasCustomTabRedirectActivity
import com.facebook.internal.Validate.hasFacebookActivity
import com.facebook.internal.Validate.hasInternetPermissions

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object DialogPresenter {
  @JvmStatic
  fun setupAppCallForCannotShowError(appCall: AppCall) {
    val e =
        FacebookException(
            "Unable to show the provided content via the web or the installed version of the " +
                "Facebook app. Some dialogs are only supported starting API 14.")
    setupAppCallForValidationError(appCall, e)
  }

  @JvmStatic
  fun setupAppCallForValidationError(appCall: AppCall, validationError: FacebookException?) {
    setupAppCallForErrorResult(appCall, validationError)
  }

  @JvmStatic
  fun present(appCall: AppCall, activity: Activity) {
    activity.startActivityForResult(appCall.requestIntent, appCall.requestCode)
    appCall.setPending()
  }

  @JvmStatic
  fun present(appCall: AppCall, fragmentWrapper: FragmentWrapper) {
    fragmentWrapper.startActivityForResult(appCall.requestIntent, appCall.requestCode)
    appCall.setPending()
  }

  @JvmStatic
  fun present(
      appCall: AppCall,
      registry: ActivityResultRegistry,
      callbackManager: CallbackManager?
  ) {
    val requestIntent = appCall.requestIntent ?: return
    startActivityForResultWithAndroidX(
        registry, callbackManager, requestIntent, appCall.requestCode)
    appCall.setPending()
  }

  @JvmStatic
  fun startActivityForResultWithAndroidX(
      registry: ActivityResultRegistry,
      callbackManager: CallbackManager?,
      intent: Intent,
      requestCode: Int
  ) {
    var launcher: ActivityResultLauncher<Intent>? = null
    launcher =
        registry.register<Intent, Pair<Int, Intent?>>(
            "facebook-dialog-request-$requestCode",
            object : ActivityResultContract<Intent, Pair<Int, Intent?>>() {
              override fun createIntent(context: Context, input: Intent): Intent {
                return input
              }

              override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, Intent?> {
                return Pair.create(resultCode, intent)
              }
            },
            ActivityResultCallback<Pair<Int, Intent?>> { result ->
              var innerCallbackManager = callbackManager
              if (innerCallbackManager == null) {
                innerCallbackManager = CallbackManagerImpl()
              }
              innerCallbackManager.onActivityResult(requestCode, result.first, result.second)
              launcher?.let {
                synchronized(it) {
                  it.unregister()
                  launcher = null
                }
              }
            })
    launcher?.launch(intent)
  }

  @JvmStatic
  fun canPresentNativeDialogWithFeature(feature: DialogFeature): Boolean {
    return (getProtocolVersionForNativeDialog(feature).protocolVersion !=
        NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @JvmStatic
  fun canPresentWebFallbackDialogWithFeature(feature: DialogFeature): Boolean {
    return getDialogWebFallbackUri(feature) != null
  }

  @JvmStatic
  fun setupAppCallForErrorResult(appCall: AppCall, exception: FacebookException?) {
    if (exception == null) {
      return
    }
    hasFacebookActivity(getApplicationContext())
    val errorResultIntent = Intent()
    errorResultIntent.setClass(getApplicationContext(), FacebookActivity::class.java)
    errorResultIntent.action = FacebookActivity.PASS_THROUGH_CANCEL_ACTION
    setupProtocolRequestIntent(
        errorResultIntent,
        appCall.callId.toString(),
        null,
        getLatestKnownVersion(),
        createBundleForException(exception))
    appCall.requestIntent = errorResultIntent
  }

  @JvmStatic
  fun setupAppCallForWebDialog(appCall: AppCall, actionName: String?, parameters: Bundle?) {
    hasFacebookActivity(getApplicationContext())
    hasInternetPermissions(getApplicationContext())
    val intentParameters = Bundle()
    intentParameters.putString(NativeProtocol.WEB_DIALOG_ACTION, actionName)
    intentParameters.putBundle(NativeProtocol.WEB_DIALOG_PARAMS, parameters)
    val webDialogIntent = Intent()
    setupProtocolRequestIntent(
        webDialogIntent,
        appCall.callId.toString(),
        actionName,
        getLatestKnownVersion(),
        intentParameters)
    webDialogIntent.setClass(getApplicationContext(), FacebookActivity::class.java)
    webDialogIntent.action = FacebookDialogFragment.TAG
    appCall.requestIntent = webDialogIntent
  }

  @JvmStatic
  fun setupAppCallForWebFallbackDialog(
      appCall: AppCall,
      parameters: Bundle?,
      feature: DialogFeature
  ) {
    hasFacebookActivity(getApplicationContext())
    hasInternetPermissions(getApplicationContext())
    val featureName = feature.name
    var fallbackUrl = getDialogWebFallbackUri(feature)
    if (fallbackUrl == null) {
      throw FacebookException("Unable to fetch the Url for the DialogFeature : '$featureName'")
    }

    // Since we're talking to the server here, let's use the latest version we know about.
    // We know we are going to be communicating over a bucketed protocol.
    val protocolVersion = getLatestKnownVersion()
    val webParams =
        getQueryParamsForPlatformActivityIntentWebFallback(
            appCall.callId.toString(), protocolVersion, parameters)
            ?: throw FacebookException("Unable to fetch the app's key-hash")

    // Now form the Uri
    fallbackUrl =
        if (fallbackUrl.isRelative) {
          buildUri(getDialogAuthority(), fallbackUrl.toString(), webParams)
        } else {
          buildUri(fallbackUrl.authority, fallbackUrl.path, webParams)
        }
    val intentParameters = Bundle()
    intentParameters.putString(NativeProtocol.WEB_DIALOG_URL, fallbackUrl.toString())
    intentParameters.putBoolean(NativeProtocol.WEB_DIALOG_IS_FALLBACK, true)
    val webDialogIntent = Intent()
    setupProtocolRequestIntent(
        webDialogIntent,
        appCall.callId.toString(),
        feature.getAction(),
        getLatestKnownVersion(),
        intentParameters)
    webDialogIntent.setClass(getApplicationContext(), FacebookActivity::class.java)
    webDialogIntent.action = FacebookDialogFragment.TAG
    appCall.requestIntent = webDialogIntent
  }

  @JvmStatic
  fun setupAppCallForNativeDialog(
      appCall: AppCall,
      parameterProvider: ParameterProvider,
      feature: DialogFeature
  ) {
    val context = getApplicationContext()
    val action = feature.getAction()
    val protocolVersionResult = getProtocolVersionForNativeDialog(feature)
    val protocolVersion = protocolVersionResult.protocolVersion
    if (protocolVersion == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
      throw FacebookException(
          "Cannot present this dialog. This likely means that the " +
              "Facebook app is not installed.")
    }
    var params: Bundle?
    params =
        if (isVersionCompatibleWithBucketedIntent(protocolVersion)) {
          // Facebook app supports the new bucketed protocol
          parameterProvider.parameters
        } else {
          // Facebook app only supports the old flat protocol
          parameterProvider.legacyParameters
        }
    if (params == null) {
      params = Bundle()
    }
    val intent =
        createPlatformActivityIntent(
            context, appCall.callId.toString(), action, protocolVersionResult, params)
            ?: throw FacebookException(
                "Unable to create Intent; this likely means the" + "Facebook app is not installed.")
    appCall.requestIntent = intent
  }

  @JvmStatic
  fun setupAppCallForCustomTabDialog(appCall: AppCall, action: String?, parameters: Bundle?) {
    hasCustomTabRedirectActivity(getApplicationContext(), getDefaultRedirectURI())
    hasInternetPermissions(getApplicationContext())
    val intent = Intent(getApplicationContext(), CustomTabMainActivity::class.java)
    intent.putExtra(CustomTabMainActivity.EXTRA_ACTION, action)
    intent.putExtra(CustomTabMainActivity.EXTRA_PARAMS, parameters)
    intent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, getChromePackage())
    setupProtocolRequestIntent(
        intent, appCall.callId.toString(), action, getLatestKnownVersion(), null)
    appCall.requestIntent = intent
  }

  private fun getDialogWebFallbackUri(feature: DialogFeature): Uri? {
    val featureName = feature.name
    val action = feature.getAction()
    val applicationId = getApplicationId()
    val config = getDialogFeatureConfig(applicationId, action, featureName)
    var fallbackUrl: Uri? = null
    if (config != null) {
      fallbackUrl = config.fallbackUrl
    }
    return fallbackUrl
  }

  @JvmStatic
  fun getProtocolVersionForNativeDialog(
      feature: DialogFeature
  ): NativeProtocol.ProtocolVersionQueryResult {
    val applicationId = getApplicationId()
    val action = feature.getAction()
    val featureVersionSpec = getVersionSpecForFeature(applicationId, action, feature)
    return getLatestAvailableProtocolVersionForAction(action, featureVersionSpec)
  }

  private fun getVersionSpecForFeature(
      applicationId: String,
      actionName: String,
      feature: DialogFeature
  ): IntArray {
    // Return the value from DialogFeatureConfig if available. Otherwise, just
    // default to the min-version
    val config = getDialogFeatureConfig(applicationId, actionName, feature.name)
    return config?.versionSpec ?: intArrayOf(feature.getMinVersion())
  }

  @JvmStatic
  fun logDialogActivity(context: Context, eventName: String, outcome: String) {
    val logger = InternalAppEventsLogger(context)
    val parameters = Bundle()
    parameters.putString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME, outcome)
    logger.logEventImplicitly(eventName, parameters)
  }

  interface ParameterProvider {
    val parameters: Bundle?
    val legacyParameters: Bundle?
  }
}
