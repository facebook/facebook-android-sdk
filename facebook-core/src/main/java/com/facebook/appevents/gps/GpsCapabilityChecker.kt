/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
 
package com.facebook.appevents.gps

import android.os.Build

object GpsCapabilityChecker {
    @JvmStatic
    fun useOutcomeReceiver(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
