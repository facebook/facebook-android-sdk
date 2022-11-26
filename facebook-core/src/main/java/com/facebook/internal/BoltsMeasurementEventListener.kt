/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@AutoHandleExceptions
class BoltsMeasurementEventListener private constructor(context: Context) : BroadcastReceiver() {
  private val applicationContext: Context = context.applicationContext
  companion object {
    private var singleton: BoltsMeasurementEventListener? = null

    @VisibleForTesting
    internal val MEASUREMENT_EVENT_NOTIFICATION_NAME = "com.parse.bolts.measurement_event"
    private const val MEASUREMENT_EVENT_NAME_KEY = "event_name"
    private const val MEASUREMENT_EVENT_ARGS_KEY = "event_args"
    private const val BOLTS_MEASUREMENT_EVENT_PREFIX = "bf_"

    @JvmStatic
    fun getInstance(context: Context): BoltsMeasurementEventListener? {
      if (singleton != null) {
        return singleton
      }
      val listener = BoltsMeasurementEventListener(context)
      listener.open()
      singleton = listener
      return singleton
    }
  }

  private fun open() {
    val broadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    broadcastManager.registerReceiver(this, IntentFilter(MEASUREMENT_EVENT_NOTIFICATION_NAME))
  }

  private fun close() {
    val broadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    broadcastManager.unregisterReceiver(this)
  }

  @Throws(Throwable::class)
  fun finalize() {
    close()
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    val logger = InternalAppEventsLogger(context)
    val eventName =
        BOLTS_MEASUREMENT_EVENT_PREFIX + intent?.getStringExtra(MEASUREMENT_EVENT_NAME_KEY)
    val eventArgs = intent?.getBundleExtra(MEASUREMENT_EVENT_ARGS_KEY)
    val logData = Bundle()
    val keySet = eventArgs?.keySet()
    if (keySet != null) {
      for (key in keySet) {
        val safeKey =
            key.replace("[^0-9a-zA-Z _-]".toRegex(), "-")
                .replace("^[ -]*".toRegex(), "")
                .replace("[ -]*$".toRegex(), "")
        logData.putString(safeKey, eventArgs.get(key) as String?)
      }
    }
    logger.logEvent(eventName, logData)
  }
}
