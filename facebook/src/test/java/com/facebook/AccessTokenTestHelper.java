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

package com.facebook;

import android.os.Bundle;

public final class AccessTokenTestHelper {

    public static Bundle toLegacyCacheBundle(AccessToken accessToken) {
        Bundle bundle = new Bundle();

        LegacyTokenHelper.putToken(bundle, accessToken.getToken());
        LegacyTokenHelper.putDate(
                bundle,
                LegacyTokenHelper.EXPIRATION_DATE_KEY,
                accessToken.getExpires());
        LegacyTokenHelper.putPermissions(bundle, accessToken.getPermissions());
        LegacyTokenHelper.putDeclinedPermissions(
                bundle, accessToken.getDeclinedPermissions());
        LegacyTokenHelper.putSource(bundle, accessToken.getSource());
        LegacyTokenHelper.putDate(
                bundle,
                LegacyTokenHelper.LAST_REFRESH_DATE_KEY,
                accessToken.getLastRefresh());
        LegacyTokenHelper.putApplicationId(
                bundle, accessToken.getApplicationId());

        return bundle;
    }
}
