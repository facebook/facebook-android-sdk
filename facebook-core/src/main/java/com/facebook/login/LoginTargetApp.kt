/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

enum class LoginTargetApp(private val targetApp: String) {
  FACEBOOK("facebook"),
  INSTAGRAM("instagram");

  override fun toString(): String = targetApp

  companion object {
    /**
     * Return the LoginTargetApp by given string
     *
     * @param stringValue
     * @return LoginTargetApp default return LoginTargetApp.FACEBOOK
     */
    @JvmStatic
    fun fromString(stringValue: String?): LoginTargetApp {
      for (targetApp in values()) {
        if (targetApp.toString() == stringValue) {
          return targetApp
        }
      }
      return FACEBOOK
    }
  }
}
