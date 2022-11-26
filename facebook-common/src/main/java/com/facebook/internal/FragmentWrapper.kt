/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
