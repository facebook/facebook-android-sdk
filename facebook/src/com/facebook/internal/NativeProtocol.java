/**
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

package com.facebook.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookSdk;
import com.facebook.login.DefaultAudience;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class NativeProtocol {

    public static final int NO_PROTOCOL_AVAILABLE = -1;

    private static final String FACEBOOK_PROXY_AUTH_ACTIVITY = "com.facebook.katana.ProxyAuth";
    private static final String FACEBOOK_TOKEN_REFRESH_ACTIVITY =
            "com.facebook.katana.platform.TokenRefreshService";

    public static final String FACEBOOK_PROXY_AUTH_PERMISSIONS_KEY = "scope";
    public static final String FACEBOOK_PROXY_AUTH_APP_ID_KEY = "client_id";
    public static final String FACEBOOK_PROXY_AUTH_E2E_KEY = "e2e";

    // ---------------------------------------------------------------------------------------------
    // Native Protocol updated 2012-11

    static final String INTENT_ACTION_PLATFORM_ACTIVITY = "com.facebook.platform.PLATFORM_ACTIVITY";
    static final String INTENT_ACTION_PLATFORM_SERVICE = "com.facebook.platform.PLATFORM_SERVICE";

    public static final int PROTOCOL_VERSION_20121101 = 20121101;
    public static final int PROTOCOL_VERSION_20130502 = 20130502;
    public static final int PROTOCOL_VERSION_20130618 = 20130618;
    public static final int PROTOCOL_VERSION_20131107 = 20131107;
    public static final int PROTOCOL_VERSION_20140204 = 20140204;
    public static final int PROTOCOL_VERSION_20140324 = 20140324;
    public static final int PROTOCOL_VERSION_20140701 = 20140701;
    public static final int PROTOCOL_VERSION_20141001 = 20141001;
    public static final int PROTOCOL_VERSION_20141028 = 20141028;
    public static final int PROTOCOL_VERSION_20141107 = 20141107; // Bucketed Result Intents
    public static final int PROTOCOL_VERSION_20141218 = 20141218;

    public static final String EXTRA_PROTOCOL_VERSION =
            "com.facebook.platform.protocol.PROTOCOL_VERSION";
    public static final String EXTRA_PROTOCOL_ACTION =
            "com.facebook.platform.protocol.PROTOCOL_ACTION";
    public static final String EXTRA_PROTOCOL_CALL_ID =
            "com.facebook.platform.protocol.CALL_ID";
    public static final String EXTRA_GET_INSTALL_DATA_PACKAGE =
            "com.facebook.platform.extra.INSTALLDATA_PACKAGE";

    public static final String EXTRA_PROTOCOL_BRIDGE_ARGS =
            "com.facebook.platform.protocol.BRIDGE_ARGS";

    public static final String EXTRA_PROTOCOL_METHOD_ARGS =
            "com.facebook.platform.protocol.METHOD_ARGS";

    public static final String EXTRA_PROTOCOL_METHOD_RESULTS =
            "com.facebook.platform.protocol.RESULT_ARGS";

    public static final String BRIDGE_ARG_APP_NAME_STRING = "app_name";
    public static final String BRIDGE_ARG_ACTION_ID_STRING = "action_id";
    public static final String BRIDGE_ARG_ERROR_BUNDLE = "error";

    public static final String EXTRA_DIALOG_COMPLETE_KEY =
            "com.facebook.platform.extra.DID_COMPLETE";
    public static final String EXTRA_DIALOG_COMPLETION_GESTURE_KEY =
            "com.facebook.platform.extra.COMPLETION_GESTURE";

    public static final String RESULT_ARGS_DIALOG_COMPLETE_KEY = "didComplete";
    public static final String RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY = "completionGesture";

    // Messages supported by PlatformService:
    public static final int MESSAGE_GET_ACCESS_TOKEN_REQUEST    = 0x10000;
    public static final int MESSAGE_GET_ACCESS_TOKEN_REPLY      = 0x10001;
    static final int MESSAGE_GET_PROTOCOL_VERSIONS_REQUEST      = 0x10002;
    static final int MESSAGE_GET_PROTOCOL_VERSIONS_REPLY        = 0x10003;
    public static final int MESSAGE_GET_INSTALL_DATA_REQUEST    = 0x10004;
    public static final int MESSAGE_GET_INSTALL_DATA_REPLY      = 0x10005;
    public static final int MESSAGE_GET_LIKE_STATUS_REQUEST     = 0x10006;
    public static final int MESSAGE_GET_LIKE_STATUS_REPLY       = 0x10007;

    // MESSAGE_ERROR_REPLY data keys:
    // See STATUS_*

    // MESSAGE_GET_ACCESS_TOKEN_REQUEST data keys:
    // EXTRA_APPLICATION_ID

    // MESSAGE_GET_ACCESS_TOKEN_REPLY data keys:
    // EXTRA_ACCESS_TOKEN
    // EXTRA_EXPIRES_SECONDS_SINCE_EPOCH
    // EXTRA_PERMISSIONS

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
    static final String EXTRA_PROTOCOL_VERSIONS = "com.facebook.platform.extra.PROTOCOL_VERSIONS";

    // Values of EXTRA_PROTOCOL_ACTION supported by PlatformActivity:
    public static final String ACTION_FEED_DIALOG =
            "com.facebook.platform.action.request.FEED_DIALOG";
    public static final String ACTION_MESSAGE_DIALOG =
            "com.facebook.platform.action.request.MESSAGE_DIALOG";
    public static final String ACTION_OGACTIONPUBLISH_DIALOG =
            "com.facebook.platform.action.request.OGACTIONPUBLISH_DIALOG";
    public static final String ACTION_OGMESSAGEPUBLISH_DIALOG =
            "com.facebook.platform.action.request.OGMESSAGEPUBLISH_DIALOG";
    public static final String ACTION_LIKE_DIALOG =
            "com.facebook.platform.action.request.LIKE_DIALOG";
    // The value of ACTION_APPINVITE_DIALOG is different since that is what is on the server.
    public static final String ACTION_APPINVITE_DIALOG =
            "com.facebook.platform.action.request.APPINVITES_DIALOG";

    // Extras supported for ACTION_LOGIN_DIALOG:
    public static final String EXTRA_PERMISSIONS = "com.facebook.platform.extra.PERMISSIONS";
    public static final String EXTRA_APPLICATION_ID = "com.facebook.platform.extra.APPLICATION_ID";
    public static final String EXTRA_APPLICATION_NAME =
            "com.facebook.platform.extra.APPLICATION_NAME";
    public static final String EXTRA_USER_ID = "com.facebook.platform.extra.USER_ID";

    // Extras returned by setResult() for ACTION_LOGIN_DIALOG
    public static final String EXTRA_ACCESS_TOKEN = "com.facebook.platform.extra.ACCESS_TOKEN";
    public static final String EXTRA_EXPIRES_SECONDS_SINCE_EPOCH =
            "com.facebook.platform.extra.EXPIRES_SECONDS_SINCE_EPOCH";
    // EXTRA_PERMISSIONS

    public static final String RESULT_ARGS_ACCESS_TOKEN = "access_token";
    public static final String RESULT_ARGS_EXPIRES_SECONDS_SINCE_EPOCH =
            "expires_seconds_since_epoch";
    public static final String RESULT_ARGS_PERMISSIONS = "permissions";

    // OG objects will have this key to set to true if they should be created as part of OG Action
    // publish
    public static final String OPEN_GRAPH_CREATE_OBJECT_KEY = "fbsdk:create_object";
    // Determines whether an image is user generated
    public static final String IMAGE_USER_GENERATED_KEY = "user_generated";
    // url key for images
    public static final String IMAGE_URL_KEY = "url";

    // Keys for status data in MESSAGE_ERROR_REPLY from PlatformService and for error
    // extras returned by PlatformActivity's setResult() in case of errors:
    public static final String STATUS_ERROR_TYPE = "com.facebook.platform.status.ERROR_TYPE";
    public static final String STATUS_ERROR_DESCRIPTION =
            "com.facebook.platform.status.ERROR_DESCRIPTION";
    public static final String STATUS_ERROR_CODE = "com.facebook.platform.status.ERROR_CODE";
    public static final String STATUS_ERROR_SUBCODE = "com.facebook.platform.status.ERROR_SUBCODE";
    public static final String STATUS_ERROR_JSON = "com.facebook.platform.status.ERROR_JSON";

    public static final String BRIDGE_ARG_ERROR_TYPE = "error_type";
    public static final String BRIDGE_ARG_ERROR_DESCRIPTION = "error_description";
    public static final String BRIDGE_ARG_ERROR_CODE = "error_code";
    public static final String BRIDGE_ARG_ERROR_SUBCODE = "error_subcode";
    public static final String BRIDGE_ARG_ERROR_JSON = "error_json";

    // Expected values for ERROR_KEY_TYPE.  Clients should tolerate other values:
    public static final String ERROR_UNKNOWN_ERROR = "UnknownError";
    public static final String ERROR_PROTOCOL_ERROR = "ProtocolError";
    public static final String ERROR_USER_CANCELED = "UserCanceled";
    public static final String ERROR_APPLICATION_ERROR = "ApplicationError";
    public static final String ERROR_NETWORK_ERROR = "NetworkError";
    public static final String ERROR_PERMISSION_DENIED = "PermissionDenied";
    public static final String ERROR_SERVICE_DISABLED = "ServiceDisabled";

    public static final String WEB_DIALOG_URL = "url";
    public static final String WEB_DIALOG_ACTION = "action";
    public static final String WEB_DIALOG_PARAMS = "params";
    public static final String WEB_DIALOG_IS_FALLBACK = "is_fallback";

    public static final String AUDIENCE_ME = "only_me";
    public static final String AUDIENCE_FRIENDS = "friends";
    public static final String AUDIENCE_EVERYONE = "everyone";

    private static final String CONTENT_SCHEME = "content://";
    private static final String PLATFORM_PROVIDER = ".provider.PlatformProvider";
    private static final String PLATFORM_PROVIDER_VERSIONS = PLATFORM_PROVIDER + "/versions";

    // Columns returned by PlatformProvider
    private static final String PLATFORM_PROVIDER_VERSION_COLUMN = "version";

    private static abstract class NativeAppInfo {
        abstract protected String getPackage();

        private static final String FBI_HASH = "a4b7452e2ed8f5f191058ca7bbfd26b0d3214bfc";
        private static final String FBL_HASH = "5e8f16062ea3cd2c4a0d547876baa6f38cabf625";
        private static final String FBR_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9";

        private static final HashSet<String> validAppSignatureHashes = buildAppSignatureHashes();

        private TreeSet<Integer> availableVersions;

        private static HashSet<String> buildAppSignatureHashes() {
            HashSet<String> set = new HashSet<String>();
            set.add(FBR_HASH);
            set.add(FBI_HASH);
            set.add(FBL_HASH);
            return set;
        }

        public boolean validateSignature(Context context, String packageName) {
            String brand = Build.BRAND;
            int applicationFlags = context.getApplicationInfo().flags;
            if (brand.startsWith("generic") &&
                    (applicationFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // We are debugging on an emulator, don't validate package signature.
                return true;
            }

            PackageInfo packageInfo = null;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

            for (Signature signature : packageInfo.signatures) {
                String hashedSignature = Utility.sha1hash(signature.toByteArray());
                if (validAppSignatureHashes.contains(hashedSignature)) {
                    return true;
                }
            }

            return false;
        }

        public TreeSet<Integer> getAvailableVersions() {
            if (availableVersions == null) {
                fetchAvailableVersions(false);
            }
            return availableVersions;
        }

        private synchronized void fetchAvailableVersions(boolean force) {
            if (force || availableVersions == null) {
                availableVersions = fetchAllAvailableProtocolVersionsForAppInfo(this);
            }
        }
    }

    private static class KatanaAppInfo extends NativeAppInfo {
        static final String KATANA_PACKAGE = "com.facebook.katana";

        @Override
        protected String getPackage() {
            return KATANA_PACKAGE;
        }
    }

    private static class MessengerAppInfo extends NativeAppInfo {
        static final String MESSENGER_PACKAGE = "com.facebook.orca";

        @Override
        protected String getPackage() {
            return MESSENGER_PACKAGE;
        }
    }

    private static class WakizashiAppInfo extends NativeAppInfo {
        static final String WAKIZASHI_PACKAGE = "com.facebook.wakizashi";

        @Override
        protected String getPackage() {
            return WAKIZASHI_PACKAGE;
        }
    }

    private static final NativeAppInfo FACEBOOK_APP_INFO = new KatanaAppInfo();
    private static List<NativeAppInfo> facebookAppInfoList = buildFacebookAppList();
    private static Map<String, List<NativeAppInfo>> actionToAppInfoMap = buildActionToAppInfoMap();
    private static AtomicBoolean protocolVersionsAsyncUpdating = new AtomicBoolean(false);

    private static List<NativeAppInfo> buildFacebookAppList() {
        List<NativeAppInfo> list = new ArrayList<NativeAppInfo>();

        // Katana needs to be the first thing in the list since it will get selected as the default
        // FACEBOOK_APP_INFO
        list.add(FACEBOOK_APP_INFO);
        list.add(new WakizashiAppInfo());

        return list;
    }

    private static Map<String, List<NativeAppInfo>> buildActionToAppInfoMap() {
        Map<String, List<NativeAppInfo>> map = new HashMap<String, List<NativeAppInfo>>();

        ArrayList<NativeAppInfo> messengerAppInfoList = new ArrayList<NativeAppInfo>();
        messengerAppInfoList.add(new MessengerAppInfo());

        // Add individual actions and the list they should try
        map.put(ACTION_OGACTIONPUBLISH_DIALOG, facebookAppInfoList);
        map.put(ACTION_FEED_DIALOG, facebookAppInfoList);
        map.put(ACTION_LIKE_DIALOG, facebookAppInfoList);
        map.put(ACTION_APPINVITE_DIALOG, facebookAppInfoList);
        map.put(ACTION_MESSAGE_DIALOG, messengerAppInfoList);
        map.put(ACTION_OGMESSAGEPUBLISH_DIALOG, messengerAppInfoList);

        return map;
    }

    static Intent validateActivityIntent(Context context, Intent intent, NativeAppInfo appInfo) {
        if (intent == null) {
            return null;
        }

        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return null;
        }

        if (!appInfo.validateSignature(context, resolveInfo.activityInfo.packageName)) {
            return null;
        }

        return intent;
    }

    static Intent validateServiceIntent(Context context, Intent intent, NativeAppInfo appInfo) {
        if (intent == null) {
            return null;
        }

        ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, 0);
        if (resolveInfo == null) {
            return null;
        }

        if (!appInfo.validateSignature(context, resolveInfo.serviceInfo.packageName)) {
            return null;
        }

        return intent;
    }

    public static Intent createProxyAuthIntent(
            Context context,
            String applicationId,
            Collection<String> permissions,
            String e2e,
            boolean isRerequest,
            boolean isForPublish,
            DefaultAudience defaultAudience) {
        for (NativeAppInfo appInfo : facebookAppInfoList) {
            Intent intent = new Intent()
                    .setClassName(appInfo.getPackage(), FACEBOOK_PROXY_AUTH_ACTIVITY)
                    .putExtra(FACEBOOK_PROXY_AUTH_APP_ID_KEY, applicationId);

            if (!Utility.isNullOrEmpty(permissions)) {
                intent.putExtra(
                        FACEBOOK_PROXY_AUTH_PERMISSIONS_KEY, TextUtils.join(",", permissions));
            }
            if (!Utility.isNullOrEmpty(e2e)) {
                intent.putExtra(FACEBOOK_PROXY_AUTH_E2E_KEY, e2e);
            }

            intent.putExtra(
                    ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
                    ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST);
            intent.putExtra(
                    ServerProtocol.DIALOG_PARAM_RETURN_SCOPES,
                    ServerProtocol.DIALOG_RETURN_SCOPES_TRUE);
            if (isForPublish) {
                intent.putExtra(
                        ServerProtocol.DIALOG_PARAM_DEFAULT_AUDIENCE,
                        defaultAudience.getNativeProtocolAudience());
            }

            // Override the API Version for Auth
            intent.putExtra(
                    ServerProtocol.DIALOG_PARAM_LEGACY_OVERRIDE,
                    ServerProtocol.GRAPH_API_VERSION);

            // Set the re-request auth type for requests
            if (isRerequest) {
                intent.putExtra(
                        ServerProtocol.DIALOG_PARAM_AUTH_TYPE,
                        ServerProtocol.DIALOG_REREQUEST_AUTH_TYPE);
            }

            intent = validateActivityIntent(context, intent, appInfo);

            if (intent != null) {
                return intent;
            }
        }
        return null;
    }

    public static Intent createTokenRefreshIntent(Context context) {
        for (NativeAppInfo appInfo : facebookAppInfoList) {
            Intent intent = new Intent()
                    .setClassName(appInfo.getPackage(), FACEBOOK_TOKEN_REFRESH_ACTIVITY);

            intent = validateServiceIntent(context, intent, appInfo);

            if (intent != null) {
                return intent;
            }
        }
        return null;
    }

    public static final int getLatestKnownVersion() {
        return KNOWN_PROTOCOL_VERSIONS.get(0);
    }

    // Note: be sure this stays sorted in descending order; add new versions at the beginning
    private static final List<Integer> KNOWN_PROTOCOL_VERSIONS =
            Arrays.asList(
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
                    PROTOCOL_VERSION_20121101
            );

    private static Intent findActivityIntent(
            Context context,
            String activityAction,
            String internalAction) {
        List<NativeAppInfo> list = actionToAppInfoMap.get(internalAction);
        if (list == null) {
            return null;
        }

        Intent intent = null;
        for (NativeAppInfo appInfo : list) {
            intent = new Intent()
                    .setAction(activityAction)
                    .setPackage(appInfo.getPackage())
                    .addCategory(Intent.CATEGORY_DEFAULT);
            intent = validateActivityIntent(context, intent, appInfo);
            if (intent != null) {
                return intent;
            }
        }

        return intent;
    }

    public static boolean isVersionCompatibleWithBucketedIntent(int version) {
        return KNOWN_PROTOCOL_VERSIONS.contains(version) && version >= PROTOCOL_VERSION_20140701;
    }

    /**
     * Will create an Intent that can be used to invoke an action in a Facebook app via the
     * Native Protocol
     */
    public static Intent createPlatformActivityIntent(
            Context context,
            String callId,
            String action,
            int version,
            Bundle extras) {
        Intent intent = findActivityIntent(context, INTENT_ACTION_PLATFORM_ACTIVITY, action);
        if (intent == null) {
            return null;
        }

        setupProtocolRequestIntent(intent, callId, action, version, extras);

        return intent;
    }

    /**
     * Will setup the passed in Intent in the shape of a Native Protocol request Intent.
     */
    public static void setupProtocolRequestIntent(
            Intent intent,
            String callId,
            String action,
            int version,
            Bundle params) {
        String applicationId = FacebookSdk.getApplicationId();
        String applicationName = FacebookSdk.getApplicationName();

        intent.putExtra(EXTRA_PROTOCOL_VERSION, version)
                .putExtra(EXTRA_PROTOCOL_ACTION, action)
                .putExtra(EXTRA_APPLICATION_ID, applicationId);

        if (isVersionCompatibleWithBucketedIntent(version)) {
            // This is a bucketed intent
            Bundle bridgeArguments = new Bundle();
            bridgeArguments.putString(BRIDGE_ARG_ACTION_ID_STRING, callId);
            Utility.putNonEmptyString(bridgeArguments, BRIDGE_ARG_APP_NAME_STRING, applicationName);

            intent.putExtra(EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArguments);

            Bundle methodArguments = (params == null) ? new Bundle() : params;
            intent.putExtra(EXTRA_PROTOCOL_METHOD_ARGS, methodArguments);
        } else {
            // This is the older flat intent
            intent.putExtra(EXTRA_PROTOCOL_CALL_ID, callId);
            if (!Utility.isNullOrEmpty(applicationName)) {
                intent.putExtra(EXTRA_APPLICATION_NAME, applicationName);
            }
            intent.putExtras(params);
        }
    }

    /**
     * Use this method to set a result on an Activity, where the result needs to be in the shape
     * of the native protocol used for native dialogs.
     */
    public static Intent createProtocolResultIntent(
            Intent requestIntent,
            Bundle results,
            FacebookException error) {
        UUID callId = NativeProtocol.getCallIdFromIntent(requestIntent);
        if (callId == null) {
            return null;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PROTOCOL_VERSION, getProtocolVersionFromIntent(requestIntent));

        Bundle bridgeArguments = new Bundle();
        bridgeArguments.putString(BRIDGE_ARG_ACTION_ID_STRING, callId.toString());
        if (error != null) {
            bridgeArguments.putBundle(
                    BRIDGE_ARG_ERROR_BUNDLE, NativeProtocol.createBundleForException(error));
        }
        resultIntent.putExtra(EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArguments);

        if (results != null) {
            resultIntent.putExtra(EXTRA_PROTOCOL_METHOD_RESULTS, results);
        }

        return resultIntent;
    }

    public static Intent createPlatformServiceIntent(Context context) {
        for (NativeAppInfo appInfo : facebookAppInfoList) {
            Intent intent = new Intent(INTENT_ACTION_PLATFORM_SERVICE)
                    .setPackage(appInfo.getPackage())
                    .addCategory(Intent.CATEGORY_DEFAULT);
            intent = validateServiceIntent(context, intent, appInfo);
            if (intent != null) {
                return intent;
            }
        }
        return null;
    }

    public static int getProtocolVersionFromIntent(Intent intent) {
        return intent.getIntExtra(EXTRA_PROTOCOL_VERSION, 0);
    }

    public static UUID getCallIdFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        int version = getProtocolVersionFromIntent(intent);
        String callIdString = null;
        if (isVersionCompatibleWithBucketedIntent(version)) {
            Bundle bridgeArgs = intent.getBundleExtra(EXTRA_PROTOCOL_BRIDGE_ARGS);
            if (bridgeArgs != null) {
                callIdString = bridgeArgs.getString(BRIDGE_ARG_ACTION_ID_STRING);
            }
        } else {
            callIdString = intent.getStringExtra(EXTRA_PROTOCOL_CALL_ID);
        }

        UUID callId = null;
        if (callIdString != null) {
            try {
                callId = UUID.fromString(callIdString);
            } catch (IllegalArgumentException exception) {
            }
        }
        return callId;
    }

    public static Bundle getBridgeArgumentsFromIntent(Intent intent) {
        int version = getProtocolVersionFromIntent(intent);
        if (!isVersionCompatibleWithBucketedIntent(version)) {
            return null;
        }

        return intent.getBundleExtra(EXTRA_PROTOCOL_BRIDGE_ARGS);
    }

    public static Bundle getMethodArgumentsFromIntent(Intent intent) {
        int version = getProtocolVersionFromIntent(intent);
        if (!isVersionCompatibleWithBucketedIntent(version)) {
            return intent.getExtras();
        }

        return intent.getBundleExtra(EXTRA_PROTOCOL_METHOD_ARGS);
    }

    public static Bundle getSuccessResultsFromIntent(Intent resultIntent) {
        int version = getProtocolVersionFromIntent(resultIntent);
        Bundle extras = resultIntent.getExtras();
        if (!isVersionCompatibleWithBucketedIntent(version) || extras == null) {
            return extras;
        }

        return extras.getBundle(EXTRA_PROTOCOL_METHOD_RESULTS);
    }

    public static boolean isErrorResult(Intent resultIntent) {
        Bundle bridgeArgs = getBridgeArgumentsFromIntent(resultIntent);
        if (bridgeArgs != null) {
            return bridgeArgs.containsKey(BRIDGE_ARG_ERROR_BUNDLE);
        } else {
            return resultIntent.hasExtra(STATUS_ERROR_TYPE);
        }
    }

    public static Bundle getErrorDataFromResultIntent(Intent resultIntent) {
        if (!isErrorResult(resultIntent)) {
            return null;
        }

        Bundle bridgeArgs = getBridgeArgumentsFromIntent(resultIntent);
        if (bridgeArgs != null) {
            return bridgeArgs.getBundle(BRIDGE_ARG_ERROR_BUNDLE);
        }

        return resultIntent.getExtras();
    }

    public static FacebookException getExceptionFromErrorData(Bundle errorData) {
        if (errorData == null) {
            return null;
        }

        String type = errorData.getString(BRIDGE_ARG_ERROR_TYPE);
        if (type == null) {
            type = errorData.getString(STATUS_ERROR_TYPE);
        }

        String description = errorData.getString(BRIDGE_ARG_ERROR_DESCRIPTION);
        if (description == null) {
            description = errorData.getString(STATUS_ERROR_DESCRIPTION);
        }

        if (type != null && type.equalsIgnoreCase(ERROR_USER_CANCELED)) {
            return new FacebookOperationCanceledException(description);
        }

        /* TODO parse error values and create appropriate exception class */
        return new FacebookException(description);
    }

    public static Bundle createBundleForException(FacebookException e) {
        if (e == null) {
            return null;
        }

        Bundle errorBundle = new Bundle();
        errorBundle.putString(BRIDGE_ARG_ERROR_DESCRIPTION, e.toString());
        if (e instanceof FacebookOperationCanceledException) {
            errorBundle.putString(BRIDGE_ARG_ERROR_TYPE, ERROR_USER_CANCELED);
        }

        return errorBundle;
    }

    public static int getLatestAvailableProtocolVersionForService(final int minimumVersion) {
        // Services are currently always against the Facebook App
        return getLatestAvailableProtocolVersionForAppInfoList(
                facebookAppInfoList, new int[]{minimumVersion});
    }

    public static int getLatestAvailableProtocolVersionForAction(
            String action,
            int[] versionSpec) {
        List<NativeAppInfo> appInfoList = actionToAppInfoMap.get(action);
        return getLatestAvailableProtocolVersionForAppInfoList(appInfoList, versionSpec);
    }

    private static int getLatestAvailableProtocolVersionForAppInfoList(
            List<NativeAppInfo> appInfoList,
            int[] versionSpec) {
        // Kick off an update
        updateAllAvailableProtocolVersionsAsync();

        if (appInfoList == null) {
            return NO_PROTOCOL_AVAILABLE;
        }

        // Could potentially cache the NativeAppInfo to latestProtocolVersion
        for (NativeAppInfo appInfo : appInfoList) {
            int protocolVersion =
                    computeLatestAvailableVersionFromVersionSpec(
                            appInfo.getAvailableVersions(),
                            getLatestKnownVersion(),
                            versionSpec);

            if (protocolVersion != NO_PROTOCOL_AVAILABLE) {
                return protocolVersion;
            }
        }

        return NO_PROTOCOL_AVAILABLE;
    }

    public static void updateAllAvailableProtocolVersionsAsync() {
        if (!protocolVersionsAsyncUpdating.compareAndSet(false, true)) {
            return;
        }

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (NativeAppInfo appInfo : facebookAppInfoList) {
                        appInfo.fetchAvailableVersions(true);
                    }
                } finally {
                    protocolVersionsAsyncUpdating.set(false);
                }
            }
        });
    }

    private static TreeSet<Integer> fetchAllAvailableProtocolVersionsForAppInfo(
            NativeAppInfo appInfo) {
        TreeSet<Integer> allAvailableVersions = new TreeSet<>();

        Context appContext = FacebookSdk.getApplicationContext();
        ContentResolver contentResolver = appContext.getContentResolver();

        String [] projection = new String[]{ PLATFORM_PROVIDER_VERSION_COLUMN };
        Uri uri = buildPlatformProviderVersionURI(appInfo);
        Cursor c = null;
        try {
            // First see if the base provider exists as a check for whether the native app is
            // installed. We do this prior to querying, to prevent errors from being output to
            // logcat saying that the provider was not found.
            PackageManager pm = FacebookSdk.getApplicationContext().getPackageManager();
            String contentProviderName = appInfo.getPackage() + PLATFORM_PROVIDER;
            ProviderInfo pInfo = pm.resolveContentProvider(contentProviderName, 0);
            if (pInfo != null) {
                c = contentResolver.query(uri, projection, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        int version = c.getInt(c.getColumnIndex(PLATFORM_PROVIDER_VERSION_COLUMN));
                        allAvailableVersions.add(version);
                    }
                }
            }

            return allAvailableVersions;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static int computeLatestAvailableVersionFromVersionSpec(
            TreeSet<Integer> allAvailableFacebookAppVersions,
            int latestSdkVersion,
            int[] versionSpec) {
        // Remember that these ranges are sorted in ascending order and can be unbounded. So we are
        // starting from the end of the version-spec array and working backwards, to try get the
        // newest possible version
        int versionSpecIndex = versionSpec.length - 1;
        Iterator<Integer> fbAppVersionsIterator =
                allAvailableFacebookAppVersions.descendingIterator();
        int latestFacebookAppVersion = -1;

        while (fbAppVersionsIterator.hasNext()) {
            int fbAppVersion = fbAppVersionsIterator.next();

            // We're holding on to the greatest fb-app version available.
            latestFacebookAppVersion = Math.max(latestFacebookAppVersion, fbAppVersion);

            // If there is a newer version in the versionSpec, throw it away, we don't have it
            while (versionSpecIndex >= 0 && versionSpec[versionSpecIndex] > fbAppVersion) {
                versionSpecIndex--;
            }

            if (versionSpecIndex < 0) {
                // There was no fb app version that fell into any range in the versionSpec - or -
                // the versionSpec was empty, which means that this action is not supported.
                return NO_PROTOCOL_AVAILABLE;
            }

            // If we are here, we know we are within a range specified in the versionSpec. We should
            // see if it is a disabled or enabled range.

            if (versionSpec[versionSpecIndex] == fbAppVersion) {
                // if the versionSpecIndex is even, it is enabled; if odd, disabled
                return (
                        versionSpecIndex % 2 == 0 ?
                                Math.min(latestFacebookAppVersion, latestSdkVersion) :
                                NO_PROTOCOL_AVAILABLE
                );
            }
        }

        return NO_PROTOCOL_AVAILABLE;
    }

    private static Uri buildPlatformProviderVersionURI(NativeAppInfo appInfo) {
        return Uri.parse(CONTENT_SCHEME + appInfo.getPackage() + PLATFORM_PROVIDER_VERSIONS);
    }
}
