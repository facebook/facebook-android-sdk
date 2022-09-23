/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** A callback class for the Facebook SDK. */
interface FacebookCallback<RESULT> {
  /**
   * Called when the dialog completes without error.
   *
   * Note: This will be called instead of [.onCancel] if any of the following conditions are true.
   *
   * * [com.facebook.share.widget.MessageDialog] is used.
   * * The logged in Facebook user has not authorized the app that has initiated the dialog.
   *
   * @param result Result from the dialog
   */
  fun onSuccess(result: RESULT)

  /**
   * Called when the dialog is canceled.
   *
   * Note: [.onSuccess] will be called instead if any of the following conditions are true.
   *
   * * [com.facebook.share.widget.MessageDialog] is used.
   * * The logged in Facebook user has not authorized the app that has initiated the dialog.
   */
  fun onCancel()

  /**
   * Called when the dialog finishes with an error.
   *
   * @param error The error that occurred
   */
  fun onError(error: FacebookException)
}
