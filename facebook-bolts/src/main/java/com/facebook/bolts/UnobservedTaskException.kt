/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import java.lang.RuntimeException

/** Used to signify that a Task's error went unobserved. */
class UnobservedTaskException(cause: Throwable?) : RuntimeException(cause)
