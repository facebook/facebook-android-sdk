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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.FacebookActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        LoginClient.class,
        Validate.class,
        Utility.class,
        FacebookSdk.class,
        AccessToken.class,
        FetchedAppSettings.class,
        FetchedAppSettingsManager.class
})
public class CustomTabLoginMethodHandlerTest extends LoginHandlerTestCase {
    private final static String SIGNED_REQUEST_STR = "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ"
            + "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0";
    private final static String CHROME_PACKAGE = "com.android.chrome";
    private final static String DEV_PACKAGE = "com.chrome.dev";
    private final static String BETA_PACKAGE = "com.chrome.beta";

    private CustomTabLoginMethodHandler handler;
    private LoginClient.Request request;

    @Before
    public void setUp() {
        handler = new CustomTabLoginMethodHandler(mockLoginClient);
        request = createRequest();
    }

    @Test
    public void testCustomTabHandlesSuccess() {
        final Bundle bundle = new Bundle();
        bundle.putString("access_token", ACCESS_TOKEN);
        bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA));
        bundle.putString("code", "Something else");
        bundle.putString("signed_request", SIGNED_REQUEST_STR);
        handler.onComplete(request, bundle, null);

        final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());

        final LoginClient.Result result = resultArgumentCaptor.getValue();
        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.SUCCESS, result.code);

        final AccessToken token = result.token;
        assertNotNull(token);
        assertEquals(ACCESS_TOKEN, token.getToken());
        assertDateDiffersWithinDelta(new Date(), token.getExpires(), EXPIRES_IN_DELTA * 1000, 1000);
        TestUtils.assertSamePermissions(PERMISSIONS, token.getPermissions());
    }

    @Test
    public void testCustomTabHandlesCancel() {
        handler.onComplete(request, null, new FacebookOperationCanceledException());

        final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());
        final LoginClient.Result result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.CANCEL, result.code);
        assertNull(result.token);
        assertNotNull(result.errorMessage);
    }

    @Test
    public void testCustomTabHandlesError() {
        handler.onComplete(request, null, new FacebookException(ERROR_MESSAGE));

        final ArgumentCaptor<LoginClient.Result> resultArgumentCaptor =
                ArgumentCaptor.forClass(LoginClient.Result.class);
        verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture());
        final LoginClient.Result result = resultArgumentCaptor.getValue();

        assertNotNull(result);
        assertEquals(LoginClient.Result.Code.ERROR, result.code);
        assertNull(result.token);
        assertNotNull(result.errorMessage);
        assertEquals(ERROR_MESSAGE, result.errorMessage);
    }

    @Test
    public void testTryAuthorizeNeedsRedirectActivity() {
        mockTryAuthorize();

        mockChromeCustomTabsSupported(true, CHROME_PACKAGE);
        mockCustomTabsAllowed(true);
        mockCustomTabRedirectActivity(true);
        assertTrue(handler.tryAuthorize(request));

        mockCustomTabsAllowed(false);
        assertFalse(handler.tryAuthorize(request));
    }

    @Test
    public void testTryAuthorizeWithChromePackage() {
        mockTryAuthorize();
        mockCustomTabsAllowed(true);
        mockCustomTabRedirectActivity(true);

        mockChromeCustomTabsSupported(true, CHROME_PACKAGE);
        assertTrue(handler.tryAuthorize(request));
    }

    @Test
    public void testTryAuthorizeWithChromeBetaPackage() {
        mockTryAuthorize();
        mockCustomTabsAllowed(true);
        mockCustomTabRedirectActivity(true);

        mockChromeCustomTabsSupported(true, BETA_PACKAGE);
        assertTrue(handler.tryAuthorize(request));
    }

    @Test
    public void testTryAuthorizeWithChromeDevPackage() {
        mockTryAuthorize();
        mockCustomTabsAllowed(true);
        mockCustomTabRedirectActivity(true);

        mockChromeCustomTabsSupported(true, DEV_PACKAGE);
        assertTrue(handler.tryAuthorize(request));
    }

    @Test
    public void testTryAuthorizeWithoutChromePackage() {
        mockTryAuthorize();
        mockCustomTabsAllowed(true);
        mockCustomTabRedirectActivity(true);

        mockChromeCustomTabsSupported(true, "not.chrome.package");
        assertFalse(handler.tryAuthorize(request));
    }

    private void mockTryAuthorize() {
        mockStatic(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        mockStatic(AccessToken.class);
        when(AccessToken.getCurrentAccessToken()).thenReturn(null);
        Fragment fragment = mock(LoginFragment.class);
        when(mockLoginClient.getFragment()).thenReturn(fragment);
    }

    private void mockChromeCustomTabsSupported(final boolean supported, final String packageName) {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        ResolveInfo resolveInfo = new ResolveInfo();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = packageName;
        resolveInfo.serviceInfo = serviceInfo;
        if (supported) {
            resolveInfos.add(resolveInfo);
        }
        final PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentServices(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);
        activity = mock(FacebookActivity.class);
        when(mockLoginClient.getActivity()).thenReturn(activity);
        when(activity.getPackageManager()).thenReturn(packageManager);
    }

    private void mockCustomTabRedirectActivity(final boolean hasActivity) {
        mockStatic(Validate.class);
        when(Validate.hasCustomTabRedirectActivity(any(Context.class))).thenReturn(hasActivity);
    }

    private void mockCustomTabsAllowed(final boolean allowed) {
        final FetchedAppSettings settings = mock(FetchedAppSettings.class);
        when(settings.getCustomTabsEnabled()).thenReturn(allowed);
        mockStatic(FetchedAppSettingsManager.class);
        when(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyString())).thenReturn(settings);
    }

}
