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

  /**
   * Specifies that only the web view dialog auth should be used.
   * @deprecated Web view login is deprecated. This value will be removed in a future release. More
   * information at https://developers.facebook.com/docs/facebook-login/android/deprecating-webviews
   */
  @Deprecated("Webview is deprecated as of 11/5/2021")
  WEB_VIEW_ONLY(false, false, true, false, false, false, false),

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
  fun allowsGetTokenAuth(): Boolean {
    return allowsGetTokenAuth
  }

  /** Note: This getter is for internal only */
  fun allowsKatanaAuth(): Boolean {
    return allowsKatanaAuth
  }

  /** Note: This getter is for internal only */
  fun allowsWebViewAuth(): Boolean {
    return allowsWebViewAuth
  }

  /** Note: This getter is for internal only */
  fun allowsDeviceAuth(): Boolean {
    return allowsDeviceAuth
  }

  /** Note: This getter is for internal only */
  fun allowsCustomTabAuth(): Boolean {
    return allowsCustomTabAuth
  }

  /** Note: This getter is for internal only */
  fun allowsFacebookLiteAuth(): Boolean {
    return allowsFacebookLiteAuth
  }

  /** Note: This getter is for internal only */
  fun allowsInstagramAppAuth(): Boolean {
    return allowsInstagramAppAuth
  }
}
