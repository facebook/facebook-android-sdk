/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
