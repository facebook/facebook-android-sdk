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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@PrepareForTest( {
        AccessTokenCache.class,
        FacebookSdk.class,
        LegacyTokenHelper.class,
        Utility.class})
public class AccessTokenCacheTest extends FacebookPowerMockTestCase {

    private final String TOKEN_STRING = "A token of my esteem";
    private final String USER_ID = "1000";
    private final List<String> PERMISSIONS = Arrays.asList("walk", "chew gum");
    private final Date EXPIRES = new Date(2025, 5, 3);
    private final Date LAST_REFRESH = new Date(2023, 8, 15);
    private final String APP_ID = "1234";

    private SharedPreferences sharedPreferences;
    @Mock private LegacyTokenHelper cachingStrategy;
    private AccessTokenCache.SharedPreferencesTokenCachingStrategyFactory
            cachingStrategyFactory;

    @Before
    public void before() throws Exception {
        mockStatic(FacebookSdk.class);
        sharedPreferences = Robolectric.application.getSharedPreferences(
                AccessTokenManager.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        cachingStrategyFactory = mock(
                AccessTokenCache.SharedPreferencesTokenCachingStrategyFactory.class);
        when(cachingStrategyFactory.create()).thenReturn(cachingStrategy);
        stub(PowerMockito.method(Utility.class, "awaitGetGraphMeRequestWithCache")).toReturn(
                new JSONObject().put("id", "1000"));
    }


    @Test
    public void testLoadReturnsFalseIfNoCachedToken() {
        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken accessToken = cache.load();

        assertNull(accessToken);
        PowerMockito.verifyZeroInteractions(cachingStrategy);
    }

    @Test
    public void testLoadReturnsFalseIfNoCachedOrLegacyToken() {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken accessToken = cache.load();

        assertNull(accessToken);
    }

    @Test
    public void testLoadReturnsFalseIfEmptyCachedTokenAndDoesNotCheckLegacy() {

        JSONObject jsonObject = new JSONObject();
        sharedPreferences.edit().putString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY,
                jsonObject.toString()).commit();

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken accessToken = cache.load();

        assertNull(accessToken);
        verifyZeroInteractions(cachingStrategy);
    }

    @Test
    public void testLoadReturnsFalseIfNoCachedTokenAndEmptyLegacyToken() {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        when(cachingStrategy.load()).thenReturn(new Bundle());

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken accessToken = cache.load();

        assertNull(accessToken);
    }

    @Test
    public void testLoadValidCachedToken() throws JSONException {
        AccessToken accessToken = createAccessToken();
        JSONObject jsonObject = accessToken.toJSONObject();
        sharedPreferences.edit().putString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY,
                jsonObject.toString()).commit();

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken loadedAccessToken = cache.load();

        assertNotNull(loadedAccessToken);
        assertEquals(accessToken, loadedAccessToken);
    }

    @Test
    public void testLoadSetsCurrentTokenIfNoCachedTokenButValidLegacyToken() {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        AccessToken accessToken = createAccessToken();
        when(cachingStrategy.load()).thenReturn(
                AccessTokenTestHelper.toLegacyCacheBundle(accessToken));

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        AccessToken loadedAccessToken = cache.load();

        assertNotNull(loadedAccessToken);
        assertEquals(accessToken, loadedAccessToken);
    }

    @Test
    public void testLoadSavesTokenWhenUpgradingFromLegacyToken() throws JSONException {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        AccessToken accessToken = createAccessToken();
        when(cachingStrategy.load()).thenReturn(
                AccessTokenTestHelper.toLegacyCacheBundle(accessToken));

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);
        cache.load();

        assertTrue(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY));

        AccessToken savedAccessToken = AccessToken.createFromJSONObject(
                new JSONObject(sharedPreferences.getString(
                        AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, null)));
        assertEquals(accessToken, savedAccessToken);
    }

    @Test
    public void testLoadClearsLegacyCacheWhenUpgradingFromLegacyToken() {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        AccessToken accessToken = createAccessToken();
        when(cachingStrategy.load()).thenReturn(
                AccessTokenTestHelper.toLegacyCacheBundle(accessToken));

        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);
        cache.load();

        verify(cachingStrategy, times(1)).clear();
    }

    @Test
    public void testSaveRequiresToken() {
        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        try {
            cache.save(null);
            fail();
        } catch (NullPointerException exception) {
        }
    }

    @Test
    public void testSaveWritesToCacheIfToken() throws JSONException {
        AccessToken accessToken = createAccessToken();
        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        cache.save(accessToken);

        verify(cachingStrategy, never()).save(any(Bundle.class));
        assertTrue(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY));

        AccessToken savedAccessToken = AccessToken.createFromJSONObject(
                new JSONObject(sharedPreferences.getString(
                        AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, null)));
        assertEquals(accessToken, savedAccessToken);
    }

    @Test
    public void testClearCacheClearsCache() {
        AccessToken accessToken = createAccessToken();
        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        cache.save(accessToken);

        cache.clear();

        assertFalse(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY));
        verify(cachingStrategy, never()).clear();
    }

    @Test
    public void testClearCacheClearsLegacyCache() {
        when(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true);

        AccessToken accessToken = createAccessToken();
        AccessTokenCache cache = new AccessTokenCache(sharedPreferences, cachingStrategyFactory);

        cache.save(accessToken);

        cache.clear();

        assertFalse(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY));
        verify(cachingStrategy, times(1)).clear();
    }

    private AccessToken createAccessToken() {
        return createAccessToken(TOKEN_STRING, USER_ID);
    }

    private AccessToken createAccessToken(String tokenString, String userId) {
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
