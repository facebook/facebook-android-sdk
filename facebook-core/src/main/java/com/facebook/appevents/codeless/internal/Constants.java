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

package com.facebook.appevents.codeless.internal;

public class Constants {
    public static final int MAX_TREE_DEPTH = 25;
    public static final String IS_CODELESS_EVENT_KEY = "_is_fb_codeless";

    public static final String EVENT_MAPPING_PATH_TYPE_KEY = "path_type";
    public static final String PATH_TYPE_RELATIVE = "relative";
    public static final String PATH_TYPE_ABSOLUTE = "absolute";

    public static final String PLATFORM = "android";
    public static final int APP_INDEXING_SCHEDULE_INTERVAL_MS = 1000;
    public static final String APP_INDEXING_ENABLED = "is_app_indexing_enabled";
    public static final String DEVICE_SESSION_ID = "device_session_id";
    public static final String EXTINFO = "extinfo";

    public static final String APP_INDEXING = "app_indexing";
    public static final String BUTTON_SAMPLING = "button_sampling";
}
