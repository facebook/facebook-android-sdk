/**
 * Copyright 2010-present Facebook.
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

package com.facebook.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.facebook.*;
import com.facebook.android.R;
import com.facebook.model.GraphUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a Fragment that displays a list of a user's friends and allows one or more of the
 * friends to be selected.
 */
public class FriendPickerFragment extends PickerFragment<GraphUser> {
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate what user's
     * friends should be shown. The default is to display the currently authenticated user's friends.
     */
    public static final String USER_ID_BUNDLE_KEY = "com.facebook.widget.FriendPickerFragment.UserId";
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate whether the
     * picker should allow more than one friend to be selected or not.
     */
    public static final String MULTI_SELECT_BUNDLE_KEY = "com.facebook.widget.FriendPickerFragment.MultiSelect";

    private static final String ID = "id";
    private static final String NAME = "name";

    private String userId;

    private boolean multiSelect = true;

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public FriendPickerFragment() {
        this(null);
    }

    /**
     * Constructor.
     * @param args  a Bundle that optionally contains one or more values containing additional
     *              configuration information for the Fragment.
     */
    @SuppressLint("ValidFragment")
    public FriendPickerFragment(Bundle args) {
        super(GraphUser.class, R.layout.com_facebook_friendpickerfragment, args);
        setFriendPickerSettingsFromBundle(args);
    }

    /**
     * Gets the ID of the user whose friends should be displayed. If null, the default is to
     * show the currently authenticated user's friends.
     * @return the user ID, or null
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the ID of the user whose friends should be displayed. If null, the default is to
     * show the currently authenticated user's friends.
     * @param userId     the user ID, or null
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets whether the user can select multiple friends, or only one friend.
     * @return true if the user can select multiple friends, false if only one friend
     */
    public boolean getMultiSelect() {
        return multiSelect;
    }

    /**
     * Sets whether the user can select multiple friends, or only one friend.
     * @param multiSelect    true if the user can select multiple friends, false if only one friend
     */
    public void setMultiSelect(boolean multiSelect) {
        if (this.multiSelect != multiSelect) {
            this.multiSelect = multiSelect;
            setSelectionStrategy(createSelectionStrategy());
        }
    }

    /**
     * Gets the currently-selected list of users.
     * @return the currently-selected list of users
     */
    public List<GraphUser> getSelection() {
        return getSelectedGraphObjects();
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.com_facebook_friend_picker_fragment);

        setMultiSelect(a.getBoolean(R.styleable.com_facebook_friend_picker_fragment_multi_select, multiSelect));

        a.recycle();
    }

    public void setSettingsFromBundle(Bundle inState) {
        super.setSettingsFromBundle(inState);
        setFriendPickerSettingsFromBundle(inState);
    }

    void saveSettingsToBundle(Bundle outState) {
        super.saveSettingsToBundle(outState);

        outState.putString(USER_ID_BUNDLE_KEY, userId);
        outState.putBoolean(MULTI_SELECT_BUNDLE_KEY, multiSelect);
    }

    @Override
    PickerFragmentAdapter<GraphUser> createAdapter() {
        PickerFragmentAdapter<GraphUser> adapter = new PickerFragmentAdapter<GraphUser>(
                this.getActivity()) {

            @Override
            protected int getGraphObjectRowLayoutId(GraphUser graphObject) {
                return R.layout.com_facebook_picker_list_row;
            }

            @Override
            protected int getDefaultPicture() {
                return R.drawable.com_facebook_profile_default_icon;
            }

        };
        adapter.setShowCheckbox(true);
        adapter.setShowPicture(getShowPictures());
        adapter.setSortFields(Arrays.asList(new String[]{NAME}));
        adapter.setGroupByField(NAME);

        return adapter;
    }

    @Override
    LoadingStrategy createLoadingStrategy() {
        return new ImmediateLoadingStrategy();
    }

    @Override
    SelectionStrategy createSelectionStrategy() {
        return multiSelect ? new MultiSelectionStrategy() : new SingleSelectionStrategy();
    }

    @Override
    Request getRequestForLoadData(Session session) {
        if (adapter == null) {
            throw new FacebookException("Can't issue requests until Fragment has been created.");
        }

        String userToFetch = (userId != null) ? userId : "me";
        return createRequest(userToFetch, extraFields, session);
    }

    @Override
    String getDefaultTitleText() {
        return getString(R.string.com_facebook_choose_friends);
    }

    private Request createRequest(String userID, Set<String> extraFields, Session session) {
        Request request = Request.newGraphPathRequest(session, userID + "/friends", null);

        Set<String> fields = new HashSet<String>(extraFields);
        String[] requiredFields = new String[]{
                ID,
                NAME
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

    private void setFriendPickerSettingsFromBundle(Bundle inState) {
        // We do this in a separate non-overridable method so it is safe to call from the constructor.
        if (inState != null) {
            if (inState.containsKey(USER_ID_BUNDLE_KEY)) {
                setUserId(inState.getString(USER_ID_BUNDLE_KEY));
            }
            setMultiSelect(inState.getBoolean(MULTI_SELECT_BUNDLE_KEY, multiSelect));
        }
    }

    private class ImmediateLoadingStrategy extends LoadingStrategy {
        @Override
        protected void onLoadFinished(GraphObjectPagingLoader<GraphUser> loader,
                SimpleGraphObjectCursor<GraphUser> data) {
            super.onLoadFinished(loader, data);

            // We could be called in this state if we are clearing data or if we are being re-attached
            // in the middle of a query.
            if (data == null || loader.isLoading()) {
                return;
            }

            if (data.areMoreObjectsAvailable()) {
                // We got results, but more are available.
                followNextLink();
            } else {
                // We finished loading results.
                hideActivityCircle();

                // If this was from the cache, schedule a delayed refresh query (unless we got no results
                // at all, in which case refresh immediately.
                if (data.isFromCache()) {
                    loader.refreshOriginalRequest(data.getCount() == 0 ? CACHED_RESULT_REFRESH_DELAY : 0);
                }
            }
        }

        private void followNextLink() {
            // This may look redundant, but this causes the circle to be alpha-dimmed if we have results.
            displayActivityCircle();

            loader.followNextLink();
        }
    }
}
