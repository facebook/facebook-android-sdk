/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

/** Specifies the behaviors to try during login. */
enum class LoginBehavior {
  /**
   * Specifies that login should attempt login in using a native app in the Facebook Family of Apps,
   * and if that does not work, fall back to web dialog auth. This is the default behavior.
   */
  NATIVE_WITH_FALLBACK(true, true, true, false, true, true, true),

  /**
   * Specifies that login should only attempt to login using a native app in the Facebook Family of
   * Apps. If the native app cannot be used, then the login fails.
   */
  NATIVE_ONLY(true, true, false, false, false, true, true),

  /** Specifies that login should only attempt to use Katana Proxy Login. */
  KATANA_ONLY(false, true, false, false, false, false, false),

  /** Specifies that only the web dialog auth should be used. */
  WEB_ONLY(false, false, true, false, true, false, false),

  /** Specifies that only the web dialog auth (from anywhere) should be used */
  DIALOG_ONLY(false, true, true, false, true, true, true),

  /**
   * Specifies that device login authentication flow should be used. Use it via ({@link
   * com.facebook.login.widget.DeviceLoginButton DeviceLoginButton} or ({@link
   * com.facebook.login.DeviceLoginManager DeviceLoginManager} to authenticate.
   */
  DEVICE_AUTH(false, false, false, true, false, false, false);

  private val allowsGetTokenAuth: Boolean
  private val allowsKatanaAuth: Boolean
  private val allowsWebViewAuth: Boolean
  private val allowsDeviceAuth: Boolean
  private val allowsCustomTabAuth: Boolean
  private val allowsFacebookLiteAuth: Boolean
  private val allowsInstagramAppAuth: Boolean

  constructor(
      allowsGetTokenAuth: Boolean,
      allowsKatanaAuth: Boolean,
      allowsWebViewAuth: Boolean,
      allowsDeviceAuth: Boolean,
      allowsCustomTabAuth: Boolean,
      allowsFacebookLiteAuth: Boolean,
      allowsInstagramAppAuth: Boolean
  ) {
    this.allowsGetTokenAuth = allowsGetTokenAuth
    this.allowsKatanaAuth = allowsKatanaAuth
    this.allowsWebViewAuth = allowsWebViewAuth
    this.allowsDeviceAuth = allowsDeviceAuth
    this.allowsCustomTabAuth = allowsCustomTabAuth
    this.allowsFacebookLiteAuth = allowsFacebookLiteAuth
    this.allowsInstagramAppAuth = allowsInstagramAppAuth
  }

  /** Note: This getter is for internal only */
  fun allowsGetTokenAuth(): Boolean = allowsGetTokenAuth

  /** Note: This getter is for internal only */
  fun allowsKatanaAuth(): Boolean = allowsKatanaAuth

  /** Note: This getter is for internal only */
  fun allowsWebViewAuth(): Boolean = allowsWebViewAuth

  /** Note: This getter is for internal only */
  fun allowsDeviceAuth(): Boolean = allowsDeviceAuth

  /** Note: This getter is for internal only */
  fun allowsCustomTabAuth(): Boolean = allowsCustomTabAuth

  /** Note: This getter is for internal only */
  fun allowsFacebookLiteAuth(): Boolean = allowsFacebookLiteAuth

  /** Note: This getter is for internal only */
  fun allowsInstagramAppAuth(): Boolean = allowsInstagramAppAuth
}
