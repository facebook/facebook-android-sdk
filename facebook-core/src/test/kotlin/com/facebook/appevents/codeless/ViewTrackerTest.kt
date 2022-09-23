/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import com.facebook.appevents.codeless.CodelessMatcher.ViewMatcher.Companion.findViewByPath
import com.facebook.appevents.codeless.internal.EventBinding.Companion.getInstanceFromJson
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ViewTrackerTest : CodelessTestBase() {
  @Test
  fun testFindView() {
    val sample =
        ("{'event_name': 'sample_event'," +
            "'method': 'MANUAL', 'event_type': 'CLICK'," +
            "'app_version': '1.0', 'path_type': 'absolute'," +
            "'path': [" +
            "   {'class_name': 'android.widget.LinearLayout'}," +
            "   {'class_name': 'android.widget.LinearLayout'}," +
            "   {" +
            "       'class_name': 'android.widget.TextView'," +
            "       'text': 'Inner Label'" +
            "   }" +
            "]" +
            "}")
    val json = JSONObject(sample)
    val sampleBinding = getInstanceFromJson(json)
    Assert.assertNotNull(sampleBinding)
    val matched = findViewByPath(sampleBinding, root, sampleBinding.viewPath, 0, -1, "Activity")
    Assert.assertEquals(1, matched.size.toLong())
  }
}
