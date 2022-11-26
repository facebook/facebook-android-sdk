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
enum class CameraEffectFeature(private val minVersion: Int) : DialogFeature {
  SHARE_CAMERA_EFFECT(NativeProtocol.PROTOCOL_VERSION_20170417);

  /** This method is for internal use only. */
  override fun getAction(): String = NativeProtocol.ACTION_CAMERA_EFFECT

  /** This method is for internal use only. */
  override fun getMinVersion(): Int = minVersion
}
