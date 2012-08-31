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
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private OnErrorListener onErrorListener;

    private OnDataChangedListener onDataChangedListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    private GraphObjectFilter<T> filter;
    private GraphObjectPagingLoader.PagingMode pagingMode;
    private boolean showPictures = true;
    private int layout;
    private SessionTracker sessionTracker;

    WorkerFragment<T> workerFragment;
    private ListView listView;

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

        listView = (ListView) view.findViewById(R.id.listView);
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

        return view;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        if (savedInstanceState != null && savedInstanceState.containsKey(WORKER_FRAGMENT_TAG)) {
            // Are we being re-created? If so, we want to re-attach to an existing worker fragment.
            String workerFragmentTag = savedInstanceState.getString(WORKER_FRAGMENT_TAG);
            workerFragment = (WorkerFragment<T>) fm.findFragmentByTag(workerFragmentTag);
        }

        if (sessionTracker == null) {
            sessionTracker = new SessionTracker(getActivity(), null);
        }

        if (workerFragment == null) {
            // Create a new worker fragment, point it at us, and add it to the FragmentManager.
            workerFragment = new WorkerFragment<T>(cacheIdentity, graphObjectClass);
            workerFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(workerFragment, workerFragment.getTagString()).commit();
        }

        setSettingsFromBundle(savedInstanceState);

        // TODO restore state of UI
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
    public GraphObjectFilter<T> getFilter() {
        return filter;
    }

    @Override
    public void setFilter(GraphObjectFilter<T> filter) {
        this.filter = filter;
    }

    @Override
    public Session getSession() {
        return sessionTracker.getSession();
    }

    @Override
    public void setSession(Session session) {
        sessionTracker.setSession(session);
        if (workerFragment != null) {
            workerFragment.setSession(session);
        }
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
        if (!forceReload && !workerFragment.getAdapter().isEmpty()) {
            return;
        }

        loadDataSkippingRoundTripIfCached();
    }

    @Override
    public void setSettingsFromBundle(Bundle inState) {
        setGraphObjectListFragmentSettingsFromBundle(inState);
    }

    boolean filterIncludesItem(T graphObject) {
        if (filter != null) {
            return filter.includeItem(graphObject);
        }
        return true;
    }

    Set<T> getSelectedGraphObjects() {
        validateWorkerFragmentCreated();

        return workerFragment.getSelectedGraphObjects();
    }

    void saveSettingsToBundle(Bundle outState) {
        outState.putBoolean(SHOW_PICTURES_BUNDLE_KEY, showPictures);
    }

    abstract Request getRequestForLoadData(Session session);

    abstract GraphObjectAdapter<T> createAdapter();

    GraphObjectPagingLoader.PagingMode getPagingMode() {
        return pagingMode;
    }

    void onLoadingData() {
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
        onLoadingData();

        workerFragment.getAdapter().clear();

        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }

        Request request = getRequestForLoadData(getSession());
        if (request != null) {
            workerFragment.startLoading(request, true);
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
        private boolean attached = false;

        WorkerFragment(String cacheIdentity, Class<T> graphObjectClass) {
            Validate.notNullOrEmpty(cacheIdentity, "cacheIdentity");
            Validate.notNull(graphObjectClass, "graphObjectClass");

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

        public void setSession(Session session) {
            if (session != sessionTracker.getSession()) {
                clearResults();
            }
            sessionTracker.setSession(session);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            targetFragment = (GraphObjectListFragment<T>) getTargetFragment();
            if (targetFragment == null) {
                throw new FacebookException(
                        "GraphObjectListFragment.WorkerFragment created without a target fragment.");
            }

            adapter = targetFragment.createAdapter();

            loader = new GraphObjectPagingLoader<T>(cacheIdentity, targetFragment.getPagingMode(),
                    graphObjectClass);
            loader.setCallback(pagingLoaderCallback);

            // If we are in AS_NEEDED paging mode, set up a listener to handle requests for data from the adapter.
            if (targetFragment.getPagingMode() == GraphObjectPagingLoader.PagingMode.AS_NEEDED) {
                adapter.setDataNeededListener(new AdapterDataNeededListener());
            }

            // We want the WorkerFragment to persist across its parent activity(/ies) coming and going.
            setRetainInstance(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            targetFragment = (GraphObjectListFragment<T>) getTargetFragment();
            if (targetFragment == null) {
                throw new FacebookException(
                        "GraphObjectListFragment.WorkerFragment created without a target fragment.");
            }

            // Make sure we're using the right listeners and other settings for our new target fragment.
            onDataChangedListener = targetFragment.getOnDataChangedListener();
            onErrorListener = targetFragment.getOnErrorListener();

            adapter.setShowPicture(targetFragment.getShowPictures());
            adapter.setFilter(new GraphObjectAdapter.Filter<T>() {
                @Override
                public boolean includeItem(T graphObject) {
                    return targetFragment.filterIncludesItem(graphObject);
                }
            });

            // Find the UI elements we need in the new target fragment.
            listView = (ListView) targetFragment.getView().findViewById(R.id.listView);
            listView.setAdapter(adapter);
            listView.setOnScrollListener(onScrollListener);
            activityCircle = (ProgressBar) targetFragment.getView().findViewById(R.id.activity_circle);

            // Tell the adapter to re-filter/rebuild sections, as the filter or data may have changed.
            adapter.rebuildAndNotify();
            // Tell the loader to resume following next-links if it should.
            loader.resume();

            attached = true;
        }

        @Override
        public void onDetach() {
            attached = false;

            loader.pause();

            listView.setAdapter(null);
            listView.setOnScrollListener(null);

            listView = null;
            activityCircle = null;
            targetFragment = null;

            adapter.cancelPendingDownloads();
            // Forget about the filter. We'll re-filter items when we are re-attached.
            adapter.setFilter(null);

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
            float alpha = (!adapter.isEmpty()) ? .25f : 1.0f;
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
            if (attached) {
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
                GraphObjectAdapter.SectionAndItem<T> anchorItem = adapter.getSectionAndItem(anchorPosition);
                final int top = (view != null &&
                        anchorItem.getType() != GraphObjectAdapter.SectionAndItem.Type.ACTIVITY_CIRCLE) ?
                        view.getTop() : 0;

                // Now actually add the results.
                adapter.add(results, true);

                if (view != null && anchorItem != null) {
                    // Put the item back in the same spot it was.
                    final int newPositionOfItem = adapter.getPosition(anchorItem.sectionKey, anchorItem.graphObject);
                    if (newPositionOfItem != -1) {
                        listView.setSelectionFromTop(newPositionOfItem, top);
                    }
                }
            } else {
                // We have no UI, just add the results.
                adapter.add(results, false);
            }

            // TODO port: log perf
            if (onDataChangedListener != null) {
                onDataChangedListener.onDataChanged();
            }
        }

        private void clearResults() {
            if (adapter != null) {
                boolean selection = adapter.getSelectedGraphObjects().size() > 0;
                boolean data = !adapter.isEmpty();

                adapter.clear();

                // Tell anyone who cares the data and possibly selection has changed.
                if (data && onDataChangedListener != null) {
                    onDataChangedListener.onDataChanged();
                }
                if (selection && targetFragment != null && targetFragment.getOnSelectionChangedListener() != null) {
                    targetFragment.getOnSelectionChangedListener().onSelectionChanged();
                }
            }
        }

        // Implementation of GraphObjectPagingLoader.Callback
        private final GraphObjectPagingLoader.Callback<T> pagingLoaderCallback = new GraphObjectPagingLoader.Callback<T>() {
            @Override
            public void onLoading(String url, GraphObjectPagingLoader loader) {
                // Show the activity circle all the time in IMMEDIATE paging mode, or only if we are empty in
                // AS_NEEDED (otherwise we display a smaller activity circle in the last cell of the list, which
                // is handeld by GraphObjectAdapter).
                if (targetFragment != null &&
                        (targetFragment.getPagingMode() == GraphObjectPagingLoader.PagingMode.IMMEDIATE ||
                                adapter.isEmpty())) {
                    displayActivityCircle();
                }
            }

            @Override
            public void onLoaded(final GraphObjectList<T> results, GraphObjectPagingLoader loader) {
                // If we are getting results as-needed, hide the activity circle as soon as we get
                // results.
                if (targetFragment != null &&
                        targetFragment.getPagingMode() == GraphObjectPagingLoader.PagingMode.AS_NEEDED) {
                    hideActivityCircle();
                }

                addResultsToAdapter(results);
            }

            @Override
            public void onFinishedLoadingData(GraphObjectPagingLoader loader) {
                hideActivityCircle();
                adapter.onDoneLoadingResults();

                if (onDataChangedListener != null) {
                    onDataChangedListener.onDataChanged();
                }

                // TODO port: if cached, kick off refresh
            }

            @Override
            public void onError(final FacebookException error, GraphObjectPagingLoader loader) {
                hideActivityCircle();
                if (onErrorListener != null) {
                    onErrorListener.onError(error);
                }
            }
        };

        private final SessionTracker sessionTracker = new SessionTracker(getActivity(),
                new Session.StatusCallback() {
                    @Override
                    public void call(Session session, SessionState state, Exception exception) {
                        if (!session.getIsOpened()) {
                            // When a session is closed, we want to clear out our data so the user isn't seeing
                            // data that might be a privacy issue.
                            clearResults();
                        }
                    }
                });

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

        private class AdapterDataNeededListener implements GraphObjectAdapter.DataNeededListener {
            @Override
            public void onDataNeeded() {
                loader.followNextLink();
            }
        }
    }
}
