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
import android.content.Intent;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest( { LoginClient.class })
public class KatanaProxyLoginMethodHandlerTest extends LoginHandlerTestCase {
    private final static String SIGNED_REQUEST_STR = "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ"
            + "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0";

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        FacebookSdk.sdkInitialize(Robolectric.application);
    }

    @Test
    public void testProxyAuthHandlesSuccess() {
        Bundle bundle = new Bundle();
        bundle.putLong("expires_in", EXPIRES_IN_DELTA);
        bundle.putString("access_token", ACCESS_TOKEN);
        bundle.putString("signed_request", SIGNED_REQUEST_STR);

        Intent intent = new Intent();
        intent.putExtras(bundle);

        KatanaProxyLoginMethodHandler handler = new KatanaProxyLoginMethodHandler(mockLoginClient);

        LoginClient.Request request = createRequest();
        when(mockLoginClient.getPendingRequest()).thenReturn(request);

        handler.tryAuthorize(request);
        handler.onActivityResult(0, Activity.RESULT_OK, intent);

        ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

        LoginClient.Result result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

        AccessToken token = result.token;
        assertNotNull(token);
        assertEquals(ACCESS_TOKEN, token.getToken());
        assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
        TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());
    }

    @Test
    public void testProxyAuthHandlesCancel() {
        Bundle bundle = new Bundle();
        bundle.putString("error", ERROR_MESSAGE);

        Intent intent = new Intent();
        intent.putExtras(bundle);

        KatanaProxyLoginMethodHandler handler = new KatanaProxyLoginMethodHandler(mockLoginClient);

        LoginClient.Request request = createRequest();
        handler.tryAuthorize(request);
        handler.onActivityResult(0, Activity.RESULT_CANCELED, intent);

        ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

        LoginClient.Result result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.CANCEL, result.code);

        assertNull(result.token);
        assertNotNull(result.errorMessage);
        assertTrue(result.errorMessage.contains(ERROR_MESSAGE));
    }

    @Test
    public void testProxyAuthHandlesCancelErrorMessage() {
        Bundle bundle = new Bundle();
        bundle.putString("error", "access_denied");

        Intent intent = new Intent();
        intent.putExtras(bundle);

        KatanaProxyLoginMethodHandler handler = new KatanaProxyLoginMethodHandler(mockLoginClient);

        LoginClient.Request request = createRequest();
        handler.tryAuthorize(request);
        handler.onActivityResult(0, Activity.RESULT_CANCELED, intent);

        ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

        LoginClient.Result result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.CANCEL, result.code);

        assertNull(result.token);
    }

    @Test
    public void testProxyAuthHandlesDisabled() {
        Bundle bundle = new Bundle();
        bundle.putString("error", "service_disabled");

        Intent intent = new Intent();
        intent.putExtras(bundle);

        KatanaProxyLoginMethodHandler handler = new KatanaProxyLoginMethodHandler(mockLoginClient);

        LoginClient.Request request = createRequest();
        handler.tryAuthorize(request);
        handler.onActivityResult(0, Activity.RESULT_OK, intent);

        verify(mockLoginClient, never()).completeAndValidate(any(LoginClient.Result.class));
        verify(mockLoginClient, times(1)).tryNextHandler();
    }
}
