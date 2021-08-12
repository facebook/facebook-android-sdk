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
package com.facebook.appevents.codeless.internal

object Constants {
  const val MAX_TREE_DEPTH = 25
  const val IS_CODELESS_EVENT_KEY = "_is_fb_codeless"
  const val EVENT_MAPPING_PATH_TYPE_KEY = "path_type"
  const val PATH_TYPE_RELATIVE = "relative"
  const val PATH_TYPE_ABSOLUTE = "absolute"
  const val PLATFORM = "android"
  const val APP_INDEXING_SCHEDULE_INTERVAL_MS = 1000
  const val APP_INDEXING_ENABLED = "is_app_indexing_enabled"
  const val DEVICE_SESSION_ID = "device_session_id"
  const val EXTINFO = "extinfo"
  const val APP_INDEXING = "app_indexing"
  const val BUTTON_SAMPLING = "button_sampling"
}
