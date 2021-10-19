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

import android.content.Context
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.DefaultAudience
import com.facebook.login.LoginTargetApp
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@AutoHandleExceptions
object NativeProtocol {
  const val NO_PROTOCOL_AVAILABLE = -1
  private val TAG = NativeProtocol::class.java.name
  private const val FACEBOOK_PROXY_AUTH_ACTIVITY = "com.facebook.katana.ProxyAuth"
  private const val FACEBOOK_TOKEN_REFRESH_ACTIVITY =
      "com.facebook.katana.platform.TokenRefreshService"
  const val FACEBOOK_PROXY_AUTH_PERMISSIONS_KEY = "scope"
  const val FACEBOOK_PROXY_AUTH_APP_ID_KEY = "client_id"
  const val FACEBOOK_PROXY_AUTH_E2E_KEY = "e2e"
  const val FACEBOOK_SDK_VERSION_KEY = "facebook_sdk_version"

  // ---------------------------------------------------------------------------------------------
  // Native Protocol updated 2012-11
  const val INTENT_ACTION_PLATFORM_ACTIVITY = "com.facebook.platform.PLATFORM_ACTIVITY"
  const val INTENT_ACTION_PLATFORM_SERVICE = "com.facebook.platform.PLATFORM_SERVICE"
  const val PROTOCOL_VERSION_20121101 = 2012_11_01
  const val PROTOCOL_VERSION_20130502 = 2013_05_02
  const val PROTOCOL_VERSION_20130618 = 2013_06_18
  const val PROTOCOL_VERSION_20131107 = 2013_11_07
  const val PROTOCOL_VERSION_20140204 = 2014_02_04
  const val PROTOCOL_VERSION_20140324 = 2014_03_24
  const val PROTOCOL_VERSION_20140701 = 2014_07_01
  const val PROTOCOL_VERSION_20141001 = 2014_10_01
  const val PROTOCOL_VERSION_20141028 = 2014_10_28
  const val PROTOCOL_VERSION_20141107 = 2014_11_07 // Bucketed Result Intents
  const val PROTOCOL_VERSION_20141218 = 2014_12_18
  const val PROTOCOL_VERSION_20160327 = 2016_03_27
  const val PROTOCOL_VERSION_20170213 = 2017_02_13
  const val PROTOCOL_VERSION_20170411 = 2017_04_11 // express login
  const val PROTOCOL_VERSION_20170417 = 2017_04_17
  const val PROTOCOL_VERSION_20171115 = 2017_11_15
  const val PROTOCOL_VERSION_20210906 = 2021_09_06
  const val EXTRA_PROTOCOL_VERSION = "com.facebook.platform.protocol.PROTOCOL_VERSION"
  const val EXTRA_PROTOCOL_ACTION = "com.facebook.platform.protocol.PROTOCOL_ACTION"
  const val EXTRA_PROTOCOL_CALL_ID = "com.facebook.platform.protocol.CALL_ID"
  const val EXTRA_GET_INSTALL_DATA_PACKAGE = "com.facebook.platform.extra.INSTALLDATA_PACKAGE"
  const val EXTRA_PROTOCOL_BRIDGE_ARGS = "com.facebook.platform.protocol.BRIDGE_ARGS"
  const val EXTRA_PROTOCOL_METHOD_ARGS = "com.facebook.platform.protocol.METHOD_ARGS"
  const val EXTRA_PROTOCOL_METHOD_RESULTS = "com.facebook.platform.protocol.RESULT_ARGS"
  const val BRIDGE_ARG_APP_NAME_STRING = "app_name"
  const val BRIDGE_ARG_ACTION_ID_STRING = "action_id"
  const val BRIDGE_ARG_ERROR_BUNDLE = "error"
  const val EXTRA_DIALOG_COMPLETE_KEY = "com.facebook.platform.extra.DID_COMPLETE"
  const val EXTRA_DIALOG_COMPLETION_GESTURE_KEY = "com.facebook.platform.extra.COMPLETION_GESTURE"
  const val RESULT_ARGS_DIALOG_COMPLETE_KEY = "didComplete"
  const val RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY = "completionGesture"

  // Messages supported by PlatformService:
  const val MESSAGE_GET_ACCESS_TOKEN_REQUEST = 0x10000
  const val MESSAGE_GET_ACCESS_TOKEN_REPLY = 0x10001
  const val MESSAGE_GET_PROTOCOL_VERSIONS_REQUEST = 0x10002
  const val MESSAGE_GET_PROTOCOL_VERSIONS_REPLY = 0x10003
  const val MESSAGE_GET_INSTALL_DATA_REQUEST = 0x10004
  const val MESSAGE_GET_INSTALL_DATA_REPLY = 0x10005
  const val MESSAGE_GET_LIKE_STATUS_REQUEST = 0x10006
  const val MESSAGE_GET_LIKE_STATUS_REPLY = 0x10007
  const val MESSAGE_GET_AK_SEAMLESS_TOKEN_REQUEST = 0x10008
  const val MESSAGE_GET_AK_SEAMLESS_TOKEN_REPLY = 0x10009
  const val MESSAGE_GET_LOGIN_STATUS_REQUEST = 0x1000A
  const val MESSAGE_GET_LOGIN_STATUS_REPLY = 0x1000B

  // MESSAGE_ERROR_REPLY data keys:
  // See STATUS_*
  // MESSAGE_GET_ACCESS_TOKEN_REQUEST data keys:
  // EXTRA_APPLICATION_ID
  // MESSAGE_GET_ACCESS_TOKEN_REPLY data keys:
  // EXTRA_ACCESS_TOKEN
  // EXTRA_EXPIRES_SECONDS_SINCE_EPOCH
  // EXTRA_PERMISSIONS
  // EXTRA_DATA_ACCESS_EXPIRATION_TIME
  // MESSAGE_GET_LIKE_STATUS_REQUEST data keys:
  // EXTRA_APPLICATION_ID
  // EXTRA_OBJECT_ID
  // MESSAGE_GET_LIKE_STATUS_REPLY data keys:
  // EXTRA_OBJECT_IS_LIKED
  // EXTRA_LIKE_COUNT_STRING_WITH_LIKE
  // EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE
  // EXTRA_SOCIAL_SENTENCE_WITH_LIKE
  // EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE
  // EXTRA_UNLIKE_TOKEN
  // MESSAGE_GET_PROTOCOL_VERSIONS_REPLY data keys:
  const val EXTRA_PROTOCOL_VERSIONS = "com.facebook.platform.extra.PROTOCOL_VERSIONS"

  // Values of EXTRA_PROTOCOL_ACTION supported by PlatformActivity:
  const val ACTION_FEED_DIALOG = "com.facebook.platform.action.request.FEED_DIALOG"
  const val ACTION_MESSAGE_DIALOG = "com.facebook.platform.action.request.MESSAGE_DIALOG"
  const val ACTION_OGACTIONPUBLISH_DIALOG =
      "com.facebook.platform.action.request.OGACTIONPUBLISH_DIALOG"
  const val ACTION_OGMESSAGEPUBLISH_DIALOG =
      "com.facebook.platform.action.request.OGMESSAGEPUBLISH_DIALOG"
  const val ACTION_LIKE_DIALOG = "com.facebook.platform.action.request.LIKE_DIALOG"

  // The value of ACTION_APPINVITE_DIALOG is different since that is what is on the server.
  const val ACTION_APPINVITE_DIALOG = "com.facebook.platform.action.request.APPINVITES_DIALOG"
  const val ACTION_CAMERA_EFFECT = "com.facebook.platform.action.request.CAMERA_EFFECT"
  const val ACTION_SHARE_STORY = "com.facebook.platform.action.request.SHARE_STORY"

  // Extras supported for ACTION_LOGIN_DIALOG:
  const val EXTRA_PERMISSIONS = "com.facebook.platform.extra.PERMISSIONS"
  const val EXTRA_APPLICATION_ID = "com.facebook.platform.extra.APPLICATION_ID"
  const val EXTRA_APPLICATION_NAME = "com.facebook.platform.extra.APPLICATION_NAME"
  const val EXTRA_USER_ID = "com.facebook.platform.extra.USER_ID"
  const val EXTRA_LOGGER_REF = "com.facebook.platform.extra.LOGGER_REF"
  const val EXTRA_TOAST_DURATION_MS = "com.facebook.platform.extra.EXTRA_TOAST_DURATION_MS"
  const val EXTRA_GRAPH_API_VERSION = "com.facebook.platform.extra.GRAPH_API_VERSION"
  const val EXTRA_NONCE = "com.facebook.platform.extra.NONCE"

  // Extras returned by setResult() for ACTION_LOGIN_DIALOG
  const val EXTRA_ACCESS_TOKEN = "com.facebook.platform.extra.ACCESS_TOKEN"
  const val EXTRA_EXPIRES_SECONDS_SINCE_EPOCH =
      "com.facebook.platform.extra.EXPIRES_SECONDS_SINCE_EPOCH"
  const val EXTRA_DATA_ACCESS_EXPIRATION_TIME =
      "com.facebook.platform.extra.EXTRA_DATA_ACCESS_EXPIRATION_TIME"
  const val EXTRA_AUTHENTICATION_TOKEN = "com.facebook.platform.extra.ID_TOKEN"

  // EXTRA_PERMISSIONS
  const val RESULT_ARGS_ACCESS_TOKEN = "access_token"
  const val RESULT_ARGS_GRAPH_DOMAIN = "graph_domain"
  const val RESULT_ARGS_SIGNED_REQUEST = "signed request"
  const val RESULT_ARGS_EXPIRES_SECONDS_SINCE_EPOCH = "expires_seconds_since_epoch"
  const val RESULT_ARGS_PERMISSIONS = "permissions"

  // OG objects will have this key to set to true if they should be created as part of OG Action
  // publish
  const val OPEN_GRAPH_CREATE_OBJECT_KEY = "fbsdk:create_object"

  // Determines whether an image is user generated
  const val IMAGE_USER_GENERATED_KEY = "user_generated"

  // url key for images
  const val IMAGE_URL_KEY = "url"

  // Keys for status data in MESSAGE_ERROR_REPLY from PlatformService and for error
  // extras returned by PlatformActivity's setResult() in case of errors:
  const val STATUS_ERROR_TYPE = "com.facebook.platform.status.ERROR_TYPE"
  const val STATUS_ERROR_DESCRIPTION = "com.facebook.platform.status.ERROR_DESCRIPTION"
  const val STATUS_ERROR_CODE = "com.facebook.platform.status.ERROR_CODE"
  const val STATUS_ERROR_SUBCODE = "com.facebook.platform.status.ERROR_SUBCODE"
  const val STATUS_ERROR_JSON = "com.facebook.platform.status.ERROR_JSON"
  const val BRIDGE_ARG_ERROR_TYPE = "error_type"
  const val BRIDGE_ARG_ERROR_DESCRIPTION = "error_description"
  const val BRIDGE_ARG_ERROR_CODE = "error_code"
  const val BRIDGE_ARG_ERROR_SUBCODE = "error_subcode"
  const val BRIDGE_ARG_ERROR_JSON = "error_json"

  // Expected values for ERROR_KEY_TYPE.  Clients should tolerate other values:
  const val ERROR_UNKNOWN_ERROR = "UnknownError"
  const val ERROR_PROTOCOL_ERROR = "ProtocolError"
  const val ERROR_USER_CANCELED = "UserCanceled"
  const val ERROR_APPLICATION_ERROR = "ApplicationError"
  const val ERROR_NETWORK_ERROR = "NetworkError"
  const val ERROR_PERMISSION_DENIED = "PermissionDenied"
  const val ERROR_SERVICE_DISABLED = "ServiceDisabled"
  const val WEB_DIALOG_URL = "url"
  const val WEB_DIALOG_ACTION = "action"
  const val WEB_DIALOG_PARAMS = "params"
  const val WEB_DIALOG_IS_FALLBACK = "is_fallback"
  const val AUDIENCE_ME = "only_me"
  const val AUDIENCE_FRIENDS = "friends"
  const val AUDIENCE_EVERYONE = "everyone"
  private const val CONTENT_SCHEME = "content://"
  private const val PLATFORM_PROVIDER = ".provider.PlatformProvider"
  private const val PLATFORM_PROVIDER_VERSIONS = "$PLATFORM_PROVIDER/versions"

  // Columns returned by PlatformProvider
  private const val PLATFORM_PROVIDER_VERSION_COLUMN = "version"
  private val facebookAppInfoList = buildFacebookAppList()
  private val effectCameraAppInfoList = buildEffectCameraAppInfoList()
  private val actionToAppInfoMap = buildActionToAppInfoMap()
  private val protocolVersionsAsyncUpdating = AtomicBoolean(false)
  private fun buildFacebookAppList(): List<NativeAppInfo> {
    // Katana needs to be the first thing in the list since it will get selected as the default
    // FACEBOOK_APP_INFO
    return arrayListOf(KatanaAppInfo(), WakizashiAppInfo())
  }

  private fun buildEffectCameraAppInfoList(): List<NativeAppInfo> {
    // Add the effect test app in first position to make it the default choice.
    val list: ArrayList<NativeAppInfo> = arrayListOf(EffectTestAppInfo())
    list.addAll(buildFacebookAppList())
    return list
  }

  private fun buildActionToAppInfoMap(): Map<String, List<NativeAppInfo>> {
    val map: MutableMap<String, List<NativeAppInfo>> = HashMap()
    val messengerAppInfoList = ArrayList<NativeAppInfo>()
    messengerAppInfoList.add(MessengerAppInfo())

    // Add individual actions and the list they should try
    map[ACTION_OGACTIONPUBLISH_DIALOG] = facebookAppInfoList
    map[ACTION_FEED_DIALOG] = facebookAppInfoList
    map[ACTION_LIKE_DIALOG] = facebookAppInfoList
    map[ACTION_APPINVITE_DIALOG] = facebookAppInfoList
    map[ACTION_MESSAGE_DIALOG] = messengerAppInfoList
    map[ACTION_OGMESSAGEPUBLISH_DIALOG] = messengerAppInfoList
    map[ACTION_CAMERA_EFFECT] = effectCameraAppInfoList
    map[ACTION_SHARE_STORY] = facebookAppInfoList
    return map
  }

  @JvmStatic
  fun validateActivityIntent(context: Context, intent: Intent?, appInfo: NativeAppInfo?): Intent? {
    if (intent == null) {
      return null
    }
    val resolveInfo = context.packageManager.resolveActivity(intent, 0) ?: return null
    return if (!validateSignature(context, resolveInfo.activityInfo.packageName)) {
      null
    } else intent
  }

  @JvmStatic
  fun validateServiceIntent(context: Context, intent: Intent?, appInfo: NativeAppInfo?): Intent? {
    if (intent == null) {
      return null
    }
    val resolveInfo = context.packageManager.resolveService(intent, 0) ?: return null
    return if (!validateSignature(context, resolveInfo.serviceInfo.packageName)) {
      null
    } else intent
  }

  @JvmStatic
  fun createFacebookLiteIntent(
      context: Context,
      applicationId: String,
      permissions: Collection<String?>,
      e2e: String,
      isRerequest: Boolean,
      isForPublish: Boolean,
      defaultAudience: DefaultAudience,
      clientState: String,
      authType: String,
      messengerPageId: String?,
      resetMessengerState: Boolean,
      isFamilyLogin: Boolean,
      shouldSkipAccountDedupe: Boolean
  ): Intent? {
    val appInfo: NativeAppInfo = FBLiteAppInfo()
    var intent =
        createNativeAppIntent(
            appInfo,
            applicationId,
            permissions,
            e2e,
            isForPublish,
            defaultAudience,
            clientState,
            authType,
            false,
            messengerPageId,
            resetMessengerState,
            LoginTargetApp.FACEBOOK,
            isFamilyLogin,
            shouldSkipAccountDedupe,
            "")
    intent = validateActivityIntent(context, intent, appInfo)
    return intent
  }

  @JvmStatic
  fun createInstagramIntent(
      context: Context,
      applicationId: String,
      permissions: Collection<String?>,
      e2e: String,
      isRerequest: Boolean,
      isForPublish: Boolean,
      defaultAudience: DefaultAudience,
      clientState: String,
      authType: String,
      messengerPageId: String?,
      resetMessengerState: Boolean,
      isFamilyLogin: Boolean,
      shouldSkipAccountDedupe: Boolean
  ): Intent? {
    val appInfo: NativeAppInfo = InstagramAppInfo()
    var intent =
        createNativeAppIntent(
            appInfo,
            applicationId,
            permissions,
            e2e,
            isForPublish,
            defaultAudience,
            clientState,
            authType,
            false,
            messengerPageId,
            resetMessengerState,
            LoginTargetApp.INSTAGRAM,
            isFamilyLogin,
            shouldSkipAccountDedupe,
            "")
    intent = validateActivityIntent(context, intent, appInfo)
    return intent
  }

  private fun createNativeAppIntent(
      appInfo: NativeAppInfo,
      applicationId: String,
      permissions: Collection<String?>,
      e2e: String,
      isForPublish: Boolean,
      defaultAudience: DefaultAudience,
      clientState: String,
      authType: String,
      ignoreAppSwitchToLoggedOut: Boolean,
      messengerPageId: String?,
      resetMessengerState: Boolean,
      targetApp: LoginTargetApp,
      isFamilyLogin: Boolean,
      shouldSkipAccountDedupe: Boolean,
      nonce: String?
  ): Intent? {
    val activityName = appInfo.getLoginActivity() ?: return null
    // the NativeApp doesn't have a login activity
    val intent =
        Intent()
            .setClassName(appInfo.getPackage(), activityName)
            .putExtra(FACEBOOK_PROXY_AUTH_APP_ID_KEY, applicationId)
    intent.putExtra(FACEBOOK_SDK_VERSION_KEY, FacebookSdk.getSdkVersion())
    if (!Utility.isNullOrEmpty(permissions)) {
      intent.putExtra(FACEBOOK_PROXY_AUTH_PERMISSIONS_KEY, TextUtils.join(",", permissions))
    }
    if (!Utility.isNullOrEmpty(e2e)) {
      intent.putExtra(FACEBOOK_PROXY_AUTH_E2E_KEY, e2e)
    }
    intent.putExtra(ServerProtocol.DIALOG_PARAM_STATE, clientState)
    intent.putExtra(ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE, appInfo.getResponseType())
    intent.putExtra(ServerProtocol.DIALOG_PARAM_NONCE, nonce)
    intent.putExtra(
        ServerProtocol.DIALOG_PARAM_RETURN_SCOPES, ServerProtocol.DIALOG_RETURN_SCOPES_TRUE)
    if (isForPublish) {
      intent.putExtra(
          ServerProtocol.DIALOG_PARAM_DEFAULT_AUDIENCE, defaultAudience.nativeProtocolAudience)
    }

    // Override the API Version for Auth
    intent.putExtra(ServerProtocol.DIALOG_PARAM_LEGACY_OVERRIDE, FacebookSdk.getGraphApiVersion())
    intent.putExtra(ServerProtocol.DIALOG_PARAM_AUTH_TYPE, authType)
    if (ignoreAppSwitchToLoggedOut) {
      intent.putExtra(ServerProtocol.DIALOG_PARAM_FAIL_ON_LOGGED_OUT, true)
    }

    intent.putExtra(ServerProtocol.DIALOG_PARAM_MESSENGER_PAGE_ID, messengerPageId)
    intent.putExtra(ServerProtocol.DIALOG_PARAM_RESET_MESSENGER_STATE, resetMessengerState)
    if (isFamilyLogin) {
      intent.putExtra(ServerProtocol.DIALOG_PARAM_FX_APP, targetApp.toString())
    }
    if (shouldSkipAccountDedupe) {
      intent.putExtra(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, true)
    }
    return intent
  }

  @JvmStatic
  fun createProxyAuthIntents(
      context: Context?,
      applicationId: String,
      permissions: Collection<String?>,
      e2e: String,
      isRerequest: Boolean,
      isForPublish: Boolean,
      defaultAudience: DefaultAudience,
      clientState: String,
      authType: String,
      ignoreAppSwitchToLoggedOut: Boolean,
      messengerPageId: String?,
      resetMessengerState: Boolean,
      isFamilyLogin: Boolean,
      shouldSkipAccountDedupe: Boolean,
      nonce: String?,
  ): List<Intent> {
    return facebookAppInfoList.mapNotNull {
      createNativeAppIntent(
          it,
          applicationId,
          permissions,
          e2e,
          isForPublish,
          defaultAudience,
          clientState,
          authType,
          ignoreAppSwitchToLoggedOut,
          messengerPageId,
          resetMessengerState,
          LoginTargetApp.FACEBOOK,
          isFamilyLogin,
          shouldSkipAccountDedupe,
          nonce)
    }
  }

  @JvmStatic
  fun createTokenRefreshIntent(context: Context): Intent? {
    for (appInfo in facebookAppInfoList) {
      var intent: Intent? =
          Intent().setClassName(appInfo.getPackage(), FACEBOOK_TOKEN_REFRESH_ACTIVITY)
      intent = validateServiceIntent(context, intent, appInfo)
      if (intent != null) {
        return intent
      }
    }
    return null
  }

  @JvmStatic fun getLatestKnownVersion(): Int = KNOWN_PROTOCOL_VERSIONS[0]

  // Note: be sure this stays sorted in descending order; add new versions at the beginning
  private val KNOWN_PROTOCOL_VERSIONS =
      arrayOf(
          PROTOCOL_VERSION_20210906,
          PROTOCOL_VERSION_20170417,
          PROTOCOL_VERSION_20160327,
          PROTOCOL_VERSION_20141218,
          PROTOCOL_VERSION_20141107,
          PROTOCOL_VERSION_20141028,
          PROTOCOL_VERSION_20141001,
          PROTOCOL_VERSION_20140701,
          PROTOCOL_VERSION_20140324,
          PROTOCOL_VERSION_20140204,
          PROTOCOL_VERSION_20131107,
          PROTOCOL_VERSION_20130618,
          PROTOCOL_VERSION_20130502,
          PROTOCOL_VERSION_20121101)

  @JvmStatic
  fun isVersionCompatibleWithBucketedIntent(version: Int): Boolean {
    return KNOWN_PROTOCOL_VERSIONS.contains(version) && version >= PROTOCOL_VERSION_20140701
  }

  /**
   * Will create an Intent that can be used to invoke an action in a Facebook app via the Native
   * Protocol
   */
  @JvmStatic
  fun createPlatformActivityIntent(
      context: Context,
      callId: String?,
      action: String?,
      versionResult: ProtocolVersionQueryResult?,
      extras: Bundle?
  ): Intent? {
    if (versionResult == null) {
      return null
    }
    val appInfo = versionResult.appInfo ?: return null
    var intent: Intent? =
        Intent()
            .setAction(INTENT_ACTION_PLATFORM_ACTIVITY)
            .setPackage(appInfo.getPackage())
            .addCategory(Intent.CATEGORY_DEFAULT)
    intent = validateActivityIntent(context, intent, appInfo)
    if (intent == null) {
      return null
    }
    setupProtocolRequestIntent(intent, callId, action, versionResult.protocolVersion, extras)
    return intent
  }

  /** Will setup the passed in Intent in the shape of a Native Protocol request Intent. */
  @JvmStatic
  fun setupProtocolRequestIntent(
      intent: Intent,
      callId: String?,
      action: String?,
      version: Int,
      params: Bundle?
  ) {
    val applicationId = FacebookSdk.getApplicationId()
    val applicationName = FacebookSdk.getApplicationName()
    intent
        .putExtra(EXTRA_PROTOCOL_VERSION, version)
        .putExtra(EXTRA_PROTOCOL_ACTION, action)
        .putExtra(EXTRA_APPLICATION_ID, applicationId)
    if (isVersionCompatibleWithBucketedIntent(version)) {
      // This is a bucketed intent
      val bridgeArguments = Bundle()
      bridgeArguments.putString(BRIDGE_ARG_ACTION_ID_STRING, callId)
      Utility.putNonEmptyString(bridgeArguments, BRIDGE_ARG_APP_NAME_STRING, applicationName)
      intent.putExtra(EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArguments)
      val methodArguments = params ?: Bundle()
      intent.putExtra(EXTRA_PROTOCOL_METHOD_ARGS, methodArguments)
    } else {
      // This is the older flat intent
      intent.putExtra(EXTRA_PROTOCOL_CALL_ID, callId)
      if (!Utility.isNullOrEmpty(applicationName)) {
        intent.putExtra(EXTRA_APPLICATION_NAME, applicationName)
      }
      if (params != null) {
        intent.putExtras(params)
      }
    }
  }

  /**
   * Use this method to set a result on an Activity, where the result needs to be in the shape of
   * the native protocol used for native dialogs.
   */
  @JvmStatic
  fun createProtocolResultIntent(
      requestIntent: Intent,
      results: Bundle?,
      error: FacebookException?
  ): Intent? {
    val callId = getCallIdFromIntent(requestIntent) ?: return null
    val resultIntent = Intent()
    resultIntent.putExtra(EXTRA_PROTOCOL_VERSION, getProtocolVersionFromIntent(requestIntent))
    val bridgeArguments = Bundle()
    bridgeArguments.putString(BRIDGE_ARG_ACTION_ID_STRING, callId.toString())
    if (error != null) {
      bridgeArguments.putBundle(BRIDGE_ARG_ERROR_BUNDLE, createBundleForException(error))
    }
    resultIntent.putExtra(EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArguments)
    if (results != null) {
      resultIntent.putExtra(EXTRA_PROTOCOL_METHOD_RESULTS, results)
    }
    return resultIntent
  }

  @JvmStatic
  fun createPlatformServiceIntent(context: Context): Intent? {
    for (appInfo in facebookAppInfoList) {
      var intent: Intent? =
          Intent(INTENT_ACTION_PLATFORM_SERVICE)
              .setPackage(appInfo.getPackage())
              .addCategory(Intent.CATEGORY_DEFAULT)
      intent = validateServiceIntent(context, intent, appInfo)
      if (intent != null) {
        return intent
      }
    }
    return null
  }

  @JvmStatic
  fun getProtocolVersionFromIntent(intent: Intent): Int {
    return intent.getIntExtra(EXTRA_PROTOCOL_VERSION, 0)
  }

  @JvmStatic
  fun getCallIdFromIntent(intent: Intent?): UUID? {
    if (intent == null) {
      return null
    }
    val version = getProtocolVersionFromIntent(intent)
    var callIdString: String? = null
    if (isVersionCompatibleWithBucketedIntent(version)) {
      val bridgeArgs = intent.getBundleExtra(EXTRA_PROTOCOL_BRIDGE_ARGS)
      if (bridgeArgs != null) {
        callIdString = bridgeArgs.getString(BRIDGE_ARG_ACTION_ID_STRING)
      }
    } else {
      callIdString = intent.getStringExtra(EXTRA_PROTOCOL_CALL_ID)
    }
    var callId: UUID? = null
    if (callIdString != null) {
      try {
        callId = UUID.fromString(callIdString)
      } catch (exception: IllegalArgumentException) {}
    }
    return callId
  }

  @JvmStatic
  fun getBridgeArgumentsFromIntent(intent: Intent): Bundle? {
    val version = getProtocolVersionFromIntent(intent)
    return if (!isVersionCompatibleWithBucketedIntent(version)) {
      null
    } else intent.getBundleExtra(EXTRA_PROTOCOL_BRIDGE_ARGS)
  }

  @JvmStatic
  fun getMethodArgumentsFromIntent(intent: Intent): Bundle? {
    val version = getProtocolVersionFromIntent(intent)
    return if (!isVersionCompatibleWithBucketedIntent(version)) {
      intent.extras
    } else intent.getBundleExtra(EXTRA_PROTOCOL_METHOD_ARGS)
  }

  @JvmStatic
  fun getSuccessResultsFromIntent(resultIntent: Intent): Bundle? {
    val version = getProtocolVersionFromIntent(resultIntent)
    val extras = resultIntent.extras
    return if (!isVersionCompatibleWithBucketedIntent(version) || extras == null) {
      extras
    } else extras.getBundle(EXTRA_PROTOCOL_METHOD_RESULTS)
  }

  @JvmStatic
  fun isErrorResult(resultIntent: Intent): Boolean {
    val bridgeArgs = getBridgeArgumentsFromIntent(resultIntent)
    return bridgeArgs?.containsKey(BRIDGE_ARG_ERROR_BUNDLE)
        ?: resultIntent.hasExtra(STATUS_ERROR_TYPE)
  }

  @JvmStatic
  fun getErrorDataFromResultIntent(resultIntent: Intent): Bundle? {
    if (!isErrorResult(resultIntent)) {
      return null
    }
    val bridgeArgs = getBridgeArgumentsFromIntent(resultIntent)
    return if (bridgeArgs != null) {
      bridgeArgs.getBundle(BRIDGE_ARG_ERROR_BUNDLE)
    } else resultIntent.extras
  }

  @JvmStatic
  fun getExceptionFromErrorData(errorData: Bundle?): FacebookException? {
    if (errorData == null) {
      return null
    }
    var type = errorData.getString(BRIDGE_ARG_ERROR_TYPE)
    if (type == null) {
      type = errorData.getString(STATUS_ERROR_TYPE)
    }
    var description = errorData.getString(BRIDGE_ARG_ERROR_DESCRIPTION)
    if (description == null) {
      description = errorData.getString(STATUS_ERROR_DESCRIPTION)
    }
    return if (type != null && type.equals(ERROR_USER_CANCELED, ignoreCase = true)) {
      FacebookOperationCanceledException(description)
    } else FacebookException(description)

    /* TODO parse error values and create appropriate exception class */
  }

  @JvmStatic
  fun createBundleForException(e: FacebookException?): Bundle? {
    if (e == null) {
      return null
    }
    val errorBundle = Bundle()
    errorBundle.putString(BRIDGE_ARG_ERROR_DESCRIPTION, e.toString())
    if (e is FacebookOperationCanceledException) {
      errorBundle.putString(BRIDGE_ARG_ERROR_TYPE, ERROR_USER_CANCELED)
    }
    return errorBundle
  }

  @JvmStatic
  fun getLatestAvailableProtocolVersionForService(minimumVersion: Int): Int {
    // Services are currently always against the Facebook App
    return getLatestAvailableProtocolVersionForAppInfoList(
            facebookAppInfoList, intArrayOf(minimumVersion))
        .protocolVersion
  }

  @JvmStatic
  fun getLatestAvailableProtocolVersionForAction(
      action: String,
      versionSpec: IntArray
  ): ProtocolVersionQueryResult {
    val appInfoList = actionToAppInfoMap[action] ?: emptyList()
    return getLatestAvailableProtocolVersionForAppInfoList(appInfoList, versionSpec)
  }

  private fun getLatestAvailableProtocolVersionForAppInfoList(
      appInfoList: List<NativeAppInfo>?,
      versionSpec: IntArray
  ): ProtocolVersionQueryResult {
    // Kick off an update
    updateAllAvailableProtocolVersionsAsync()
    if (appInfoList == null) {
      return ProtocolVersionQueryResult.createEmpty()
    }

    // Could potentially cache the NativeAppInfo to latestProtocolVersion
    for (appInfo in appInfoList) {
      val protocolVersion =
          computeLatestAvailableVersionFromVersionSpec(
              appInfo.getAvailableVersions(), getLatestKnownVersion(), versionSpec)
      if (protocolVersion != NO_PROTOCOL_AVAILABLE) {
        return ProtocolVersionQueryResult.create(appInfo, protocolVersion)
      }
    }
    return ProtocolVersionQueryResult.createEmpty()
  }

  @JvmStatic
  fun updateAllAvailableProtocolVersionsAsync() {
    if (!protocolVersionsAsyncUpdating.compareAndSet(false, true)) {
      return
    }
    FacebookSdk.getExecutor().execute {
      try {
        for (appInfo in facebookAppInfoList) {
          appInfo.fetchAvailableVersions(true)
        }
      } finally {
        protocolVersionsAsyncUpdating.set(false)
      }
    }
  }

  private fun fetchAllAvailableProtocolVersionsForAppInfo(appInfo: NativeAppInfo): TreeSet<Int> {
    val allAvailableVersions = TreeSet<Int>()
    val appContext = FacebookSdk.getApplicationContext()
    val contentResolver = appContext.contentResolver
    val projection = arrayOf(PLATFORM_PROVIDER_VERSION_COLUMN)
    val uri = buildPlatformProviderVersionURI(appInfo)
    var c: Cursor? = null
    try {
      // First see if the base provider exists as a check for whether the native app is
      // installed. We do this prior to querying, to prevent errors from being output to
      // logcat saying that the provider was not found.
      val pm = FacebookSdk.getApplicationContext().packageManager
      val contentProviderName = appInfo.getPackage() + PLATFORM_PROVIDER
      var providerInfo: ProviderInfo? = null
      try {
        providerInfo = pm.resolveContentProvider(contentProviderName, 0)
      } catch (e: RuntimeException) {
        // Accessing a dead provider will cause an DeadObjectException in the
        // package manager. It will be thrown as a Runtime Exception.
        // This will cause a incorrect indication of if the FB app installed but
        // it is better then crashing.
        Log.e(TAG, "Failed to query content resolver.", e)
      }
      if (providerInfo != null) {
        c =
            try {
              contentResolver.query(uri, projection, null, null, null)
            } catch (ex: NullPointerException) {
              Log.e(TAG, "Failed to query content resolver.")
              // Meizu devices running Android 5.0+ have a bug where they can throw a
              // NullPointerException when trying resolve a ContentProvider. Additionally,
              // rarely some 5.0+ devices have a bug which can rarely cause a
              // SecurityException to be thrown. Also, on some Samsung 4.4 / 7.0 / 7.1 devices
              // an IllegalArgumentException may be thrown with message "attempt to launch
              // content provider before system ready".  This will cause a incorrect indication
              // of if the FB app installed but it is better then crashing.
              null
            } catch (ex: SecurityException) {
              Log.e(TAG, "Failed to query content resolver.")
              null
            } catch (ex: IllegalArgumentException) {
              Log.e(TAG, "Failed to query content resolver.")
              null
            }
        if (c != null) {
          while (c.moveToNext()) {
            val version = c.getInt(c.getColumnIndex(PLATFORM_PROVIDER_VERSION_COLUMN))
            allAvailableVersions.add(version)
          }
        }
      }
    } finally {
      c?.close()
    }
    return allAvailableVersions
  }

  @JvmStatic
  fun computeLatestAvailableVersionFromVersionSpec(
      allAvailableFacebookAppVersions: TreeSet<Int>?,
      latestSdkVersion: Int,
      versionSpec: IntArray
  ): Int {
    if (allAvailableFacebookAppVersions == null) {
      return NO_PROTOCOL_AVAILABLE
    }
    // Remember that these ranges are sorted in ascending order and can be unbounded. So we are
    // starting from the end of the version-spec array and working backwards, to try get the
    // newest possible version
    var versionSpecIndex = versionSpec.size - 1
    val fbAppVersionsIterator = allAvailableFacebookAppVersions.descendingIterator()
    var latestFacebookAppVersion = -1
    while (fbAppVersionsIterator.hasNext()) {
      val fbAppVersion = fbAppVersionsIterator.next()

      // We're holding on to the greatest fb-app version available.
      latestFacebookAppVersion = max(latestFacebookAppVersion, fbAppVersion)

      // If there is a newer version in the versionSpec, throw it away, we don't have it
      while (versionSpecIndex >= 0 && versionSpec[versionSpecIndex] > fbAppVersion) {
        versionSpecIndex--
      }
      if (versionSpecIndex < 0) {
        // There was no fb app version that fell into any range in the versionSpec - or -
        // the versionSpec was empty, which means that this action is not supported.
        return NO_PROTOCOL_AVAILABLE
      }

      // If we are here, we know we are within a range specified in the versionSpec. We should
      // see if it is a disabled or enabled range.
      if (versionSpec[versionSpecIndex] == fbAppVersion) {
        // if the versionSpecIndex is even, it is enabled; if odd, disabled
        return if (versionSpecIndex % 2 == 0) min(latestFacebookAppVersion, latestSdkVersion)
        else NO_PROTOCOL_AVAILABLE
      }
    }
    return NO_PROTOCOL_AVAILABLE
  }

  private fun buildPlatformProviderVersionURI(appInfo: NativeAppInfo): Uri {
    return Uri.parse(CONTENT_SCHEME + appInfo.getPackage() + PLATFORM_PROVIDER_VERSIONS)
  }

  abstract class NativeAppInfo {
    abstract fun getPackage(): String
    abstract fun getLoginActivity(): String?
    private var availableVersions: TreeSet<Int>? = null
    open fun getResponseType(): String =
        ServerProtocol.DIALOG_RESPONSE_TYPE_ID_TOKEN_AND_SIGNED_REQUEST
    open fun onAvailableVersionsNullOrEmpty() = Unit

    fun getAvailableVersions(): TreeSet<Int>? {
      if (availableVersions == null || availableVersions?.isEmpty() != false) {
        fetchAvailableVersions(false)
      }
      return availableVersions
    }

    @Synchronized
    fun fetchAvailableVersions(force: Boolean) {
      if (force || availableVersions == null || availableVersions?.isEmpty() != false) {
        availableVersions = fetchAllAvailableProtocolVersionsForAppInfo(this)
      }
      if (availableVersions.isNullOrEmpty()) {
        onAvailableVersionsNullOrEmpty()
      }
    }
  }

  private class KatanaAppInfo : NativeAppInfo() {
    override fun getLoginActivity() = FACEBOOK_PROXY_AUTH_ACTIVITY
    override fun getPackage() = "com.facebook.katana"

    override fun onAvailableVersionsNullOrEmpty() {
      if (isAndroidAPIVersionNotLessThan30()) {
        Log.w(
            TAG,
            "Apps that target Android API 30+ (Android 11+) cannot call Facebook native apps unless the package visibility needs are declared. Please follow https://developers.facebook.com/docs/android/troubleshooting/#faq_267321845055988 to make the declaration.")
      }
    }

    private fun isAndroidAPIVersionNotLessThan30(): Boolean {
      val context = FacebookSdk.getApplicationContext()
      val applicationInfo = context.applicationInfo
      return applicationInfo.targetSdkVersion >= 30
    }
  }

  private class MessengerAppInfo : NativeAppInfo() {
    override fun getLoginActivity() = null
    override fun getPackage() = "com.facebook.orca"
  }

  private class WakizashiAppInfo : NativeAppInfo() {
    override fun getLoginActivity() = FACEBOOK_PROXY_AUTH_ACTIVITY
    override fun getPackage() = "com.facebook.wakizashi"
  }

  private class FBLiteAppInfo : NativeAppInfo() {
    override fun getLoginActivity() = FACEBOOK_LITE_ACTIVITY
    override fun getPackage() = "com.facebook.lite"
    companion object {
      const val FACEBOOK_LITE_ACTIVITY = "com.facebook.lite.platform.LoginGDPDialogActivity"
    }
  }

  private class InstagramAppInfo : NativeAppInfo() {
    override fun getLoginActivity(): String = "com.instagram.platform.AppAuthorizeActivity"
    override fun getPackage(): String = "com.instagram.android"
    override fun getResponseType(): String = ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES
  }

  private class EffectTestAppInfo : NativeAppInfo() {
    override fun getLoginActivity() = null
    override fun getPackage() = "com.facebook.arstudio.player"
  }

  class ProtocolVersionQueryResult private constructor() {
    var appInfo: NativeAppInfo? = null
      private set
    var protocolVersion = 0
      private set

    companion object {
      @JvmStatic
      fun create(nativeAppInfo: NativeAppInfo?, protocolVersion: Int): ProtocolVersionQueryResult {
        val result = ProtocolVersionQueryResult()
        result.appInfo = nativeAppInfo
        result.protocolVersion = protocolVersion
        return result
      }

      @JvmStatic
      fun createEmpty(): ProtocolVersionQueryResult {
        val result = ProtocolVersionQueryResult()
        result.protocolVersion = NO_PROTOCOL_AVAILABLE
        return result
      }
    }
  }
}
