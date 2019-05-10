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

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockSharedPreference implements SharedPreferences {

    private final HashMap<String, Object> preferenceMap;

    private final MockEditor editor;

    public MockSharedPreference () {
        preferenceMap = new HashMap<>();
        editor = new MockEditor(preferenceMap);
    }

    @Override
    public Map<String, ?> getAll() {
        return preferenceMap;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return (String) preferenceMap.getOrDefault(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return (Set<String>) preferenceMap.getOrDefault(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return (int) preferenceMap.getOrDefault(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return (long) preferenceMap.getOrDefault(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return (float) preferenceMap.getOrDefault(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return (boolean) preferenceMap.getOrDefault(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return preferenceMap.containsKey(key);
    }

    @Override
    public Editor edit() {
        return editor;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    public static class MockEditor implements Editor {

        private final Map<String, Object> preferenceMap;

        MockEditor(final Map<String, Object> map) {
            preferenceMap = map;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            preferenceMap.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            preferenceMap.put(key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            preferenceMap.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            preferenceMap.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            preferenceMap.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            preferenceMap.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            preferenceMap.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            preferenceMap.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {

        }
    }
}
