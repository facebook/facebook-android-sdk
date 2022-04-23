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

package com.facebook.share.internal

import android.os.Bundle
import com.facebook.internal.Utility
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareVideoContent
import java.util.UUID

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object LegacyNativeDialogParameters {
  @JvmStatic
  fun create(
      callId: UUID,
      shareContent: ShareContent<*, *>,
      shouldFailOnDataError: Boolean
  ): Bundle? {
    var nativeParams: Bundle? = null
    when (shareContent) {
      is ShareLinkContent -> {
        nativeParams = create(shareContent, shouldFailOnDataError)
      }
      is SharePhotoContent -> {
        val photoUrls = ShareInternalUtility.getPhotoUrls(shareContent, callId) ?: listOf()
        nativeParams = create(shareContent, photoUrls, shouldFailOnDataError)
      }
      is ShareVideoContent -> {
        // Not supported
        nativeParams = null
      }
    }
    return nativeParams
  }

  private fun create(linkContent: ShareLinkContent, dataErrorsFatal: Boolean): Bundle {
    return createBaseParameters(linkContent, dataErrorsFatal)
  }

  private fun create(
      photoContent: SharePhotoContent,
      imageUrls: List<String>,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(photoContent, dataErrorsFatal)
    params.putStringArrayList(ShareConstants.LEGACY_PHOTOS, ArrayList(imageUrls))
    return params
  }

  private fun createBaseParameters(content: ShareContent<*, *>, dataErrorsFatal: Boolean): Bundle {
    val params = Bundle()
    Utility.putUri(params, ShareConstants.LEGACY_LINK, content.contentUrl)
    Utility.putNonEmptyString(params, ShareConstants.LEGACY_PLACE_TAG, content.placeId)
    Utility.putNonEmptyString(params, ShareConstants.LEGACY_REF, content.ref)
    params.putBoolean(ShareConstants.LEGACY_DATA_FAILURES_FATAL, dataErrorsFatal)
    val peopleIds = content.peopleIds
    if (!peopleIds.isNullOrEmpty()) {
      params.putStringArrayList(ShareConstants.LEGACY_FRIEND_TAGS, ArrayList(peopleIds))
    }
    return params
  }
}
