/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.os.Bundle
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.internal.AppCall

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * Callback class to allow derivations of FacebookDialogBase to do custom operations
 */
abstract class ResultProcessor(private val appCallback: FacebookCallback<*>?) {
  abstract fun onSuccess(appCall: AppCall, results: Bundle?)

  /** Override this if anything needs to be done on cancellation (e.g. Logging) */
  open fun onCancel(appCall: AppCall) {
    appCallback?.onCancel()
  }

  /** Override this if anything needs to be done on error (e.g. Logging) */
  open fun onError(appCall: AppCall, error: FacebookException) {
    appCallback?.onError(error)
  }
}
