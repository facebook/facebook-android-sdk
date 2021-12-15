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
