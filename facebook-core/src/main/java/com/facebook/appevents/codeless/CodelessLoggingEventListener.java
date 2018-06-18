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

package com.facebook.appevents.codeless;

import android.os.Bundle;
import android.view.View;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.appevents.codeless.internal.Constants;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.appevents.codeless.internal.EventBinding;
import com.facebook.appevents.internal.AppEventUtility;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

class CodelessLoggingEventListener {
    private static final String TAG = CodelessLoggingEventListener.class.getCanonicalName();

    public static AutoLoggingAccessibilityDelegate
    getAccessibilityDelegate(EventBinding mapping, View rootView, View hostView) {
        return new AutoLoggingAccessibilityDelegate(mapping, rootView, hostView);
    }

    public static class AutoLoggingAccessibilityDelegate extends View.AccessibilityDelegate {
        public AutoLoggingAccessibilityDelegate(final EventBinding mapping,
                                                final View rootView,
                                                final View hostView) {
            if (null == mapping || null == rootView || null == hostView) {
                return;
            }

            this.existingDelegate = ViewHierarchy.getExistingDelegate(hostView);

            this.mapping = mapping;
            this.hostView = new WeakReference<View>(hostView);
            this.rootView = new WeakReference<View>(rootView);
            EventBinding.ActionType type = mapping.getType();

            switch (mapping.getType()) {
                case CLICK:
                    this.accessibilityEventType = AccessibilityEvent.TYPE_VIEW_CLICKED;
                    break;
                case SELECTED:
                    this.accessibilityEventType = AccessibilityEvent.TYPE_VIEW_SELECTED;
                    break;
                case TEXT_CHANGED:
                    this.accessibilityEventType = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
                    break;
                default:
                    throw new FacebookException("Unsupported action type: " + type.toString());
            }
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            if (eventType == AccessibilityEvent.INVALID_POSITION) {
                Log.e(TAG, "Unsupported action type");
            }
            if (eventType != this.accessibilityEventType) {
                return;
            }

            if (null != this.existingDelegate &&
                    !(this.existingDelegate instanceof AutoLoggingAccessibilityDelegate)) {
                this.existingDelegate.sendAccessibilityEvent(host, eventType);
            }

            logEvent();
        }

        private void logEvent() {
            final String eventName = this.mapping.getEventName();
            final Bundle parameters = CodelessMatcher.getParameters(
                    mapping,
                    rootView.get(),
                    hostView.get()
            );

            if (parameters.containsKey(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)) {
                String value = parameters.getString(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM);
                parameters.putDouble(
                        AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM,
                        AppEventUtility.normalizePrice(value));
            }

            parameters.putString(Constants.IS_CODELESS_EVENT_KEY, "1");

            final Bundle params = parameters;
            FacebookSdk.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    final Context context = FacebookSdk.getApplicationContext();
                    final AppEventsLogger appEventsLogger = AppEventsLogger.newLogger(context);
                    appEventsLogger.logEvent(eventName, params);
                }
            });
        }

        private EventBinding mapping;
        private WeakReference<View> hostView;
        private WeakReference<View> rootView;
        private int accessibilityEventType;
        private View.AccessibilityDelegate existingDelegate;
    }
}
