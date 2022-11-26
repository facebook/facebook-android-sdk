/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import com.facebook.FacebookException
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.internal.NativeProtocol.createProtocolResultIntent
import com.facebook.internal.NativeProtocol.getMethodArgumentsFromIntent
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.logd

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class FacebookDialogFragment : DialogFragment() {
  /** The dialog should be set before the show method is called. */
  var innerDialog: Dialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initDialog()
  }

  @VisibleForTesting
  internal fun initDialog() {
    if (innerDialog != null) {
      return
    }
    val activity = activity ?: return
    val intent = activity.intent
    val params = getMethodArgumentsFromIntent(intent)
    val isWebFallback = params?.getBoolean(NativeProtocol.WEB_DIALOG_IS_FALLBACK, false) ?: false
    val webDialog: WebDialog?
    if (!isWebFallback) {
      val actionName = params?.getString(NativeProtocol.WEB_DIALOG_ACTION)
      val webParams = params?.getBundle(NativeProtocol.WEB_DIALOG_PARAMS)
      if (isNullOrEmpty(actionName)) {
        logd(TAG, "Cannot start a WebDialog with an empty/missing 'actionName'")
        activity.finish()
        return
      }
      webDialog =
          WebDialog.Builder(activity, actionName as String, webParams)
              .setOnCompleteListener { values, error -> onCompleteWebDialog(values, error) }
              .build()
    } else {
      val url = params?.getString(NativeProtocol.WEB_DIALOG_URL)
      if (isNullOrEmpty(url)) {
        logd(TAG, "Cannot start a fallback WebDialog with an empty/missing 'url'")
        activity.finish()
        return
      }
      val redirectUrl = String.format("fb%s://bridge/", getApplicationId())
      webDialog = FacebookWebFallbackDialog.newInstance(activity, url as String, redirectUrl)
      webDialog.onCompleteListener =
          WebDialog.OnCompleteListener { values, _
            -> // Error data is nested in the values since this is in the form of a
            // Native protocol response
            onCompleteWebFallbackDialog(values)
          }
    }
    innerDialog = webDialog
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    // Prevents an NPE crash in the support library
    if (innerDialog == null) {
      onCompleteWebDialog(null, null)
      showsDialog = false
      return super.onCreateDialog(savedInstanceState)
    }
    return innerDialog as Dialog
  }

  override fun onResume() {
    super.onResume()
    if (innerDialog is WebDialog) {
      (innerDialog as WebDialog).resize()
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (innerDialog is WebDialog && isResumed) {
      (innerDialog as WebDialog).resize()
    }
  }

  override fun onDestroyView() {
    val dialog = dialog
    if (dialog != null && retainInstance) {
      dialog.setDismissMessage(null)
    }
    super.onDestroyView()
  }

  private fun onCompleteWebDialog(values: Bundle?, error: FacebookException?) {
    val fragmentActivity = activity ?: return
    val resultIntent = createProtocolResultIntent(fragmentActivity.intent, values, error)
    val resultCode = if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED
    fragmentActivity.setResult(resultCode, resultIntent)
    fragmentActivity.finish()
  }

  private fun onCompleteWebFallbackDialog(values: Bundle?) {
    val fragmentActivity = activity ?: return
    val resultIntent = Intent()
    resultIntent.putExtras(values ?: Bundle())
    fragmentActivity.setResult(Activity.RESULT_OK, resultIntent)
    fragmentActivity.finish()
  }

  companion object {
    const val TAG = "FacebookDialogFragment"
  }
}
