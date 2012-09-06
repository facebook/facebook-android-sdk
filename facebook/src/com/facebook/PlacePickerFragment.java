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
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.facebook.android.R;

import java.util.*;

public class PlacePickerFragment extends GraphObjectListFragment<GraphPlace> {
    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate the radius in meters around
     * the center point to search. The default is 1000 meters.
     */
    public static final String RADIUS_IN_METERS_BUNDLE_KEY = "com.facebook.PlacePickerFragment.RadiusInMeters";
    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate what how many results to
     * return at a time. The default is 100 results.
     */
    public static final String RESULTS_LIMIT_BUNDLE_KEY = "com.facebook.PlacePickerFragment.ResultsLimit";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate what search text should
     * be sent to the service. The default is to have no search text.
     */
    public static final String SEARCH_TEXT_BUNDLE_KEY = "com.facebook.PlacePickerFragment.SearchText";
    /**
     * The key for a Location parameter in the fragment's Intent bundle to indicate what geographical
     * location should be the center of the search.
     */
    public static final String LOCATION_BUNDLE_KEY = "com.facebook.PlacePickerFragment.Location";

    /**
     * The default radius around the center point to search.
     */
    public static final int DEFAULT_RADIUS_IN_METERS = 1000;
    /**
     * The default number of results to retrieve.
     */
    public static final int DEFAULT_RESULTS_LIMIT = 100;

    private static final String CACHE_IDENTITY = "PlacePickerFragment";
    private static final int searchTextTimerDelayInMilliseconds = 2 * 1000;

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String LOCATION = "location";
    private static final String CATEGORY = "category";
    private static final String PICTURE = "picture";
    private static final String WERE_HERE_COUNT = "were_here_count";

    private Location location;
    private int radiusInMeters = DEFAULT_RADIUS_IN_METERS;
    private int resultsLimit = DEFAULT_RESULTS_LIMIT;
    private String searchText;
    private Timer searchTextTimer;
    private boolean hasSearchTextChangedSinceLastQuery;

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public PlacePickerFragment() {
        this(null);
    }

    /**
     * Constructor.
     * @param args  a Bundle that optionally contains one or more values containing additional
     *              configuration information for the Fragment.
     */
    public PlacePickerFragment(Bundle args) {
        super(CACHE_IDENTITY, GraphPlace.class, R.layout.place_picker_fragment, args);
        setPlacePickerSettingsFromBundle(args);
    }

    /**
     * Gets the location to search around. Either the location or the search text (or both) must be specified.
     * @return the Location to search around
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location to search around. Either the location or the search text (or both) must be specified.
     * @param location   the Location to search around
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the radius in meters around the location to search.
     * @return the radius in meters
     */
    public int getRadiusInMeters() {
        return radiusInMeters;
    }

    /**
     * Sets the radius in meters around the location to search.
     * @param radiusInMeters     the radius in meters
     */
    public void setRadiusInMeters(int radiusInMeters) {
        this.radiusInMeters = radiusInMeters;
    }

    /**
     * Gets the number of results to retrieve.
     * @return the number of results to retrieve
     */
    public int getResultsLimit() {
        return resultsLimit;
    }

    /**
     * Sets the number of results to retrieve.
     * @param resultsLimit   the number of results to retrieve
     */
    public void setResultsLimit(int resultsLimit) {
        this.resultsLimit = resultsLimit;
    }

    /**
     * Gets the search text (e.g., category, name) to search for. Either the location or the search
     * text (or both) must be specified.
     * @return the search text
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Sets the search text (e.g., category, name) to search for. Either the location or the search
     * text (or both) must be specified.
     * @param searchText     the search text
     */
    public void setSearchText(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            searchText = null;
        }
        this.searchText = searchText;
    }

    /**
     * Sets the search text and reloads the data in the control. This is used to provide search-box
     * functionality where the user may be typing or editing text rapidly. It uses a timer to avoid repeated
     * requerying, preferring to wait until the user pauses typing to refresh the data.
     * @param searchText    the search text
     * @param forceReloadEventIfSameText    if true, will reload even if the search text has not changed; if false,
     *                                      identical search text will not force a reload
     */
    public void setSearchTextAndReload(String searchText, boolean forceReloadEventIfSameText) {
        if (!forceReloadEventIfSameText && Utility.stringsEqualOrEmpty(this.searchText, searchText)) {
            return;
        }

        setSearchText(searchText);

        // If search text is being set in response to user input, it is wasteful to send a new request
        // with every keystroke. Send a request the first time the search text is set, then set up a 2-second timer
        // and send whatever changes the user has made since then. (If nothing has changed
        // in 2 seconds, we reset so the next change will cause an immediate re-query.)
        if (searchTextTimer == null) {
            searchTextTimer = createSearchTextTimer();
            loadData(true);
        } else {
            hasSearchTextChangedSinceLastQuery = true;
        }
    }

    /**
     * Gets the currently-selected place.
     * @return the currently-selected place, or null if there is none
     */
    public GraphPlace getSelection() {
        Collection<GraphPlace> selection = getSelectedGraphObjects();
        return (selection != null && selection.size() > 0) ? selection.iterator().next() : null;
    }

    public void setSettingsFromBundle(Bundle inState) {
        super.setSettingsFromBundle(inState);
        setPlacePickerSettingsFromBundle(inState);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.PlacePickerFragment);

        setRadiusInMeters(a.getInt(R.styleable.PlacePickerFragment_radius_in_meters, radiusInMeters));
        setResultsLimit(a.getInt(R.styleable.PlacePickerFragment_results_limit, resultsLimit));
        if (a.hasValue(R.styleable.PlacePickerFragment_results_limit)) {
            setSearchText(a.getString(R.styleable.PlacePickerFragment_search_text));
        }

        a.recycle();
    }

    void saveSettingsToBundle(Bundle outState) {
        super.saveSettingsToBundle(outState);

        outState.putInt(RADIUS_IN_METERS_BUNDLE_KEY, radiusInMeters);
        outState.putInt(RESULTS_LIMIT_BUNDLE_KEY, resultsLimit);
        outState.putString(SEARCH_TEXT_BUNDLE_KEY, searchText);
        outState.putParcelable(LOCATION_BUNDLE_KEY, location);
    }

    @Override
    void onLoadingData() {
        // TODO need a replacement mechanism for this?
        hasSearchTextChangedSinceLastQuery = false;
    }

    @Override
    Request getRequestForLoadData(Session session) {
        return createRequest(location, radiusInMeters, resultsLimit, searchText, extraFields, session);
    }

    @Override
    GraphObjectListFragmentAdapter<GraphPlace> createAdapter() {
        GraphObjectListFragmentAdapter<GraphPlace> adapter = new GraphObjectListFragmentAdapter<GraphPlace>(
                this.getActivity()) {
            @Override
            protected CharSequence getSubTitleOfGraphObject(GraphPlace graphObject) {
                String category = graphObject.getCategory();
                Integer wereHereCount = (Integer) graphObject.get(WERE_HERE_COUNT);

                String result = null;
                if (category != null && wereHereCount != null) {
                    result = getString(R.string.PlacePicker_SubtitleFormat, category, wereHereCount);
                } else if (category == null && wereHereCount != null) {
                    result = getString(R.string.PlacePicker_SubtitleFormatWereHereOnly, wereHereCount);
                } else if (category != null && wereHereCount == null) {
                    result = getString(R.string.PlacePicker_SubtitleFormatCategoryOnly, category);
                }
                return result;
            }

            @Override
            protected int getGraphObjectRowLayoutId(GraphPlace graphObject) {
                return R.layout.place_picker_list_row;
            }

            @Override
            protected int getDefaultPicture() {
                return R.drawable.place_default_icon;
            }

        };
        adapter.setShowCheckbox(false);
        adapter.setShowPicture(getShowPictures());
        return adapter;
    }

    @Override
    LoadingStrategy createLoadingStrategy() {
        return new AsNeededLoadingStrategy();
    }

    @Override
    SelectionStrategy createSelectionStrategy() {
        return new SingleSelectionStrategy();
    }

    private static Request createRequest(Location location, int radiusInMeters, int resultsLimit, String searchText,
            Set<String> extraFields,
            Session session) {
        Request request = Request.newPlacesSearchRequest(session, location, radiusInMeters, resultsLimit, searchText,
                null);

        Set<String> fields = new HashSet<String>(extraFields);
        String[] requiredFields = new String[]{
                ID,
                NAME,
                LOCATION,
                CATEGORY,
                PICTURE,
                WERE_HERE_COUNT
        };
        fields.addAll(Arrays.asList(requiredFields));

        Bundle parameters = request.getParameters();
        parameters.putString("fields", TextUtils.join(",", fields));
        request.setParameters(parameters);

        return request;
    }

    private void setPlacePickerSettingsFromBundle(Bundle inState) {
        // We do this in a separate non-overridable method so it is safe to call from the constructor.
        if (inState != null) {
            setRadiusInMeters(inState.getInt(RADIUS_IN_METERS_BUNDLE_KEY, radiusInMeters));
            setResultsLimit(inState.getInt(RESULTS_LIMIT_BUNDLE_KEY, resultsLimit));
            if (inState.containsKey(SEARCH_TEXT_BUNDLE_KEY)) {
                setSearchText(inState.getString(SEARCH_TEXT_BUNDLE_KEY));
            }
            if (inState.containsKey(LOCATION_BUNDLE_KEY)) {
                Location location = inState.getParcelable(LOCATION_BUNDLE_KEY);
                setLocation(location);
            }
        }
    }

    private Timer createSearchTextTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onSearchTextTimerTriggered();
            }
        }, searchTextTimerDelayInMilliseconds, searchTextTimerDelayInMilliseconds);

        return timer;
    }

    private void onSearchTextTimerTriggered() {
        if (hasSearchTextChangedSinceLastQuery) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadData(true);
                }
            });
        } else {
            // Nothing has changed in 2 seconds. Invalidate and forget about this timer.
            // Next time the user types, we will fire a query immediately again.
            searchTextTimer.cancel();
            searchTextTimer = null;
        }
    }

    private class AsNeededLoadingStrategy extends LoadingStrategy {
        @Override
        public void attach(GraphObjectAdapter<GraphPlace> adapter) {
            super.attach(adapter);

            this.adapter.setDataNeededListener(new GraphObjectAdapter.DataNeededListener() {
                @Override
                public void onDataNeeded() {
                    // Do nothing if we are currently loading data . We will get notified again when that load finishes if the adapter still
                    // needs more data. Otherwise, follow the next link.
                    if (!loader.isLoading()) {
                        loader.followNextLink();
                    }
                }
            });
        }

        @Override
        protected void onLoadFinished(GraphObjectPagingLoader<GraphPlace> loader,
                SimpleGraphObjectCursor<GraphPlace> data) {
            super.onLoadFinished(loader, data);

            // We could be called in this state if we are clearing data or if we are being re-attached
            // in the middle of a query.
            if (data == null || loader.isLoading()) {
                return;
            }

            hideActivityCircle();

            if (data.isFromCache()) {
                // Only the first page can be cached, since all subsequent pages will be round-tripped. Force
                // a refresh of the first page before we allow paging to begin. If the first page produced
                // no data, launch the refresh immediately, otherwise schedule it for later.
                loader.refreshOriginalRequest(data.areMoreObjectsAvailable() ? CACHED_RESULT_REFRESH_DELAY : 0);
            }
        }
    }
}
