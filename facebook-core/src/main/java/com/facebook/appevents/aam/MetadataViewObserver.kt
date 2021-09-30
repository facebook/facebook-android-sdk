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
package com.facebook.appevents.aam

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.annotation.UiThread
import com.facebook.appevents.InternalAppEventsLogger.Companion.setInternalUserData
import com.facebook.appevents.aam.MetadataMatcher.getAroundViewIndicators
import com.facebook.appevents.aam.MetadataMatcher.getCurrentViewIndicators
import com.facebook.appevents.aam.MetadataMatcher.matchIndicator
import com.facebook.appevents.aam.MetadataMatcher.matchValue
import com.facebook.appevents.aam.MetadataRule.Companion.getRules
import com.facebook.appevents.internal.AppEventUtility.getRootView
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
internal class MetadataViewObserver private constructor(activity: Activity) :
    ViewTreeObserver.OnGlobalFocusChangeListener {
  private val processedText: MutableSet<String> = mutableSetOf()
  private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
  private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
  private val isTracking: AtomicBoolean = AtomicBoolean(false)

  private fun startTracking() {
    if (isTracking.getAndSet(true)) {
      return
    }
    val rootView = getRootView(activityWeakReference.get()) ?: return
    val observer = rootView.viewTreeObserver
    if (observer.isAlive) {
      observer.addOnGlobalFocusChangeListener(this)
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
    observer.removeOnGlobalFocusChangeListener(this)
  }

  override fun onGlobalFocusChanged(oldView: View?, newView: View?) {
    oldView?.let { process(it) }
    newView?.let { process(it) }
  }

  private fun process(view: View) {
    val runnable = Runnable {
      if (view !is EditText) {
        return@Runnable
      }
      processEditText(view)
    }
    runOnUIThread(runnable)
  }

  private fun processEditText(view: View) {
    val text = (view as EditText).text.toString().trim().toLowerCase()
    if (text.isEmpty() || processedText.contains(text) || text.length > MAX_TEXT_LENGTH) {
      return
    }
    processedText.add(text)
    val userData: MutableMap<String, String> = HashMap()
    val currentViewIndicators = getCurrentViewIndicators(view)
    var aroundTextIndicators: List<String>? = null
    for (rule in getRules()) {
      val normalizedText = preNormalize(rule.name, text)
      // 1. match value if value rule is not empty
      if (rule.valRule.isNotEmpty() && !matchValue(normalizedText, rule.valRule)) {
        continue
      }

      // 2. match indicator
      if (matchIndicator(currentViewIndicators, rule.keyRules)) {
        putUserData(userData, rule.name, normalizedText)
        continue
      }
      // only fetch once
      if (aroundTextIndicators == null) {
        aroundTextIndicators = getAroundViewIndicators(view)
      }
      if (MetadataMatcher.matchIndicator(aroundTextIndicators, rule.keyRules)) {
        putUserData(userData, rule.name, normalizedText)
      }
    }
    setInternalUserData(userData)
  }

  private fun runOnUIThread(runnable: Runnable) {
    if (Thread.currentThread() === Looper.getMainLooper().thread) {
      runnable.run()
    } else {
      uiThreadHandler.post(runnable)
    }
  }

  companion object {
    private const val MAX_TEXT_LENGTH = 100
    private val observers: MutableMap<Int, MetadataViewObserver> = hashMapOf()
    @UiThread
    @JvmStatic
    fun startTrackingActivity(activity: Activity) {
      val key = activity.hashCode()
      val observer = observers.getOrPut(key) { MetadataViewObserver(activity) }
      observer.startTracking()
    }

    @UiThread
    @JvmStatic
    fun stopTrackingActivity(activity: Activity) {
      val key = activity.hashCode()
      observers[key]?.let {
        observers.remove(key)
        it.stopTracking()
      }
    }

    private fun preNormalize(key: String, value: String): String {
      return if ("r2" == key) {
        value.replace("[^\\d.]".toRegex(), "")
      } else value
    }

    private fun putUserData(userData: MutableMap<String, String>, key: String, value: String) {
      var value = value
      when (key) {
        "r3" ->
            value =
                if (value.startsWith("m") || value.startsWith("b") || value.startsWith("ge")) {
                  "m"
                } else {
                  "f"
                }
        "r4", "r5" -> value = value.replace("[^a-z]+".toRegex(), "") // lowercase already
        "r6" ->
            if (value.contains("-")) {
              val splitArray = value.split("-".toRegex()).toTypedArray()
              value = splitArray[0]
            }
      }
      userData[key] = value
    }
  }
}
