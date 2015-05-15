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

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

@PrepareForTest( { FacebookSdk.class })
public class AccessTokenTrackerTest extends FacebookPowerMockTestCase {

    private final List<String> PERMISSIONS = Arrays.asList("walk", "chew gum");
    private final Date EXPIRES = new Date(2025, 5, 3);
    private final Date LAST_REFRESH = new Date(2023, 8, 15);
    private final String APP_ID = "1234";
    private final String USER_ID = "1000";

    private LocalBroadcastManager localBroadcastManager;
    private TestAccessTokenTracker accessTokenTracker = null;

    @Before
    public void before() throws Exception {
        mockStatic(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        when(FacebookSdk.getApplicationContext()).thenReturn(Robolectric.application);

        localBroadcastManager = LocalBroadcastManager.getInstance(Robolectric.application);
    }

    @After
    public void after() throws Exception {
        if (accessTokenTracker != null && accessTokenTracker.isTracking()) {
            accessTokenTracker.stopTracking();
        }
    }

    @Test
    public void testRequiresSdkToBeInitialized() {
        try {
            when(FacebookSdk.isInitialized()).thenReturn(false);

            accessTokenTracker = new TestAccessTokenTracker();

            fail();
        } catch (FacebookSdkNotInitializedException exception) {
        }
    }

    @Test
    public void testDefaultsToTracking() {
        accessTokenTracker = new TestAccessTokenTracker();

        assertTrue(accessTokenTracker.isTracking());
    }

    @Test
    public void testCanTurnTrackingOff() {
        accessTokenTracker = new TestAccessTokenTracker();

        accessTokenTracker.stopTracking();

        assertFalse(accessTokenTracker.isTracking());
    }

    @Test
    public void testCanTurnTrackingOn() {
        accessTokenTracker = new TestAccessTokenTracker();

        accessTokenTracker.stopTracking();
        accessTokenTracker.startTracking();

        assertTrue(accessTokenTracker.isTracking());
    }

    @Test
    public void testCallbackCalledOnBroadcastReceived() throws Exception {
        accessTokenTracker = new TestAccessTokenTracker();

        AccessToken oldAccessToken = createAccessToken("I'm old!");
        AccessToken currentAccessToken = createAccessToken("I'm current!");

        sendBroadcast(oldAccessToken, currentAccessToken);


        assertNotNull(accessTokenTracker.currentAccessToken);
        assertEquals(currentAccessToken.getToken(), accessTokenTracker.currentAccessToken.getToken());
        assertNotNull(accessTokenTracker.oldAccessToken);
        assertEquals(oldAccessToken.getToken(), accessTokenTracker.oldAccessToken.getToken());
    }

    private AccessToken createAccessToken(String tokenString) {
        return new AccessToken(
                tokenString,
                APP_ID,
                USER_ID,
                PERMISSIONS,
                null,
                AccessTokenSource.WEB_VIEW,
                EXPIRES,
                LAST_REFRESH);
    }

    private void sendBroadcast(AccessToken oldAccessToken, AccessToken currentAccessToken) {
        Intent intent = new Intent(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED);

        intent.putExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN, oldAccessToken);
        intent.putExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN, currentAccessToken);

        localBroadcastManager.sendBroadcast(intent);
    }

    class TestAccessTokenTracker extends AccessTokenTracker {

        public AccessToken currentAccessToken;
        public AccessToken oldAccessToken;

        public TestAccessTokenTracker() {
            super();
        }

        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
            AccessToken currentAccessToken) {
            this.oldAccessToken = oldAccessToken;
            this.currentAccessToken = currentAccessToken;
        }
    }
}
