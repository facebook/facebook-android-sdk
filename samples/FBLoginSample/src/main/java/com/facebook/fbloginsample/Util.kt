/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Util {
  fun makePrettyDate(getCreatedTime: String): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.US)
    val pos = ParsePosition(0)
    val then = formatter.parse(getCreatedTime, pos).time
    val now = Date().time
    val seconds = (now - then) / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    var friendly: String?
    val num: Long
    when {
      days > 0 -> {
        num = days
        friendly = "$days day"
      }
      hours > 0 -> {
        num = hours
        friendly = "$hours hour"
      }
      minutes > 0 -> {
        num = minutes
        friendly = "$minutes minute"
      }
      else -> {
        num = seconds
        friendly = "$seconds second"
      }
    }
    if (num > 1) {
      friendly += "s"
    }
    return "$friendly ago"
  }
}
