/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.view.MotionEvent
import android.view.View
import com.facebook.appevents.codeless.CodelessLoggingEventListener.logEvent
import com.facebook.appevents.codeless.internal.EventBinding
import com.facebook.appevents.codeless.internal.ViewHierarchy.getExistingOnTouchListener
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference

@AutoHandleExceptions
object RCTCodelessLoggingEventListener {
  @JvmStatic
  fun getOnTouchListener(
      mapping: EventBinding,
      rootView: View,
      hostView: View
  ): AutoLoggingOnTouchListener {
    return AutoLoggingOnTouchListener(mapping, rootView, hostView)
  }

  class AutoLoggingOnTouchListener(mapping: EventBinding, rootView: View, hostView: View) :
      View.OnTouchListener {
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
      val rootViewValue = rootView.get()
      val hostViewValue = hostView.get()
      if (rootViewValue != null &&
          hostViewValue != null &&
          motionEvent.action == MotionEvent.ACTION_UP) {
        logEvent(mapping, rootViewValue, hostViewValue)
      }

      // If there is an existing listener then call its onTouch function else return false
      return (existingOnTouchListener != null && existingOnTouchListener.onTouch(view, motionEvent))
    }

    private val mapping: EventBinding = mapping
    private val hostView: WeakReference<View> = WeakReference(hostView)
    private val rootView: WeakReference<View> = WeakReference(rootView)
    private val existingOnTouchListener: View.OnTouchListener? =
        getExistingOnTouchListener(hostView)
    var supportCodelessLogging = true
  }
}
