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

import com.facebook.FacebookException;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 * <p/>
 * This class provides utility methods that are useful in graph API interactions.
 */
public class GraphUtil {
    private static final String[] dateFormats = new String[] {
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
    };

    /**
     * Creates a JSONObject for an open graph action that is suitable for posting.
     * @param type the Open Graph action type for the object, or null if it will be specified later
     * @return a JSONObject
     */
    public static JSONObject createOpenGraphActionForPost(String type) {
        JSONObject action = new JSONObject();
        if (type != null) {
            try {
                action.put("type", type);
            } catch (JSONException e) {
                throw new FacebookException(
                        "An error occurred while setting up the open graph action",
                        e);
            }
        }
        return action;
    }


    /**
     * Creates a JSONObject for an open graph object that is suitable for posting.
     * @param type the Open Graph object type for the object, or null if it will be specified later
     * @return a JSONObject
     */
    public static JSONObject createOpenGraphObjectForPost(String type) {
        return createOpenGraphObjectForPost(type, null, null, null, null, null, null);
    }

    /**
     * Creates a JSONObject for an open graph object that is suitable for posting.
     * @param type the Open Graph object type for the object, or null if it will be specified later
     * @param title the title of the object, or null if it will be specified later
     * @param imageUrl the URL of an image associated with the object, or null
     * @param url the URL associated with the object, or null
     * @param description the description of the object, or null
     * @param objectProperties the properties of the open graph object
     * @param id the id of the object if the post is for update
     * @return a JSONObject
     */
    public static JSONObject createOpenGraphObjectForPost(
            String type,
            String title,
            String imageUrl,
            String url,
            String description,
            JSONObject objectProperties,
            String id) {
        JSONObject openGraphObject = new JSONObject();
        try {
            if (type != null) {
                openGraphObject.put("type", type);
            }
            openGraphObject.put("title", title);

            if (imageUrl != null) {
                JSONObject imageUrlObject = new JSONObject();
                imageUrlObject.put("url", imageUrl);
                JSONArray imageUrls = new JSONArray();
                imageUrls.put(imageUrlObject);
                openGraphObject.put("image", imageUrls);
            }

            openGraphObject.put("url", url);
            openGraphObject.put("description", description);
            openGraphObject.put(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY, true);

            if (objectProperties != null) {
                openGraphObject.put("data", objectProperties);
            }

            if (id != null) {
                openGraphObject.put("id", id);
            }
        } catch (JSONException e) {
            throw new FacebookException("An error occurred while setting up the graph object", e);
        }
        return openGraphObject;
    }

    /**
     * Determines if the open graph object is for posting
     * @param object The open graph object to check
     * @return True if the open graph object was created for posting
     */
    public static boolean isOpenGraphObjectForPost(JSONObject object) {
        return object != null
                ? object.optBoolean(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY) : false;
    }
}
