/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.checkFeature
import com.facebook.internal.instrument.errorreport.ErrorReportHandler.save
import java.util.Random

/** Represents an error condition specific to the Facebook SDK for Android. */
open class FacebookException : RuntimeException {
  /** Constructs a new FacebookException. */
  constructor() : super()

  /**
   * Constructs a new FacebookException.
   *
   * @param message the detail message of this exception
   */
  constructor(message: String?) : super(message) {
    val rand = Random()
    if (message != null && FacebookSdk.isInitialized() && rand.nextInt(100) > 50) {
      checkFeature(FeatureManager.Feature.ErrorReport) { enabled ->
        if (enabled) {
          try {
            save(message)
          } catch (ex: Exception) {
            /*no op*/
          }
        }
      }
    }
  }

  /**
   * Constructs a new FacebookException.
   *
   * @param format the format string (see [java.util.Formatter.format])
   * @param args the list of arguments passed to the formatter.
   */
  constructor(format: String?, vararg args: Any?) : this(format?.format(*args))

  /**
   * Constructs a new FacebookException.
   *
   * @param message the detail message of this exception
   * @param throwable the cause of this exception
   */
  constructor(message: String?, throwable: Throwable?) : super(message, throwable)

  /**
   * Constructs a new FacebookException.
   *
   * @param throwable the cause of this exception
   */
  constructor(throwable: Throwable?) : super(throwable)

  override fun toString(): String {
    // Throwable.toString() returns "FacebookException:{message}". Returning just "{message}"
    // should be fine here.
    return message ?: ""
  }

  companion object {
    const val serialVersionUID: Long = 1
  }
}
