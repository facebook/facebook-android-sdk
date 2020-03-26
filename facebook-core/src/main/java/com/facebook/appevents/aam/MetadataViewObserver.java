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

package com.facebook.appevents.aam;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoHandleExceptions
final class MetadataViewObserver implements ViewTreeObserver.OnGlobalFocusChangeListener {
    private static final String TAG = MetadataViewObserver.class.getCanonicalName();
    private static final int MAX_TEXT_LENGTH = 100;

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

    @UiThread
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

    @UiThread
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
        final View rootView = AppEventUtility.getRootView(activityWeakReference.get());
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
        final View rootView = AppEventUtility.getRootView(activityWeakReference.get());
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
                processEditText(view);
            }
        };

        runOnUIThread(runnable);
    }

    private void processEditText(final View view) {
        final String text = ((EditText) view).getText().toString().trim().toLowerCase();
        if (text.isEmpty() || processedText.contains(text) || text.length() > MAX_TEXT_LENGTH) {
            return;
        }
        processedText.add(text);
        Map<String, String> userData = new HashMap<>();

        List<String> currentViewIndicators = MetadataMatcher.getCurrentViewIndicators(view);
        List<String> aroundTextIndicators = null;

        for (MetadataRule rule : MetadataRule.getRules()) {
            String normalizedText = preNormalize(rule.getName(), text);
            // 1. match value if value rule is not empty
            if (!rule.getValRule().isEmpty() && !MetadataMatcher.matchValue(normalizedText, rule.getValRule())) {
                continue;
            }

            // 2. match indicator
            if (MetadataMatcher.matchIndicator(currentViewIndicators, rule.getKeyRules())) {
                putUserData(userData, rule.getName(), normalizedText);
                continue;
            }
            // only fetch once
            if (aroundTextIndicators == null) {
                aroundTextIndicators = MetadataMatcher.getAroundViewIndicators(view);
            }
            if (MetadataMatcher.matchIndicator(aroundTextIndicators, rule.getKeyRules())) {
                putUserData(userData, rule.getName(), normalizedText);
            }
        }
        InternalAppEventsLogger.setInternalUserData(userData);
    }

    private static String preNormalize(String key, String val) {
        if ("r2".equals(key)) {
            return val.replaceAll("[^\\d.]", "");
        }
        return val;
    }

    private static void putUserData(Map<String, String> userData, String key, String val) {
        switch (key) {
            case "r3":
                if (val.startsWith("m") || val.startsWith("b") || val.startsWith("ge")) {
                    val = "m";
                } else {
                    val = "f";
                }
                break;
            case "r4":
            case "r5":
                val = val.replaceAll("[^a-z]+", ""); // lowercase already
                break;
            case "r6":
                if (val.contains("-")) {
                    String[] splitArray = val.split("-");
                    val = splitArray[0];
                }
        }

        userData.put(key, val);
    }

    private void runOnUIThread(Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            uiThreadHandler.post(runnable);
        }
    }
}
