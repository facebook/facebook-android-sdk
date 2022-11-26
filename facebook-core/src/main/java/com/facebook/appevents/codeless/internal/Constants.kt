/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
