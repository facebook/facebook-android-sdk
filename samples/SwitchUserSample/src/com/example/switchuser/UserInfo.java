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

package com.example.switchuser;

import android.os.Parcel;
import android.os.Parcelable;
import com.facebook.AccessToken;

class UserInfo implements Parcelable {
  private String userName;
  private AccessToken accessToken;

  public UserInfo(String userName, AccessToken accessToken) {
    this.userName = userName;
    this.accessToken = accessToken;
  }

  public String getUserName() {
    return userName;
  }

  public AccessToken getAccessToken() {
    return accessToken;
  }

  UserInfo(Parcel parcel) {
    this.userName = parcel.readString();
    this.accessToken = parcel.readParcelable(UserInfo.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(userName);
    dest.writeParcelable(accessToken, 0);
  }

  public static final Parcelable.Creator<UserInfo> CREATOR =
      new Parcelable.Creator() {

        @Override
        public UserInfo createFromParcel(Parcel source) {
          return new UserInfo(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
          return new UserInfo[size];
        }
      };
}
