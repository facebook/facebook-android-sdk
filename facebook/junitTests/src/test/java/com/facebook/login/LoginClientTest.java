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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.facebook.AccessToken;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({ LoginClient.class })
public class LoginClientTest extends FacebookPowerMockTestCase {

    private static final String ACCESS_TOKEN = "An access token for user 1";
    private static final String USER_ID = "1001";
    private static final String APP_ID = "2002";


    private static final long EXPIRES_IN_DELTA = 3600 * 24 * 60;
    private static final HashSet<String> PERMISSIONS = new HashSet<String>(
        Arrays.asList("go outside", "come back in"));
    private static final String ERROR_MESSAGE = "This is bad!";

    @Mock private Fragment mockFragment;

    @Before
    public void before() throws Exception {
        FragmentActivity activity =
            Robolectric.buildActivity(FragmentActivity.class).create().get();
        when(mockFragment.getActivity()).thenReturn(activity);
    }

    @Test
    public void testReauthorizationWithSameFbidSucceeds() throws Exception {
        FacebookSdk.sdkInitialize(Robolectric.application);
        LoginClient.Request request = createRequest(ACCESS_TOKEN);

        AccessToken token = new AccessToken(
                ACCESS_TOKEN,
                APP_ID,
                USER_ID,
                PERMISSIONS,
                null,
                null,
                null,
                null);
        LoginClient.Result result = LoginClient.Result.createTokenResult(request, token);

        LoginClient.OnCompletedListener listener = mock(LoginClient.OnCompletedListener.class);

        LoginClient client = new LoginClient(mockFragment);
        client.setOnCompletedListener(listener);

        client.completeAndValidate(result);

        ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
            ArgumentCaptor.forClass(LoginClient.Result.class);

        verify(listener).onCompleted(resultArgumentCaptor.capture());

        result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

        AccessToken resultToken = result.token;
        assertNotNull(resultToken);
        assertEquals(ACCESS_TOKEN, resultToken.getToken());

        // We don't care about ordering.
        assertEquals(PERMISSIONS, resultToken.getPermissions());
    }

    @Test
    public void testRequestParceling() {
        LoginClient.Request request = createRequest(ACCESS_TOKEN);

        LoginClient.Request unparceledRequest = TestUtils.parcelAndUnparcel(request);

        assertEquals(LoginBehavior.SSO_WITH_FALLBACK, unparceledRequest.getLoginBehavior());
        assertEquals(new HashSet<String>(PERMISSIONS), unparceledRequest.getPermissions());
        assertEquals(DefaultAudience.FRIENDS, unparceledRequest.getDefaultAudience());
        assertEquals("1234", unparceledRequest.getApplicationId());
        assertEquals("5678", unparceledRequest.getAuthId());
        assertFalse(unparceledRequest.isRerequest());
    }

    @Test
    public void testResultParceling() {
        LoginClient.Request request = new LoginClient.Request(
                LoginBehavior.SUPPRESS_SSO,
                null,
                DefaultAudience.EVERYONE,
                null,
                null);
        request.setRerequest(true);
        AccessToken token1 = new AccessToken(
                "Token2",
                "12345",
                "1000",
                null,
                null,
                null,
                null,
                null);
        LoginClient.Result result = new LoginClient.Result(
                request,
                LoginClient.Result.Code.SUCCESS,
                token1,
                "error 1",
                "123"
        );

        LoginClient.Result unparceledResult = TestUtils.parcelAndUnparcel(result);
        LoginClient.Request unparceledRequest = unparceledResult.request;

        assertEquals(LoginBehavior.SUPPRESS_SSO, unparceledRequest.getLoginBehavior());
        assertEquals(new HashSet<String>(), unparceledRequest.getPermissions());
        assertEquals(DefaultAudience.EVERYONE, unparceledRequest.getDefaultAudience());
        assertEquals(null, unparceledRequest.getApplicationId());
        assertEquals(null, unparceledRequest.getAuthId());
        assertTrue(unparceledRequest.isRerequest());

        assertEquals(LoginClient.Result.Code.SUCCESS, unparceledResult.code);
        assertEquals(token1, unparceledResult.token);
        assertEquals("error 1", unparceledResult.errorMessage);
        assertEquals("123", unparceledResult.errorCode);
    }


    protected LoginClient.Request createRequest(String previousAccessTokenString) {
        return new LoginClient.Request(
                LoginBehavior.SSO_WITH_FALLBACK,
                new HashSet<String>(PERMISSIONS),
                DefaultAudience.FRIENDS,
                "1234",
                "5678");
    }

}
