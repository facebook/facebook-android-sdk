/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import com.facebook.internal.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class AccessTokenTests extends AndroidTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testEmptyToken() {
        List<String> permissions = Utility.arrayList();
        AccessToken token = AccessToken.createEmptyToken(permissions);
        TestUtils.assertSamePermissions(permissions, token);
        assertEquals("", token.getToken());
        assertTrue(token.isInvalid());
        assertTrue(token.getExpires().before(new Date()));
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testEmptyTokenWithPermissions() {
        List<String> permissions = Utility.arrayList("stream_publish");
        AccessToken token = AccessToken.createEmptyToken(permissions);
        TestUtils.assertSamePermissions(permissions, token);
        assertEquals("", token.getToken());
        assertTrue(token.isInvalid());
        assertTrue(token.getExpires().before(new Date()));
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFromDialog() {
        List<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";

        Bundle bundle = new Bundle();
        bundle.putString("access_token", token);
        bundle.putString("expires_in", "60");

        AccessToken accessToken = AccessToken.createFromDialog(permissions, bundle);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.WEB_VIEW, accessToken.getSource());
        assertTrue(!accessToken.isInvalid());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFromSSOWithExpiresString() {
        List<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";

        Intent intent = new Intent();
        intent.putExtra("access_token", token);
        intent.putExtra("expires_in", "60");
        intent.putExtra("extra_extra", "Something unrelated");

        AccessToken accessToken = AccessToken.createFromWebSSO(permissions, intent);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.getSource());
        assertTrue(!accessToken.isInvalid());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFromSSOWithExpiresLong() {
        List<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";

        Intent intent = new Intent();
        intent.putExtra("access_token", token);
        intent.putExtra("expires_in", 60L);
        intent.putExtra("extra_extra", "Something unrelated");

        AccessToken accessToken = AccessToken.createFromWebSSO(permissions, intent);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.getSource());
        assertTrue(!accessToken.isInvalid());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFromNativeLogin() {
        ArrayList<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";

        long nowSeconds = new Date().getTime() / 1000;
        Intent intent = new Intent();
        intent.putExtra(NativeProtocol.EXTRA_ACCESS_TOKEN, token);
        intent.putExtra(NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, nowSeconds + 60L);
        intent.putExtra(NativeProtocol.EXTRA_PERMISSIONS, permissions);

        AccessToken accessToken = AccessToken.createFromNativeLogin(intent);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, accessToken.getSource());
        assertTrue(!accessToken.isInvalid());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCacheRoundtrip() {
        ArrayList<String> permissions = Utility.arrayList("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";
        Date later = TestUtils.nowPlusSeconds(60);
        Date earlier = TestUtils.nowPlusSeconds(-60);

        Bundle bundle = new Bundle();
        TokenCache.putToken(bundle, token);
        TokenCache.putExpirationDate(bundle, later);
        TokenCache.putSource(bundle, AccessTokenSource.FACEBOOK_APPLICATION_WEB);
        TokenCache.putLastRefreshDate(bundle, earlier);
        TokenCache.putPermissions(bundle, permissions);

        AccessToken accessToken = AccessToken.createFromCache(bundle);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.getSource());
        assertTrue(!accessToken.isInvalid());

        Bundle cache = accessToken.toCacheBundle();
        TestUtils.assertEqualContents(bundle, cache);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCachePutGet() {
        Bundle bundle = new Bundle();

        for (String token : new String[] { "", "A completely random token value" }) {
            TokenCache.putToken(bundle, token);
            assertEquals(token, TokenCache.getToken(bundle));
        }

        for (Date date : new Date[] { new Date(42), new Date() }) {
            TokenCache.putExpirationDate(bundle, date);
            assertEquals(date, TokenCache.getExpirationDate(bundle));

            TokenCache.putLastRefreshDate(bundle, date);
            assertEquals(date, TokenCache.getLastRefreshDate(bundle));
        }

        for (long milliseconds : new long[] { 0, -1, System.currentTimeMillis() }) {
            TokenCache.putExpirationMilliseconds(bundle, milliseconds);
            assertEquals(milliseconds, TokenCache.getExpirationMilliseconds(bundle));

            TokenCache.putLastRefreshMilliseconds(bundle, milliseconds);
            assertEquals(milliseconds, TokenCache.getLastRefreshMilliseconds(bundle));
        }

        for (AccessTokenSource source : AccessTokenSource.values()) {
            TokenCache.putSource(bundle, source);
            assertEquals(source, TokenCache.getSource(bundle));
        }

        List<String> normalList = Arrays.asList("", "Another completely random token value");
        List<String> emptyList = Arrays.asList();
        ArrayList<String> normalArrayList = new ArrayList<String>(normalList);
        ArrayList<String> emptyArrayList = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        List<List<String>> permissionLists = Arrays
                .asList(normalList, emptyList, normalArrayList, emptyArrayList);
        for (List<String> list : permissionLists) {
            TokenCache.putPermissions(bundle, list);
            TestUtils.assertSamePermissions(list, TokenCache.getPermissions(bundle));
        }
        normalArrayList.add(null);
    }
    
    @SmallTest
    public void testBasicSerialization() throws IOException {
        AccessToken accessToken = AccessToken.createFromString("a token",
                Arrays.asList("permission_1", "permission_2"), AccessTokenSource.WEB_VIEW);
        AccessToken res = TestUtils.serializeAndUnserialize(accessToken);
        
        // if one field got serialized most likely all other non transient fields
        // got serialized correctly.
        assertEquals(accessToken.getPermissions(), res.getPermissions());
        assertEquals(accessToken.getToken(), res.getToken());
        assertEquals(accessToken.getSource(), res.getSource());
    }
}
