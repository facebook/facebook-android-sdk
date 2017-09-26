// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.share.internal;

import com.facebook.share.model.CameraEffectArguments;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 *
 * Utility methods for JSON representation of Open Graph models.
 */
public class CameraEffectJSONUtility {
    private static final Map<Class<?>, CameraEffectJSONUtility.Setter> SETTERS = new HashMap<>();

    static {
        SETTERS.put(String.class, new CameraEffectJSONUtility.Setter() {
            @Override
            public void setOnArgumentsBuilder(
                    CameraEffectArguments.Builder builder,
                    String key,
                    Object value)
                    throws JSONException {
                builder.putArgument(key, (String) value);
            }

            @Override
            public void setOnJSON(JSONObject json, String key, Object value) throws JSONException {
                json.put(key, value);
            }
        });
        SETTERS.put(String[].class, new CameraEffectJSONUtility.Setter() {
            @Override
            public void setOnArgumentsBuilder(
                    CameraEffectArguments.Builder builder,
                    String key,
                    Object value)
                    throws JSONException {
                throw new IllegalArgumentException("Unexpected type from JSON");
            }

            @Override
            public void setOnJSON(JSONObject json, String key, Object value) throws JSONException {
                JSONArray jsonArray = new JSONArray();
                for (String stringValue : (String[]) value) {
                    jsonArray.put(stringValue);
                }
                json.put(key, jsonArray);
            }
        });

        SETTERS.put(JSONArray.class, new CameraEffectJSONUtility.Setter() {
            @Override
            public void setOnArgumentsBuilder(
                    CameraEffectArguments.Builder builder,
                    String key,
                    Object value)
                    throws JSONException {
                // Only strings are supported for now
                JSONArray jsonArray = (JSONArray) value;
                String[] argsArray = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object current = jsonArray.get(i);
                    if (current instanceof String) {
                        argsArray[i] = (String) current;
                    } else {
                        throw new IllegalArgumentException(
                                "Unexpected type in an array: " + current.getClass());
                    }
                }
                builder.putArgument(key, argsArray);
            }

            @Override
            public void setOnJSON(JSONObject json, String key, Object value) throws JSONException {
                throw new IllegalArgumentException("JSONArray's are not supported in bundles.");
            }
        });
    }

    public interface Setter {
        void setOnArgumentsBuilder(CameraEffectArguments.Builder builder, String key, Object value)
                throws JSONException;
        void setOnJSON(JSONObject json, String key, Object value)
                throws JSONException;
    }

    public static JSONObject convertToJSON(CameraEffectArguments arguments)
            throws JSONException {
        if (arguments == null) {
            return null;
        }

        JSONObject json = new JSONObject();

        for (String key : arguments.keySet()) {
            Object value = arguments.get(key);
            if (value == null) {
                // Null is not supported.
                continue;
            }

            CameraEffectJSONUtility.Setter setter = SETTERS.get(value.getClass());
            if (setter == null) {
                throw new IllegalArgumentException("Unsupported type: " + value.getClass());
            }
            setter.setOnJSON(json, key, value);
        }

        return json;
    }

    public static CameraEffectArguments convertToCameraEffectArguments(JSONObject jsonObject)
            throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        CameraEffectArguments.Builder builder = new CameraEffectArguments.Builder();
        Iterator<String> jsonIterator = jsonObject.keys();
        while (jsonIterator.hasNext()) {
            String key = jsonIterator.next();
            Object value = jsonObject.get(key);
            if (value == null || value == JSONObject.NULL) {
                // Null is not supported.
                continue;
            }

            CameraEffectJSONUtility.Setter setter = SETTERS.get(value.getClass());
            if (setter == null) {
                throw new IllegalArgumentException("Unsupported type: " + value.getClass());
            }
            setter.setOnArgumentsBuilder(builder, key, value);
        }

        return builder.build();
    }
}
