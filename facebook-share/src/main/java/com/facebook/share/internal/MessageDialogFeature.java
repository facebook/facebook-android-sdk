/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal;

import com.facebook.internal.DialogFeature;
import com.facebook.internal.NativeProtocol;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public enum MessageDialogFeature implements DialogFeature {
  /**
   * Indicates whether the native Message dialog itself is supported by the installed version of the
   * Facebook Messenger application.
   */
  MESSAGE_DIALOG(NativeProtocol.PROTOCOL_VERSION_20140204),
  /** Indicates whether the native Message dialog supports sharing of photo images. */
  PHOTOS(NativeProtocol.PROTOCOL_VERSION_20140324),
  /** Indicates whether the native Message dialog supports sharing of videos. */
  VIDEO(NativeProtocol.PROTOCOL_VERSION_20141218),

  /**
   * Indicates whether the native Message dialog supports sharing of Messenger generic template
   * Content.
   */
  MESSENGER_GENERIC_TEMPLATE(NativeProtocol.PROTOCOL_VERSION_20171115),

  /**
   * Indicates whether the native Message dialog supports sharing of Messenger open graph music
   * template content.
   */
  MESSENGER_OPEN_GRAPH_MUSIC_TEMPLATE(NativeProtocol.PROTOCOL_VERSION_20171115),

  /**
   * Indicates whether the native Message dialog supports sharing of Messenger media template
   * content.
   */
  MESSENGER_MEDIA_TEMPLATE(NativeProtocol.PROTOCOL_VERSION_20171115),
  ;

  private int minVersion;

  MessageDialogFeature(int minVersion) {
    this.minVersion = minVersion;
  }

  /** This method is for internal use only. */
  public String getAction() {
    return NativeProtocol.ACTION_MESSAGE_DIALOG;
  }

  /** This method is for internal use only. */
  public int getMinVersion() {
    return minVersion;
  }
}
