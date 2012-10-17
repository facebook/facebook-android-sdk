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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.*;
import com.facebook.android.R;

import java.util.*;

abstract class GraphObjectListFragment<T extends GraphObject> extends Fragment
        implements PickerFragment<T>, LoaderManager.LoaderCallbacks<SimpleGraphObjectCursor<T>> {
    private static final String SELECTION_BUNDLE_KEY = "com.facebook.android.GraphObjectListFragment.Selection";

    private final int layout;
    private OnErrorListener onErrorListener;
    private OnDataChangedListener onDataChangedListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    private OnDoneButtonClickedListener onDoneButtonClickedListener;
    private GraphObjectFilter<T> filter;
    private boolean showPictures = true;
    private boolean showTitleBar = true;
    private ListView listView;
    HashSet<String> extraFields = new HashSet<String>();
    GraphObjectAdapter<T> adapter;
    private final Class<T> graphObjectClass;
    private LoadingStrategy loadingStrategy;
    private SelectionStrategy selectionStrategy;
    private ProgressBar activityCircle;
    private SessionTracker sessionTracker;
    private String titleText;
    private String doneButtonText;
    private TextView titleTextView;
    private Button doneButton;
    private Drawable titleBarBackground;
    private Drawable doneButtonBackground;

    GraphObjectListFragment(Class<T> graphObjectClass, int layout, Bundle args) {
        this.graphObjectClass = graphObjectClass;
        this.layout = layout;

        setGraphObjectListFragmentSettingsFromBundle(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = createAdapter();
        adapter.setFilter(new GraphObjectAdapter.Filter<T>() {
            @Override
            public boolean includeItem(T graphObject) {
                return filterIncludesItem(graphObject);
            }
        });
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.com_facebook_picker_fragment);

        setShowPictures(a.getBoolean(R.styleable.com_facebook_picker_fragment_show_pictures, showPictures));
        String extraFieldsString = a.getString(R.styleable.com_facebook_picker_fragment_extra_fields);
        if (extraFieldsString != null) {
            String[] strings = extraFieldsString.split(",");
            setExtraFields(Arrays.asList(strings));
        }

        showTitleBar = a.getBoolean(R.styleable.com_facebook_picker_fragment_show_title_bar, showTitleBar);
        titleText = a.getString(R.styleable.com_facebook_picker_fragment_title_text);
        doneButtonText = a.getString(R.styleable.com_facebook_picker_fragment_done_button_text);
        titleBarBackground = a.getDrawable(R.styleable.com_facebook_picker_fragment_title_bar_background);
        doneButtonBackground = a.getDrawable(R.styleable.com_facebook_picker_fragment_done_button_background);

        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(layout, container, false);

        listView = (ListView) view.findViewById(R.id.com_facebook_picker_list_view);
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
        listView.setOnScrollListener(onScrollListener);
        listView.setAdapter(adapter);

        activityCircle = (ProgressBar) view.findViewById(R.id.com_facebook_picker_activity_circle);

        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sessionTracker = new SessionTracker(getActivity(), new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (!session.isOpened()) {
                    // When a session is closed, we want to clear out our data so it is not visible to subsequent users
                    clearResults();
                }
            }
        });

        setSettingsFromBundle(savedInstanceState);

        loadingStrategy = createLoadingStrategy();
        loadingStrategy.attach(adapter);

        selectionStrategy = createSelectionStrategy();
        selectionStrategy.readSelectionFromBundle(savedInstanceState, SELECTION_BUNDLE_KEY);

        // Should we display a title bar? (We need to do this after we've retrieved our bundle settings.)
        if (showTitleBar) {
            inflateTitleBar((ViewGroup) getView());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listView.setOnScrollListener(null);
        listView.setAdapter(null);

        loadingStrategy.detach();
        sessionTracker.stopTracking();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveSettingsToBundle(outState);
        selectionStrategy.saveSelectionToBundle(outState, SELECTION_BUNDLE_KEY);
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
    public OnDoneButtonClickedListener getOnDoneButtonClickedListener() {
        return onDoneButtonClickedListener;
    }

    @Override
    public void setOnDoneButtonClickedListener(OnDoneButtonClickedListener onDoneButtonClickedListener) {
        this.onDoneButtonClickedListener = onDoneButtonClickedListener;
    }

    @Override
    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
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
    }

    @Override
    public boolean getShowPictures() {
        return showPictures;
    }

    @Override
    public void setShowPictures(boolean showPictures) {
        this.showPictures = showPictures;
    }

    public Set<String> getExtraFields() {
        return new HashSet<String>(extraFields);
    }

    public void setExtraFields(Collection<String> fields) {
        extraFields = new HashSet<String>();
        if (fields != null) {
            extraFields.addAll(fields);
        }
    }

    @Override
    public void loadData(boolean forceReload) {
        if (!forceReload && !adapter.isEmpty()) {
            return;
        }

        loadDataSkippingRoundTripIfCached();
    }

    @Override
    public void setSettingsFromBundle(Bundle inState) {
        setGraphObjectListFragmentSettingsFromBundle(inState);
    }

    @Override
    public GraphObjectPagingLoader<T> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        return new GraphObjectPagingLoader<T>(getActivity(), graphObjectClass);
    }

    @Override
    public void onLoaderReset(Loader<SimpleGraphObjectCursor<T>> loader) {
        adapter.changeCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<SimpleGraphObjectCursor<T>> loader, SimpleGraphObjectCursor<T> data) {
        adapter.changeCursor(data);
    }

    boolean filterIncludesItem(T graphObject) {
        if (filter != null) {
            return filter.includeItem(graphObject);
        }
        return true;
    }

    List<T> getSelectedGraphObjects() {
        return adapter.getGraphObjectsById(selectionStrategy.getSelectedIds());
    }

    void saveSettingsToBundle(Bundle outState) {
        outState.putBoolean(SHOW_PICTURES_BUNDLE_KEY, showPictures);
        if (!extraFields.isEmpty()) {
            outState.putString(EXTRA_FIELDS_BUNDLE_KEY, TextUtils.join(",", extraFields));
        }
        outState.putBoolean(SHOW_TITLE_BAR_BUNDLE_KEY, showTitleBar);
        outState.putString(TITLE_TEXT_BUNDLE_KEY, titleText);
        outState.putString(DONE_BUTTON_TEXT_BUNDLE_KEY, doneButtonText);
    }

    abstract Request getRequestForLoadData(Session session);

    abstract GraphObjectListFragmentAdapter<T> createAdapter();

    abstract LoadingStrategy createLoadingStrategy();

    abstract SelectionStrategy createSelectionStrategy();

    void onLoadingData() {
    }

    String getDefaultTitleText() {
        return null;
    }

    String getDefaultDoneButtonText() {
        return getString(R.string.com_facebook_picker_done_button_text);
    }

    void displayActivityCircle() {
        if (activityCircle != null) {
            layoutActivityCircle();
            activityCircle.setVisibility(View.VISIBLE);
        }
    }

    void layoutActivityCircle() {
        // If we've got no data, make the activity circle full-opacity. Otherwise we'll dim it to avoid
        //  cluttering the UI.
        float alpha = (!adapter.isEmpty()) ? .25f : 1.0f;
        Utility.setAlpha(activityCircle, alpha);
    }

    void hideActivityCircle() {
        if (activityCircle != null) {
            // We use an animation to dim the activity circle; need to clear this or it will remain visible.
            activityCircle.clearAnimation();
            activityCircle.setVisibility(View.INVISIBLE);
        }
    }

    void setSelectionStrategy(SelectionStrategy selectionStrategy) {
        if (selectionStrategy != this.selectionStrategy) {
            this.selectionStrategy = selectionStrategy;
            if (adapter != null) {
                // Adapter should cause a re-render.
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setGraphObjectListFragmentSettingsFromBundle(Bundle inState) {
        // We do this in a separate non-overridable method so it is safe to call from the constructor.
        if (inState != null) {
            showPictures = inState.getBoolean(SHOW_PICTURES_BUNDLE_KEY, showPictures);
            String extraFieldsString = inState.getString(EXTRA_FIELDS_BUNDLE_KEY);
            if (extraFieldsString != null) {
                String[] strings = extraFieldsString.split(",");
                setExtraFields(Arrays.asList(strings));
            }
            showTitleBar = inState.getBoolean(SHOW_TITLE_BAR_BUNDLE_KEY, showTitleBar);
            String titleTextString = inState.getString(TITLE_TEXT_BUNDLE_KEY);
            if (titleTextString != null) {
                titleText = titleTextString;
                if (titleTextView != null) {
                    titleTextView.setText(titleText);
                }
            }
            String doneButtonTextString = inState.getString(DONE_BUTTON_TEXT_BUNDLE_KEY);
            if (doneButtonTextString != null) {
                doneButtonText = doneButtonTextString;
                if (doneButton != null) {
                    doneButton.setText(doneButtonText);
                }
            }
        }
    }

    private void inflateTitleBar(ViewGroup view) {
        ViewStub stub = (ViewStub) view.findViewById(R.id.com_facebook_picker_title_bar_stub);
        if (stub != null) {
            View titleBar = stub.inflate();

            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);
            layoutParams.addRule(RelativeLayout.BELOW, R.id.com_facebook_picker_title_bar);
            listView.setLayoutParams(layoutParams);

            if (titleBarBackground != null) {
                titleBar.setBackgroundDrawable(titleBarBackground);
            }

            doneButton = (Button) view.findViewById(R.id.com_facebook_picker_done_button);
            if (doneButton != null) {
                doneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onDoneButtonClickedListener != null) {
                            onDoneButtonClickedListener.onDoneButtonClicked();
                        }
                    }
                });

                if (doneButtonText == null) {
                    doneButtonText = getDefaultDoneButtonText();
                }
                if (doneButtonText != null) {
                    doneButton.setText(doneButtonText);
                }

                if (doneButtonBackground != null) {
                    doneButton.setBackgroundDrawable(doneButtonBackground);
                }
            }

            titleTextView = (TextView) view.findViewById(R.id.com_facebook_picker_title);
            if (titleTextView != null) {
                if (titleText == null) {
                    titleText = getDefaultTitleText();
                }
                if (titleText != null) {
                    titleTextView.setText(titleText);
                }
            }
        }
    }

    private void onListItemClick(ListView listView, View v, int position) {
        @SuppressWarnings("unchecked")
        T graphObject = (T) listView.getItemAtPosition(position);
        String id = adapter.getIdOfGraphObject(graphObject);
        selectionStrategy.toggleSelection(id);
        adapter.notifyDataSetChanged();

        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }
    }

    private void loadDataSkippingRoundTripIfCached() {
        clearResults();

        Request request = getRequestForLoadData(getSession());
        if (request != null) {
            onLoadingData();
            loadingStrategy.startLoading(request);
        }
    }

    private void clearResults() {
        if (adapter != null) {
            boolean wasSelection = !selectionStrategy.isEmpty();
            boolean wasData = !adapter.isEmpty();

            loadingStrategy.clearResults();
            selectionStrategy.clear();
            adapter.notifyDataSetChanged();

            // Tell anyone who cares the data and selection has changed, if they have.
            if (wasData && onDataChangedListener != null) {
                onDataChangedListener.onDataChanged();
            }
            if (wasSelection && onSelectionChangedListener != null) {
                onSelectionChangedListener.onSelectionChanged();
            }
        }
    }

    void updateAdapter(SimpleGraphObjectCursor<T> data) {
        if (adapter != null) {
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
            boolean dataChanged = adapter.changeCursor(data);

            if (view != null && anchorItem != null) {
                // Put the item back in the same spot it was.
                final int newPositionOfItem = adapter.getPosition(anchorItem.sectionKey, anchorItem.graphObject);
                if (newPositionOfItem != -1) {
                    listView.setSelectionFromTop(newPositionOfItem, top);
                }
            }

            if (dataChanged && onDataChangedListener != null) {
                onDataChangedListener.onDataChanged();
            }
        }
    }

    private void reprioritizeDownloads() {
        int firstVisibleItem = listView.getFirstVisiblePosition();
        int lastVisibleItem = listView.getLastVisiblePosition();

        if (lastVisibleItem >= 0) {
            int visibleItemCount = lastVisibleItem + 1 - firstVisibleItem;
            adapter.prioritizeViewRange(firstVisibleItem, visibleItemCount);
        }
    }

    private ListView.OnScrollListener onScrollListener = new ListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == ListView.OnScrollListener.SCROLL_STATE_IDLE) {
                reprioritizeDownloads();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    abstract class LoadingStrategy {
        protected final static int CACHED_RESULT_REFRESH_DELAY = 2 * 1000;

        protected GraphObjectPagingLoader<T> loader;
        protected GraphObjectAdapter<T> adapter;

        public void attach(GraphObjectAdapter<T> adapter) {
            loader = (GraphObjectPagingLoader<T>) getLoaderManager().initLoader(0, null,
                    new LoaderManager.LoaderCallbacks<SimpleGraphObjectCursor<T>>() {
                        @Override
                        public Loader<SimpleGraphObjectCursor<T>> onCreateLoader(int id, Bundle args) {
                            return LoadingStrategy.this.onCreateLoader();
                        }

                        @Override
                        public void onLoadFinished(Loader<SimpleGraphObjectCursor<T>> loader,
                                SimpleGraphObjectCursor<T> data) {
                            if (loader != LoadingStrategy.this.loader) {
                                throw new FacebookException("Received callback for unknown loader.");
                            }
                            LoadingStrategy.this.onLoadFinished((GraphObjectPagingLoader<T>) loader, data);
                        }

                        @Override
                        public void onLoaderReset(Loader<SimpleGraphObjectCursor<T>> loader) {
                            if (loader != LoadingStrategy.this.loader) {
                                throw new FacebookException("Received callback for unknown loader.");
                            }
                            LoadingStrategy.this.onLoadReset((GraphObjectPagingLoader<T>) loader);
                        }
                    });

            loader.setOnErrorListener(new GraphObjectPagingLoader.OnErrorListener() {
                @Override
                public void onError(FacebookException error, GraphObjectPagingLoader<?> loader) {
                    hideActivityCircle();
                    if (onErrorListener != null) {
                        onErrorListener.onError(error);
                    }
                }
            });

            this.adapter = adapter;
            // Tell the adapter about any data we might already have.
            this.adapter.changeCursor(loader.getCursor());
        }

        public void detach() {
            adapter.setDataNeededListener(null);
            loader.setOnErrorListener(null);

            loader = null;
            adapter = null;
        }

        public void clearResults() {
            if (loader != null) {
                loader.clearResults();
            }
        }

        public void startLoading(Request request) {
            if (loader != null) {
                loader.startLoading(request, true);
            }
        }

        protected GraphObjectPagingLoader<T> onCreateLoader() {
            return new GraphObjectPagingLoader<T>(getActivity(), graphObjectClass);
        }

        protected void onLoadReset(GraphObjectPagingLoader<T> loader) {
            adapter.changeCursor(null);
        }

        protected void onLoadFinished(GraphObjectPagingLoader<T> loader, SimpleGraphObjectCursor<T> data) {
            updateAdapter(data);
        }
    }

    abstract class SelectionStrategy {
        abstract boolean isSelected(String id);

        abstract void toggleSelection(String id);

        abstract Collection<String> getSelectedIds();

        abstract void clear();

        abstract boolean isEmpty();

        abstract boolean shouldShowCheckBoxIfUnselected();

        abstract void saveSelectionToBundle(Bundle outBundle, String key);

        abstract void readSelectionFromBundle(Bundle inBundle, String key);
    }

    class SingleSelectionStrategy extends SelectionStrategy {
        private String selectedId;

        public Collection<String> getSelectedIds() {
            return Arrays.asList(new String[]{selectedId});
        }

        @Override
        boolean isSelected(String id) {
            return selectedId != null && id != null && selectedId.equals(id);
        }

        @Override
        void toggleSelection(String id) {
            if (selectedId != null && selectedId.equals(id)) {
                selectedId = null;
            } else {
                selectedId = id;
            }
        }

        @Override
        void saveSelectionToBundle(Bundle outBundle, String key) {
            if (!TextUtils.isEmpty(selectedId)) {
                outBundle.putString(key, selectedId);
            }
        }

        @Override
        void readSelectionFromBundle(Bundle inBundle, String key) {
            if (inBundle != null) {
                selectedId = inBundle.getString(key);
            }
        }

        @Override
        public void clear() {
            selectedId = null;
        }

        @Override
        boolean isEmpty() {
            return selectedId == null;
        }

        @Override
        boolean shouldShowCheckBoxIfUnselected() {
            return false;
        }
    }

    class MultiSelectionStrategy extends SelectionStrategy {
        private Set<String> selectedIds = new HashSet<String>();

        public Collection<String> getSelectedIds() {
            return selectedIds;
        }

        @Override
        boolean isSelected(String id) {
            return id != null && selectedIds.contains(id);
        }

        @Override
        void toggleSelection(String id) {
            if (id != null) {
                if (selectedIds.contains(id)) {
                    selectedIds.remove(id);
                } else {
                    selectedIds.add(id);
                }
            }
        }

        @Override
        void saveSelectionToBundle(Bundle outBundle, String key) {
            if (!selectedIds.isEmpty()) {
                String ids = TextUtils.join(",", selectedIds);
                outBundle.putString(key, ids);
            }
        }

        @Override
        void readSelectionFromBundle(Bundle inBundle, String key) {
            if (inBundle != null) {
                String ids = inBundle.getString(key);
                if (ids != null) {
                    String[] splitIds = TextUtils.split(ids, ",");
                    selectedIds.clear();
                    Collections.addAll(selectedIds, splitIds);
                }
            }
        }

        @Override
        public void clear() {
            selectedIds.clear();
        }

        @Override
        boolean isEmpty() {
            return selectedIds.isEmpty();
        }

        @Override
        boolean shouldShowCheckBoxIfUnselected() {
            return true;
        }
    }

    abstract class GraphObjectListFragmentAdapter<U extends GraphObject> extends GraphObjectAdapter<T> {
        public GraphObjectListFragmentAdapter(Context context) {
            super(context);
        }

        @Override
        boolean isGraphObjectSelected(String graphObjectId) {
            return selectionStrategy.isSelected(graphObjectId);
        }

        @Override
        void updateCheckboxState(CheckBox checkBox, boolean graphObjectSelected) {
            checkBox.setChecked(graphObjectSelected);
            checkBox.setVisibility(selectionStrategy.shouldShowCheckBoxIfUnselected() ?
                    View.VISIBLE : View.GONE);
        }
    }
}
