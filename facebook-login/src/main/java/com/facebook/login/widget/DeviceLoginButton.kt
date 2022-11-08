/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.DeviceLoginManager
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager

/**
 * A Log In/Log Out button that maintains login state and logs in/out for the app.
 *
 * This control requires the app ID and client token to be specified in the AndroidManifest.xml.
 */
class DeviceLoginButton : LoginButton {
  /**
   * Uri that will be used to redirect the user to after they complete the
   * device login flow on the external device.
   *
   * The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
   */
  var deviceRedirectUri: Uri? = null

  /**
   * Create the LoginButton by inflating from XML and applying a style.
   *
   * @see LoginButton
   */
  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

  /**
   * Create the LoginButton by inflating from XML
   *
   * @see LoginButton
   */
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  /**
   * Create the LoginButton by inflating from XML
   *
   * @see LoginButton
   */
  constructor(context: Context) : super(context)

  override val newLoginClickListener: LoginClickListener
    get() = DeviceLoginClickListener()

  @AutoHandleExceptions
  private inner class DeviceLoginClickListener : LoginClickListener() {
    override fun getLoginManager(): LoginManager {
      val manager = DeviceLoginManager.instance
      manager.setDefaultAudience(defaultAudience)
      manager.setLoginBehavior(LoginBehavior.DEVICE_AUTH)
      manager.deviceRedirectUri = deviceRedirectUri
      return manager
    }
  }
}
