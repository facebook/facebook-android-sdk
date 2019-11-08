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

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;

import com.facebook.FacebookSdk;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.appevents.codeless.internal.ViewHierarchy;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

final class ViewOnClickListener implements View.OnClickListener {
    private static final String TAG = ViewOnClickListener.class.getCanonicalName();

    private @Nullable View.OnClickListener baseListener;

    private static final Set<Integer> viewsAttachedListener = new HashSet<>();

    private WeakReference<View> rootViewWeakReference;
    private WeakReference<View> hostViewWeakReference;
    private String activityName;

    static void attachListener(View hostView, View rootView, String activityName) {
        try {
            int key = hostView.hashCode();
            if (!(hostView instanceof AdapterView) && !viewsAttachedListener.contains(key)) {
                hostView.setOnClickListener(
                        new ViewOnClickListener(hostView, rootView, activityName));
                viewsAttachedListener.add(key);
            }
        } catch (Exception e) {
            // swallow
        }
    }

    private ViewOnClickListener(View hostView, View rootView, String activityName) {
        baseListener = ViewHierarchy.getExistingOnClickListener(hostView);
        hostViewWeakReference = new WeakReference<>(hostView);
        rootViewWeakReference = new WeakReference<>(rootView);
        this.activityName = activityName;
    }

    @Override
    public void onClick(View view) {
        if (baseListener != null) {
            baseListener.onClick(view);
        }
        process();
    }

    private void process() {
        View rootView = rootViewWeakReference.get();
        View hostView = hostViewWeakReference.get();
        if (rootView == null || hostView == null) {
            return;
        }
        try {
            String predictedEvent = ""; // TODO (T54293420) add prediction and dedupe
            if (SuggestedEventsManager.isProdctionEvents(predictedEvent)) {
                InternalAppEventsLogger logger = new InternalAppEventsLogger(
                        FacebookSdk.getApplicationContext());
                logger.logEventFromSE(predictedEvent);
            } else if (SuggestedEventsManager.isEligibleEvents(predictedEvent)) {
                // TODO: T54293420 send event to the endpoint
            }
        } catch (Exception e) {
            // swallow
        }
    }
}
