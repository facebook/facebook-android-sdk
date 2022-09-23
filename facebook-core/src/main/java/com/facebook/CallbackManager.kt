/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Intent
import com.facebook.internal.CallbackManagerImpl

/**
 * The CallbackManager manages the callbacks into the FacebookSdk from an Activity's or Fragment's
 * onActivityResult() method.
 */
fun interface CallbackManager {
  /**
   * The method that should be called from the Activity's or Fragment's onActivityResult method.
   *
   * @param requestCode The request code that's received by the Activity or Fragment.
   * @param resultCode The result code that's received by the Activity or Fragment.
   * @param data The result data that's received by the Activity or Fragment.
   * @return true If the result could be handled.
   */
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean

  /** The factory class for the [com.facebook.CallbackManager]. */
  object Factory {
    /**
     * Creates an instance of [com.facebook.CallbackManager].
     *
     * @return an instance of [com.facebook.CallbackManager].
     */
    @JvmStatic
    fun create(): CallbackManager {
      return CallbackManagerImpl()
    }
  }

  /** A type to contains all the parameters for the legacy activity result. */
  data class ActivityResultParameters(val requestCode: Int, val resultCode: Int, val data: Intent?)
}
