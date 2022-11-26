/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

internal enum class FlushReason {
  EXPLICIT,
  TIMER,
  SESSION_CHANGE,
  PERSISTED_EVENTS,
  EVENT_THRESHOLD,
  EAGER_FLUSHING_EVENT
}
