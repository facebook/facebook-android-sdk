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

package com.facebook.share.internal;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.facebook.internal.Validate;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 *
 * Utility methods for JSON representation of Open Graph models.
 */
public final class OpenGraphJSONUtility {
    /**
     * Converts an action to a JSONObject.
     *
     * NOTE: All images are removed from the JSON representation and must be added to the builder
     * separately.
     *
     * @param action {@link com.facebook.share.model.ShareOpenGraphAction} to be converted.
     * @return {@link org.json.JSONObject} representing the action.
     * @throws JSONException
     */
    public static JSONObject toJSONObject(
            final ShareOpenGraphAction action,
            final PhotoJSONProcessor photoJSONProcessor) throws JSONException {
        final JSONObject result = new JSONObject();
        final Set<String> keys = action.keySet();
        for (String key : keys) {
            result.put(key, toJSONValue(action.get(key), photoJSONProcessor));
        }
        return result;
    }

    private static JSONObject toJSONObject(
            final ShareOpenGraphObject object,
            final PhotoJSONProcessor photoJSONProcessor) throws JSONException {
        final JSONObject result = new JSONObject();
        final Set<String> keys = object.keySet();
        for (String key : keys) {
            result.put(key, toJSONValue(object.get(key), photoJSONProcessor));
        }
        return result;
    }

    private static JSONArray toJSONArray(
            final List list,
            final PhotoJSONProcessor photoJSONProcessor) throws JSONException {
        final JSONArray result = new JSONArray();
        for (Object item : list) {
            result.put(toJSONValue(item, photoJSONProcessor));
        }
        return result;
    }

    public static Object toJSONValue(
            @Nullable final Object object,
            final PhotoJSONProcessor photoJSONProcessor) throws JSONException {
        if (object == null) {
            return JSONObject.NULL;
        }
        if ((object instanceof String) ||
                (object instanceof Boolean) ||
                (object instanceof Double) ||
                (object instanceof Float) ||
                (object instanceof Integer) ||
                (object instanceof Long)) {
            return object;
        }
        if (object instanceof SharePhoto) {
            if (photoJSONProcessor != null) {
                return photoJSONProcessor.toJSONObject((SharePhoto) object);
            }
            return null;
        }
        if (object instanceof ShareOpenGraphObject) {
            return toJSONObject((ShareOpenGraphObject) object, photoJSONProcessor);
        }
        if (object instanceof List) {
            return toJSONArray((List) object, photoJSONProcessor);
        }
        throw new IllegalArgumentException(
                "Invalid object found for JSON serialization: " +object.toString());
    }

    private OpenGraphJSONUtility() {}

    public interface PhotoJSONProcessor {
        public JSONObject toJSONObject(SharePhoto photo);
    }
}
