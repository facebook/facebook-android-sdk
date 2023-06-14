/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
object ProtectedModeManager {
  private var enabled = false

  @JvmStatic
  fun enable() {
    enabled = true
  }

  /** Process parameters for protected mode */
  @JvmStatic
  fun processParametersForProtectedMode(
    parameters: Bundle?
  ) {
    // stub
  }
}
