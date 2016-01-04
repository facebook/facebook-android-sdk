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

import android.annotation.SuppressLint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
class JsonUtil {
    static void jsonObjectClear(JSONObject jsonObject) {
        @SuppressWarnings("unchecked")
        Iterator<String> keys = (Iterator<String>) jsonObject.keys();
        while (keys.hasNext()) {
            keys.next();
            keys.remove();
        }
    }

    static boolean jsonObjectContainsValue(JSONObject jsonObject, Object value) {
        @SuppressWarnings("unchecked")
        Iterator<String> keys = (Iterator<String>) jsonObject.keys();
        while (keys.hasNext()) {
            Object thisValue = jsonObject.opt(keys.next());
            if (thisValue != null && thisValue.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private final static class JSONObjectEntry implements Map.Entry<String, Object> {
        private final String key;
        private final Object value;

        JSONObjectEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @SuppressLint("FieldGetter")
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object setValue(Object object) {
            throw new UnsupportedOperationException("JSONObjectEntry is immutable");
        }

    }

    static Set<Map.Entry<String, Object>> jsonObjectEntrySet(JSONObject jsonObject) {
        HashSet<Map.Entry<String, Object>> result = new HashSet<Map.Entry<String, Object>>();

        @SuppressWarnings("unchecked")
        Iterator<String> keys = (Iterator<String>) jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.opt(key);
            result.add(new JSONObjectEntry(key, value));
        }

        return result;
    }

    static Set<String> jsonObjectKeySet(JSONObject jsonObject) {
        HashSet<String> result = new HashSet<String>();

        @SuppressWarnings("unchecked")
        Iterator<String> keys = (Iterator<String>) jsonObject.keys();
        while (keys.hasNext()) {
            result.add(keys.next());
        }

        return result;
    }

    static void jsonObjectPutAll(JSONObject jsonObject, Map<String, Object> map) {
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            try {
                jsonObject.putOpt(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    static Collection<Object> jsonObjectValues(JSONObject jsonObject) {
        ArrayList<Object> result = new ArrayList<Object>();

        @SuppressWarnings("unchecked")
        Iterator<String> keys = (Iterator<String>) jsonObject.keys();
        while (keys.hasNext()) {
            result.add(jsonObject.opt(keys.next()));
        }

        return result;
    }
}
