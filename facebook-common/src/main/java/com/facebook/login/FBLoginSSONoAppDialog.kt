/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.facebook.common.R

/**
 * Dialog shown when the Facebook app is not installed on the device.
 *
 * Shown when [FBLoginSSOLauncher] has `showWithoutFBApp` enabled and no resolvable SSO intent is
 * found. Presents the user with options to continue with Facebook (via Chrome Custom Tabs) or
 * dismiss.
 */
class FBLoginSSONoAppDialog : DialogFragment() {

  /** Called when the dialog is dismissed (via X, "Not now", tapping outside, or back press). */
  var onDismissListener: (() -> Unit)? = null

  /** Called when the user taps "Continue with Facebook". */
  var onContinueListener: (() -> Unit)? = null

  private var continueClicked = false

  companion object {
    const val TAG = "FBLoginSSONoAppDialog"
    private const val ARG_FB4A_OUTDATED = "fb4a_outdated"

    @JvmStatic
    fun newInstance(fb4aOutdated: Boolean = false): FBLoginSSONoAppDialog {
      val dialog = FBLoginSSONoAppDialog()
      val args = Bundle()
      args.putBoolean(ARG_FB4A_OUTDATED, fb4aOutdated)
      dialog.arguments = args
      return dialog
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NO_TITLE, R.style.com_facebook_sso_noapp_dialog)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.com_facebook_sso_noapp_dialog, container, false)

    val fb4aOutdated = arguments?.getBoolean(ARG_FB4A_OUTDATED, false) ?: false
    if (fb4aOutdated) {
      view.findViewById<TextView>(R.id.com_facebook_sso_noapp_title)
          .setText(R.string.com_facebook_sso_outdated_title)
      view.findViewById<TextView>(R.id.com_facebook_sso_noapp_body)
          .setText(R.string.com_facebook_sso_outdated_body)
    }

    view.findViewById<View>(R.id.com_facebook_sso_noapp_close)
        .setOnClickListener { dismiss() }

    view.findViewById<View>(R.id.com_facebook_sso_noapp_not_now)
        .setOnClickListener { dismiss() }

    view.findViewById<View>(R.id.com_facebook_sso_noapp_continue)
        .setOnClickListener {
          continueClicked = true
          onContinueListener?.invoke()
          dismiss()
        }

    return view
  }

  override fun onStart() {
    super.onStart()
    val isLandscape =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    dialog?.window?.apply {
      setBackgroundDrawableResource(android.R.color.transparent)
      if (isLandscape) {
        val width = (resources.displayMetrics.widthPixels * 0.6).toInt()
        setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.CENTER)
      } else {
        setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.BOTTOM)
      }
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    if (!continueClicked) {
      onDismissListener?.invoke()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    onContinueListener = null
    onDismissListener = null
  }
}
