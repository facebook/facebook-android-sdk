/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** Indicates where a Facebook access token was obtained from. */
enum class AccessTokenSource(private val canExtendToken: Boolean) {
  /** Indicates an access token has not been obtained, or is otherwise invalid. */
  NONE(false),

  /**
   * Indicates an access token was obtained by the user logging in through the Facebook app for
   * Android using the web login dialog.
   */
  FACEBOOK_APPLICATION_WEB(true),

  /**
   * Indicates an access token was obtained by the user logging in through the Facebook app for
   * Android using the native login dialog.
   */
  FACEBOOK_APPLICATION_NATIVE(true),

  /**
   * Indicates an access token was obtained by asking the Facebook app for the current token based
   * on permissions the user has already granted to the app. No dialog was shown to the user in this
   * case.
   */
  FACEBOOK_APPLICATION_SERVICE(true),

  /** Indicates an access token was obtained by the user logging in through the Web-based dialog. */
  WEB_VIEW(true),

  /**
   * Indicates an access token was obtained by the user logging in through the Web-based dialog on a
   * Chrome Custom Tab.
   */
  CHROME_CUSTOM_TAB(true),

  /** Indicates an access token is for a test user rather than an actual Facebook user. */
  TEST_USER(true),

  /** Indicates an access token constructed with a Client Token. */
  CLIENT_TOKEN(true),

  /** Indicates an access token constructed from facebook.com/device */
  DEVICE_AUTH(true),

  /**
   * Indicates an access token was obtained by the user logging in through the Instagram app for
   * Android using the web login dialog.
   */
  INSTAGRAM_APPLICATION_WEB(true),

  /**
   * Indicates an access token was obtained by the user logging in to Instagram through the
   * Web-based dialog on a Chrome Custom Tab.
   */
  INSTAGRAM_CUSTOM_CHROME_TAB(true),

  /**
   * Indicates an access token was obtained by the user logging in to Instagram through the
   * Web-based dialog.
   */
  INSTAGRAM_WEB_VIEW(true);

  /** @return canExtendToken */
  fun canExtendToken(): Boolean = canExtendToken

  /** @return if this token is from instagram */
  fun fromInstagram(): Boolean {
    return when (this) {
      INSTAGRAM_APPLICATION_WEB,
      INSTAGRAM_CUSTOM_CHROME_TAB,
      INSTAGRAM_WEB_VIEW -> true
      else -> false
    }
  }
}
