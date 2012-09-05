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

import java.util.*;

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
        super(CACHE_IDENTITY, GraphUser.class, R.layout.friend_picker_fragment, args);
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
        if (this.multiSelect != multiSelect) {
            this.multiSelect = multiSelect;
            setSelectionStrategy(createSelectionStrategy());
        }
    }

    public List<GraphUser> getSelection() {
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

    @Override
    GraphObjectListFragmentAdapter<GraphUser> createAdapter() {
        GraphObjectListFragmentAdapter<GraphUser> adapter = new GraphObjectListFragmentAdapter<GraphUser>(
                this.getActivity()) {

            @Override
            protected int getGraphObjectRowLayoutId(GraphUser graphObject) {
                return R.layout.profile_picker_list_row;
            }

            @Override
            protected int getDefaultPicture() {
                return R.drawable.profile_default_icon;
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

    private static Request createRequest(String userID, Set<String> extraFields, Session session) {
        Request request = Request.newGraphPathRequest(session, userID + "/friends", null);

        Set<String> fields = new HashSet<String>(extraFields);
        String[] requiredFields = new String[]{
                ID,
                NAME,
                PICTURE
        };
        fields.addAll(Arrays.asList(requiredFields));

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
