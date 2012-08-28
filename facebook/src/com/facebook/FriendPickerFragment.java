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
import android.text.TextUtils;
import android.util.AttributeSet;
import com.facebook.android.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
 * Exposes a thing.
 */
public class FriendPickerFragment extends GraphObjectListFragment<GraphUser> {
    public static final String USER_ID_BUNDLE_KEY = "com.facebook.FriendPickerFragment.UserId";
    public static final String MULTI_SELECT_BUNDLE_KEY = "com.facebook.FriendPickerFragment.MultiSelect";

    private static final String CACHE_IDENTITY = "FriendPickerFragment";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PICTURE = "picture";

    private String userId;

    private boolean multiSelect = true;

    public FriendPickerFragment() {
        this(null);
    }

    public FriendPickerFragment(Bundle args) {
        super(CACHE_IDENTITY, GraphUser.class, GraphObjectPagingLoader.PagingMode.IMMEDIATE,
                R.layout.friend_picker_fragment, args);
        setFriendPickerSettingsFromBundle(args);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean getMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        if (workerFragment != null &&
                workerFragment.getAdapter() != null) {
            workerFragment.getAdapter().setSelectionStyle(getSelectionStyle());
        }
    }

    public Set<GraphUser> getSelection() {
        return getSelectedGraphObjects();
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.FriendPickerFragment);

        setMultiSelect(a.getBoolean(R.styleable.FriendPickerFragment_multi_select, multiSelect));

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

    GraphObjectAdapter.SelectionStyle getSelectionStyle() {
        return multiSelect ? GraphObjectAdapter.SelectionStyle.MULTI_SELECT : GraphObjectAdapter.SelectionStyle.SINGLE_SELECT;
    }

    @Override
    GraphObjectAdapter<GraphUser> createAdapter() {
        GraphObjectAdapter<GraphUser> adapter = new GraphObjectAdapter<GraphUser>(this.getActivity()) {

            @Override
            protected int getGraphObjectRowLayoutId(GraphUser graphObject) {
                return R.layout.profile_picker_list_row;
            }

            @Override
            protected int getDefaultPicture() {
                return R.drawable.profile_default_icon;
            }

        };
        adapter.setSelectionStyle(getSelectionStyle());
        adapter.setShowCheckbox(true);
        adapter.setShowPicture(getShowPictures());
        return adapter;
    }

    @Override
    Request getRequestForLoadData() {
        String[] sortFields = new String[]{NAME};
        String groupByField = NAME;

        workerFragment.getAdapter().setSortFields(Arrays.asList(sortFields));
        workerFragment.getAdapter().setGroupByField(NAME);

        String userToFetch = (userId != null) ? userId : "me";
        return createRequest(userToFetch, sortFields, groupByField, getSession());
    }

    private static Request createRequest(String userID, String[] sortFields, String groupByField, Session session) {
        Request request = Request.newGraphPathRequest(session, userID + "/friends", null);

        Set<String> fields = new HashSet<String>();
        // TODO get user requested fields
        String[] requiredFields = new String[]{
                ID,
                NAME,
                PICTURE
        };
        fields.addAll(Arrays.asList(requiredFields));
        fields.addAll(Arrays.asList(sortFields));
        fields.add(groupByField);

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
}
