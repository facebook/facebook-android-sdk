/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.applinks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to encapsulate an app link, and provide methods for constructing the data from various
 * sources
 */
public class AppLinkData {

  /**
   * Key that should be used to pull out the UTC Unix tap-time from the arguments for this app link.
   */
  public static final String ARGUMENTS_TAPTIME_KEY = "com.facebook.platform.APPLINK_TAP_TIME_UTC";

  /** Key that should be used to get the "referer_data" field for this app link. */
  public static final String ARGUMENTS_REFERER_DATA_KEY = "referer_data";

  /** Key that should be used to get the "extras" field for this app link. */
  public static final String ARGUMENTS_EXTRAS_KEY = "extras";

  /**
   * Key that should be used to pull out the native class that would have been used if the applink
   * was deferred.
   */
  public static final String ARGUMENTS_NATIVE_CLASS_KEY =
      "com.facebook.platform.APPLINK_NATIVE_CLASS";

  /**
   * Key that should be used to pull out the native url that would have been used if the applink was
   * deferred.
   */
  public static final String ARGUMENTS_NATIVE_URL = "com.facebook.platform.APPLINK_NATIVE_URL";

  private static final String BUNDLE_APPLINK_ARGS_KEY = "com.facebook.platform.APPLINK_ARGS";
  private static final String BUNDLE_AL_APPLINK_DATA_KEY = "al_applink_data";
  private static final String APPLINK_BRIDGE_ARGS_KEY = "bridge_args";
  private static final String APPLINK_METHOD_ARGS_KEY = "method_args";
  private static final String APPLINK_VERSION_KEY = "version";
  private static final String BRIDGE_ARGS_METHOD_KEY = "method";
  private static final String DEFERRED_APP_LINK_EVENT = "DEFERRED_APP_LINK";
  private static final String DEFERRED_APP_LINK_PATH = "%s/activities";

  private static final String DEFERRED_APP_LINK_ARGS_FIELD = "applink_args";
  private static final String DEFERRED_APP_LINK_CLASS_FIELD = "applink_class";
  private static final String DEFERRED_APP_LINK_CLICK_TIME_FIELD = "click_time";
  private static final String DEFERRED_APP_LINK_URL_FIELD = "applink_url";

  private static final String AUTO_APPLINK_FLAG_KEY = "is_auto_applink";
  private static final String METHOD_ARGS_TARGET_URL_KEY = "target_url";
  private static final String METHOD_ARGS_REF_KEY = "ref";
  private static final String REFERER_DATA_REF_KEY = "fb_ref";
  private static final String EXTRAS_DEEPLINK_CONTEXT_KEY = "deeplink_context";
  private static final String PROMOTION_CODE_KEY = "promo_code";
  private static final String TAG = AppLinkData.class.getCanonicalName();

  @Nullable private String ref;
  @Nullable private Uri targetUri;
  @Nullable private JSONObject arguments;
  @Nullable private Bundle argumentBundle;
  @Nullable private String promotionCode;
  @Nullable private JSONObject appLinkData;

  /**
   * Asynchronously fetches app link information that might have been stored for use after
   * installation of the app
   *
   * @param context The context
   * @param completionHandler CompletionHandler to be notified with the AppLinkData object or null
   *     if none is available. Must not be null.
   */
  public static void fetchDeferredAppLinkData(
      Context context, CompletionHandler completionHandler) {
    fetchDeferredAppLinkData(context, null, completionHandler);
  }

  /**
   * Asynchronously fetches app link information that might have been stored for use after
   * installation of the app
   *
   * @param context The context
   * @param applicationId Facebook application Id. If null, it is taken from the manifest
   * @param completionHandler CompletionHandler to be notified with the AppLinkData object or null
   *     if none is available. Must not be null.
   */
  public static void fetchDeferredAppLinkData(
      Context context, String applicationId, final CompletionHandler completionHandler) {
    Validate.notNull(context, "context");
    Validate.notNull(completionHandler, "completionHandler");

    if (applicationId == null) {
      applicationId = Utility.getMetadataApplicationId(context);
    }

    Validate.notNull(applicationId, "applicationId");

    final Context applicationContext = context.getApplicationContext();
    final String applicationIdCopy = applicationId;
    FacebookSdk.getExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                fetchDeferredAppLinkFromServer(
                    applicationContext, applicationIdCopy, completionHandler);
              }
            });
  }

  private static void fetchDeferredAppLinkFromServer(
      Context context, String applicationId, final CompletionHandler completionHandler) {

    JSONObject deferredApplinkParams = new JSONObject();
    try {
      deferredApplinkParams.put("event", DEFERRED_APP_LINK_EVENT);
      Utility.setAppEventAttributionParameters(
          deferredApplinkParams,
          AttributionIdentifiers.getAttributionIdentifiers(context),
          AppEventsLogger.getAnonymousAppDeviceGUID(context),
          FacebookSdk.getLimitEventAndDataUsage(context),
          context);
      Utility.setAppEventExtendedDeviceInfoParameters(
          deferredApplinkParams, FacebookSdk.getApplicationContext());
      deferredApplinkParams.put("application_package_name", context.getPackageName());
    } catch (JSONException e) {
      throw new FacebookException("An error occurred while preparing deferred app link", e);
    }

    String deferredApplinkUrlPath = String.format(DEFERRED_APP_LINK_PATH, applicationId);
    AppLinkData appLinkData = null;

    try {
      GraphRequest deferredApplinkRequest =
          GraphRequest.newPostRequest(null, deferredApplinkUrlPath, deferredApplinkParams, null);
      GraphResponse deferredApplinkResponse = deferredApplinkRequest.executeAndWait();
      JSONObject jsonResponse = deferredApplinkResponse.getJSONObject();
      if (jsonResponse != null) {
        final String appLinkArgsJsonString = jsonResponse.optString(DEFERRED_APP_LINK_ARGS_FIELD);
        final long tapTimeUtc = jsonResponse.optLong(DEFERRED_APP_LINK_CLICK_TIME_FIELD, -1);
        final String appLinkClassName = jsonResponse.optString(DEFERRED_APP_LINK_CLASS_FIELD);
        final String appLinkUrl = jsonResponse.optString(DEFERRED_APP_LINK_URL_FIELD);

        if (!TextUtils.isEmpty(appLinkArgsJsonString)) {
          appLinkData = createFromJson(appLinkArgsJsonString);
          if (appLinkData != null) {
            if (tapTimeUtc != -1) {
              try {
                if (appLinkData.arguments != null) {
                  appLinkData.arguments.put(ARGUMENTS_TAPTIME_KEY, tapTimeUtc);
                }
                if (appLinkData.argumentBundle != null) {
                  appLinkData.argumentBundle.putString(
                      ARGUMENTS_TAPTIME_KEY, Long.toString(tapTimeUtc));
                }
              } catch (JSONException e) {
                Utility.logd(TAG, "Unable to put tap time in AppLinkData.arguments");
              }
            }

            if (appLinkClassName != null) {
              try {
                if (appLinkData.arguments != null) {
                  appLinkData.arguments.put(ARGUMENTS_NATIVE_CLASS_KEY, appLinkClassName);
                }
                if (appLinkData.argumentBundle != null) {
                  appLinkData.argumentBundle.putString(
                      ARGUMENTS_NATIVE_CLASS_KEY, appLinkClassName);
                }
              } catch (JSONException e) {
                Utility.logd(TAG, "Unable to put app link class name in AppLinkData.arguments");
              }
            }

            if (appLinkUrl != null) {
              try {
                if (appLinkData.arguments != null) {
                  appLinkData.arguments.put(ARGUMENTS_NATIVE_URL, appLinkUrl);
                }
                if (appLinkData.argumentBundle != null) {
                  appLinkData.argumentBundle.putString(ARGUMENTS_NATIVE_URL, appLinkUrl);
                }
              } catch (JSONException e) {
                Utility.logd(TAG, "Unable to put app link URL in AppLinkData.arguments");
              }
            }
          }
        }
      }
    } catch (Exception e) {
      Utility.logd(TAG, "Unable to fetch deferred applink from server");
    }

    completionHandler.onDeferredAppLinkDataFetched(appLinkData);
  }

  /**
   * Parses out any app link data from the Intent of the Activity passed in.
   *
   * @param activity Activity that was started because of an app link
   * @return AppLinkData if found. null if not.
   */
  @AutoHandleExceptions
  @Nullable
  public static AppLinkData createFromActivity(Activity activity) {
    Validate.notNull(activity, "activity");
    Intent intent = activity.getIntent();
    if (intent == null) {
      return null;
    }

    AppLinkData appLinkData = createFromAlApplinkData(intent);
    if (appLinkData == null) {
      String appLinkArgsJsonString = intent.getStringExtra(BUNDLE_APPLINK_ARGS_KEY);
      appLinkData = createFromJson(appLinkArgsJsonString);
    }
    if (appLinkData == null) {
      // Try regular app linking
      appLinkData = createFromUri(intent.getData());
    }

    return appLinkData;
  }

  /**
   * Parses out any app link data from the Intent passed in.
   *
   * @param intent Intent from the Activity that started because of an app link
   * @return AppLinkData if found. null if not.
   */
  @AutoHandleExceptions
  @Nullable
  public static AppLinkData createFromAlApplinkData(Intent intent) {
    if (intent == null) {
      return null;
    }

    Bundle applinks = intent.getBundleExtra(BUNDLE_AL_APPLINK_DATA_KEY);
    if (applinks == null) {
      return null;
    }

    AppLinkData appLinkData = new AppLinkData();
    appLinkData.targetUri = intent.getData();
    appLinkData.appLinkData = getAppLinkData(appLinkData.targetUri);
    if (appLinkData.targetUri == null) {
      String targetUriString = applinks.getString(METHOD_ARGS_TARGET_URL_KEY);
      if (targetUriString != null) {
        appLinkData.targetUri = Uri.parse(targetUriString);
      }
    }
    appLinkData.argumentBundle = applinks;
    appLinkData.arguments = null;
    Bundle refererData = applinks.getBundle(ARGUMENTS_REFERER_DATA_KEY);
    if (refererData != null) {
      appLinkData.ref = refererData.getString(REFERER_DATA_REF_KEY);
    }

    Bundle extras = applinks.getBundle(ARGUMENTS_EXTRAS_KEY);
    if (extras != null) {
      String deeplinkContext = extras.getString(EXTRAS_DEEPLINK_CONTEXT_KEY);
      if (deeplinkContext != null) {
        try {
          JSONObject dlContextJson = new JSONObject(deeplinkContext);
          if (dlContextJson.has(PROMOTION_CODE_KEY)) {
            appLinkData.promotionCode = dlContextJson.getString(PROMOTION_CODE_KEY);
          }
        } catch (JSONException e) {
          Utility.logd(TAG, "Unable to parse deeplink_context JSON", e);
        }
      }
    }

    return appLinkData;
  }

  @Nullable
  private static AppLinkData createFromJson(String jsonString) {
    if (jsonString == null) {
      return null;
    }

    try {
      // Any missing or malformed data will result in a JSONException
      JSONObject appLinkArgsJson = new JSONObject(jsonString);
      String version = appLinkArgsJson.getString(APPLINK_VERSION_KEY);

      JSONObject bridgeArgs = appLinkArgsJson.getJSONObject(APPLINK_BRIDGE_ARGS_KEY);
      String method = bridgeArgs.getString(BRIDGE_ARGS_METHOD_KEY);
      if (method.equals("applink") && version.equals("2")) {
        // We have a new deep link
        AppLinkData appLinkData = new AppLinkData();

        appLinkData.arguments = appLinkArgsJson.getJSONObject(APPLINK_METHOD_ARGS_KEY);
        // first look for the "ref" key in the top level args
        if (appLinkData.arguments.has(METHOD_ARGS_REF_KEY)) {
          appLinkData.ref = appLinkData.arguments.getString(METHOD_ARGS_REF_KEY);
        } else if (appLinkData.arguments.has(ARGUMENTS_REFERER_DATA_KEY)) {
          // if it's not in the top level args, it could be in the "referer_data" blob
          JSONObject refererData = appLinkData.arguments.getJSONObject(ARGUMENTS_REFERER_DATA_KEY);
          if (refererData.has(REFERER_DATA_REF_KEY)) {
            appLinkData.ref = refererData.getString(REFERER_DATA_REF_KEY);
          }
        }

        if (appLinkData.arguments.has(METHOD_ARGS_TARGET_URL_KEY)) {
          appLinkData.targetUri =
              Uri.parse(appLinkData.arguments.getString(METHOD_ARGS_TARGET_URL_KEY));
          appLinkData.appLinkData = getAppLinkData(appLinkData.targetUri);
        }

        if (appLinkData.arguments.has(ARGUMENTS_EXTRAS_KEY)) {
          JSONObject extrasData = appLinkData.arguments.getJSONObject(ARGUMENTS_EXTRAS_KEY);
          if (extrasData.has(EXTRAS_DEEPLINK_CONTEXT_KEY)) {
            JSONObject deeplink_context = extrasData.getJSONObject(EXTRAS_DEEPLINK_CONTEXT_KEY);
            if (deeplink_context.has(PROMOTION_CODE_KEY)) {
              appLinkData.promotionCode = deeplink_context.getString(PROMOTION_CODE_KEY);
            }
          }
        }

        appLinkData.argumentBundle = toBundle(appLinkData.arguments);

        return appLinkData;
      }
    } catch (JSONException e) {
      Utility.logd(TAG, "Unable to parse AppLink JSON", e);
    } catch (FacebookException e) {
      Utility.logd(TAG, "Unable to parse AppLink JSON", e);
    }

    return null;
  }

  @Nullable
  private static AppLinkData createFromUri(Uri appLinkDataUri) {
    if (appLinkDataUri == null) {
      return null;
    }

    AppLinkData appLinkData = new AppLinkData();
    appLinkData.targetUri = appLinkDataUri;
    appLinkData.appLinkData = getAppLinkData(appLinkData.targetUri);
    return appLinkData;
  }

  private static Bundle toBundle(JSONObject node) throws JSONException {
    Bundle bundle = new Bundle();
    Iterator<String> fields = node.keys();
    while (fields.hasNext()) {
      String key = fields.next();
      Object value;
      value = node.get(key);

      if (value instanceof JSONObject) {
        bundle.putBundle(key, toBundle((JSONObject) value));
      } else if (value instanceof JSONArray) {
        JSONArray valueArr = (JSONArray) value;
        if (valueArr.length() == 0) {
          bundle.putStringArray(key, new String[0]);
        } else {
          Object firstNode = valueArr.get(0);
          if (firstNode instanceof JSONObject) {
            Bundle[] bundles = new Bundle[valueArr.length()];
            for (int i = 0; i < valueArr.length(); i++) {
              bundles[i] = toBundle(valueArr.getJSONObject(i));
            }
            bundle.putParcelableArray(key, bundles);
          } else if (firstNode instanceof JSONArray) {
            // we don't support nested arrays
            throw new FacebookException("Nested arrays are not supported.");
          } else { // just use the string value
            String[] arrValues = new String[valueArr.length()];
            for (int i = 0; i < valueArr.length(); i++) {
              arrValues[i] = valueArr.get(i).toString();
            }
            bundle.putStringArray(key, arrValues);
          }
        }
      } else {
        bundle.putString(key, value.toString());
      }
    }
    return bundle;
  }

  @AutoHandleExceptions
  @Nullable
  private static JSONObject getAppLinkData(@Nullable Uri uri) {
    if (uri == null) {
      return null;
    }

    String data = uri.getQueryParameter(BUNDLE_AL_APPLINK_DATA_KEY);
    if (data == null) {
      return null;
    }
    try {
      return new JSONObject(data);
    } catch (JSONException e) {
      /* no op */
    }
    return null;
  }

  private AppLinkData() {}

  public boolean isAutoAppLink() {
    if (null == targetUri) {
      return false;
    }
    String host = targetUri.getHost();
    String scheme = targetUri.getScheme();
    String expectedHost = "applinks";
    String expectedScheme = String.format("fb%s", FacebookSdk.getApplicationId());
    boolean autoFlag = appLinkData != null && appLinkData.optBoolean(AUTO_APPLINK_FLAG_KEY);
    return autoFlag && expectedHost.equals(host) && expectedScheme.equals(scheme);
  }

  /**
   * Returns the target uri for this App Link.
   *
   * @return target uri
   */
  @Nullable
  public Uri getTargetUri() {
    return targetUri;
  }

  /**
   * Returns the ref for this App Link.
   *
   * @return ref
   */
  @Nullable
  public String getRef() {
    return ref;
  }

  /**
   * Returns the promotion code for this App Link.
   *
   * @return promotion code
   */
  @Nullable
  public String getPromotionCode() {
    return promotionCode;
  }

  /**
   * The full set of arguments for this app link. Properties like target uri & ref are typically
   * picked out of this set of arguments.
   *
   * @return App link related arguments as a bundle.
   */
  @Nullable
  public Bundle getArgumentBundle() {
    return argumentBundle;
  }

  /**
   * The referer data associated with the app link. This will contain Facebook specific information
   * like fb_access_token, fb_expires_in, and fb_ref.
   *
   * @return the referer data.
   */
  @Nullable
  public Bundle getRefererData() {
    if (argumentBundle != null) {
      return argumentBundle.getBundle(ARGUMENTS_REFERER_DATA_KEY);
    }
    return null;
  }

  /**
   * Returns the data of al_applink_data which is defined in
   * https://developers.facebook.com/docs/applinks/navigation-protocol
   *
   * @return App Link data. Empty if not found.
   */
  public JSONObject getAppLinkData() {
    return appLinkData != null ? appLinkData : new JSONObject();
  }

  /** Interface to asynchronously receive AppLinkData after it has been fetched. */
  public interface CompletionHandler {
    /**
     * This method is called when deferred app link data has been fetched. If no app link data was
     * found, this method is called with null
     *
     * @param appLinkData The app link data that was fetched. Null if none was found.
     */
    void onDeferredAppLinkDataFetched(@Nullable AppLinkData appLinkData);
  }
}
