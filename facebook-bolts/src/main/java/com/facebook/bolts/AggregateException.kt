/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import java.io.PrintStream
import java.io.PrintWriter
import java.util.Collections

/**
 * Aggregates multiple `Throwable`s that may be thrown in the process of a task's execution.
 *
 * @param detailMessage The detail message for this exception.
 * @param innerThrowables The exceptions that are the cause of the current exception.
 *
 * @see Task.whenAll
 */
class AggregateException(detailMessage: String?, innerThrowables: List<Throwable?>?) :
    Exception(
        detailMessage,
        if (innerThrowables != null && innerThrowables.isNotEmpty()) innerThrowables[0] else null) {
  private val innerThrowables: List<Throwable?> =
      Collections.unmodifiableList(innerThrowables ?: emptyList())
  override fun printStackTrace(err: PrintStream) {
    super.printStackTrace(err)
    var currentIndex = -1
    for (throwable in innerThrowables) {
      err.append("\n")
      err.append("  Inner throwable #")
      err.append((++currentIndex).toString())
      err.append(": ")
      throwable?.printStackTrace(err)
      err.append("\n")
    }
  }

  override fun printStackTrace(err: PrintWriter) {
    super.printStackTrace(err)
    var currentIndex = -1
    for (throwable in innerThrowables) {
      err.append("\n")
      err.append("  Inner throwable #")
      err.append((++currentIndex).toString())
      err.append(": ")
      throwable?.printStackTrace(err)
      err.append("\n")
    }
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}
