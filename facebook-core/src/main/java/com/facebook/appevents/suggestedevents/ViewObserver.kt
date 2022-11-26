/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
      observers.remove(key)?.stopTracking()
    }
  }
}
