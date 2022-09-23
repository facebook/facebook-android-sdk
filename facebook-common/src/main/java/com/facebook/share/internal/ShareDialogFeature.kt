/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import com.facebook.internal.DialogFeature
import com.facebook.internal.NativeProtocol

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
enum class ShareDialogFeature(private val minVersion: Int) : DialogFeature {
  /**
   * Indicates whether the native Share dialog itself is supported by the installed version of the
   * Facebook application.
   */
  SHARE_DIALOG(NativeProtocol.PROTOCOL_VERSION_20130618),

  /** Indicates whether the native Share dialog supports sharing of photo images. */
  PHOTOS(NativeProtocol.PROTOCOL_VERSION_20140204),

  /** Indicates whether the native Share dialog supports sharing of videos. */
  VIDEO(NativeProtocol.PROTOCOL_VERSION_20141028),

  /** Indicates whether the native Share dialog supports sharing of multimedia. */
  MULTIMEDIA(NativeProtocol.PROTOCOL_VERSION_20160327),

  /** Indicates whether the native Share dialog supports hashtags */
  HASHTAG(NativeProtocol.PROTOCOL_VERSION_20160327),

  /** Indicates whether the native Share dialog supports quotes */
  LINK_SHARE_QUOTES(NativeProtocol.PROTOCOL_VERSION_20160327);

  /** This method is for internal use only. */
  override fun getAction(): String = NativeProtocol.ACTION_FEED_DIALOG

  /** This method is for internal use only. */
  override fun getMinVersion(): Int = minVersion
}
