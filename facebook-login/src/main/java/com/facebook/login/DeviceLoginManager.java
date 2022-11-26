/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login;

import android.net.Uri;
import androidx.annotation.Nullable;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.Collection;

/** This class manages device login and permissions for Facebook. */
@AutoHandleExceptions
public class DeviceLoginManager extends LoginManager {
  private Uri deviceRedirectUri;
  @Nullable private String deviceAuthTargetUserId;

  private static volatile DeviceLoginManager instance;

  /**
   * Getter for the login manager.
   *
   * @return The login manager.
   */
  public static DeviceLoginManager getInstance() {
    if (instance == null) {
      synchronized (DeviceLoginManager.class) {
        if (instance == null) {
          instance = new DeviceLoginManager();
        }
      }
    }
    return instance;
  }

  /**
   * Set uri to redirect the user to after they complete the device login flow on the external
   * device.
   *
   * <p>The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
   *
   * @param uri The URI to set.
   */
  public void setDeviceRedirectUri(Uri uri) {
    this.deviceRedirectUri = uri;
  }

  /**
   * Get the previously set uri that will be used to redirect the user to after they complete the
   * device login flow on the external device.
   *
   * <p>The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
   *
   * @return The current device redirect uri set.
   */
  public Uri getDeviceRedirectUri() {
    return this.deviceRedirectUri;
  }

  /**
   * Optional. Set to target the device request to a specific user.
   *
   * @param targetUserId The user id to target.
   */
  public void setDeviceAuthTargetUserId(@Nullable String targetUserId) {
    this.deviceAuthTargetUserId = targetUserId;
  }

  /**
   * Get the target user id for the device request, if any.
   *
   * @return The target user id or null if not set.
   */
  @Nullable
  public String getDeviceAuthTargetUserId() {
    return this.deviceAuthTargetUserId;
  }

  @Override
  protected LoginClient.Request createLoginRequest(Collection<String> permissions) {
    LoginClient.Request request = super.createLoginRequest(permissions);
    Uri redirectUri = getDeviceRedirectUri();
    if (redirectUri != null) {
      request.setDeviceRedirectUriString(redirectUri.toString());
    }
    String deviceTargetUserId = getDeviceAuthTargetUserId();
    if (deviceTargetUserId != null) {
      request.setDeviceAuthTargetUserId(deviceTargetUserId);
    }
    return request;
  }
}
