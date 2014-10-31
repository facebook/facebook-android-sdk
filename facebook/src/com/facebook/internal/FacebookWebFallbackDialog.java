/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import com.facebook.FacebookException;
import com.facebook.android.Util;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumSet;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 *
 * This dialog is used as a fallback when a native FacebookDialog could not be displayed. The primary reason for
 * this separation is to keep this approach for internal use only until we stabilize the API.
 */
public class FacebookWebFallbackDialog extends WebDialog {
    private static final String TAG = FacebookWebFallbackDialog.class.getName();
    private static final int OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS = 1500;

    private boolean waitingForDialogToClose;

    public static boolean presentWebFallback(final Context context,
                                             String dialogUrl,
                                             String applicationId,
                                             final FacebookDialog.PendingCall appCall,
                                             final FacebookDialog.Callback callback) {
        if (Utility.isNullOrEmpty(dialogUrl)) {
            return false;
        }

        String redirectUrl = String.format("fb%s://bridge/", applicationId);

        // Show the webdialog.
        FacebookWebFallbackDialog fallbackWebDialog = new FacebookWebFallbackDialog(
                context, dialogUrl, redirectUrl);
        fallbackWebDialog.setOnCompleteListener(new WebDialog.OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, FacebookException error) {
                Intent dummyIntent = new Intent();
                dummyIntent.putExtras(values == null ? new Bundle() : values);
                FacebookDialog.handleActivityResult(
                        context,
                        appCall,
                        appCall.getRequestCode(),
                        dummyIntent,
                        callback);
            }
        });

        fallbackWebDialog.show();
        return true;
    }

    private FacebookWebFallbackDialog(Context context, String url, String expectedRedirectUrl) {
        super(context, url);

        setExpectedRedirectUrl(expectedRedirectUrl);
    }

    @Override
    protected Bundle parseResponseUri(String url) {
        Uri responseUri = Uri.parse(url);
        Bundle queryParams = Utility.parseUrlQueryString(responseUri.getQuery());

        // Convert Bridge args to the format that the Native dialog code understands.
        String bridgeArgsJSONString = queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS);
        queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS);

        if (!Utility.isNullOrEmpty(bridgeArgsJSONString)) {
            Bundle bridgeArgs;
            try {
                JSONObject bridgeArgsJSON = new JSONObject(bridgeArgsJSONString);
                bridgeArgs = BundleJSONConverter.convertToBundle(bridgeArgsJSON);
                queryParams.putBundle(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArgs);
            } catch (JSONException je) {
                Utility.logd(TAG, "Unable to parse bridge_args JSON", je);
            }
        }

        // Convert Method results to the format that the Native dialog code understands.
        String methodResultsJSONString = queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS);
        queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS);

        if (!Utility.isNullOrEmpty(methodResultsJSONString)) {
            methodResultsJSONString = Utility.isNullOrEmpty(methodResultsJSONString) ? "{}" : methodResultsJSONString;
            Bundle methodResults;
            try {
                JSONObject methodArgsJSON = new JSONObject(methodResultsJSONString);
                methodResults = BundleJSONConverter.convertToBundle(methodArgsJSON);
                queryParams.putBundle(NativeProtocol.EXTRA_PROTOCOL_METHOD_RESULTS, methodResults);
            } catch (JSONException je) {
                Utility.logd(TAG, "Unable to parse bridge_args JSON", je);
            }
        }

        // The web host does not send a numeric version back. Put the latest known version in there so NativeProtocol
        // can continue parsing the response.
        queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_VERSION);
        queryParams.putInt(NativeProtocol.EXTRA_PROTOCOL_VERSION, NativeProtocol.getLatestKnownVersion());

        return queryParams;
    }

    @Override
    public void dismiss() {
        WebView webView = getWebView();

        if (isListenerCalled() || webView == null || !webView.isShown()) {
            // If the listener has been called, or if the WebView isn't visible, we cannot give the dialog a chance
            // to respond. So defer to the parent implementation.
            super.dismiss();
            return;
        }

        // If we have already notified the dialog to close, then ignore this request to dismiss. The timer will
        // honor the request.
        if (waitingForDialogToClose) {
            return;
        }
        waitingForDialogToClose = true;

        // Now fire off the event that will tell the dialog to wind down.
        String eventJS =
                "(function() {" +
                "  var event = document.createEvent('Event');" +
                "  event.initEvent('fbPlatformDialogMustClose',true,true);" +
                "  document.dispatchEvent(event);" +
                "})();";
        webView.loadUrl("javascript:" + eventJS);

        // Set up a timeout for the dialog to respond. If the timer expires, we need to honor the user's desire to
        // dismiss the dialog.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isListenerCalled()) {
                            // If we get here, then the dialog did not close quickly enough. So we need to honor the user's
                            // wish to cancel.
                            sendCancelToListener();
                        }
                    }
                },
                OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS);
    }
}
