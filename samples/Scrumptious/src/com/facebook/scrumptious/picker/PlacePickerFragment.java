/**
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

package com.facebook.scrumptious.picker;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import com.facebook.*;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.scrumptious.R;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;

import org.json.JSONObject;

import java.util.*;

public class PlacePickerFragment extends PickerFragment {
    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate the radius in meters around
     * the center point to search. The default is 1000 meters.
     */
    public static final String RADIUS_IN_METERS_BUNDLE_KEY = "com.facebook.scrumptious.widget.PlacePickerFragment.RadiusInMeters";
    /**
     * The key for an int parameter in the fragment's Intent bundle to indicate what how many results to
     * return at a time. The default is 100 results.
     */
    public static final String RESULTS_LIMIT_BUNDLE_KEY = "com.facebook.scrumptious.widget.PlacePickerFragment.ResultsLimit";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate what search text should
     * be sent to the service. The default is to have no search text.
     */
    public static final String SEARCH_TEXT_BUNDLE_KEY = "com.facebook.scrumptious.widget.PlacePickerFragment.SearchText";
    /**
     * The key for a Location parameter in the fragment's Intent bundle to indicate what geographical
     * location should be the center of the search.
     */
    public static final String LOCATION_BUNDLE_KEY = "com.facebook.scrumptious.widget.PlacePickerFragment.Location";
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate that the fragment
     * should display a search box and automatically update the search text as it changes.
     */
    public static final String SHOW_SEARCH_BOX_BUNDLE_KEY = "com.facebook.scrumptious.widget.PlacePickerFragment.ShowSearchBox";

    /**
     * The default radius around the center point to search.
     */
    public static final int DEFAULT_RADIUS_IN_METERS = 1000;
    /**
     * The default number of results to retrieve.
     */
    public static final int DEFAULT_RESULTS_LIMIT = 100;

    private static final int searchTextTimerDelayInMilliseconds = 2 * 1000;

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String LOCATION = "location";
    private static final String CATEGORY = "category";
    private static final String WERE_HERE_COUNT = "were_here_count";
    private static final String TAG = "PlacePickerFragment";

    private Location location;
    private int radiusInMeters = DEFAULT_RADIUS_IN_METERS;
    private int resultsLimit = DEFAULT_RESULTS_LIMIT;
    private String searchText;
    private Timer searchTextTimer;
    private boolean hasSearchTextChangedSinceLastQuery;
    private boolean showSearchBox = true;
    private EditText searchBox;

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public PlacePickerFragment() {
        super(R.layout.picker_placepickerfragment);
    }

    /**
     * Gets the location to search around. Either the location or the search text (or both) must be specified.
     *
     * @return the Location to search around
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location to search around. Either the location or the search text (or both) must be specified.
     *
     * @param location the Location to search around
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the radius in meters around the location to search.
     *
     * @return the radius in meters
     */
    public int getRadiusInMeters() {
        return radiusInMeters;
    }

    /**
     * Sets the radius in meters around the location to search.
     *
     * @param radiusInMeters the radius in meters
     */
    public void setRadiusInMeters(int radiusInMeters) {
        this.radiusInMeters = radiusInMeters;
    }

    /**
     * Gets the number of results to retrieve.
     *
     * @return the number of results to retrieve
     */
    public int getResultsLimit() {
        return resultsLimit;
    }

    /**
     * Sets the number of results to retrieve.
     *
     * @param resultsLimit the number of results to retrieve
     */
    public void setResultsLimit(int resultsLimit) {
        this.resultsLimit = resultsLimit;
    }

    /**
     * Gets the search text (e.g., category, name) to search for. Either the location or the search
     * text (or both) must be specified.
     *
     * @return the search text
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Sets the search text (e.g., category, name) to search for. Either the location or the search
     * text (or both) must be specified. If a search box is displayed, this will update its contents
     * to the specified text.
     *
     * @param searchText the search text
     */
    public void setSearchText(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            searchText = null;
        }
        this.searchText = searchText;
        if (this.searchBox != null) {
            this.searchBox.setText(searchText);
        }
    }

    /**
     * Sets the search text and reloads the data in the control. This is used to provide search-box
     * functionality where the user may be typing or editing text rapidly. It uses a timer to avoid repeated
     * requerying, preferring to wait until the user pauses typing to refresh the data. Note that this
     * method will NOT update the text in the search box, if any, as it is intended to be called as a result
     * of changes to the search box (and is public to enable applications to provide their own search box
     * UI instead of the default one).
     *
     * @param searchText                 the search text
     * @param forceReloadEventIfSameText if true, will reload even if the search text has not changed; if false,
     *                                   identical search text will not force a reload
     */
    public void onSearchBoxTextChanged(String searchText, boolean forceReloadEventIfSameText) {
        if (!forceReloadEventIfSameText && Utility.stringsEqualOrEmpty(this.searchText, searchText)) {
            return;
        }

        if (TextUtils.isEmpty(searchText)) {
            searchText = null;
        }
        this.searchText = searchText;

        // If search text is being set in response to user input, it is wasteful to send a new request
        // with every keystroke. Send a request the first time the search text is set, then set up a 2-second timer
        // and send whatever changes the user has made since then. (If nothing has changed
        // in 2 seconds, we reset so the next change will cause an immediate re-query.)
        hasSearchTextChangedSinceLastQuery = true;
        if (searchTextTimer == null) {
            searchTextTimer = createSearchTextTimer();
        }
    }

    /**
     * Gets the currently-selected place.
     *
     * @return the currently-selected place, or null if there is none
     */
    public JSONObject getSelection() {
        Collection<JSONObject> selection = getSelectedGraphObjects();
        return (selection != null && !selection.isEmpty()) ? selection.iterator().next() : null;
    }

    @Override
    public void setSettingsFromBundle(Bundle inState) {
        super.setSettingsFromBundle(inState);
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
            showSearchBox = inState.getBoolean(SHOW_SEARCH_BOX_BUNDLE_KEY, showSearchBox);
        }
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.picker_place_picker_fragment);

        setRadiusInMeters(a.getInt(R.styleable.picker_place_picker_fragment_radius_in_meters, radiusInMeters));
        setResultsLimit(a.getInt(R.styleable.picker_place_picker_fragment_results_limit, resultsLimit));
        if (a.hasValue(R.styleable.picker_place_picker_fragment_results_limit)) {
            setSearchText(a.getString(R.styleable.picker_place_picker_fragment_search_text));
        }
        showSearchBox = a.getBoolean(R.styleable.picker_place_picker_fragment_show_search_box, showSearchBox);

        a.recycle();
    }

    @Override
    void setupViews(ViewGroup view) {
        if (showSearchBox) {
            ListView listView = (ListView) view.findViewById(R.id.com_facebook_picker_list_view);

            View searchHeaderView = getActivity().getLayoutInflater().inflate(
                    R.layout.picker_search_box, listView, false);

            listView.addHeaderView(searchHeaderView, null, false);

            searchBox = (EditText) view.findViewById(R.id.com_facebook_picker_search_text);

            searchBox.addTextChangedListener(new SearchTextWatcher());
            if (!TextUtils.isEmpty(searchText)) {
                searchBox.setText(searchText);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (searchBox != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (searchBox != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        }
    }

    void saveSettingsToBundle(Bundle outState) {
        super.saveSettingsToBundle(outState);

        outState.putInt(RADIUS_IN_METERS_BUNDLE_KEY, radiusInMeters);
        outState.putInt(RESULTS_LIMIT_BUNDLE_KEY, resultsLimit);
        outState.putString(SEARCH_TEXT_BUNDLE_KEY, searchText);
        outState.putParcelable(LOCATION_BUNDLE_KEY, location);
        outState.putBoolean(SHOW_SEARCH_BOX_BUNDLE_KEY, showSearchBox);
    }

    @Override
    void onLoadingData() {
        hasSearchTextChangedSinceLastQuery = false;
    }

    @Override
    GraphRequest getRequestForLoadData() {
        return createRequest(location, radiusInMeters, resultsLimit, searchText, extraFields);
    }

    @Override
    String getDefaultTitleText() {
        return getResources().getString(R.string.nearby);
    }

    @Override
    void logAppEvents(boolean doneButtonClicked) {
        AppEventsLogger logger = AppEventsLogger.newLogger(this.getActivity(),
                AccessToken.getCurrentAccessToken().getToken());
        Bundle parameters = new Bundle();

        // If Done was clicked, we know this completed successfully. If not, we don't know (caller might have
        // dismissed us in response to selection changing, or user might have hit back button). Either way
        // we'll log the number of selections.
        String outcome = doneButtonClicked ? AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_COMPLETED :
                AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_UNKNOWN;
        parameters.putString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME, outcome);
        parameters.putInt("num_places_picked", (getSelection() != null) ? 1 : 0);

        logger.logSdkEvent(AnalyticsEvents.EVENT_PLACE_PICKER_USAGE, null, parameters);
    }

    @Override
    PickerFragmentAdapter createAdapter() {
        PickerFragmentAdapter adapter = new PickerFragmentAdapter(
                this.getActivity()) {
            @Override
            protected CharSequence getSubTitleOfGraphObject(JSONObject graphObject) {
                String category = graphObject.optString(CATEGORY);
                int wereHereCount = graphObject.optInt(WERE_HERE_COUNT);

                String result = null;
                if (category != null && wereHereCount != 0) {
                    result = getString(R.string.picker_placepicker_subtitle_format, category, wereHereCount);
                } else if (category == null && wereHereCount != 0) {
                    result = getString(R.string.picker_placepicker_subtitle_were_here_only_format, wereHereCount);
                } else if (category != null && wereHereCount == 0) {
                    result = getString(R.string.picker_placepicker_subtitle_catetory_only_format, category);
                }
                return result;
            }

            @Override
            protected int getGraphObjectRowLayoutId(JSONObject graphObject) {
                return R.layout.picker_placepickerfragment_list_row;
            }

            @Override
            protected int getDefaultPicture() {
                return R.drawable.picker_place_default_icon;
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

    private GraphRequest createRequest(Location location, int radiusInMeters, int resultsLimit,
                                  String searchText, Set<String> extraFields) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest request = GraphRequest.newPlacesSearchRequest(accessToken, location, radiusInMeters,
                resultsLimit, searchText, null);

        Set<String> fields = new HashSet<String>(extraFields);
        String[] requiredFields = new String[]{
                ID,
                NAME,
                LOCATION,
                CATEGORY,
                WERE_HERE_COUNT
        };
        fields.addAll(Arrays.asList(requiredFields));

        String pictureField = adapter.getPictureFieldSpecifier();
        if (pictureField != null) {
            fields.add(pictureField);
        }

        Bundle parameters = request.getParameters();
        parameters.putString("fields", TextUtils.join(",", fields));
        request.setParameters(parameters);

        return request;
    }

    private Timer createSearchTextTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onSearchTextTimerTriggered();
            }
        }, 0, searchTextTimerDelayInMilliseconds);

        return timer;
    }

    private void onSearchTextTimerTriggered() {
        if (hasSearchTextChangedSinceLastQuery) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FacebookException error = null;
                    try {
                        loadData(true);
                    } catch (FacebookException fe) {
                        error = fe;
                    } catch (Exception e) {
                        error = new FacebookException(e);
                    } finally {
                        if (error != null) {
                            OnErrorListener onErrorListener = getOnErrorListener();
                            if (onErrorListener != null) {
                                onErrorListener.onError(PlacePickerFragment.this, error);
                            } else {
                                Logger.log(LoggingBehavior.REQUESTS, TAG, "Error loading data : %s", error);
                            }
                        }
                    }
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
        public void attach(GraphObjectAdapter adapter) {
            super.attach(adapter);

            this.adapter.setDataNeededListener(new GraphObjectAdapter.DataNeededListener() {
                @Override
                public void onDataNeeded() {
                    // Do nothing if we are currently loading data . We will get notified again when that load finishes
                    // if the adapter still needs more data. Otherwise, follow the next link.
                    if (!loader.isLoading()) {
                        loader.followNextLink();
                    }
                }
            });
        }

        @Override
        protected void onLoadFinished(GraphObjectPagingLoader loader, GraphObjectCursor data) {
            super.onLoadFinished(loader, data);

            // We could be called in this state if we are clearing data or if we are being re-attached
            // in the middle of a query.
            if (data == null || loader.isLoading()) {
                return;
            }

            hideActivityCircle();
        }
    }

    private class SearchTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onSearchBoxTextChanged(s.toString(), false);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
