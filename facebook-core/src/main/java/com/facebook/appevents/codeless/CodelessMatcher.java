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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.codeless.internal.Constants;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.appevents.codeless.internal.ParameterComponent;
import com.facebook.appevents.codeless.internal.PathComponent;
import com.facebook.appevents.codeless.internal.EventBinding;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.InternalSettings;
import com.facebook.internal.Utility;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.facebook.appevents.codeless.internal.PathComponent.MatchBitmaskType;

class CodelessMatcher {
    private static final String PARENT_CLASS_NAME = "..";
    private static final String CURRENT_CLASS_NAME = ".";
    private static final String TAG = CodelessMatcher.class.getCanonicalName();

    private final Handler uiThreadHandler;
    private Set<Activity> activitiesSet;
    private Set<ViewMatcher> viewMatchers;
    private HashSet<String> listenerSet;
    private HashMap<Integer, HashSet<String>> activityToListenerMap;

    private static CodelessMatcher codelessMatcher = null;

    private CodelessMatcher() {
        this.uiThreadHandler = new Handler(Looper.getMainLooper());
        this.activitiesSet = new HashSet<>();
        this.viewMatchers = new HashSet<>();
        this.listenerSet = new HashSet<>();
        this.activityToListenerMap = new HashMap<>();
    }

    public static synchronized CodelessMatcher getInstance() {
        if (codelessMatcher == null) {
            codelessMatcher = new CodelessMatcher();
        }
        return codelessMatcher;
    }

    public void add(Activity activity) {
        if (InternalSettings.isUnityApp()) {
            return;
        }
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new FacebookException("Can't add activity to CodelessMatcher on non-UI thread");
        }
        this.activitiesSet.add(activity);
        listenerSet.clear();
        if (activityToListenerMap.containsKey(activity.hashCode())) {
            listenerSet = activityToListenerMap.get(activity.hashCode());
        }
        startTracking();
    }

    public void remove(Activity activity) {
        if (InternalSettings.isUnityApp()) {
            return;
        }
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new FacebookException(
                    "Can't remove activity from CodelessMatcher on non-UI thread"
            );
        }
        this.activitiesSet.remove(activity);
        this.viewMatchers.clear();
        this.activityToListenerMap.put(activity.hashCode(),
            (HashSet<String>) listenerSet.clone());
        listenerSet.clear();
    }

    public void destroy(Activity activity) {
        this.activityToListenerMap.remove(activity.hashCode());
    }

    public static Bundle getParameters(final EventBinding mapping,
                                       final View rootView,
                                       final View hostView) {
        Bundle params = new Bundle();

        if (null == mapping) {
            return params;
        }

        List<ParameterComponent> parameters = mapping.getViewParameters();
        if (null != parameters) {
            for (ParameterComponent component : parameters) {
                if (component.value != null && component.value.length() > 0) {
                    params.putString(component.name, component.value);
                } else if (component.path.size() > 0){
                    List<MatchedView> matchedViews;
                    final String pathType = component.pathType;
                    if (pathType.equals(Constants.PATH_TYPE_RELATIVE)) {
                        matchedViews = ViewMatcher.findViewByPath(
                                mapping,
                                hostView,
                                component.path,
                                0,
                                -1,
                                hostView.getClass().getSimpleName()
                        );
                    } else {
                        matchedViews = ViewMatcher.findViewByPath(
                                mapping,
                                rootView,
                                component.path,
                                0,
                                -1,
                                rootView.getClass().getSimpleName()
                        );
                    }

                    for (MatchedView view : matchedViews) {
                        if (view.getView() == null) {
                            continue;
                        }
                        String text = ViewHierarchy.getTextOfView(view.getView());
                        if (text.length() > 0) {
                            params.putString(component.name, text);
                            break;
                        }
                    }
                }
            }
        }

        return params;
    }

    private void startTracking() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            matchViews();
        } else {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    matchViews();
                }
            });

        }
    }

    private void matchViews() {
        for (Activity activity : this.activitiesSet) {
            final View rootView = activity.getWindow().getDecorView().getRootView();
            final String activityName = activity.getClass().getSimpleName();
            ViewMatcher matcher = new ViewMatcher(
                    rootView, uiThreadHandler, listenerSet, activityName);
            this.viewMatchers.add(matcher);
        }
    }

    public static class MatchedView {
        private WeakReference<View> view;
        private String viewMapKey;

        public MatchedView(View view, String viewMapKey) {
            this.view = new WeakReference<View>(view);
            this.viewMapKey = viewMapKey;
        }

        @Nullable
        public View getView() {
            return (this.view == null) ? null : this.view.get();
        }

        public String getViewMapKey() {
            return this.viewMapKey;
        }
    }

    protected static class ViewMatcher implements ViewTreeObserver.OnGlobalLayoutListener,
            ViewTreeObserver.OnScrollChangedListener, Runnable {
        private WeakReference<View> rootView;
        @Nullable private List<EventBinding> eventBindings;
        private final Handler handler;
        private HashSet<String> listenerSet;
        private final String activityName;

        public ViewMatcher(View rootView,
                           Handler handler,
                           HashSet<String> listenerSet,
                           final String activityName) {
            this.rootView = new WeakReference<View>(rootView);
            this.handler = handler;
            this.listenerSet = listenerSet;
            this.activityName = activityName;

            this.handler.postDelayed(this, 200);
        }

        @Override
        public void run() {
            final String appId = FacebookSdk.getApplicationId();
            FetchedAppSettings appSettings =
                    FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId);
            if (appSettings == null || !appSettings.getCodelessEventsEnabled()) {
                return;
            }

            this.eventBindings = EventBinding.parseArray(appSettings.getEventBindings());

            if (this.eventBindings != null) {
                View rootView = this.rootView.get();
                if (rootView == null) {
                    return;
                }
                ViewTreeObserver observer = rootView.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.addOnGlobalLayoutListener(this);
                    observer.addOnScrollChangedListener(this);
                }

                startMatch();
            }
        }

        @Override
        public void onGlobalLayout() {
            startMatch();
        }

        @Override
        public void onScrollChanged() {
            startMatch();
        }

        private void startMatch() {
            if (this.eventBindings != null && this.rootView.get() != null) {
                for (int i = 0; i < this.eventBindings.size(); i++) {
                    EventBinding binding = this.eventBindings.get(i);
                    findView(binding, this.rootView.get());
                }
            }
        }

        public void findView(final EventBinding mapping, final View rootView) {
            if (mapping == null || rootView == null) {
                return;
            }

            if (!TextUtils.isEmpty(mapping.getActivityName()) &&
                    !mapping.getActivityName().equals(this.activityName)) {
                return;
            }

            List<PathComponent> path = mapping.getViewPath();

            if (path.size() > Constants.MAX_TREE_DEPTH) {
                return;
            }

            List<MatchedView> matchedViews = findViewByPath(
                    mapping,
                    rootView,
                    path,
                    0,
                    -1,
                    this.activityName);
            for (MatchedView view: matchedViews) {
                attachListener(view, rootView, mapping);
            }
        }

        public static List<MatchedView> findViewByPath(final EventBinding mapping,
                                   final View view,
                                   final List<PathComponent> path,
                                   final int level,
                                   final int index,
                                   String mapKey) {
            mapKey += "." + String.valueOf(index);
            List<MatchedView> result = new ArrayList<>();
            if (null == view) {
                return result;
            }

            if (level >= path.size()) {
                // Match all children views if their parent view is matched
                result.add(new MatchedView(view, mapKey));
            } else {
                PathComponent pathElement = path.get(level);
                if (pathElement.className.equals(PARENT_CLASS_NAME)) {
                    ViewParent parent = view.getParent();
                    if (parent instanceof ViewGroup) {
                        final ViewGroup viewGroup = (ViewGroup)parent;
                        List<View> visibleViews = findVisibleChildren(viewGroup);
                        final int childCount = visibleViews.size();
                        for (int i = 0; i < childCount; i++) {
                            View child = visibleViews.get(i);
                            List<MatchedView> matchedViews = findViewByPath(
                                    mapping,
                                    child,
                                    path,
                                    level + 1,
                                    i,
                                    mapKey);
                            result.addAll(matchedViews);
                        }
                    }

                    return result;
                } else if (pathElement.className.equals(CURRENT_CLASS_NAME)) {
                    // Set self as selected element
                    result.add(new MatchedView(view, mapKey));

                    return result;
                }

                if (!isTheSameView(view, pathElement, index)) {
                    return result;
                }

                // Found it!
                if (level == path.size() - 1) {
                    result.add(new MatchedView(view, mapKey));
                }
            }

            if (view instanceof ViewGroup) {
                final ViewGroup viewGroup = (ViewGroup) view;
                List<View> visibleViews = findVisibleChildren(viewGroup);
                final int childCount = visibleViews.size();
                for (int i = 0; i < childCount; i++) {
                    View child = visibleViews.get(i);
                    List<MatchedView> matchedViews = findViewByPath(
                            mapping,
                            child,
                            path,
                            level + 1,
                            i,
                            mapKey);
                    result.addAll(matchedViews);
                }
            }

            return result;
        }

        private static boolean isTheSameView(
                final View targetView,
                final PathComponent pathElement,
                final int index) {
            if (pathElement.index != -1 && index != pathElement.index) {
                return false;
            }

            if (!targetView.getClass().getCanonicalName().equals(pathElement.className)) {
                if (pathElement.className.matches(".*android\\..*")) {
                    String[] names = pathElement.className.split("\\.");
                    if (names.length > 0) {
                        String SimpleName = names[names.length - 1];
                        if (!targetView.getClass().getSimpleName().equals(SimpleName)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            if ((pathElement.matchBitmask
                    & MatchBitmaskType.ID.getValue()) > 0) {
                if (pathElement.id != targetView.getId()) {
                    return false;
                }
            }

            if ((pathElement.matchBitmask
                    & MatchBitmaskType.TEXT.getValue()) > 0) {
                String pathText = pathElement.text;
                String text = ViewHierarchy.getTextOfView(targetView);
                String hashedText = Utility.coerceValueIfNullOrEmpty(
                        Utility.sha256hash(text), "");

                if (!pathText.equals(text) && !pathText.equals(hashedText)) {
                    return false;
                }
            }

            if ((pathElement.matchBitmask
                    & MatchBitmaskType.DESCRIPTION.getValue()) > 0) {
                String pathDesc = pathElement.description;
                String targetDesc = targetView.getContentDescription() == null ? "" :
                        String.valueOf(targetView.getContentDescription());
                String hashedDesc = Utility.coerceValueIfNullOrEmpty(
                        Utility.sha256hash(targetDesc), "");
                if (!pathDesc.equals(targetDesc) && !pathDesc.equals(hashedDesc)) {
                    return false;
                }
            }

            if ((pathElement.matchBitmask
                    & MatchBitmaskType.HINT.getValue()) > 0) {
                String pathHint = pathElement.hint;
                String targetHint = ViewHierarchy.getHintOfView(targetView);
                String hashedHint = Utility.coerceValueIfNullOrEmpty(
                        Utility.sha256hash(targetHint), "");

                if (!pathHint.equals(targetHint) && !pathHint.equals(hashedHint)) {
                    return false;
                }
            }

            if ((pathElement.matchBitmask
                    & MatchBitmaskType.TAG.getValue()) > 0) {
                String tag = pathElement.tag;
                String targetTag = targetView.getTag() == null ? "" :
                        String.valueOf(targetView.getTag());
                String hashedTag = Utility.coerceValueIfNullOrEmpty(
                        Utility.sha256hash(targetTag), "");
                if (!tag.equals(targetTag) && !tag.equals(hashedTag)) {
                    return false;
                }
            }

            return true;
        }

        private static List<View> findVisibleChildren(ViewGroup viewGroup) {
            List<View> visibleViews = new ArrayList<>();
            final int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    visibleViews.add(child);
                }
            }
            return visibleViews;
        }

        private void attachListener(final MatchedView matchedView,
                                    final View rootView,
                                    final EventBinding mapping) {
            if (mapping == null) {
                return;
            }
            try {
                View view = matchedView.getView();
                if (view == null) {
                    return;
                }

                // If it's React Native Button, then attach React Native OnTouchListener
                View RCTRootView = ViewHierarchy.findRCTRootView(view);
                if (null != RCTRootView && ViewHierarchy.isRCTButton(view, RCTRootView)) {
                    attachRCTListener(matchedView, rootView, RCTRootView, mapping);
                    return;
                }

                // Skip if the view comes from React Native
                if (view.getClass().getName().startsWith("com.facebook.react")) {
                    return;
                }

                final String mapKey = matchedView.getViewMapKey();
                View.OnClickListener existingListener =
                        ViewHierarchy.getExistingOnClickListener(view);
                boolean listenerExists = existingListener != null;
                boolean isCodelessListener = listenerExists && existingListener instanceof
                        CodelessLoggingEventListener.AutoLoggingOnClickListener;
                boolean listenerSupportCodelessLogging = isCodelessListener &&
                        ((CodelessLoggingEventListener.AutoLoggingOnClickListener)
                                existingListener).getSupportCodelessLogging();
                if (!this.listenerSet.contains(mapKey) &&
                        (!listenerExists ||
                                !isCodelessListener || !listenerSupportCodelessLogging)) {
                    View.OnClickListener listener =
                            CodelessLoggingEventListener.getOnClickListener(
                                    mapping, rootView, view);
                    view.setOnClickListener(listener);
                    this.listenerSet.add(mapKey);
                }
            } catch (FacebookException e) {
                Log.e(TAG, "Failed to attach auto logging event listener.", e);
            }
        }

        private void attachRCTListener(final MatchedView matchedView,
                                       final View rootView,
                                       final View RCTRootView,
                                       final EventBinding mapping){
            if (mapping == null) {
                return;
            }
            View view = matchedView.getView();
            if (view == null || !ViewHierarchy.isRCTButton(view, RCTRootView)) {
                return;
            }

            final String mapKey = matchedView.getViewMapKey();
            View.OnTouchListener existingListener =
                    ViewHierarchy.getExistingOnTouchListener(view);
            boolean listenerExists = existingListener != null;
            boolean isCodelessListener = listenerExists && existingListener instanceof
                    RCTCodelessLoggingEventListener.AutoLoggingOnTouchListener;
            boolean listenerSupportCodelessLogging = isCodelessListener &&
                    ((RCTCodelessLoggingEventListener.AutoLoggingOnTouchListener)
                            existingListener).getSupportCodelessLogging();
            if (!this.listenerSet.contains(mapKey) &&
                    (!listenerExists ||
                            !isCodelessListener || !listenerSupportCodelessLogging)) {
                View.OnTouchListener listener =
                        RCTCodelessLoggingEventListener.getOnTouchListener(
                                mapping, rootView, view);
                view.setOnTouchListener(listener);
                this.listenerSet.add(mapKey);
            }
        }
    }
}
