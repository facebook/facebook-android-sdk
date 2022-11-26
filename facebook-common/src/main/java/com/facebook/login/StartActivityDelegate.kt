/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.app.Activity
import android.content.Intent

internal interface StartActivityDelegate {
  fun startActivityForResult(intent: Intent, requestCode: Int)
  val activityContext: Activity?
}
