/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.login;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.facebook.AccessTokenSource;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;

class InstagramAppLoginMethodHandler extends NativeAppLoginMethodHandler {

  InstagramAppLoginMethodHandler(LoginClient loginClient) {
    super(loginClient);
  }

  @Override
  public String getNameForLogging() {
    return "instagram_login";
  }

  @Override
  public AccessTokenSource getTokenSource() {
    return AccessTokenSource.INSTAGRAM_APPLICATION_WEB;
  }

  @Override
  public int tryAuthorize(LoginClient.Request request) {
    String e2e = LoginClient.getE2E();
    Intent intent =
        NativeProtocol.createInstagramIntent(
            getLoginClient().getActivity(),
            request.getApplicationId(),
            request.getPermissions(),
            e2e,
            request.isRerequest(),
            request.hasPublishPermission(),
            request.getDefaultAudience(),
            getClientState(request.getAuthId()),
            request.getAuthType(),
            request.getMessengerPageId(),
            request.getResetMessengerState(),
            request.isFamilyLogin(),
            request.shouldSkipAccountDeduplication());

    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

    boolean result = tryIntent(intent, LoginClient.getLoginRequestCode());
    return result ? 1 : 0;
  }

  InstagramAppLoginMethodHandler(Parcel source) {
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

  public static final Parcelable.Creator<InstagramAppLoginMethodHandler> CREATOR =
      new Parcelable.Creator() {

        @Override
        public InstagramAppLoginMethodHandler createFromParcel(Parcel source) {
          return new InstagramAppLoginMethodHandler(source);
        }

        @Override
        public InstagramAppLoginMethodHandler[] newArray(int size) {
          return new InstagramAppLoginMethodHandler[size];
        }
      };
}
