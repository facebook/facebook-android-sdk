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

package com.facebook.login.widget;

import android.app.Activity;

import com.facebook.FacebookTestCase;
import com.facebook.junittests.MainActivity;
import com.facebook.junittests.R;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginManager;

import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.ArrayList;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

public class LoginButtonTest extends FacebookTestCase {

    @Test
    public void testLoginButtonWithReadPermissions() throws Exception {
        LoginManager loginManager = mock(LoginManager.class);
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();

        LoginButton loginButton = (LoginButton) activity.findViewById(R.id.login_button);
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add("user_location");
        loginButton.setReadPermissions(permissions);
        loginButton.setDefaultAudience(DefaultAudience.EVERYONE);
        loginButton.setLoginManager(loginManager);
        loginButton.performClick();

        verify(loginManager).logInWithReadPermissions(activity, permissions);
        verify(loginManager, never())
                .logInWithPublishPermissions(isA(Activity.class), anyCollection());
        // Verify default audience is channeled
        verify(loginManager).setDefaultAudience(DefaultAudience.EVERYONE);
    }

    @Test
    public void testLoginButtonWithPublishPermissions() throws Exception {
        LoginManager loginManager = mock(LoginManager.class);
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();

        LoginButton loginButton = (LoginButton) activity.findViewById(R.id.login_button);
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add("publish_actions");
        loginButton.setPublishPermissions(permissions);
        loginButton.setLoginManager(loginManager);
        loginButton.performClick();

        verify(loginManager, never())
                .logInWithReadPermissions(isA(Activity.class), anyCollection());
        verify(loginManager).logInWithPublishPermissions(activity, permissions);
    }

    @Test
    public void testCantSetReadThenPublishPermissions() throws Exception {
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();

        LoginButton loginButton = (LoginButton) activity.findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_location");
        try {
            loginButton.setPublishPermissions("publish_actions");
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail();
    }

    @Test
    public void testCantSetPublishThenReadPermissions() throws Exception {
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();

        LoginButton loginButton = (LoginButton) activity.findViewById(R.id.login_button);
        loginButton.setPublishPermissions("publish_actions");
        try {
            loginButton.setReadPermissions("user_location");
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail();
    }
}
