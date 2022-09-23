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
public enum AppInviteDialogFeature implements DialogFeature {

  // This matches a value in a sitevar. DO NOT CHANGE
  APP_INVITES_DIALOG(NativeProtocol.PROTOCOL_VERSION_20140701);

  private int minVersion;

  AppInviteDialogFeature(int minVersion) {
    this.minVersion = minVersion;
  }

  public String getAction() {
    return NativeProtocol.ACTION_APPINVITE_DIALOG;
  }

  public int getMinVersion() {
    return minVersion;
  }
}
