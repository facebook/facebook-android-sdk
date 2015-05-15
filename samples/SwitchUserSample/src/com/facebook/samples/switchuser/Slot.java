/**
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

package com.facebook.samples.switchuser;

import com.facebook.AccessToken;
import com.facebook.login.LoginBehavior;

public class Slot {
    private UserInfo userInfo;
    private final UserInfoCache userInfoCache;
    private LoginBehavior loginBehavior;

    public Slot(int slotNumber, LoginBehavior loginBehavior) {
        this.loginBehavior = loginBehavior;
        this.userInfoCache = new UserInfoCache(slotNumber);
        this.userInfo = userInfoCache.get();
    }

    public LoginBehavior getLoginBehavior() {
        return loginBehavior;
    }

    public String getUserName() {
        return (userInfo != null) ? userInfo.getUserName() : null;
    }

    public AccessToken getAccessToken() {
        return (userInfo != null) ? userInfo.getAccessToken() : null;
    }

    public String getUserId() {
        return (userInfo != null) ? userInfo.getAccessToken().getUserId() : null;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo user) {
        userInfo = user;
        if (user == null) {
            return;
        }

        userInfoCache.put(user);
    }

    public void clear() {
        userInfo = null;
        userInfoCache.clear();
    }
}
