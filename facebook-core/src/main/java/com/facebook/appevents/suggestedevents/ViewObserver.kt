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

package com.facebook.appevents.suggestedevents

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver
import com.facebook.appevents.codeless.internal.SensitiveUserDataUtils
import com.facebook.appevents.internal.AppEventUtility.getRootView
import com.facebook.appevents.suggestedevents.ViewOnClickListener.Companion.attachListener
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
internal class ViewObserver private constructor(activity: Activity) :
    ViewTreeObserver.OnGlobalLayoutListener {
  private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
  private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
  private val isTracking: AtomicBoolean = AtomicBoolean(false)
  private fun startTracking() {
    if (isTracking.getAndSet(true)) {
      return
    }
    val rootView = getRootView(activityWeakReference.get()) ?: return
    val observer = rootView.viewTreeObserver
    if (observer.isAlive) {
      observer.addOnGlobalLayoutListener(this)
      process()
    }
  }

  private fun stopTracking() {
    if (!isTracking.getAndSet(false)) {
      return
    }
    val rootView = getRootView(activityWeakReference.get()) ?: return
    val observer = rootView.viewTreeObserver
    if (!observer.isAlive) {
      return
    }
    observer.removeOnGlobalLayoutListener(this)
  }

  override fun onGlobalLayout() {
    process()
  }

  private fun process() {
    val runnable = Runnable {
      try {
        val rootView = getRootView(activityWeakReference.get())
        val activity = activityWeakReference.get()
        if (rootView == null || activity == null) {
          return@Runnable
        }
        val clickableViews = SuggestedEventViewHierarchy.getAllClickableViews(rootView)
        for (view in clickableViews) {
          if (SensitiveUserDataUtils.isSensitiveUserData(view)) {
            continue
          }
          val text = SuggestedEventViewHierarchy.getTextOfViewRecursively(view)
          if (text.isNotEmpty() && text.length <= MAX_TEXT_LENGTH) {
            attachListener(view, rootView, activity.localClassName)
          }
        }
      } catch (e: Exception) {
        /*no op*/
      }
    }
    if (Thread.currentThread() === Looper.getMainLooper().thread) {
      runnable.run()
    } else {
      uiThreadHandler.post(runnable)
    }
  }

  companion object {
    private val observers: MutableMap<Int, ViewObserver> = HashMap()
    private const val MAX_TEXT_LENGTH = 300

    @JvmStatic
    fun startTrackingActivity(activity: Activity) {
      val key = activity.hashCode()
      val observer = observers.getOrPut(key) { ViewObserver(activity) }
      observer.startTracking()
    }

    @JvmStatic
    fun stopTrackingActivity(activity: Activity) {
      val key = activity.hashCode()
      observers[key]?.let {
        observers.remove(key)
        it.stopTracking()
      }
    }
  }
}
