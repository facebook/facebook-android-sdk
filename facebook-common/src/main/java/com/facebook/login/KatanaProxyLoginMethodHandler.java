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

package com.facebook.login;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.facebook.FacebookSdk;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import java.util.List;

class KatanaProxyLoginMethodHandler extends NativeAppLoginMethodHandler {

  KatanaProxyLoginMethodHandler(LoginClient loginClient) {
    super(loginClient);
  }

  @Override
  String getNameForLogging() {
    return "katana_proxy_auth";
  }

  @Override
  int tryAuthorize(LoginClient.Request request) {
    LoginBehavior behavior = request.getLoginBehavior();
    boolean ignoreAppSwitchToLoggedOut =
        FacebookSdk.ignoreAppSwitchToLoggedOut
            && CustomTabUtils.getChromePackage() != null
            && behavior.allowsCustomTabAuth();
    String e2e = LoginClient.getE2E();
    List<Intent> intents =
        NativeProtocol.createProxyAuthIntents(
            loginClient.getActivity(),
            request.getApplicationId(),
            request.getPermissions(),
            e2e,
            request.isRerequest(),
            request.hasPublishPermission(),
            request.getDefaultAudience(),
            getClientState(request.getAuthId()),
            request.getAuthType(),
            ignoreAppSwitchToLoggedOut,
            request.getMessengerPageId(),
            request.getResetMessengerState(),
            request.isFamilyLogin(),
            request.shouldSkipAccountDeduplication(),
            request.getNonce());

    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

    if (intents == null) {
      return 0;
    }

    for (int i = 0; i < intents.size(); i++) {
      boolean launchedIntent = tryIntent(intents.get(i), LoginClient.getLoginRequestCode());
      if (launchedIntent) {
        return i + 1;
      }
    }

    return 0;
  }

  @Override
  public boolean shouldKeepTrackOfMultipleIntents() {
    return true;
  }

  KatanaProxyLoginMethodHandler(Parcel source) {
    super(source);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
  }

  public static final Parcelable.Creator<KatanaProxyLoginMethodHandler> CREATOR =
      new Parcelable.Creator<KatanaProxyLoginMethodHandler>() {

        @Override
        public KatanaProxyLoginMethodHandler createFromParcel(Parcel source) {
          return new KatanaProxyLoginMethodHandler(source);
        }

        @Override
        public KatanaProxyLoginMethodHandler[] newArray(int size) {
          return new KatanaProxyLoginMethodHandler[size];
        }
      };
}
