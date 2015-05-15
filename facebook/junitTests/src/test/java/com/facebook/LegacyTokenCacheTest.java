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

import android.os.Bundle;

import com.facebook.internal.Utility;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@PrepareForTest( {Utility.class})
public final class LegacyTokenCacheTest extends FacebookPowerMockTestCase {

    private static final String BOOLEAN_KEY = "booleanKey";
    private static final String BOOLEAN_ARRAY_KEY = "booleanArrayKey";
    private static final String BYTE_KEY = "byteKey";
    private static final String BYTE_ARRAY_KEY = "byteArrayKey";
    private static final String SHORT_KEY = "shortKey";
    private static final String SHORT_ARRAY_KEY = "shortArrayKey";
    private static final String INT_KEY = "intKey";
    private static final String INT_ARRAY_KEY = "intArrayKey";
    private static final String LONG_KEY = "longKey";
    private static final String LONG_ARRAY_KEY = "longArrayKey";
    private static final String FLOAT_ARRAY_KEY = "floatKey";
    private static final String FLOAT_KEY = "floatArrayKey";
    private static final String DOUBLE_KEY = "doubleKey";
    private static final String DOUBLE_ARRAY_KEY = "doubleArrayKey";
    private static final String CHAR_KEY = "charKey";
    private static final String CHAR_ARRAY_KEY = "charArrayKey";
    private static final String STRING_KEY = "stringKey";
    private static final String STRING_LIST_KEY = "stringListKey";
    private static final String SERIALIZABLE_KEY = "serializableKey";

    private static Random random = new Random((new Date()).getTime());

    @Override
    public void setUp() {
        super.setUp();

        FacebookSdk.sdkInitialize(Robolectric.application);
    }

    @Before
    public void before() throws Exception {
        stub(PowerMockito.method(Utility.class, "awaitGetGraphMeRequestWithCache")).toReturn(
                new JSONObject().put("id", "1000"));
    }

    @Test
    public void testAllTypes() {
        Bundle originalBundle = new Bundle();

        putBoolean(BOOLEAN_KEY, originalBundle);
        putBooleanArray(BOOLEAN_ARRAY_KEY, originalBundle);
        putByte(BYTE_KEY, originalBundle);
        putByteArray(BYTE_ARRAY_KEY, originalBundle);
        putShort(SHORT_KEY, originalBundle);
        putShortArray(SHORT_ARRAY_KEY, originalBundle);
        putInt(INT_KEY, originalBundle);
        putIntArray(INT_ARRAY_KEY, originalBundle);
        putLong(LONG_KEY, originalBundle);
        putLongArray(LONG_ARRAY_KEY, originalBundle);
        putFloat(FLOAT_KEY, originalBundle);
        putFloatArray(FLOAT_ARRAY_KEY, originalBundle);
        putDouble(DOUBLE_KEY, originalBundle);
        putDoubleArray(DOUBLE_ARRAY_KEY, originalBundle);
        putChar(CHAR_KEY, originalBundle);
        putCharArray(CHAR_ARRAY_KEY, originalBundle);
        putString(STRING_KEY, originalBundle);
        putStringList(STRING_LIST_KEY, originalBundle);
        originalBundle.putSerializable(SERIALIZABLE_KEY, AccessTokenSource.FACEBOOK_APPLICATION_WEB);

        ensureApplicationContext();

        LegacyTokenHelper cache = new LegacyTokenHelper(Robolectric.application);
        cache.save(originalBundle);

        LegacyTokenHelper cache2 = new LegacyTokenHelper(Robolectric.application);
        Bundle cachedBundle = cache2.load();

        assertEquals(originalBundle.getBoolean(BOOLEAN_KEY), cachedBundle.getBoolean(BOOLEAN_KEY));
        assertArrayEquals(originalBundle.getBooleanArray(BOOLEAN_ARRAY_KEY), cachedBundle.getBooleanArray(BOOLEAN_ARRAY_KEY));
        assertEquals(originalBundle.getByte(BYTE_KEY), cachedBundle.getByte(BYTE_KEY));
        assertArrayEquals(originalBundle.getByteArray(BYTE_ARRAY_KEY), cachedBundle.getByteArray(BYTE_ARRAY_KEY));
        assertEquals(originalBundle.getShort(SHORT_KEY), cachedBundle.getShort(SHORT_KEY));
        assertArrayEquals(originalBundle.getShortArray(SHORT_ARRAY_KEY), cachedBundle.getShortArray(SHORT_ARRAY_KEY));
        assertEquals(originalBundle.getInt(INT_KEY), cachedBundle.getInt(INT_KEY));
        assertArrayEquals(originalBundle.getIntArray(INT_ARRAY_KEY), cachedBundle.getIntArray(INT_ARRAY_KEY));
        assertEquals(originalBundle.getLong(LONG_KEY), cachedBundle.getLong(LONG_KEY));
        assertArrayEquals(originalBundle.getLongArray(LONG_ARRAY_KEY), cachedBundle.getLongArray(LONG_ARRAY_KEY));
        assertEquals(originalBundle.getFloat(FLOAT_KEY), cachedBundle.getFloat(FLOAT_KEY), TestUtils.DOUBLE_EQUALS_DELTA);
        assertArrayEquals(originalBundle.getFloatArray(FLOAT_ARRAY_KEY), cachedBundle.getFloatArray(FLOAT_ARRAY_KEY));
        assertEquals(originalBundle.getDouble(DOUBLE_KEY), cachedBundle.getDouble(DOUBLE_KEY), TestUtils.DOUBLE_EQUALS_DELTA);
        assertArrayEquals(originalBundle.getDoubleArray(DOUBLE_ARRAY_KEY), cachedBundle.getDoubleArray(DOUBLE_ARRAY_KEY));
        assertEquals(originalBundle.getChar(CHAR_KEY), cachedBundle.getChar(CHAR_KEY));
        assertArrayEquals(originalBundle.getCharArray(CHAR_ARRAY_KEY), cachedBundle.getCharArray(CHAR_ARRAY_KEY));
        assertEquals(originalBundle.getString(STRING_KEY), cachedBundle.getString(STRING_KEY));
        assertListEquals(originalBundle.getStringArrayList(STRING_LIST_KEY), cachedBundle.getStringArrayList(
                STRING_LIST_KEY));
        assertEquals(originalBundle.getSerializable(SERIALIZABLE_KEY),
                cachedBundle.getSerializable(SERIALIZABLE_KEY));
    }

    @Test
    public void testMultipleCaches() {
        Bundle bundle1 = new Bundle(), bundle2 = new Bundle();

        bundle1.putInt(INT_KEY, 10);
        bundle1.putString(STRING_KEY, "ABC");
        bundle2.putInt(INT_KEY, 100);
        bundle2.putString(STRING_KEY, "xyz");

        ensureApplicationContext();

        LegacyTokenHelper cache1 = new LegacyTokenHelper(Robolectric.application);
        LegacyTokenHelper cache2 = new LegacyTokenHelper(Robolectric.application, "CustomCache");

        cache1.save(bundle1);
        cache2.save(bundle2);

        // Get new references to make sure we are getting persisted data.
        // Reverse the cache references for fun.
        cache1 = new LegacyTokenHelper(Robolectric.application, "CustomCache");
        cache2 = new LegacyTokenHelper(Robolectric.application);

        Bundle newBundle1 = cache1.load(), newBundle2 = cache2.load();

        assertEquals(bundle2.getInt(INT_KEY), newBundle1.getInt(INT_KEY));
        assertEquals(bundle2.getString(STRING_KEY), newBundle1.getString(STRING_KEY));
        assertEquals(bundle1.getInt(INT_KEY), newBundle2.getInt(INT_KEY));
        assertEquals(bundle1.getString(STRING_KEY), newBundle2.getString(STRING_KEY));
    }

    @Test
    public void testCacheRoundtrip() {
        Set<String> permissions = Utility.hashSet("stream_publish", "go_outside_and_play");
        String token = "AnImaginaryTokenValue";
        Date later = TestUtils.nowPlusSeconds(60);
        Date earlier = TestUtils.nowPlusSeconds(-60);
        String applicationId = "1234";

        LegacyTokenHelper cache =
                new LegacyTokenHelper(Robolectric.application);
        cache.clear();

        Bundle bundle = new Bundle();
        LegacyTokenHelper.putToken(bundle, token);
        LegacyTokenHelper.putExpirationDate(bundle, later);
        LegacyTokenHelper.putSource(
                bundle,
                AccessTokenSource.FACEBOOK_APPLICATION_NATIVE);
        LegacyTokenHelper.putLastRefreshDate(bundle, earlier);
        LegacyTokenHelper.putPermissions(bundle, permissions);
        LegacyTokenHelper.putDeclinedPermissions(
                bundle,
                Utility.arrayList("whatever"));
        LegacyTokenHelper.putApplicationId(bundle, applicationId);

        cache.save(bundle);
        bundle = cache.load();

        AccessToken accessToken = AccessToken.createFromLegacyCache(bundle);
        TestUtils.assertSamePermissions(permissions, accessToken);
        assertEquals(token, accessToken.getToken());
        assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, accessToken.getSource());
        assertTrue(!accessToken.isExpired());

        Bundle cachedBundle = AccessTokenTestHelper.toLegacyCacheBundle(accessToken);
        TestUtils.assertEqualContents(bundle, cachedBundle);
    }

    private static void assertArrayEquals(Object a1, Object a2) {
        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals(a1.getClass(), a2.getClass());
        assertTrue("Not an array", a1.getClass().isArray());

        int length = Array.getLength(a1);
        assertEquals(length, Array.getLength(a2));
        for (int i = 0; i < length; i++) {
            Object a1Value = Array.get(a1, i);
            Object a2Value = Array.get(a2, i);

            assertEquals(a1Value, a2Value);
        }
    }

    private static void assertListEquals(List<?> l1, List<?> l2) {
        assertNotNull(l1);
        assertNotNull(l2);

        Iterator<?> i1 = l1.iterator(), i2 = l2.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            assertEquals(i1.next(), i2.next());
        }

        assertTrue("Lists not of the same length", !i1.hasNext());
        assertTrue("Lists not of the same length", !i2.hasNext());
    }

    private static void putInt(String key, Bundle bundle) {
        bundle.putInt(key, random.nextInt());
    }

    private static void putIntArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextInt();
        }
        bundle.putIntArray(key, array);
    }

    private static void putShort(String key, Bundle bundle) {
        bundle.putShort(key, (short)random.nextInt());
    }

    private static void putShortArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        short[] array = new short[length];
        for (int i = 0; i < length; i++) {
            array[i] = (short)random.nextInt();
        }
        bundle.putShortArray(key, array);
    }

    private static void putByte(String key, Bundle bundle) {
        bundle.putByte(key, (byte)random.nextInt());
    }

    private static void putByteArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        byte[] array = new byte[length];
        random.nextBytes(array);
        bundle.putByteArray(key, array);
    }

    private static void putBoolean(String key, Bundle bundle) {
        bundle.putBoolean(key, random.nextBoolean());
    }

    private static void putBooleanArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        boolean[] array = new boolean[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextBoolean();
        }
        bundle.putBooleanArray(key, array);
    }

    private static void putLong(String key, Bundle bundle) {
        bundle.putLong(key, random.nextLong());
    }

    private static void putLongArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextLong();
        }
        bundle.putLongArray(key, array);
    }

    private static void putFloat(String key, Bundle bundle) {
        bundle.putFloat(key, random.nextFloat());
    }

    private static void putFloatArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        float[] array = new float[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextFloat();
        }
        bundle.putFloatArray(key, array);
    }

    private static void putDouble(String key, Bundle bundle) {
        bundle.putDouble(key, random.nextDouble());
    }

    private static void putDoubleArray(String key, Bundle bundle) {
        int length = random.nextInt(50);
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextDouble();
        }
        bundle.putDoubleArray(key, array);
    }

    private static void putChar(String key, Bundle bundle) {
        bundle.putChar(key, getChar());
    }

    private static void putCharArray(String key, Bundle bundle) {
        bundle.putCharArray(key, getCharArray());
    }

    private static void putString(String key, Bundle bundle) {
        bundle.putString(key, new String(getCharArray()));
    }

    private static void putStringList(String key, Bundle bundle) {
        int length = random.nextInt(50);
        ArrayList<String> stringList = new ArrayList<String>(length);
        while (0 < length--) {
            if (length == 0) {
                stringList.add(null);
            } else {
                stringList.add(new String(getCharArray()));
            }
        }

        bundle.putStringArrayList(key, stringList);
    }

    private static char[] getCharArray() {
        int length = random.nextInt(50);
        char[] array = new char[length];
        for (int i = 0; i < length; i++) {
            array[i] = getChar();
        }

        return array;
    }

    private static char getChar() {
        return (char)random.nextInt(255);
    }

    private void ensureApplicationContext() {
        // Since the test case is not running on the UI thread, the applicationContext might
        // not be ready (i.e. it might be null). Wait for a bit to resolve this.
        long waitedFor = 0;
        try {
            // Don't hold up execution for too long.
            while (Robolectric.application.getApplicationContext() == null && waitedFor <= 2000) {
                Thread.sleep(50);
                waitedFor += 50;
            }
        }
        catch (InterruptedException e) {
        }
    }

}
