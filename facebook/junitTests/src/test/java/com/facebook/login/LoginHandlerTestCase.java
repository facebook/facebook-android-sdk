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

import android.app.Activity;
import android.support.v4.app.FragmentActivity;

import com.facebook.FacebookPowerMockTestCase;

import org.junit.Before;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public abstract class LoginHandlerTestCase extends FacebookPowerMockTestCase {
    protected static final String ACCESS_TOKEN = "An access token";
    protected static final String USER_ID = "1000";
    protected static final long EXPIRES_IN_DELTA = 3600 * 24 * 60;
    protected static final HashSet<String> PERMISSIONS = new HashSet<String>(
            Arrays.asList("go outside", "come back in"));
    protected static final String ERROR_MESSAGE = "This is bad!";

    protected FragmentActivity activity;
    protected LoginClient mockLoginClient;

    @Before
    public void before() throws Exception {
        mockLoginClient = mock(LoginClient.class);
        activity = Robolectric.buildActivity(FragmentActivity.class).create().get();
        when(mockLoginClient.getActivity()).thenReturn(activity);
    }

    protected LoginClient.Request createRequest() {
        return createRequest(null);
    }

    protected LoginClient.Request createRequest(String previousAccessTokenString) {

        return new LoginClient.Request(
                LoginBehavior.SSO_WITH_FALLBACK,
                new HashSet<String>(PERMISSIONS),
                DefaultAudience.FRIENDS,
                "1234",
                "5678");
    }

    protected void assertDateDiffersWithinDelta(Date expected, Date actual, long expectedDifference,
                                                long deltaInMsec) {

        long delta = Math.abs(expected.getTime() - actual.getTime()) - expectedDifference;
        assertTrue(delta < deltaInMsec);
    }
}
