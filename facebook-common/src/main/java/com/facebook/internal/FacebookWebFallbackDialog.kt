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
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.facebook.internal.BundleJSONConverter.convertToBundle
import com.facebook.internal.NativeProtocol.getLatestKnownVersion
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.logd
import com.facebook.internal.Utility.parseUrlQueryString
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * This dialog is used as a fallback when a native FacebookDialog could not be displayed. The
 * primary reason for this separation is to keep this approach for internal use only until we
 * stabilize the API.
 */
class FacebookWebFallbackDialog
private constructor(context: Context, url: String, expectedRedirectUrl: String) :
    WebDialog(context, url) {
  private var waitingForDialogToClose = false
  override fun parseResponseUri(url: String?): Bundle {
    val responseUri = Uri.parse(url)
    val queryParams = parseUrlQueryString(responseUri.query)

    // Convert Bridge args to the format that the Native dialog code understands.
    val bridgeArgsJSONString =
        queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS)
    queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS)
    if (!isNullOrEmpty(bridgeArgsJSONString)) {
      try {
        val bridgeArgsJSON = JSONObject(bridgeArgsJSONString)
        val bridgeArgs = convertToBundle(bridgeArgsJSON)
        queryParams.putBundle(NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS, bridgeArgs)
      } catch (je: JSONException) {
        logd(TAG, "Unable to parse bridge_args JSON", je)
      }
    }

    // Convert Method results to the format that the Native dialog code understands.
    val methodResultsJSONString =
        queryParams.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS)
    queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS)
    if (!isNullOrEmpty(methodResultsJSONString)) {
      try {
        val methodArgsJSON = JSONObject(methodResultsJSONString)
        val methodResults = convertToBundle(methodArgsJSON)
        queryParams.putBundle(NativeProtocol.EXTRA_PROTOCOL_METHOD_RESULTS, methodResults)
      } catch (je: JSONException) {
        logd(TAG, "Unable to parse bridge_args JSON", je)
      }
    }

    // The web host does not send a numeric version back. Put the latest known version in there
    // so NativeProtocol can continue parsing the response.
    queryParams.remove(ServerProtocol.FALLBACK_DIALOG_PARAM_VERSION)
    queryParams.putInt(NativeProtocol.EXTRA_PROTOCOL_VERSION, getLatestKnownVersion())
    return queryParams
  }

  override fun cancel() {
    val webView = webView

    // If the page hasn't loaded, or the listener is already called, then we can't interrupt
    // this cancellation. Either the JS won't be ready to consume the event, or the listener
    // has already processed a result.
    // So let's just handle this cancellation in the standard way.
    if (!isPageFinished || isListenerCalled || webView == null || !webView.isShown) {
      super.cancel()
      return
    }

    // Return right away if we have already queued up the delayed-cancel call.
    if (waitingForDialogToClose) {
      return
    }
    waitingForDialogToClose = true

    // Now fire off the event that will tell the dialog to wind down.
    val eventJS =
        ("(function() {" +
            "  var event = document.createEvent('Event');" +
            "  event.initEvent('fbPlatformDialogMustClose',true,true);" +
            "  document.dispatchEvent(event);" +
            "})();")
    webView.loadUrl("javascript:$eventJS")

    // Set up a timeout for the dialog to respond. If the timer expires, we need to honor
    // the user's desire to dismiss the dialog.
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(
        { // If we get here, then the dialog did not close quickly enough.
          // So we need to honor the user's wish to cancel and we should do
          // so without allowing interruptions.
          super@FacebookWebFallbackDialog.cancel()
        },
        OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS.toLong())
  }

  companion object {
    private val TAG = FacebookWebFallbackDialog::class.java.name
    private const val OS_BACK_BUTTON_RESPONSE_TIMEOUT_MILLISECONDS = 1500

    @JvmStatic
    fun newInstance(
        context: Context,
        url: String,
        expectedRedirectUrl: String
    ): FacebookWebFallbackDialog {
      initDefaultTheme(context)
      return FacebookWebFallbackDialog(context, url, expectedRedirectUrl)
    }
  }

  init {
    setExpectedRedirectUrl(expectedRedirectUrl)
  }
}
