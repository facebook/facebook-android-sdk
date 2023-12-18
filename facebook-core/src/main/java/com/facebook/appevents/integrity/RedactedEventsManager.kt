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
object RedactedEventsManager {
    private var enabled = false

    @JvmStatic
    fun enable() {
        enabled = true
    }
    
    @JvmStatic
    fun processEventsRedaction(eventName: String): String {
        // stub
        return eventName
    }
}
