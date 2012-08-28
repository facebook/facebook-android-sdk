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

import java.util.Set;

abstract class GraphObjectListFragment<T extends GraphObject> extends Fragment implements PickerFragment<T> {
    private static final String WORKER_FRAGMENT_TAG = "com.facebook.GraphObjectListFragment.WorkerFragmentTag";

    private final String cacheIdentity;
    private final Class<T> graphObjectClass;
    private boolean trackActiveSession;
    private OnErrorListener onErrorListener;

    private OnDataChangedListener onDataChangedListener;

    private OnSelectionChangedListener onSelectionChangedListener;
    private GraphObjectPagingLoader.PagingMode pagingMode;
    private boolean showPictures = true;
    private int layout;
    private Session session;

    WorkerFragment<T> workerFragment;

    GraphObjectListFragment(String cacheIdentity, Class<T> graphObjectClass,
            GraphObjectPagingLoader.PagingMode pagingMode, int layout, Bundle args) {
        this.cacheIdentity = cacheIdentity;
        this.graphObjectClass = graphObjectClass;
        this.pagingMode = pagingMode;
        this.layout = layout;

        setGraphObjectListFragmentSettingsFromBundle(args);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.GraphObjectListFragment);

        setShowPictures(a.getBoolean(R.styleable.GraphObjectListFragment_show_pictures, showPictures));

        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layout, container, false);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick((ListView) parent, v, position);
            }
        });
        listView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // We don't actually do anything differently on long-clicks, but setting the listener
                // enables the selector transition that we have for visual consistency with the
                // Facebook app's pickers.
                return false;
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
            workerFragment = new WorkerFragment<T>(cacheIdentity, createAdapter(), graphObjectClass);
            workerFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(workerFragment, workerFragment.getTagString()).commit();
        }

        setSettingsFromBundle(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(WORKER_FRAGMENT_TAG, workerFragment.getTag());

        saveSettingsToBundle(outState);
        // TODO save state of UI
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setSettingsFromBundle(args);
    }

    @Override
    public OnDataChangedListener getOnDataChangedListener() {
        return onDataChangedListener;
    }

    @Override
    public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
        this.onDataChangedListener = onDataChangedListener;
        if (workerFragment != null) {
            workerFragment.setOnDataChangedListener(onDataChangedListener);
        }
    }

    @Override
    public OnSelectionChangedListener getOnSelectionChangedListener() {
        return onSelectionChangedListener;
    }

    @Override
    public void setOnSelectionChangedListener(
            OnSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListener = onSelectionChangedListener;
    }


    @Override
    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
        if (workerFragment != null) {
            workerFragment.setOnErrorListener(onErrorListener);
        }
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void setSession(Session session) {
        // TODO port: handle active session
        this.session = session;

        trackActiveSession = (session == null);
    }

    @Override
    public boolean getShowPictures() {
        return showPictures;
    }

    @Override
    public void setShowPictures(boolean showPictures) {
        this.showPictures = showPictures;
        if (workerFragment != null) {
            workerFragment.getAdapter().setShowPicture(showPictures);
        }
    }

    @Override
    public void loadData(boolean forceReload) {
        validateWorkerFragmentCreated();

        // TODO: this is inefficient if a user changes orientation and continually restarts a lengthy query.
        // Make this dependent on whether or not a query is in progress, not on whether we have gotten results.
        if (!forceReload && workerFragment.getAdapter().getCount() > 0) {
            return;
        }

        if (session == null ||
                (trackActiveSession == true && session == Session.getActiveSession())) {
            setSession(Session.getActiveSession());
            trackActiveSession = true;
        }

        loadDataSkippingRoundTripIfCached();
    }

    @Override
    public void setSettingsFromBundle(Bundle inState) {
        setGraphObjectListFragmentSettingsFromBundle(inState);
    }

    Set<T> getSelectedGraphObjects() {
        validateWorkerFragmentCreated();

        return workerFragment.getSelectedGraphObjects();
    }

    void saveSettingsToBundle(Bundle outState) {
        outState.putBoolean(SHOW_PICTURES_BUNDLE_KEY, showPictures);
    }

    abstract Request getRequestForLoadData();

    abstract GraphObjectAdapter<T> createAdapter();

    GraphObjectPagingLoader.PagingMode getPagingMode() {
        return pagingMode;
    }

    private void setGraphObjectListFragmentSettingsFromBundle(Bundle inState) {
        // We do this in a separate non-overridable method so it is safe to call from the constructor.
        if (inState != null) {
            showPictures = inState.getBoolean(SHOW_PICTURES_BUNDLE_KEY, showPictures);
        }
    }

    private void validateWorkerFragmentCreated() {
        if (workerFragment == null) {
            throw new FacebookException("Illegal to call loadData() before the fragment has been created.");
        }
    }

    private void onListItemClick(ListView listView, View v, int position) {
        @SuppressWarnings("unchecked")
        T graphObject = (T) listView.getItemAtPosition(position);
        workerFragment.getAdapter().toggleSelection(graphObject, listView);

        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }
    }

    private void loadDataSkippingRoundTripIfCached() {
        workerFragment.getAdapter().clear();

        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }

        if (session != null) {
            workerFragment.startLoading(getRequestForLoadData(), true);
        }
    }

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

        WorkerFragment(String cacheIdentity, GraphObjectAdapter<T> adapter, Class<T> graphObjectClass) {
            Validate.notNullOrEmpty(cacheIdentity, "cacheIdentity");
            Validate.notNull(adapter, "adapter");
            Validate.notNull(graphObjectClass, "graphObjectClass");

            synchronized (staticSyncObject) {
                tag = nextTag++;
            }
            this.adapter = adapter;
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
            // We want the WorkerFragment to persist across its parent activity(/ies) coming and going.
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

            onDataChangedListener = targetFragment.getOnDataChangedListener();
            onErrorListener = targetFragment.getOnErrorListener();
            adapter.setShowPicture(targetFragment.getShowPictures());

            listView = (ListView) targetFragment.getView().findViewById(R.id.listView);
            listView.setAdapter(adapter);
            listView.setOnScrollListener(onScrollListener);

            activityCircle = (ProgressBar) targetFragment.getView().findViewById(R.id.activity_circle);

            if (loader == null) {
                loader = new GraphObjectPagingLoader<T>(cacheIdentity, targetFragment.getPagingMode(),
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
            return adapter.getSelectedGraphObjects();
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
            float alpha = (adapter.getCount() > 0) ? .25f : 1.0f;
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
                // If we are getting results as-needed, hide the activity circle as soon as we get
                // results.
                if (targetFragment == null ||
                        targetFragment.getPagingMode() == GraphObjectPagingLoader.PagingMode.AS_NEEDED) {
                    hideActivityCircle();
                }
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
