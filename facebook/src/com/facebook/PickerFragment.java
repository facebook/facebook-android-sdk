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

import android.os.Bundle;

import java.util.Collection;
import java.util.Set;

/**
 * Provides functionality common to SDK UI elements that allow the user to pick one or more
 * graph objects (e.g., places, friends) from a list of possibilities. The UI is exposed as a
 * Fragment to allow to it to be included in an Activity along with other Fragments. The Fragments
 * can be configured by passing parameters as part of their Intent bundle, or (for certain
 * properties) by specifying attributes in their XML layout files.
 * <br/>
 * PickerFragments support callbacks that will be called in the event of an error, when the
 * underlying data has been changed, or when the set of selected graph objects changes.
 */
public interface PickerFragment<T extends GraphObject> {
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate whether the
     * picker should show pictures (if available) for the graph objects.
     */
    static final String SHOW_PICTURES_BUNDLE_KEY = "com.facebook.PickerFragment.ShowPictures";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate which extra fields
     * beyond the default fields should be retrieved for any graph objects in the results.
     */
    static final String EXTRA_FIELDS_BUNDLE_KEY = "com.facebook.PickerFragment.ExtraFields";
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate whether the
     * picker should display a title bar with a Done button.
     */
    static final String SHOW_TITLE_BAR_BUNDLE_KEY = "com.facebook.PickerFragment.ShowTitleBar";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate the text to
     * display in the title bar.
     */
    static final String TITLE_TEXT_BUNDLE_KEY = "com.facebook.PickerFragment.TitleText";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate the text to
     * display in the Done btuton.
     */
    static final String DONE_BUTTON_TEXT_BUNDLE_KEY = "com.facebook.PickerFragment.DoneButtonText";

    /**
     * Gets the current OnDataChangedListener for this fragment, which will be called whenever
     * the underlying data being displaying in the picker has changed.
     * @return the OnDataChangedListener, or null if there is none
     */
    OnDataChangedListener getOnDataChangedListener();

    /**
     * Sets the current OnDataChangedListener for this fragment, which will be called whenever
     * the underlying data being displaying in the picker has changed.
     * @param onDataChangedListener     the OnDataChangedListener, or null if there is none
     */
    void setOnDataChangedListener(OnDataChangedListener onDataChangedListener);

    /**
     * Gets the current OnSelectionChangedListener for this fragment, which will be called
     * whenever the user selects or unselects a graph object in the list.
     * @return the OnSelectionChangedListener, or null if there is none
     */
    OnSelectionChangedListener getOnSelectionChangedListener();

    /**
     * Sets the current OnSelectionChangedListener for this fragment, which will be called
     * whenever the user selects or unselects a graph object in the list.
     * @param onSelectionChangedListener     the OnSelectionChangedListener, or null if there is none
     */
    void setOnSelectionChangedListener(
            OnSelectionChangedListener onSelectionChangedListener);

    /**
     * Gets the current OnDoneButtonClickedListener for this fragment, which will be called
     * when the user clicks the Done button.
     * @return the OnDoneButtonClickedListener, or null if there is none
     */
    OnDoneButtonClickedListener getOnDoneButtonClickedListener();

    /**
     * Sets the current OnDoneButtonClickedListener for this fragment, which will be called
     * when the user clicks the Done button. This will only be possible if the title bar is
     * being shown in this fragment.
     * @param onDoneButtonClickedListener     the OnDoneButtonClickedListener, or null if there is none
     */
    void setOnDoneButtonClickedListener(OnDoneButtonClickedListener onDoneButtonClickedListener);

    /**
     * Gets the current OnErrorListener for this fragment, which will be called in the event
     * of network or other errors encountered while populating the graph objects in the list.
     * @return the OnErrorListener, or null if there is none
     */
    OnErrorListener getOnErrorListener();

    /**
     * Sets the current OnErrorListener for this fragment, which will be called in the event
     * of network or other errors encountered while populating the graph objects in the list.
     * @param onErrorListener    the OnErrorListener, or null if there is none
     */
    void setOnErrorListener(OnErrorListener onErrorListener);

    /**
     * Gets the current filter for this fragment, which will be called for each graph object
     * returned from the service to determine if it should be displayed in the list.
     * If no filter is specified, all retrieved graph objects will be displayed.
     * @return the GraphObjectFilter, or null if there is none
     */
    GraphObjectFilter<T> getFilter();

    /**
     * Sets the current filter for this fragment, which will be called for each graph object
     * returned from the service to determine if it should be displayed in the list.
     * If no filter is specified, all retrieved graph objects will be displayed.
     * @param filter     the GraphObjectFilter, or null if there is none
     */
    void setFilter(GraphObjectFilter<T> filter);

    /**
     * Gets the Session to use for any Facebook requests this fragment will make.
     * @return the Session that will be used for any Facebook requests, or null if there is none
     */
    Session getSession();

    /**
     * Sets the Session to use for any Facebook requests this fragment will make. If the
     * parameter is null, the fragment will use the current active session, if any.
     * @param session   the Session to use for Facebook requests, or null to use the active session
     */
    void setSession(Session session);

    /**
     * Gets whether to display pictures, if available, for displayed graph objects.
     * @return true if pictures should be displayed, false if not
     */
    boolean getShowPictures();

    /**
     * Gets whether to display pictures, if available, for displayed graph objects.
     * @param showPictures   true if pictures should be displayed, false if not
     */
    void setShowPictures(boolean showPictures);

    /**
     * Gets the extra fields to request for the retrieved graph objects.
     * @return the extra fields to request
     */
    Set<String> getExtraFields();

    /**
     * Sets the extra fields to request for the retrieved graph objects.
     * @param fields     the extra fields to request
     */
    void setExtraFields(Collection<String> fields);

    /**
     * Causes the picker to load data from the service and display it to the user.
     * @param forceReload if true, data will be loaded even if there is already data being displayed;
     *                    if false, data will not be re-loaded if it is already displayed
     */
    void loadData(boolean forceReload);

    /**
     * Updates the properties of the PickerFragment based on the contents of the supplied Bundle;
     * calling Activities may use this to pass additional configuration information to the
     * PickerFragment beyond what is specified in its XML layout.
     * @param inState   a Bundle containing keys corresponding to properties of the PickerFragment
     */
    void setSettingsFromBundle(Bundle inState);

    /**
     * Callback interface that will be called when a network or other error is encountered
     * while retrieving graph objects.
     */
    interface OnErrorListener {
        /**
         * Called when a network or other error is encountered.
         * @param error     a FacebookException representing the error that was encountered.
         */
        void onError(FacebookException error);
    }

    /**
     * Callback interface that will be called when the underlying data being displayed in the
     * picker has been updated.
     */
    interface OnDataChangedListener {
        /**
         * Called when the set of data being displayed in the picker has changed.
         */
        void onDataChanged();
    }

    /**
     * Callback interface that will be called when the user selects or unselects graph objects
     * in the picker.
     */
    interface OnSelectionChangedListener {
        /**
         * Called when the user selects or unselects graph objects in the picker.
         */
        void onSelectionChanged();
    }

    /**
     * Callback interface that will be called when the user clicks the Done button on the
     * title bar.
     */
    interface OnDoneButtonClickedListener {
        /**
         * Called when the user clicks the Done button.
         */
        void onDoneButtonClicked();
    }

    /**
     * Callback interface that will be called to determine if a graph object should be displayed.
     * @param <T>
     */
    interface GraphObjectFilter<T> {
        /**
         * Called to determine if a graph object should be displayed.
         * @param graphObject       the graph object
         * @return true to display the graph object, false to hide it
         */
        boolean includeItem(T graphObject);
    }
}
