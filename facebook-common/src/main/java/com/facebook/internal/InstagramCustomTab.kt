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
