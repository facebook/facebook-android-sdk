/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import com.facebook.internal.NativeProtocol

/**
 * Certain operations such as publishing a status or publishing a photo require an audience. When
 * the user grants an application permission to perform a publish operation, a default audience is
 * selected as the publication ceiling for the application. This enumerated value allows the
 * application to select which audience to ask the user to grant publish permission for.
 */
enum class DefaultAudience(val nativeProtocolAudience: String?) {
  /** Represents an invalid default audience value, can be used when only reading. */
  NONE(null),

  /** Indicates only the user is able to see posts made by the application. */
  ONLY_ME(NativeProtocol.AUDIENCE_ME),

  /** Indicates that the user's friends are able to see posts made by the application. */
  FRIENDS(NativeProtocol.AUDIENCE_FRIENDS),
  /** Indicates that all Facebook users are able to see posts made by the application. */
  EVERYONE(NativeProtocol.AUDIENCE_EVERYONE)
}
