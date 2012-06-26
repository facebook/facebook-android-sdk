/**
 * Copyright 2010 Facebook, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

public final class UtilityTests extends AndroidTestCase {

    public void testCamelCaseToLowercase() {
        assertEquals("hello_world", Utility.convertCamelCaseToLowercaseWithUnderscores("HelloWorld"));
        assertEquals("hello_world", Utility.convertCamelCaseToLowercaseWithUnderscores("helloWorld"));
    }

    public void testJsonObjectClear() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Utility.jsonObjectClear(jsonObject);
        assertEquals(0, jsonObject.length());
    }

    public void testJsonObjectContainsValue() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        assertTrue(Utility.jsonObjectContainsValue(jsonObject, "pocus"));
        assertFalse(Utility.jsonObjectContainsValue(jsonObject, "Fred"));
    }

    public void testJsonObjectEntrySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<Entry<String, Object>> entrySet = Utility.jsonObjectEntrySet(jsonObject);
        assertEquals(2, entrySet.size());
    }

    public void testJsonObjectKeySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Set<String> keySet = Utility.jsonObjectKeySet(jsonObject);
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("hello"));
        assertFalse(keySet.contains("world"));
    }

    public void testJsonObjectPutAll() throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("hocus", "pocus");

        JSONObject jsonObject = new JSONObject();
        Utility.jsonObjectPutAll(jsonObject, map);

        assertEquals("pocus", jsonObject.get("hocus"));
        assertEquals(2, jsonObject.length());
    }

    public void testJsonObjectValues() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        Collection<Object> values = Utility.jsonObjectValues(jsonObject);

        assertEquals(2, values.size());
        assertTrue(values.contains("world"));
    }

}
