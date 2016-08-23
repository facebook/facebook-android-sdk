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

package com.facebook.login;

/**
 * Specifies the behaviors to try during login.
 */
public enum LoginBehavior {
    /**
     * Specifies that login should attempt login in using the Facebook App, and if that
     * does not work fall back to web dialog auth. This is the default behavior.
     */
    NATIVE_WITH_FALLBACK(true, true, true, false, true, true),

    /**
     * Specifies that login should only attempt to login using the Facebook App.
     * If the Facebook App cannot be used then the login fails.
     */
    NATIVE_ONLY(true, true, false, false, false, true),

    /**
     * Specifies that login should only attempt to use Katana Proxy Login.
     */
    KATANA_ONLY(false, true, false, false, false, false),

    /**
     * Specifies that only the web dialog auth should be used.
     */
    WEB_ONLY(false, false, true, false, true, false),

    /**
     * Specifies that only the web view dialog auth should be used.
     */
    WEB_VIEW_ONLY(false, false, true, false, false, false),

    /**
     * Specifies that device login authentication flow should be used.
     * Use it via ({@link com.facebook.login.widget.DeviceLoginButton DeviceLoginButton}
     * or ({@link com.facebook.login.DeviceLoginManager DeviceLoginManager} to authenticate.
     */
    DEVICE_AUTH(false, false, false, true, false, false);

    private final boolean allowsGetTokenAuth;
    private final boolean allowsKatanaAuth;
    private final boolean allowsWebViewAuth;
    private final boolean allowsDeviceAuth;
    private final boolean allowsCustomTabAuth;
    private final boolean allowsFacebookLiteAuth;

    private LoginBehavior(
            boolean allowsGetTokenAuth,
            boolean allowsKatanaAuth,
            boolean allowsWebViewAuth,
            boolean allowsDeviceAuth,
            boolean allowsCustomTabAuth,
            boolean allowsFacebookLiteAuth) {
        this.allowsGetTokenAuth = allowsGetTokenAuth;
        this.allowsKatanaAuth = allowsKatanaAuth;
        this.allowsWebViewAuth = allowsWebViewAuth;
        this.allowsDeviceAuth = allowsDeviceAuth;
        this.allowsCustomTabAuth = allowsCustomTabAuth;
        this.allowsFacebookLiteAuth = allowsFacebookLiteAuth;
    }

    boolean allowsGetTokenAuth() {
        return allowsGetTokenAuth;
    }

    boolean allowsKatanaAuth() {
        return allowsKatanaAuth;
    }

    boolean allowsWebViewAuth() {
        return allowsWebViewAuth;
    }

    boolean allowsDeviceAuth() {
        return allowsDeviceAuth;
    }

    boolean allowsCustomTabAuth() {
        return allowsCustomTabAuth;
    }

    boolean allowsFacebookLiteAuth() {
        return allowsFacebookLiteAuth;
    }
}
