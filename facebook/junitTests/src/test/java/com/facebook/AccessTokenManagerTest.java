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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class AccessTokenManagerTest extends FacebookTestCase {

    private final String TOKEN_STRING = "A token of my esteem";
    private final String USER_ID = "1000";
    private final List<String> PERMISSIONS = Arrays.asList("walk", "chew gum");
    private final Date EXPIRES = new Date(2025, 5, 3);
    private final Date LAST_REFRESH = new Date(2023, 8, 15);
    private final String APP_ID = "1234";

    private LocalBroadcastManager localBroadcastManager;
    @Mock private AccessTokenCache accessTokenCache;

    @Before
    public void before() throws Exception {
        localBroadcastManager = LocalBroadcastManager.getInstance(Robolectric.application);
    }

    @Test
    public void testRequiresLocalBroadcastManager() {
        try {
            AccessTokenManager accessTokenManager = new AccessTokenManager(null, accessTokenCache);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testRequiresTokenCache() {
        try {
            AccessTokenManager accessTokenManager = new AccessTokenManager(localBroadcastManager,
                    null);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testDefaultsToNoCurrentAccessToken() {
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        assertNull(accessTokenManager.getCurrentAccessToken());
    }

    @Test
    public void testCanSetCurrentAccessToken() {
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        AccessToken accessToken = createAccessToken();

        accessTokenManager.setCurrentAccessToken(accessToken);

        assertEquals(accessToken, accessTokenManager.getCurrentAccessToken());
    }

    @Test
    public void testChangingAccessTokenSendsBroadcast() {
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        AccessToken accessToken = createAccessToken();

        accessTokenManager.setCurrentAccessToken(accessToken);

        final Intent intents[] = new Intent[1];
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intents[0] = intent;
            }
        };

        localBroadcastManager.registerReceiver(broadcastReceiver,
            new IntentFilter(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED));

        AccessToken anotherAccessToken = createAccessToken("another string", "1000");

        accessTokenManager.setCurrentAccessToken(anotherAccessToken);

        localBroadcastManager.unregisterReceiver(broadcastReceiver);

        Intent intent = intents[0];

        assertNotNull(intent);

        AccessToken oldAccessToken =
            (AccessToken)intent.getParcelableExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN);
        AccessToken newAccessToken =
            (AccessToken)intent.getParcelableExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN);

        assertEquals(accessToken.getToken(), oldAccessToken.getToken());
        assertEquals(anotherAccessToken.getToken(), newAccessToken.getToken());
    }

    @Test
    public void testLoadReturnsFalseIfNoCachedToken() {
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        boolean result = accessTokenManager.loadCurrentAccessToken();

        assertFalse(result);
    }

    @Test
    public void testLoadReturnsTrueIfCachedToken() {
        AccessToken accessToken = createAccessToken();
        when(accessTokenCache.load()).thenReturn(accessToken);

        AccessTokenManager accessTokenManager = createAccessTokenManager();

        boolean result = accessTokenManager.loadCurrentAccessToken();

        assertTrue(result);
    }

    @Test
    public void testLoadSetsCurrentTokenIfCached() {
        AccessToken accessToken = createAccessToken();
        when(accessTokenCache.load()).thenReturn(accessToken);

        AccessTokenManager accessTokenManager = createAccessTokenManager();

        accessTokenManager.loadCurrentAccessToken();

        assertEquals(accessToken, accessTokenManager.getCurrentAccessToken());
    }

    @Test
    public void testSaveWritesToCacheIfToken() throws JSONException {
        AccessToken accessToken = createAccessToken();
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        accessTokenManager.setCurrentAccessToken(accessToken);

        verify(accessTokenCache, times(1)).save(any(AccessToken.class));
    }

    @Test
    public void testSetEmptyTokenClearsCache() {
        AccessTokenManager accessTokenManager = createAccessTokenManager();

        accessTokenManager.setCurrentAccessToken(null);

        verify(accessTokenCache, times(1)).clear();
    }

    @Test
    public void testLoadDoesNotSave() {
        AccessToken accessToken = createAccessToken();
        when(accessTokenCache.load()).thenReturn(accessToken);

        AccessTokenManager accessTokenManager = createAccessTokenManager();

        accessTokenManager.loadCurrentAccessToken();

        verify(accessTokenCache, never()).save(any(AccessToken.class));
    }

    private AccessTokenManager createAccessTokenManager() {
        return new AccessTokenManager(localBroadcastManager, accessTokenCache);
    }

    private AccessToken createAccessToken() {
        return createAccessToken(TOKEN_STRING, USER_ID);
    }

    private AccessToken createAccessToken(String tokenString,String userId) {
        return new AccessToken(
                tokenString,
                APP_ID,
                userId,
                PERMISSIONS,
                null,
                AccessTokenSource.WEB_VIEW,
                EXPIRES,
                LAST_REFRESH);
    }
}
