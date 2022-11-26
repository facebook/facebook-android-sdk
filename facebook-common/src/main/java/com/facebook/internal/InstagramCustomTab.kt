/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookSdk.getGraphApiVersion
import com.facebook.internal.ServerProtocol.getInstagramDialogAuthority
import com.facebook.internal.Utility.buildUri
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.CustomTabLoginMethodHandler

@AutoHandleExceptions
class InstagramCustomTab(action: String, parameters: Bundle?) : CustomTab(action, parameters) {
  companion object {
    fun getURIForAction(action: String, parameters: Bundle?): Uri {
      return if (action == CustomTabLoginMethodHandler.OAUTH_DIALOG) {
        // Instagram has their own non-graph endpoint for oauth
        buildUri(getInstagramDialogAuthority(), ServerProtocol.INSTAGRAM_OAUTH_PATH, parameters)
      } else
          buildUri(
              getInstagramDialogAuthority(),
              getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
              parameters)
    }
  }

  init {
    var parameters = parameters
    if (parameters == null) {
      parameters = Bundle()
    }
    uri = getURIForAction(action, parameters)
  }
}
