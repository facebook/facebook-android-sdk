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
package com.facebook.appevents.suggestedevents;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.facebook.appevents.codeless.internal.ViewHierarchy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class ViewObserver implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = ViewObserver.class.getCanonicalName();
    private WeakReference<Activity> activityWeakReference;
    private final Handler uiThreadHandler;
    private AtomicBoolean isTracking;
    private static final Map<Integer, ViewObserver> observers = new HashMap<>();
    private static final int MAX_TEXT_LENGTH = 300;

    static void startTrackingActivity(final Activity activity) {
        int key = activity.hashCode();
        if (!observers.containsKey(key)) {
            ViewObserver observer = new ViewObserver(activity);
            observers.put(key, observer);
            observer.startTracking();
        }
    }

    static void stopTrackingActivity(final Activity activity) {
        int key = activity.hashCode();
        if (observers.containsKey(key)) {
            ViewObserver observer = observers.get(key);
            observers.remove(key);
            observer.stopTracking();
        }
    }

    private ViewObserver(Activity activity) {
        activityWeakReference = new WeakReference<>(activity);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        isTracking = new AtomicBoolean(false);
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
            observer.addOnGlobalLayoutListener(this);
            process();
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
        if (Build.VERSION.SDK_INT < 16) {
            observer.removeGlobalOnLayoutListener(this);
        } else {
            observer.removeOnGlobalLayoutListener(this);
        }
    }

    @Override
    public void onGlobalLayout() {
        process();
    }

    private void process() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    View rootView = getRootView();
                    Activity activity = activityWeakReference.get();
                    if (rootView == null || activity == null) {
                        return;
                    }

                    List<View> clickableViews = SuggestedEventViewHierarchy
                            .getAllClickableViews(rootView);
                    for (View view : clickableViews) {
                        String text = ViewHierarchy.getTextOfView(view);
                        if (!text.isEmpty() && text.length() <= MAX_TEXT_LENGTH) {
                            ViewOnClickListener.attachListener(view, rootView,
                                    activity.getLocalClassName());
                        }
                    }
                } catch (Exception e) {
                    /*no op*/
                }
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
