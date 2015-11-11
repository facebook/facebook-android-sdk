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

import android.os.Bundle;

import com.facebook.FacebookTestCase;
import com.facebook.TestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BundleJSONConverterTest extends FacebookTestCase {

    @Test
    public void testSimpleValues() throws JSONException {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("1st");
        arrayList.add("2nd");
        arrayList.add("third");

        Bundle innerBundle1 = new Bundle();
        innerBundle1.putInt("inner", 1);

        Bundle innerBundle2 = new Bundle();
        innerBundle2.putString("inner", "2");
        innerBundle2.putStringArray("deep list", new String[] {"7", "8"});

        innerBundle1.putBundle("nested bundle", innerBundle2);


        Bundle b = new Bundle();
        b.putBoolean("boolValue", true);
        b.putInt("intValue", 7);
        b.putLong("longValue", 5000000000l);
        b.putDouble("doubleValue", 3.14);
        b.putString("stringValue", "hello world");
        b.putStringArray("stringArrayValue", new String[] {"first", "second"});
        b.putStringArrayList("stringArrayListValue", arrayList);
        b.putBundle("nested", innerBundle1);


        JSONObject json = BundleJSONConverter.convertToJSON(b);
        assertNotNull(json);

        assertEquals(true, json.getBoolean("boolValue"));
        assertEquals(7, json.getInt("intValue"));
        assertEquals(5000000000l, json.getLong("longValue"));
        assertEquals(3.14, json.getDouble("doubleValue"), TestUtils.DOUBLE_EQUALS_DELTA);
        assertEquals("hello world", json.getString("stringValue"));

        JSONArray jsonArray = json.getJSONArray("stringArrayValue");
        assertEquals(2, jsonArray.length());
        assertEquals("first", jsonArray.getString(0));
        assertEquals("second", jsonArray.getString(1));

        jsonArray = json.getJSONArray("stringArrayListValue");
        assertEquals(3, jsonArray.length());
        assertEquals("1st", jsonArray.getString(0));
        assertEquals("2nd", jsonArray.getString(1));
        assertEquals("third", jsonArray.getString(2));

        JSONObject innerJson = json.getJSONObject("nested");
        assertEquals(1, innerJson.getInt("inner"));
        innerJson = innerJson.getJSONObject("nested bundle");
        assertEquals("2", innerJson.getString("inner"));

        jsonArray = innerJson.getJSONArray("deep list");
        assertEquals(2, jsonArray.length());
        assertEquals("7", jsonArray.getString(0));
        assertEquals("8", jsonArray.getString(1));

        Bundle finalBundle = BundleJSONConverter.convertToBundle(json);
        assertNotNull(finalBundle);

        assertEquals(true, finalBundle.getBoolean("boolValue"));
        assertEquals(7, finalBundle.getInt("intValue"));
        assertEquals(5000000000l, finalBundle.getLong("longValue"));
        assertEquals(3.14, finalBundle.getDouble("doubleValue"), TestUtils.DOUBLE_EQUALS_DELTA);
        assertEquals("hello world", finalBundle.getString("stringValue"));

        List<String> stringList = finalBundle.getStringArrayList("stringArrayValue");
        assertEquals(2, stringList.size());
        assertEquals("first", stringList.get(0));
        assertEquals("second", stringList.get(1));

        stringList = finalBundle.getStringArrayList("stringArrayListValue");
        assertEquals(3, stringList.size());
        assertEquals("1st", stringList.get(0));
        assertEquals("2nd", stringList.get(1));
        assertEquals("third", stringList.get(2));

        Bundle finalInnerBundle = finalBundle.getBundle("nested");
        assertEquals(1, finalInnerBundle.getInt("inner"));
        finalBundle = finalInnerBundle.getBundle("nested bundle");
        assertEquals("2", finalBundle.getString("inner"));

        stringList = finalBundle.getStringArrayList("deep list");
        assertEquals(2, stringList.size());
        assertEquals("7", stringList.get(0));
        assertEquals("8", stringList.get(1));
    }

    @Test
    public void testUnsupportedValues() throws JSONException {
        Bundle b = new Bundle();
        b.putShort("shortValue", (short)7);

        boolean exceptionCaught = false;
        try {
            BundleJSONConverter.convertToJSON(b);
        } catch (IllegalArgumentException a) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(10);
        JSONObject json = new JSONObject();
        json.put("arrayValue", jsonArray);

        exceptionCaught = false;
        try {
            BundleJSONConverter.convertToBundle(json);
        } catch (IllegalArgumentException a) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }
}
