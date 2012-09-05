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
    public static final String RADIUS_IN_METERS_BUNDLE_KEY = "com.facebook.PlacePickerFragment.RadiusInMeters";
    public static final String RESULTS_LIMIT_BUNDLE_KEY = "com.facebook.PlacePickerFragment.ResultsLimit";
    public static final String SEARCH_TEXT_BUNDLE_KEY = "com.facebook.PlacePickerFragment.SearchText";
    public static final String LOCATION_BUNDLE_KEY = "com.facebook.PlacePickerFragment.Location";

    public static final int DEFAULT_RADIUS_IN_METERS = 1000;
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

    public PlacePickerFragment() {
        this(null);
    }

    public PlacePickerFragment(Bundle args) {
        super(CACHE_IDENTITY, GraphPlace.class, GraphObjectPagingLoader.PagingMode.AS_NEEDED,
                R.layout.place_picker_fragment, args);
        setPlacePickerSettingsFromBundle(args);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getRadiusInMeters() {
        return radiusInMeters;
    }

    public void setRadiusInMeters(int radiusInMeters) {
        this.radiusInMeters = radiusInMeters;
    }

    public int getResultsLimit() {
        return resultsLimit;
    }

    public void setResultsLimit(int resultsLimit) {
        this.resultsLimit = resultsLimit;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public void setSearchTextAndReload(String searchText) {
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

    public GraphPlace getSelection() {
        Set<GraphPlace> selection = getSelectedGraphObjects();
        return (selection.size() > 0) ? selection.iterator().next() : null;
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
        hasSearchTextChangedSinceLastQuery = false;
    }

    @Override
    Request getRequestForLoadData(Session session) {
        return createRequest(location, radiusInMeters, resultsLimit, searchText, extraFields, session);
    }

    @Override
    GraphObjectAdapter<GraphPlace> createAdapter() {
        GraphObjectAdapter<GraphPlace> adapter = new GraphObjectAdapter<GraphPlace>(this.getActivity()) {
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
        adapter.setSelectionStyle(GraphObjectAdapter.SelectionStyle.SINGLE_SELECT);
        adapter.setShowCheckbox(false);
        adapter.setShowPicture(getShowPictures());
        return adapter;
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
}
