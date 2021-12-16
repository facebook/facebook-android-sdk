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

package com.facebook.appevents

import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal class FacebookSDKJSInterface(private val context: Context?) {
  @JavascriptInterface
  fun sendEvent(pixelId: String?, eventName: String?, jsonString: String?) {
    if (pixelId == null) {
      log(
          LoggingBehavior.DEVELOPER_ERRORS,
          TAG,
          "Can't bridge an event without a referral Pixel ID. " +
              "Check your webview Pixel configuration")
      return
    }
    val logger = InternalAppEventsLogger.createInstance(context)
    val parameters = jsonStringToBundle(jsonString)
    parameters.putString(PARAMETER_FBSDK_PIXEL_REFERRAL, pixelId)
    logger.logEvent(eventName, parameters)
  }

  @get:JavascriptInterface val protocol = "fbmq-0.1"

  companion object {
    val TAG = FacebookSDKJSInterface::class.java.simpleName
    private const val PARAMETER_FBSDK_PIXEL_REFERRAL = "_fb_pixel_referral_id"

    @Throws(JSONException::class)
    private fun jsonToBundle(jsonObject: JSONObject): Bundle {
      val bundle = Bundle()
      val iter: Iterator<*> = jsonObject.keys()
      while (iter.hasNext()) {
        val key = iter.next() as String
        val value = jsonObject.getString(key)
        bundle.putString(key, value)
      }
      return bundle
    }

    private fun jsonStringToBundle(jsonString: String?): Bundle {
      try {
        val jsonObject = JSONObject(jsonString)
        return jsonToBundle(jsonObject)
      } catch (ignored: JSONException) {
        // ignore
      }
      return Bundle()
    }
  }
}
