/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.FacebookSdk.getExecutor
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger.Companion.newLogger
import com.facebook.appevents.codeless.CodelessMatcher.Companion.getParameters
import com.facebook.appevents.codeless.internal.Constants
import com.facebook.appevents.codeless.internal.EventBinding
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.AppEventUtility.normalizePrice
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CodelessLoggingEventListener {
  @JvmStatic
  fun getOnClickListener(
      mapping: EventBinding,
      rootView: View,
      hostView: View
  ): AutoLoggingOnClickListener {
    return AutoLoggingOnClickListener(mapping, rootView, hostView)
  }

  @JvmStatic
  fun getOnItemClickListener(
      mapping: EventBinding,
      rootView: View,
      hostView: AdapterView<*>
  ): AutoLoggingOnItemClickListener {
    return AutoLoggingOnItemClickListener(mapping, rootView, hostView)
  }

  @JvmStatic
  internal fun logEvent(mapping: EventBinding, rootView: View, hostView: View) {
    val eventName = mapping.eventName
    val parameters = getParameters(mapping, rootView, hostView)
    updateParameters(parameters)
    getExecutor().execute {
      val context = getApplicationContext()
      val appEventsLogger = newLogger(context)
      appEventsLogger.logEvent(eventName, parameters)
    }
  }

  internal fun updateParameters(parameters: Bundle) {
    val value = parameters.getString(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)
    if (value != null) {
      parameters.putDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, normalizePrice(value))
    }
    parameters.putString(Constants.IS_CODELESS_EVENT_KEY, "1")
  }

  class AutoLoggingOnClickListener(mapping: EventBinding, rootView: View, hostView: View) :
      View.OnClickListener {
    override fun onClick(view: View) {
      // If there is an existing listener and its not the one of AutoLoggingOnClickListener
      // then call its onClick function
      existingOnClickListener?.onClick(view)
      val rootViewValue = rootView.get()
      val hostViewValue = hostView.get()
      if (rootViewValue != null && hostViewValue != null) {
        logEvent(mapping as EventBinding, rootViewValue, hostViewValue)
      }
    }

    private var mapping: EventBinding = mapping
    private var hostView: WeakReference<View> = WeakReference(hostView)
    private var rootView: WeakReference<View> = WeakReference(rootView)
    private var existingOnClickListener: View.OnClickListener? =
        ViewHierarchy.getExistingOnClickListener(hostView)
    var supportCodelessLogging = true
  }

  class AutoLoggingOnItemClickListener(
      mapping: EventBinding,
      rootView: View,
      hostView: AdapterView<*>
  ) : AdapterView.OnItemClickListener {
    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
      existingOnItemClickListener?.onItemClick(parent, view, position, id)
      val rootViewValue = rootView.get()
      val hostViewValue = hostView.get()
      if (rootViewValue != null && hostViewValue != null) {
        logEvent(mapping, rootViewValue, hostViewValue)
      }
    }

    private var mapping: EventBinding = mapping
    private var hostView: WeakReference<AdapterView<*>> = WeakReference(hostView)
    private var rootView: WeakReference<View> = WeakReference(rootView)
    private var existingOnItemClickListener: AdapterView.OnItemClickListener? =
        hostView.onItemClickListener
    var supportCodelessLogging = true
  }
}
