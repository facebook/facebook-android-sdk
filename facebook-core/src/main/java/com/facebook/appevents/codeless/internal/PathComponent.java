/*
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

package com.facebook.appevents.codeless.internal;

import org.json.JSONException;
import org.json.JSONObject;

public final class PathComponent {
    public enum MatchBitmaskType {
        ID(1),
        TEXT(1 << 1),
        TAG(1<< 2),
        DESCRIPTION(1 << 3),
        HINT(1 << 4);

        private final int value;

        MatchBitmaskType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final String PATH_CLASS_NAME_KEY = "class_name";
    private static final String PATH_INDEX_KEY = "index";
    private static final String PATH_ID_KEY = "id";
    private static final String PATH_TEXT_KEY = "text";
    private static final String PATH_TAG_KEY = "tag";
    private static final String PATH_DESCRIPTION_KEY = "description";
    private static final String PATH_HINT_KEY = "hint";
    private static final String PATH_MATCH_BITMASK_KEY = "match_bitmask";

    public final String className;
    public final int index;
    public final int id;
    public final String text;
    public final String tag;
    public final String description;
    public final String hint;
    public final int matchBitmask;

    PathComponent(final JSONObject component) throws JSONException {
        className = component.getString(PATH_CLASS_NAME_KEY);
        index = component.optInt(PATH_INDEX_KEY, -1);
        id = component.optInt(PATH_ID_KEY);
        text = component.optString(PATH_TEXT_KEY);
        tag = component.optString(PATH_TAG_KEY);
        description = component.optString(PATH_DESCRIPTION_KEY);
        hint = component.optString(PATH_HINT_KEY);
        matchBitmask = component.optInt(PATH_MATCH_BITMASK_KEY);
    }
}
