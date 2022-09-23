/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import org.junit.Assert.assertEquals
import org.junit.Test

class FacebookExceptionTest {
  @Test
  fun `test facebook exception`() {
    var exception = FacebookException()
    assertEquals("", exception.toString())

    exception = FacebookException("exception")
    assertEquals("exception", exception.toString())

    exception = FacebookException("%s exception", "test")
    assertEquals("test exception", exception.toString())

    val throwable = Throwable("throwable exception")
    exception = FacebookException(throwable)
    assertEquals(throwable.toString(), exception.toString())
  }
}
