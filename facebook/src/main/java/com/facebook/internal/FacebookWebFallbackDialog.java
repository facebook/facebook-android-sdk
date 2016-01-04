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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 * <p/>
 * This dialog is used as a fallback when a native FacebookDialog could not be displayed. The
 * primary reason for this separation is to keep this approach for internal use only until we
 * stabilize the API.
 */
public class FacebookWebFallbackDialog extends WebDialog {
    private static final String TAG = FacebookWebFallbackDialog.class.getName();
    private static final int OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS = 1500;

    private boolean waitingForDialogToClose;

    public FacebookWebFallbackDialog(Context context, String url, String expectedRedirectUrl) {
        super(context, url);

        setExpectedRedirectUrl(expectedRedirectUrl);
    }

    @Override
    protected Bundle parseResponseUri(String url) {
        Uri responseUri = Uri.parse(url);
        Bundle queryParams = Utility.parseUrlQueryString(responseUri.getQuery());

        // Convert Bridge args to the format that the Native dialog code understands.
        String bridgeArgsJSONString =
                queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS);
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
        String methodResultsJSONString =
                queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS);
        queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS);

        if (!Utility.isNullOrEmpty(methodResultsJSONString)) {
            methodResultsJSONString =
                    Utility.isNullOrEmpty(methodResultsJSONString) ? "{}" : methodResultsJSONString;
            Bundle methodResults;
            try {
                JSONObject methodArgsJSON = new JSONObject(methodResultsJSONString);
                methodResults = BundleJSONConverter.convertToBundle(methodArgsJSON);
                queryParams.putBundle(NativeProtocol.EXTRA_PROTOCOL_METHOD_RESULTS, methodResults);
            } catch (JSONException je) {
                Utility.logd(TAG, "Unable to parse bridge_args JSON", je);
            }
        }

        // The web host does not send a numeric version back. Put the latest known version in there
        // so NativeProtocol can continue parsing the response.
        queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_VERSION);
        queryParams.putInt(
                NativeProtocol.EXTRA_PROTOCOL_VERSION, NativeProtocol.getLatestKnownVersion());

        return queryParams;
    }

    @Override
    public void cancel() {
        WebView webView = getWebView();

        // If the page hasn't loaded, or the listener is already called, then we can't interrupt
        // this cancellation. Either the JS won't be ready to consume the event, or the listener
        // has already processed a result.
        // So let's just handle this cancellation in the standard way.
        if (!isPageFinished()
                || isListenerCalled()
                || webView == null
                || !webView.isShown()) {
            super.cancel();
            return;
        }

        // Return right away if we have already queued up the delayed-cancel call.
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

        // Set up a timeout for the dialog to respond. If the timer expires, we need to honor
        // the user's desire to dismiss the dialog.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        // If we get here, then the dialog did not close quickly enough.
                        // So we need to honor the user's wish to cancel and we should do
                        // so without allowing interruptions.
                        FacebookWebFallbackDialog.super.cancel();
                    }
                },
                OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS);
    }
}
