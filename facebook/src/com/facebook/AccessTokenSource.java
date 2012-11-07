/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * native Facebook app for Android.
     */
    FACEBOOK_APPLICATION(true),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * Web-based dialog.
     */
    WEB_VIEW(false),
    /**
     * Indicates an access token is for a test user rather than an actual
     * Facebook user.
     */
    TEST_USER(true);

    private final boolean canExtendToken;

    AccessTokenSource(boolean canExtendToken) {
        this.canExtendToken = canExtendToken;
    }

    boolean canExtendToken() {
        return canExtendToken;
    }
}