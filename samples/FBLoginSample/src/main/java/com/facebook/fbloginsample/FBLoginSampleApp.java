/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample;

import android.app.Application;
import com.facebook.drawee.backends.pipeline.Fresco;

public class FBLoginSampleApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    Fresco.initialize(this);
  }
}
