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
