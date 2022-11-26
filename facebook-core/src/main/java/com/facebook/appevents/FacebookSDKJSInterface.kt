/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
