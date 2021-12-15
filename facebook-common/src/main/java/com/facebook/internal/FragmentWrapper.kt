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

package com.facebook.internal

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * This is a wrapper to unify androidx (support library) fragments and the native (legacy) fragments
 * so that it's easy to call `startActivityForResult()`, even this method is deprecated. This class
 * is kept mainly for back-compatibility.
 */
@Suppress("DEPRECATION")
class FragmentWrapper {
  var supportFragment: Fragment? = null
    private set

  var nativeFragment: android.app.Fragment? = null
    private set

  constructor(fragment: Fragment) {
    supportFragment = fragment
  }

  constructor(fragment: android.app.Fragment) {
    nativeFragment = fragment
  }

  /** Call [Activity.startActivityForResult] from the fragment's containing Activity. */
  fun startActivityForResult(intent: Intent?, requestCode: Int) {
    if (supportFragment != null) {
      supportFragment?.startActivityForResult(intent, requestCode)
    } else {
      nativeFragment?.startActivityForResult(intent, requestCode)
    }
  }

  /**
   * Return the [FragmentActivity] this fragment is currently associated with. May return `null` if
   * the fragment is associated with a [Context] instead.
   */
  val activity: Activity?
    get() =
        if (supportFragment != null) {
          supportFragment?.activity
        } else {
          nativeFragment?.activity
        }
}
