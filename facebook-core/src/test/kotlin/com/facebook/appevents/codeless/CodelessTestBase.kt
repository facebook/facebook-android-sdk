/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.FacebookPowerMockTestCase
import org.junit.Before
import org.robolectric.Robolectric

abstract class CodelessTestBase : FacebookPowerMockTestCase() {
  protected lateinit var root: LinearLayout
  protected lateinit var activity: Activity
  @Before
  override fun setup() {
    super.setup()
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    root = LinearLayout(activity)
    activity.setContentView(root)
    val outerLabel = TextView(activity)
    outerLabel.text = "Outer Label"
    root.addView(outerLabel)
    val inner = LinearLayout(activity)
    root.addView(inner)
    val innerLabel = TextView(activity)
    innerLabel.text = "Inner Label"
    inner.addView(innerLabel)
  }
}
