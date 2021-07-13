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
import android.util.Log
import com.facebook.FacebookSdk
import java.util.Locale

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class ImageRequest
private constructor(
    val context: Context,
    val imageUri: Uri,
    val callback: Callback?,
    val allowCachedRedirects: Boolean,
    val callerTag: Any
) {
  fun interface Callback {
    /**
     * This method should always be called on the UI thread. ImageDownloader makes sure to do this
     * when it is responsible for issuing the ImageResponse
     *
     * @param response
     */
    fun onCompleted(response: ImageResponse?)
  }

  val isCachedRedirectAllowed: Boolean
    get() {
      return allowCachedRedirects
    }

  companion object {
    const val UNSPECIFIED_DIMENSION = 0
    private const val PATH = "%s/%s/picture"
    private const val HEIGHT_PARAM = "height"
    private const val WIDTH_PARAM = "width"
    private const val ACCESS_TOKEN_PARAM = "access_token"
    private const val MIGRATION_PARAM = "migration_overrides"
    private const val MIGRATION_VALUE = "{october_2012:true}"

    @JvmStatic
    fun getProfilePictureUri(userId: String?, width: Int, height: Int): Uri {
      return getProfilePictureUri(userId, width, height, "")
    }

    @JvmStatic
    fun getProfilePictureUri(userId: String?, width: Int, height: Int, accessToken: String?): Uri {
      var width = width
      var height = height
      Validate.notNullOrEmpty(userId, "userId")
      width = Math.max(width, UNSPECIFIED_DIMENSION)
      height = Math.max(height, UNSPECIFIED_DIMENSION)
      require(!(width == UNSPECIFIED_DIMENSION && height == UNSPECIFIED_DIMENSION)) {
        "Either width or height must be greater than 0"
      }
      val builder =
          Uri.parse(ServerProtocol.getGraphUrlBase())
              .buildUpon()
              .path(String.format(Locale.US, PATH, FacebookSdk.getGraphApiVersion(), userId))
      if (height != UNSPECIFIED_DIMENSION) {
        builder.appendQueryParameter(HEIGHT_PARAM, height.toString())
      }
      if (width != UNSPECIFIED_DIMENSION) {
        builder.appendQueryParameter(WIDTH_PARAM, width.toString())
      }
      builder.appendQueryParameter(MIGRATION_PARAM, MIGRATION_VALUE)
      if (!Utility.isNullOrEmpty(accessToken)) {
        builder.appendQueryParameter(ACCESS_TOKEN_PARAM, accessToken)
      } else {
        if (!Utility.isNullOrEmpty(FacebookSdk.getClientToken()) &&
            !Utility.isNullOrEmpty(FacebookSdk.getApplicationId())) {
          builder.appendQueryParameter(
              ACCESS_TOKEN_PARAM,
              FacebookSdk.getApplicationId() + "|" + FacebookSdk.getClientToken())
        } else {
          Log.d(
              "ImageRequest",
              "Needs access token to fetch profile picture. Without an access token a default silhoutte picture is returned")
        }
      }
      return builder.build()
    }
  }

  data class Builder(
      private val context: Context,
      private val imageUri: Uri,
  ) {

    private var callback: Callback? = null
    private var allowCachedRedirects: Boolean = false
    private var callerTag: Any? = null

    fun setCallback(callback: Callback?): Builder {
      this.callback = callback
      return this
    }

    fun setCallerTag(callerTag: Any?): Builder {
      this.callerTag = callerTag
      return this
    }

    fun setAllowCachedRedirects(allowCachedRedirects: Boolean): Builder {
      this.allowCachedRedirects = allowCachedRedirects
      return this
    }

    fun build(): ImageRequest {
      return ImageRequest(
          context,
          imageUri,
          callback,
          allowCachedRedirects,
          if (callerTag == null) Any() else checkNotNull(callerTag))
    }
  }
}
