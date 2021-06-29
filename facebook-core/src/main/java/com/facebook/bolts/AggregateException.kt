/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
