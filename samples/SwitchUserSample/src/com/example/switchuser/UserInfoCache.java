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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.util.Base64;
import com.facebook.FacebookSdk;

class UserInfoCache {
  private static final String USER_INFO_CACHE_FORMAT = "userInfo%d";
  private final String userInfoCacheKey;
  private final int slot;

  public UserInfoCache(int slotNumber) {
    userInfoCacheKey = String.format(USER_INFO_CACHE_FORMAT, slotNumber);
    slot = slotNumber;
  }

  public UserInfo get() {
    SharedPreferences prefs = getSharedPrefs();
    String encodedToken = prefs.getString(userInfoCacheKey, null);
    if (encodedToken == null) {
      return null;
    }
    UserInfo info = decodeUserInfo(encodedToken);
    if (info.getAccessToken().isExpired()) {
      clear();
      return null;
    }
    return info;
  }

  public void put(UserInfo userInfo) {
    SharedPreferences.Editor editor = getSharedPrefs().edit();
    String encodedToken = encodeUserInfo(userInfo);
    editor.putString(userInfoCacheKey, encodedToken);
    editor.apply();
  }

  public void clear() {
    SharedPreferences.Editor editor = getSharedPrefs().edit();
    editor.remove(userInfoCacheKey);
    editor.apply();
  }

  private static SharedPreferences getSharedPrefs() {
    return FacebookSdk.getApplicationContext()
        .getSharedPreferences("accessTokens", Context.MODE_PRIVATE);
  }

  private static UserInfo decodeUserInfo(String base64EncodedToken) {
    byte[] data = Base64.decode(base64EncodedToken, Base64.DEFAULT);
    Parcel parcel = Parcel.obtain();
    parcel.unmarshall(data, 0, data.length);
    parcel.setDataPosition(0);
    UserInfo userInfo = (UserInfo) parcel.readValue(UserInfo.class.getClassLoader());
    parcel.recycle();
    return userInfo;
  }

  private static String encodeUserInfo(UserInfo userInfo) {
    Parcel parcel = Parcel.obtain();
    parcel.writeValue(userInfo);
    byte[] data = parcel.marshall();
    parcel.recycle();
    return Base64.encodeToString(data, Base64.DEFAULT);
  }
}
