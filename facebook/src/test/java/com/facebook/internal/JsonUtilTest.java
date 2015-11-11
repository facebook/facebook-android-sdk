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

import com.facebook.FacebookTestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


public final class JsonUtilTest extends FacebookTestCase {

    @Test
    public void testJsonObjectClear() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        JsonUtil.jsonObjectClear(jsonObject);
        assertEquals(0, jsonObject.length());
    }

    @Test
    public void testJsonObjectContainsValue() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        assertTrue(JsonUtil.jsonObjectContainsValue(jsonObject, "pocus"));
        assertFalse(JsonUtil.jsonObjectContainsValue(jsonObject, "Fred"));
    }

    @Test
    public void testJsonObjectEntrySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<Entry<String, Object>> entrySet = JsonUtil.jsonObjectEntrySet(jsonObject);
        assertEquals(2, entrySet.size());
    }

    @Test
    public void testJsonObjectKeySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<String> keySet = JsonUtil.jsonObjectKeySet(jsonObject);
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("hello"));
        assertFalse(keySet.contains("world"));
    }

    @Test
    public void testJsonObjectPutAll() throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("hocus", "pocus");

        JSONObject jsonObject = new JSONObject();
        JsonUtil.jsonObjectPutAll(jsonObject, map);

        assertEquals("pocus", jsonObject.get("hocus"));
        assertEquals(2, jsonObject.length());
    }

    @Test
    public void testJsonObjectValues() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Collection<Object> values = JsonUtil.jsonObjectValues(jsonObject);

        assertEquals(2, values.size());
        assertTrue(values.contains("world"));
    }
}
