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

package com.facebook.appevents;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;

import java.io.Serializable;

class AccessTokenAppIdPair implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String accessTokenString;
    private final String applicationId;

    public AccessTokenAppIdPair(AccessToken accessToken) {
        this(accessToken.getToken(), FacebookSdk.getApplicationId());
    }

    public AccessTokenAppIdPair(String accessTokenString, String applicationId) {
        this.accessTokenString = Utility.isNullOrEmpty(accessTokenString)
                ? null
                : accessTokenString;
        this.applicationId = applicationId;
    }

    public String getAccessTokenString() {
        return accessTokenString;
    }

    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public int hashCode() {
        return (accessTokenString == null ? 0 : accessTokenString.hashCode()) ^
                (applicationId == null ? 0 : applicationId.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AccessTokenAppIdPair)) {
            return false;
        }
        AccessTokenAppIdPair p = (AccessTokenAppIdPair) o;
        return Utility.areObjectsEqual(p.accessTokenString, accessTokenString) &&
                Utility.areObjectsEqual(p.applicationId, applicationId);
    }

    static class SerializationProxyV1 implements Serializable {
        private static final long serialVersionUID = -2488473066578201069L;
        private final String accessTokenString;
        private final String appId;

        private SerializationProxyV1(String accessTokenString, String appId) {
            this.accessTokenString = accessTokenString;
            this.appId = appId;
        }

        private Object readResolve() {
            return new AccessTokenAppIdPair(accessTokenString, appId);
        }
    }

    private Object writeReplace() {
        return new SerializationProxyV1(accessTokenString, applicationId);
    }
}
