/**
 * Copyright 2010-present Facebook.
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


package com.facebook.internal;

import android.os.Bundle;
import android.test.suitebuilder.annotation.SmallTest;
import com.facebook.FacebookTestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BundleJSONConverterTests extends FacebookTestCase {

    @SmallTest
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
        assertEquals(3.14, json.getDouble("doubleValue"));
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
        assertEquals(3.14, finalBundle.getDouble("doubleValue"));
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

    @SmallTest
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
