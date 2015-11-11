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

/**
 * Indicates where a Facebook access token was obtained from.
 */
public enum AccessTokenSource {
    /**
     * Indicates an access token has not been obtained, or is otherwise invalid.
     */
    NONE(false),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * Facebook app for Android using the web login dialog.
     */
    FACEBOOK_APPLICATION_WEB(true),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * Facebook app for Android using the native login dialog.
     */
    FACEBOOK_APPLICATION_NATIVE(true),
    /**
     * Indicates an access token was obtained by asking the Facebook app for the
     * current token based on permissions the user has already granted to the app.
     * No dialog was shown to the user in this case.
     */
    FACEBOOK_APPLICATION_SERVICE(true),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * Web-based dialog.
     */
    WEB_VIEW(false),
    /**
     * Indicates an access token is for a test user rather than an actual
     * Facebook user.
     */
    TEST_USER(true),
    /**
     * Indicates an access token constructed with a Client Token.
     */
    CLIENT_TOKEN(true);

    private final boolean canExtendToken;

    AccessTokenSource(boolean canExtendToken) {
        this.canExtendToken = canExtendToken;
    }

    boolean canExtendToken() {
        return canExtendToken;
    }
}
