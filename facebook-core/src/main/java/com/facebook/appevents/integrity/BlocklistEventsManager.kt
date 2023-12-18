/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
object BlocklistEventsManager {
    private var enabled = false

    @JvmStatic
    fun enable() {
        enabled = true
    }

    /** check if the event is in the blocklist */
    @JvmStatic
    fun isInBlocklist(
            eventName: String
    ): Boolean {
        // stub
        return false
    }
}
