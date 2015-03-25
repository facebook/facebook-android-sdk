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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.FacebookActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class DialogPresenter {

    public static void setupAppCallForCannotShowError(AppCall appCall) {
        FacebookException e = new FacebookException(
                "Unable to show the provided content. This typically means that the Facebook " +
                        "app is not installed or up to date. If showing via the Web, this could " +
                        "mean that the content has properties that are not supported via " +
                        "this channel");
        setupAppCallForValidationError(appCall, e);
    }

    public static void setupAppCallForValidationError(
            AppCall appCall, FacebookException validationError) {
        setupAppCallForErrorResult(appCall, validationError);
    }

    public interface ParameterProvider {
        Bundle getParameters();
        Bundle getLegacyParameters();
    }

    public static void present(AppCall appCall, Activity activity) {
        activity.startActivityForResult(appCall.getRequestIntent(), appCall.getRequestCode());

        appCall.setPending();
    }

    public static void present(AppCall appCall, Fragment fragment) {
        fragment.startActivityForResult(appCall.getRequestIntent(), appCall.getRequestCode());

        appCall.setPending();
    }

    public static boolean canPresentNativeDialogWithFeature(
            DialogFeature feature) {
        return getProtocolVersionForNativeDialog(feature)
                != NativeProtocol.NO_PROTOCOL_AVAILABLE;
    }

    public static boolean canPresentWebFallbackDialogWithFeature(DialogFeature feature) {
        return getDialogWebFallbackUri(feature) != null;
    }

    public static void setupAppCallForErrorResult(AppCall appCall, FacebookException exception) {
        if (exception == null) {
            return;
        }

        Intent errorResultIntent = new Intent();
        errorResultIntent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
        errorResultIntent.setAction(FacebookActivity.PASS_THROUGH_CANCEL_ACTION);

        NativeProtocol.setupProtocolRequestIntent(
                errorResultIntent,
                appCall.getCallId().toString(),
                null,
                NativeProtocol.getLatestKnownVersion(),
                NativeProtocol.createBundleForException(exception));

        appCall.setRequestIntent(errorResultIntent);
    }

    public static void setupAppCallForWebDialog(
            AppCall appCall,
            String actionName,
            Bundle parameters) {
        Bundle intentParameters = new Bundle();
        intentParameters.putString(NativeProtocol.WEB_DIALOG_ACTION, actionName);
        intentParameters.putBundle(NativeProtocol.WEB_DIALOG_PARAMS, parameters);

        Intent webDialogIntent = new Intent();
        NativeProtocol.setupProtocolRequestIntent(
                webDialogIntent,
                appCall.getCallId().toString(),
                actionName,
                NativeProtocol.getLatestKnownVersion(),
                intentParameters);
        webDialogIntent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
        webDialogIntent.setAction(FacebookDialogFragment.TAG);

        appCall.setRequestIntent(webDialogIntent);
    }

    public static void setupAppCallForWebFallbackDialog(
            AppCall appCall,
            Bundle parameters,
            DialogFeature feature) {
        String featureName = feature.name();
        Uri fallbackUrl = getDialogWebFallbackUri(feature);
        if (fallbackUrl == null) {
            throw new FacebookException(
                    "Unable to fetch the Url for the DialogFeature : '" + featureName + "'");
        }

        // Since we're talking to the server here, let's use the latest version we know about.
        // We know we are going to be communicating over a bucketed protocol.
        int protocolVersion = NativeProtocol.getLatestKnownVersion();
        Bundle webParams = ServerProtocol.getQueryParamsForPlatformActivityIntentWebFallback(
                appCall.getCallId().toString(),
                protocolVersion,
                parameters);
        if (webParams == null) {
            throw new FacebookException("Unable to fetch the app's key-hash");
        }

        // Now form the Uri
        if (fallbackUrl.isRelative()) {
            fallbackUrl = Utility.buildUri(
                    ServerProtocol.getDialogAuthority(),
                    fallbackUrl.toString(),
                    webParams);
        } else {
            fallbackUrl = Utility.buildUri(
                    fallbackUrl.getAuthority(),
                    fallbackUrl.getPath(),
                    webParams);
        }

        Bundle intentParameters = new Bundle();
        intentParameters.putString(NativeProtocol.WEB_DIALOG_URL, fallbackUrl.toString());
        intentParameters.putBoolean(NativeProtocol.WEB_DIALOG_IS_FALLBACK, true);

        Intent webDialogIntent = new Intent();
        NativeProtocol.setupProtocolRequestIntent(
                webDialogIntent,
                appCall.getCallId().toString(),
                feature.getAction(),
                NativeProtocol.getLatestKnownVersion(),
                intentParameters);
        webDialogIntent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
        webDialogIntent.setAction(FacebookDialogFragment.TAG);

        appCall.setRequestIntent(webDialogIntent);
    }

    public static void setupAppCallForNativeDialog(
            AppCall appCall,
            ParameterProvider parameterProvider,
            DialogFeature feature) {
        Context context = FacebookSdk.getApplicationContext();
        String action = feature.getAction();
        int protocolVersion = getProtocolVersionForNativeDialog(feature);
        if (protocolVersion == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
            throw new FacebookException(
                    "Cannot present this dialog. This likely means that the " +
                            "Facebook app is not installed.");
        }

        Bundle params;
        if (NativeProtocol.isVersionCompatibleWithBucketedIntent(protocolVersion)) {
            // Facebook app supports the new bucketed protocol
            params = parameterProvider.getParameters();
        } else {
            // Facebook app only supports the old flat protocol
            params = parameterProvider.getLegacyParameters();
        }
        if (params == null) {
            params = new Bundle();
        }

        Intent intent = NativeProtocol.createPlatformActivityIntent(
                context,
                appCall.getCallId().toString(),
                action,
                protocolVersion,
                params);
        if (intent == null) {
            throw new FacebookException(
                    "Unable to create Intent; this likely means the" +
                            "Facebook app is not installed.");
        }

        appCall.setRequestIntent(intent);
    }

    private static Uri getDialogWebFallbackUri(DialogFeature feature) {
        String featureName = feature.name();
        String action = feature.getAction();
        String applicationId = FacebookSdk.getApplicationId();

        Utility.DialogFeatureConfig config =
                Utility.getDialogFeatureConfig(applicationId, action, featureName);
        Uri fallbackUrl = null;
        if (config != null) {
            fallbackUrl = config.getFallbackUrl();
        }

        return fallbackUrl;
    }

    public static int getProtocolVersionForNativeDialog(
            DialogFeature feature) {
        String applicationId = FacebookSdk.getApplicationId();
        String action = feature.getAction();
        int[] featureVersionSpec = getVersionSpecForFeature(applicationId, action, feature);

        return NativeProtocol.getLatestAvailableProtocolVersionForAction(
                action,
                featureVersionSpec);
    }

    private static int[] getVersionSpecForFeature(
            String applicationId,
            String actionName,
            DialogFeature feature) {
        // Return the value from DialogFeatureConfig if available. Otherwise, just
        // default to the min-version
        Utility.DialogFeatureConfig config =
                Utility.getDialogFeatureConfig(applicationId, actionName, feature.name());
        if (config != null) {
            return config.getVersionSpec();
        } else {
            return new int[]{feature.getMinVersion()};
        }
    }

    public static void logDialogActivity(
            Context context,
            String eventName,
            String outcome) {
        AppEventsLogger logger = AppEventsLogger.newLogger(context);
        Bundle parameters = new Bundle();
        parameters.putString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME, outcome);
        logger.logSdkEvent(eventName, null, parameters);
    }
}
