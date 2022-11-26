/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlinsampleapp

import android.app.Application
import com.facebook.appevents.AppEventsLogger

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    AppEventsLogger.activateApp(this)
  }
}
