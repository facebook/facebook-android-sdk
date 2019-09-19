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

package com.facebook.appevents.Metadata;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.appevents.InternalAppEventsLogger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class MetadataViewObserver implements ViewTreeObserver.OnGlobalFocusChangeListener {
    private static final String TAG = MetadataViewObserver.class.getCanonicalName();
    private static final Map<Integer, MetadataViewObserver> observers = new HashMap<>();
    private final Set<String> processedText = new HashSet<>();
    private final Handler uiThreadHandler;
    private WeakReference<Activity> activityWeakReference;
    private AtomicBoolean isTracking;

    private MetadataViewObserver(final Activity activity) {
        activityWeakReference = new WeakReference<>(activity);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        isTracking = new AtomicBoolean(false);
    }

    static void startTrackingActivity(final Activity activity) {
        int key = activity.hashCode();
        MetadataViewObserver observer;
        if (!observers.containsKey(key)) {
            observer = new MetadataViewObserver(activity);
            observers.put(activity.hashCode(), observer);
        } else {
            observer = observers.get(key);
        }
        observer.startTracking();
    }

    static void stopTrackingActivity(final Activity activity) {
        int key = activity.hashCode();
        if (observers.containsKey(key)) {
            MetadataViewObserver observer = observers.get(activity.hashCode());
            observers.remove(key);
            observer.stopTracking();
        }
    }

    private void startTracking() {
        if (isTracking.getAndSet(true)) {
            return;
        }
        final View rootView = getRootView();
        if (rootView == null) {
            return;
        }
        ViewTreeObserver observer = rootView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalFocusChangeListener(this);
        }
    }

    private void stopTracking() {
        if (!isTracking.getAndSet(false)) {
            return;
        }
        final View rootView = getRootView();
        if (rootView == null) {
            return;
        }
        ViewTreeObserver observer = rootView.getViewTreeObserver();
        if (!observer.isAlive()) {
            return;
        }
        observer.removeOnGlobalFocusChangeListener(this);
    }

    @Override
    public void onGlobalFocusChanged(@Nullable View oldView, @Nullable View newView) {
        if (oldView != null) {
            process(oldView);
        }
        if (newView != null) {
            process(newView);
        }
    }

    private void process(final View view) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!(view instanceof EditText)) {
                    return;
                }

                String text = ((EditText) view).getText().toString().trim();
                if (text.isEmpty() || processedText.contains(text)) {
                    return;
                }
                processedText.add(text);

                final MetadataMatcher.MatcherInput input =
                        new MetadataMatcher.MatcherInput((TextView) view);
                if (!input.isValid) {
                    return;
                }
                final Map<String, String> userData = new HashMap<>();

                FacebookSdk.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        for (MetadataRule rule : MetadataRule.getRules()) {
                            if (rule.getIsEnabled() && MetadataMatcher.match(input, rule)) {
                                userData.put(rule.getName(), input.text);
                            }
                        }
                        InternalAppEventsLogger.setInternalUserData(userData);
                    }
                });
            }
        };

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            uiThreadHandler.post(runnable);
        }
    }

    @Nullable
    private View getRootView() {
        Activity activity = activityWeakReference.get();
        if (activity == null) {
            return null;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return null;
        }
        return window.getDecorView().getRootView();
    }
}
