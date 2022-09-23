/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
interface DialogFeature {
  /** This method is for internal use only. */
  fun getAction(): String

  /** This method is for internal use only. */
  fun getMinVersion(): Int

  /**
   * This method is for internal use only.
   *
   * For all Enums that implement this interface, the name() method (in Java) or the name property
   * (in Kotlin) is already present. It returns the String representation of the Enum value,
   * verbatim.
   */
  @Suppress("INAPPLICABLE_JVM_NAME") @get:JvmName("name") val name: String
}
