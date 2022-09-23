/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import com.facebook.FacebookSdk.getGraphApiVersion
import com.facebook.internal.ServerProtocol.getDialogAuthority
import com.facebook.internal.ServerProtocol.getGamingDialogAuthority
import com.facebook.internal.Utility.buildUri
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.CustomTabPrefetchHelper

@AutoHandleExceptions
open class CustomTab(action: String, parameters: Bundle?) {
  protected var uri: Uri
  fun openCustomTab(activity: Activity, packageName: String?): Boolean {
    val session = CustomTabPrefetchHelper.getPreparedSessionOnce()
    val customTabsIntent = CustomTabsIntent.Builder(session).build()
    customTabsIntent.intent.setPackage(packageName)
    try {
      customTabsIntent.launchUrl(activity, uri)
    } catch (e: ActivityNotFoundException) {
      return false
    }
    return true
  }

  companion object {
    @JvmStatic
    open fun getURIForAction(action: String, parameters: Bundle?): Uri {
      return buildUri(
          getDialogAuthority(),
          getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
          parameters)
    }
  }

  init {
    var parameters = parameters
    if (parameters == null) {
      parameters = Bundle()
    }
    uri =
        if (action == "context_choose") {
          buildUri(
              getGamingDialogAuthority(), "/" + ServerProtocol.DIALOG_PATH + action, parameters)
        } else {
          getURIForAction(action, parameters)
        }
  }
}
