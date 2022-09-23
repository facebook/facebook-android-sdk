/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.internal

import android.os.Build
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object DateFormatter {

  internal fun format(isoDate: String?): ZonedDateTime? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      ZonedDateTime.parse(isoDate, formatter)
    } else {
      return null
    }
  }
}
