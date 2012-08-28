/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.facebook.android.R;

import java.util.HashSet;
import java.util.Set;

abstract class GraphObjectListFragment<T extends GraphObject> extends Fragment {
    private static final String WORKER_FRAGMENT_TAG = "com.facebook.GraphObjectListFragment.WorkerFragmentTag";

    private final String cacheIdentity;
    private final Class<T> graphObjectClass;
    protected Session session;
    private boolean trackActiveSession;
    private OnErrorListener onErrorListener;

    private OnDataChangedListener onDataChangedListener;
    protected WorkerFragment<T> workerFragment;
    private boolean showProfilePicture;

    public interface OnErrorListener {
        void onError(FacebookException error);
    }

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    protected GraphObjectListFragment(String cacheIdentity, Class<T> graphObjectClass) {
        this.cacheIdentity = cacheIdentity;
        this.graphObjectClass = graphObjectClass;
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.GraphObjectListFragment);

        setShowProfilePicture(a.getBoolean(R.styleable.GraphObjectListFragment_showProfilePicture, true));

        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO need to make this customizable?
        View view = inflater.inflate(R.layout.friend_picker_fragment, container, false);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick((ListView) parent, v, position);
            }
        });

        if (savedInstanceState != null) {
            // TODO restore state of UI
        }

        return view;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        if (savedInstanceState != null && savedInstanceState.containsKey(WORKER_FRAGMENT_TAG)) {
            // Are we being re-created? If so, we want to re-attach to an existing worker fragment.
            String workerFragmentTag = savedInstanceState.getString(WORKER_FRAGMENT_TAG);
            workerFragment = (WorkerFragment<T>) fm.findFragmentByTag(workerFragmentTag);
        }
        if (workerFragment == null) {
            // Create a new worker fragment, point it at us, and add it to the FragmentManager.
            workerFragment = new WorkerFragment<T>(cacheIdentity, graphObjectClass);
            workerFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(workerFragment, workerFragment.getTagString()).commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(WORKER_FRAGMENT_TAG, workerFragment.getTag());

        // TODO save state of UI
    }

    public OnDataChangedListener getOnDataChangedListener() {
        return onDataChangedListener;
    }

    public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
        this.onDataChangedListener = onDataChangedListener;
        if (workerFragment != null) {
            workerFragment.setOnDataChangedListener(onDataChangedListener);
        }
    }

    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
        if (workerFragment != null) {
            workerFragment.setOnErrorListener(onErrorListener);
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        // TODO port: handle active session
        this.session = session;

        trackActiveSession = (session == null);
    }

    public boolean getShowProfilePicture() {
        return showProfilePicture;
    }

    public void setShowProfilePicture(boolean showProfilePicture) {
        this.showProfilePicture = showProfilePicture;
        if (workerFragment != null) {
            workerFragment.getAdapter().setShowProfilePicture(showProfilePicture);
        }
    }

    public void loadData() {
        if (session == null ||
                (trackActiveSession == true && session == Session.getActiveSession())) {
            setSession(Session.getActiveSession());
            trackActiveSession = true;
        }

        loadDataSkippingRoundTripIfCached();
    }

    Set<T> getSelectedGraphObjects() {
        // TODO consider throwing if workerFragment is null
        if (workerFragment != null) {
            return workerFragment.getSelectedGraphObjects();
        }
        return new HashSet<T>();
    }

    private void onListItemClick(ListView listView, View v, int position) {
        @SuppressWarnings("unchecked")
        T graphObject = (T) listView.getItemAtPosition(position);
        workerFragment.getAdapter().toggleSelection(graphObject, v);
    }

    private void loadDataSkippingRoundTripIfCached() {
        workerFragment.getAdapter().clear();

        if (session != null) {
            workerFragment.startLoading(getRequestForLoadData(), true);
        }
    }

    protected abstract Request getRequestForLoadData();

    // Because loading the picker data can be a long-running operation, we want to retain it across Activity
    // instances. We separate the loader/adapter into a separate UI-less Fragment that we can retain via
    // setRetainInstance(), which we don't want to use in the primary Fragment because it would prevent that
    // Fragment from participating in the back stack. This class relies on getTargetFragment() to reference the
    // appropriate UI elements.
    static class WorkerFragment<T extends GraphObject> extends Fragment {
        private final static Object staticSyncObject = new Object();
        private static int nextTag = 0;

        private final String cacheIdentity;
        private final Class<T> graphObjectClass;
        private final int tag;
        private ListView listView;
        private ProgressBar activityCircle;
        private GraphObjectAdapter<T> adapter;
        private GraphObjectPagingLoader<T> loader;
        private GraphObjectListFragment<T> targetFragment;
        private OnDataChangedListener onDataChangedListener;
        private OnErrorListener onErrorListener;
        private ListView.OnScrollListener onScrollListener;

        WorkerFragment(String cacheIdentity, Class<T> graphObjectClass) {
            synchronized (staticSyncObject) {
                tag = nextTag++;
            }
            this.graphObjectClass = graphObjectClass;
            this.cacheIdentity = cacheIdentity;
        }

        String getTagString() {
            return String.format("%s-%d", getClass().getCanonicalName(), tag);
        }

        GraphObjectAdapter<T> getAdapter() {
            return adapter;
        }

        public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
            this.onDataChangedListener = onDataChangedListener;
        }

        public void setOnErrorListener(OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // TODO comment
            setRetainInstance(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            targetFragment = (GraphObjectListFragment<T>) getTargetFragment();
            if (targetFragment == null) {
                // We don't ever expect this, but just in case, do nothing.
                return;
            }

            if (adapter == null) {
                adapter = createAdapter();
            }

            onDataChangedListener = targetFragment.getOnDataChangedListener();
            onErrorListener = targetFragment.getOnErrorListener();
            adapter.setShowProfilePicture(targetFragment.getShowProfilePicture());

            listView = (ListView) targetFragment.getView().findViewById(R.id.listView);
            listView.setAdapter(adapter);
            listView.setOnScrollListener(onScrollListener);

            activityCircle = (ProgressBar) targetFragment.getView().findViewById(R.id.activity_circle);

            if (loader == null) {
                loader = new GraphObjectPagingLoader<T>(cacheIdentity, GraphObjectPagingLoader.PagingMode.IMMEDIATE,
                        graphObjectClass);
                loader.setCallback(pagingLoaderCallback);
            }
        }

        @Override
        public void onDetach() {
            listView.setAdapter(null);
            listView.setOnScrollListener(null);

            listView = null;
            activityCircle = null;
            targetFragment = null;

            adapter.cancelPendingDownloads();

            // TODO pause paging loader if we are not visible

            super.onDetach();
        }

        Set<T> getSelectedGraphObjects() {
            if (adapter != null) {
                return adapter.getSelectedGraphObjects();
            }
            return new HashSet<T>();
        }

        void startLoading(Request requestForLoadData, boolean skipRoundtripIfCached) {
            loader.startLoading(requestForLoadData, skipRoundtripIfCached);
        }

        protected void displayActivityCircle() {
            if (activityCircle != null) {
                layoutActivityCircle();
                activityCircle.setVisibility(View.VISIBLE);
            }
        }

        protected void hideActivityCircle() {
            if (activityCircle != null) {
                // We use an animation to dim the activity circle; need to clear this or it will remain visible.
                activityCircle.clearAnimation();
                activityCircle.setVisibility(View.INVISIBLE);
            }
        }

        private void layoutActivityCircle() {
            // If we've got no data, make the activity circle full-opacity. Otherwise we'll dim it to avoid
            //  cluttering the UI.
            float alpha = (adapter != null && adapter.getCount() > 0) ? .25f : 1.0f;
            Utility.setAlpha(activityCircle, alpha);
        }

        private void reprioritizeDownloads() {
            int firstVisibleItem = listView.getFirstVisiblePosition();
            int lastVisibleItem = listView.getLastVisiblePosition();

            if (lastVisibleItem >= 0) {
                int visibleItemCount = lastVisibleItem + 1 - firstVisibleItem;
                adapter.prioritizeViewRange(firstVisibleItem, visibleItemCount);
            }
        }

        private GraphObjectAdapter<T> createAdapter() {
            return new GraphObjectAdapter<T>(this.getActivity()) {
            };
        }

        private void addResultsToAdapter(GraphObjectList<T> results) {
            if (listView != null) {
                // As we fetch additional results and add them to the table, we do not
                // want the items displayed jumping around seemingly at random, frustrating the user's
                // attempts at scrolling, etc. Since results may be added anywhere in
                // the table, we choose to try to keep the first visible row in a fixed
                // position (from the user's perspective). We try to keep it positioned at
                // the same offset from the top of the screen so adding new items seems
                // smoother, as opposed to having it "snap" to a multiple of row height

                // We use the second row, to give context above and below it and avoid
                // cases where the first row is only barely visible, thus providing little context.
                // The exception is where the very first row is visible, in which case we use that.
                View view = listView.getChildAt(1);
                int anchorPosition = listView.getFirstVisiblePosition();
                if (anchorPosition > 0) {
                    anchorPosition++;
                }
                Pair<String, T> anchorItem = adapter.getSectionAndItem(anchorPosition);
                final int top = (view != null && anchorItem != null) ? view.getTop() : 0;

                // Now actually add the results.
                adapter.add(results);

                if (view != null && anchorItem != null) {
                    // Put the item back in the same spot it was.
                    final int newPositionOfItem = adapter.getPosition(anchorItem.first, anchorItem.second);
                    if (newPositionOfItem != -1) {
                        listView.setSelectionFromTop(newPositionOfItem, top);
                    }
                }
            } else {
                // We have no UI, just add the results.
                adapter.add(results);
            }

            // TODO port: log perf
            if (onDataChangedListener != null) {
                onDataChangedListener.onDataChanged();
            }
        }

        // Implementation of GraphObjectPagingLoader.Callback
        private final GraphObjectPagingLoader.Callback<T> pagingLoaderCallback = new GraphObjectPagingLoader.Callback<T>() {
            @Override
            public void onLoading(String url, GraphObjectPagingLoader loader) {
                displayActivityCircle();
            }

            @Override
            public void onLoaded(final GraphObjectList<T> results, GraphObjectPagingLoader loader) {
                addResultsToAdapter(results);
            }

            @Override
            public void onFinishedLoadingData(GraphObjectPagingLoader loader) {
                hideActivityCircle();

                if (onDataChangedListener != null) {
                    onDataChangedListener.onDataChanged();
                }

                // TODO port: if cached, kick off refresh
            }

            @Override
            public void onError(FacebookException error, GraphObjectPagingLoader loader) {
                hideActivityCircle();
                if (onErrorListener != null) {
                    onErrorListener.onError(error);
                }
            }
        };

        private class ScrollListener implements ListView.OnScrollListener {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == ListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    reprioritizeDownloads();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        }
    }
}
