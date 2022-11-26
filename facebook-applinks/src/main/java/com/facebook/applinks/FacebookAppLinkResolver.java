/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.applinks;

import android.net.Uri;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.bolts.AppLink;
import com.facebook.bolts.AppLinkResolver;
import com.facebook.bolts.Continuation;
import com.facebook.bolts.Task;
import com.facebook.bolts.TaskCompletionSource;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides an implementation for the {@link AppLinkResolver AppLinkResolver} interface that uses
 * the Facebook App Link index to resolve App Links given a URL. It also provides an additional
 * helper method that can resolve multiple App Links in a single call.
 */
@AutoHandleExceptions
public class FacebookAppLinkResolver implements AppLinkResolver {

  private static final String APP_LINK_KEY = "app_links";
  private static final String APP_LINK_ANDROID_TARGET_KEY = "android";
  private static final String APP_LINK_WEB_TARGET_KEY = "web";
  private static final String APP_LINK_TARGET_PACKAGE_KEY = "package";
  private static final String APP_LINK_TARGET_CLASS_KEY = "class";
  private static final String APP_LINK_TARGET_APP_NAME_KEY = "app_name";
  private static final String APP_LINK_TARGET_URL_KEY = "url";
  private static final String APP_LINK_TARGET_SHOULD_FALLBACK_KEY = "should_fallback";

  private final HashMap<Uri, AppLink> cachedAppLinks = new HashMap<Uri, AppLink>();

  /**
   * Asynchronously resolves App Link data for the passed in Uri
   *
   * @param uri Uri to be resolved into an App Link
   * @return A Task that, when successful, will return an AppLink for the passed in Uri. This may be
   *     null if no App Link data was found for this Uri. In the case of general server errors, the
   *     task will be completed with the corresponding error.
   */
  public Task<AppLink> getAppLinkFromUrlInBackground(final Uri uri) {
    ArrayList<Uri> uris = new ArrayList<Uri>();
    uris.add(uri);

    Task<Map<Uri, AppLink>> resolveTask = getAppLinkFromUrlsInBackground(uris);

    return resolveTask.onSuccess(
        new Continuation<Map<Uri, AppLink>, AppLink>() {
          @Override
          public AppLink then(Task<Map<Uri, AppLink>> resolveUrisTask) throws Exception {
            return resolveUrisTask.getResult().get(uri);
          }
        });
  }

  /**
   * Asynchronously resolves App Link data for multiple URLs
   *
   * @param uris A list of Uri objects to resolve into App Links
   * @return A Task that, when successful, will return a Map of Uri->AppLink for each Uri that was
   *     successfully resolved into an App Link. Uris that could not be resolved into App Links will
   *     not be present in the Map. In the case of general server errors, the task will be completed
   *     with the corresponding error.
   */
  public Task<Map<Uri, AppLink>> getAppLinkFromUrlsInBackground(List<Uri> uris) {
    final Map<Uri, AppLink> appLinkResults = new HashMap<Uri, AppLink>();
    final HashSet<Uri> urisToRequest = new HashSet<Uri>();
    StringBuilder graphRequestFields = new StringBuilder();

    for (Uri uri : uris) {
      AppLink appLink;
      synchronized (cachedAppLinks) {
        appLink = cachedAppLinks.get(uri);
      }

      if (appLink != null) {
        appLinkResults.put(uri, appLink);
      } else {
        if (!urisToRequest.isEmpty()) {
          graphRequestFields.append(',');
        }
        graphRequestFields.append(uri.toString());
        urisToRequest.add(uri);
      }
    }

    if (urisToRequest.isEmpty()) {
      return Task.forResult(appLinkResults);
    }

    final TaskCompletionSource<Map<Uri, AppLink>> taskCompletionSource =
        new TaskCompletionSource<>();

    Bundle appLinkRequestParameters = new Bundle();

    appLinkRequestParameters.putString("ids", graphRequestFields.toString());
    appLinkRequestParameters.putString(
        GraphRequest.FIELDS_PARAM,
        String.format(
            "%s.fields(%s,%s)",
            APP_LINK_KEY, APP_LINK_ANDROID_TARGET_KEY, APP_LINK_WEB_TARGET_KEY));
    GraphRequest appLinkRequest =
        new GraphRequest(
            // We will use the current access token if we have one else we will use the client
            // token
            AccessToken.getCurrentAccessToken(), /* Access Token */
            "", /* Graph path */
            appLinkRequestParameters, /* Query parameters */
            null, /* HttpMethod */
            new GraphRequest.Callback() {
              /* Callback */
              @AutoHandleExceptions
              @Override
              public void onCompleted(GraphResponse response) {
                FacebookRequestError error = response.getError();
                if (error != null) {
                  taskCompletionSource.setError(error.getException());
                  return;
                }

                JSONObject responseJson = response.getJSONObject();
                if (responseJson == null) {
                  taskCompletionSource.setResult(appLinkResults);
                  return;
                }

                for (Uri uri : urisToRequest) {
                  String uriString = uri.toString();
                  if (!responseJson.has(uriString)) {
                    continue;
                  }

                  JSONObject urlData;
                  try {
                    urlData = responseJson.getJSONObject(uri.toString());
                    JSONObject appLinkData = urlData.getJSONObject(APP_LINK_KEY);

                    JSONArray rawTargets = appLinkData.getJSONArray(APP_LINK_ANDROID_TARGET_KEY);

                    int targetsCount = rawTargets.length();
                    List<AppLink.Target> targets = new ArrayList<AppLink.Target>(targetsCount);

                    for (int i = 0; i < targetsCount; i++) {
                      AppLink.Target target = getAndroidTargetFromJson(rawTargets.getJSONObject(i));
                      if (target != null) {
                        targets.add(target);
                      }
                    }

                    Uri webFallbackUrl = getWebFallbackUriFromJson(uri, appLinkData);
                    AppLink appLink = new AppLink(uri, targets, webFallbackUrl);

                    appLinkResults.put(uri, appLink);
                    synchronized (cachedAppLinks) {
                      cachedAppLinks.put(uri, appLink);
                    }
                  } catch (JSONException e) {
                    // The data for this uri was missing or badly formed.
                    continue;
                  }
                }

                taskCompletionSource.setResult(appLinkResults);
              }
            });

    appLinkRequest.executeAsync();

    return taskCompletionSource.getTask();
  }

  private static AppLink.Target getAndroidTargetFromJson(JSONObject targetJson) {
    String packageName = tryGetStringFromJson(targetJson, APP_LINK_TARGET_PACKAGE_KEY, null);
    if (packageName == null) {
      // Package name is mandatory for each Android target
      return null;
    }
    String className = tryGetStringFromJson(targetJson, APP_LINK_TARGET_CLASS_KEY, null);
    String appName = tryGetStringFromJson(targetJson, APP_LINK_TARGET_APP_NAME_KEY, null);
    String targetUrlString = tryGetStringFromJson(targetJson, APP_LINK_TARGET_URL_KEY, null);
    Uri targetUri = null;
    if (targetUrlString != null) {
      targetUri = Uri.parse(targetUrlString);
    }

    return new AppLink.Target(packageName, className, targetUri, appName);
  }

  private static Uri getWebFallbackUriFromJson(Uri sourceUrl, JSONObject urlData) {
    // Try and get a web target. This is best effort. Any failures results in null being
    // returned.
    try {
      JSONObject webTarget = urlData.getJSONObject(APP_LINK_WEB_TARGET_KEY);
      boolean shouldFallback =
          tryGetBooleanFromJson(webTarget, APP_LINK_TARGET_SHOULD_FALLBACK_KEY, true);
      if (!shouldFallback) {
        // Don't use a fallback url
        return null;
      }

      String webTargetUrlString = tryGetStringFromJson(webTarget, APP_LINK_TARGET_URL_KEY, null);
      Uri webUri = null;
      if (webTargetUrlString != null) {
        webUri = Uri.parse(webTargetUrlString);
      }

      // If we weren't able to parse a url from the web target, use the source url
      return webUri != null ? webUri : sourceUrl;
    } catch (JSONException e) {
      // If we were missing a web target, just use the source as the web url
      return sourceUrl;
    }
  }

  private static String tryGetStringFromJson(
      JSONObject json, String propertyName, String defaultValue) {
    try {
      return json.getString(propertyName);
    } catch (JSONException e) {
      return defaultValue;
    }
  }

  private static boolean tryGetBooleanFromJson(
      JSONObject json, String propertyName, boolean defaultValue) {
    try {
      return json.getBoolean(propertyName);
    } catch (JSONException e) {
      return defaultValue;
    }
  }
}
