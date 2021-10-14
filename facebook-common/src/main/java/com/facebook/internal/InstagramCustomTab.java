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

package com.facebook.internal;

import android.net.Uri;
import android.os.Bundle;
import com.facebook.FacebookSdk;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import com.facebook.login.CustomTabLoginMethodHandler;

@AutoHandleExceptions
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class InstagramCustomTab extends CustomTab {

  public InstagramCustomTab(String action, Bundle parameters) {
    super(action, parameters);
    if (parameters == null) {
      parameters = new Bundle();
    }
    this.setUri(getURIForAction(action, parameters));
  }

  public static Uri getURIForAction(String action, Bundle parameters) {
    if (action.equals(CustomTabLoginMethodHandler.OAUTH_DIALOG)) {
      // Instagram has their own non-graph endpoint for oauth
      return Utility.buildUri(
          ServerProtocol.getInstagramDialogAuthority(),
          ServerProtocol.INSTAGRAM_OAUTH_PATH,
          parameters);
    }
    return Utility.buildUri(
        ServerProtocol.getInstagramDialogAuthority(),
        FacebookSdk.getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
        parameters);
  }
}
