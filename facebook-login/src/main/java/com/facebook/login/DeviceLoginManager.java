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

import android.net.Uri;

import java.util.Collection;

/**
 * This class manages device login and permissions for Facebook.
 */
public class DeviceLoginManager extends LoginManager {
    private Uri deviceRedirectUri;

    private static volatile DeviceLoginManager instance;

    /**
     * Getter for the login manager.
     * @return The login manager.
     */
    public static DeviceLoginManager getInstance() {
        if (instance == null) {
            synchronized (DeviceLoginManager.class) {
                if (instance == null) {
                    instance = new DeviceLoginManager();
                }
            }
        }
        return instance;
    }

    /**
     * Set uri to redirect the user to after they complete
     * the device login flow on the external device.
     * <p/>
     * The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
     *
     * @param uri The URI to set.
     */
    public void setDeviceRedirectUri(Uri uri) {
        this.deviceRedirectUri = uri;
    }

    /**
     * Get the previously set uri that will be used to redirect the user to
     * after they complete the device login flow on the external device.
     * <p/>
     * The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
     *
     * @return The current device redirect uri set.
     */
    public Uri getDeviceRedirectUri() {
        return this.deviceRedirectUri;
    }

    @Override
    protected LoginClient.Request createLoginRequest(Collection<String> permissions) {
        LoginClient.Request request = super.createLoginRequest(permissions);
        Uri redirectUri = getDeviceRedirectUri();
        if (redirectUri != null) {
            request.setDeviceRedirectUriString(redirectUri.toString());
        }
        return request;
    }
}
