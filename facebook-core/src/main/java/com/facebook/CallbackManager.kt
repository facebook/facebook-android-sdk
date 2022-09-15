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
