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
