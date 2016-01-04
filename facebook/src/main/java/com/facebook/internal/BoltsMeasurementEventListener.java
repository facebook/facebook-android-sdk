/**
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

package com.facebook.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.appevents.AppEventsLogger;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class BoltsMeasurementEventListener extends BroadcastReceiver {
    private static BoltsMeasurementEventListener _instance;

    private final static String MEASUREMENT_EVENT_NOTIFICATION_NAME =
            "com.parse.bolts.measurement_event";
    private final static String MEASUREMENT_EVENT_NAME_KEY = "event_name";
    private final static String MEASUREMENT_EVENT_ARGS_KEY = "event_args";
    private final static String BOLTS_MEASUREMENT_EVENT_PREFIX = "bf_";

    private Context applicationContext;

    private BoltsMeasurementEventListener(Context context) {
        applicationContext = context.getApplicationContext();
    }

    private void open() {
      LocalBroadcastManager broadcastManager =
              LocalBroadcastManager.getInstance(applicationContext);
      broadcastManager.registerReceiver(
              this, new IntentFilter(MEASUREMENT_EVENT_NOTIFICATION_NAME));
    }

    private void close() {
      LocalBroadcastManager broadcastManager =
              LocalBroadcastManager.getInstance(applicationContext);
      broadcastManager.unregisterReceiver(this);
    }

    public static BoltsMeasurementEventListener getInstance(Context context) {
        if (_instance != null) {
            return _instance;
        }
        _instance = new BoltsMeasurementEventListener(context);
        _instance.open();
        return _instance;
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppEventsLogger appEventsLogger = AppEventsLogger.newLogger(context);
        String eventName = BOLTS_MEASUREMENT_EVENT_PREFIX +
                intent.getStringExtra(MEASUREMENT_EVENT_NAME_KEY);
        Bundle eventArgs = intent.getBundleExtra(MEASUREMENT_EVENT_ARGS_KEY);
        Bundle logData = new Bundle();
        for(String key : eventArgs.keySet()) {
           String safeKey = key.replaceAll(
                   "[^0-9a-zA-Z _-]", "-").replaceAll("^[ -]*", "").replaceAll("[ -]*$", "");
           logData.putString(safeKey, (String)eventArgs.get(key));
        }
        appEventsLogger.logEvent(eventName, logData);
    }
}
