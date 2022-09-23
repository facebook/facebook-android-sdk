/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import androidx.activity.result.contract.ActivityResultContract

/** Represents dialogs provided by Facebook */
interface FacebookDialog<CONTENT, RESULT> {
  /**
   * Indicates whether the dialog can be shown for the content passed in.
   *
   * @param content the content to check
   * @return true if the dialog can be shown
   */
  fun canShow(content: CONTENT): Boolean

  /**
   * Shows the dialog for the content passed in.
   *
   * @param content the content to show
   */
  fun show(content: CONTENT)

  /**
   * Allows the registration of a callback that will be executed once the dialog is closed, with
   * success, cancel or error details. This should be called in the [ ]
   * [android.app.Activity.onCreate] or [ ][androidx.fragment.app.Fragment.onCreate] methods.
   *
   * @param callbackManager CallbackManager instance that will handle the onActivityResult
   * @param callback Callback to be called upon dialog completion
   */
  fun registerCallback(callbackManager: CallbackManager, callback: FacebookCallback<RESULT>)

  /**
   * Allows the registration of a callback that will be executed once the dialog is closed, with
   * success, cancel or error details. This should be called in the [ ]
   * [android.app.Activity.onCreate] or [ ][androidx.fragment.app.Fragment.onCreate] methods.
   *
   * @param callbackManager CallbackManager instance that will handle the Activity Result
   * @param callback Callback to be called upon dialog completion
   * @param requestCode The request code to use, this should be outside of the range of those
   * reserved for the Facebook SDK [com.facebook.FacebookSdk.isFacebookRequestCode].
   */
  fun registerCallback(
      callbackManager: CallbackManager,
      callback: FacebookCallback<RESULT>,
      requestCode: Int
  )
  /**
   * Creates the ActivityResultContract instance for showing the dialog with Androidx activities and
   * fragments.
   *
   * @param callbackManager CallbackManager instance that will handle the onActivityResult
   */
  fun createActivityResultContractForShowingDialog(
      callbackManager: CallbackManager?
  ): ActivityResultContract<CONTENT, CallbackManager.ActivityResultParameters>
}
