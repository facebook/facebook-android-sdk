/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.logging.dumpsys

import java.io.PrintWriter

/** This interface is used by Facebook internal end-to-end tests. */
fun interface EndToEndDumper {
  /** Try to dump. Return whether it succeeds. */
  fun maybeDump(prefix: String, writer: PrintWriter, args: Array<String>?): Boolean

  companion object {
    var instance: EndToEndDumper? = null
  }
}
