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

package com.facebook.internal;

import android.os.Parcel;

import com.facebook.FacebookTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UtilityTest extends FacebookTestCase {

    @Test
    public void testStringMapToParcel() {
        // Test null
        assertNull(roundtrip(null));

        HashMap<String, String> map = new HashMap<>();

        // Test empty
        assertEquals(0, roundtrip(map).size());

        // Test regular
        map.put("a", "100");
        map.put("b", null);
        map.put("c", "hello");

        Map<String, String> result = roundtrip(map);
        assertEquals(3, result.size());
        assertEquals(map, result);
        assertEquals("100", result.get("a"));
        assertNull(result.get("b"));
        assertEquals("hello", result.get("c"));
    }

    private Map<String, String> roundtrip(Map<String, String> input) {
        Parcel parcel = Parcel.obtain();
        try {
            Utility.writeStringMapToParcel(parcel, input);
            parcel.setDataPosition(0);
            return Utility.readStringMapFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
