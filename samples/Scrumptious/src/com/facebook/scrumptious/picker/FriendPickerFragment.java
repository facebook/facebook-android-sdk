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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.FacebookException;
import com.facebook.scrumptious.R;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.Validate;

import org.json.JSONObject;

import java.util.*;

/**
 * Provides a Fragment that displays a list of a user's friends and allows one or more of the
 * friends to be selected.
 */
public class FriendPickerFragment extends PickerFragment {
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate what user's
     * friends should be shown. The default is to display the currently authenticated user's friends.
     */
    public static final String USER_ID_BUNDLE_KEY = "com.facebook.scrumptious.widget.FriendPickerFragment.UserId";
    /**
     * The key for a boolean parameter in the fragment's Intent bundle to indicate whether the
     * picker should allow more than one friend to be selected or not.
     */
    public static final String MULTI_SELECT_BUNDLE_KEY = "com.facebook.scrumptious.widget.FriendPickerFragment.MultiSelect";
    /**
     * The key for a String parameter in the fragment's Intent bundle to indicate the type of friend picker to use.
     * This value is case sensitive, and must match the enum @{link FriendPickerType}
     */
    public static final String FRIEND_PICKER_TYPE_KEY = "com.facebook.scrumptious.widget.FriendPickerFragment.FriendPickerType";

    public enum FriendPickerType {
        FRIENDS("/friends"),
        TAGGABLE_FRIENDS("/taggable_friends"),
        INVITABLE_FRIENDS("/invitable_friends");

        private final String requestPath;

        FriendPickerType(String path) {
            this.requestPath = path;
        }

        String getRequestPath() {
            return requestPath;
        }
    }

    private static final String ID = "id";
    private static final String NAME = "name";

    private String userId;

    private boolean multiSelect = true;

    // default to Friends for backwards compatibility
    private FriendPickerType friendPickerType = FriendPickerType.FRIENDS;

    private List<String> preSelectedFriendIds = new ArrayList<String>();

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public FriendPickerFragment() {
        super(R.layout.picker_friendpickerfragment);
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
     * Sets the friend picker type for this fragment.
     * @param type the type of friend picker to use.
     */
    public void setFriendPickerType(FriendPickerType type) {
        this.friendPickerType = type;
    }

    /**
     * Sets the list of friends for pre selection. These friends will be selected by default.
     * @param userIds list of friends as ids
     */
    public void setSelectionByIds(List<String> userIds) {
        preSelectedFriendIds.addAll(userIds);
    }

    /**
     * Sets the list of friends for pre selection. These friends will be selected by default.
     * @param userIds list of friends as ids
     */
    public void setSelectionByIds(String... userIds) {
        setSelectionByIds(Arrays.asList(userIds));
    }

    /**
     * Sets the list of friends for pre selection. These friends will be selected by default.
     * @param graphUsers list of friends as GraphUsers
     */
    public void setSelection(JSONObject... graphUsers) {
        setSelection(Arrays.asList(graphUsers));
    }

    /**
     * Sets the list of friends for pre selection. These friends will be selected by default.
     * @param graphUsers list of friends as GraphUsers
     */
    public void setSelection(List<JSONObject> graphUsers) {
        List<String> userIds = new ArrayList<String>();
        for(JSONObject graphUser: graphUsers) {
            String id = graphUser.optString("id");
            Validate.notNullOrEmpty(id, "id");
            userIds.add(id);
        }
        setSelectionByIds(userIds);
    }

    /**
     * Gets the currently-selected list of users.
     * @return the currently-selected list of users
     */
    public List<JSONObject> getSelection() {
        return getSelectedGraphObjects();
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.picker_friend_picker_fragment);

        setMultiSelect(a.getBoolean(R.styleable.picker_friend_picker_fragment_multi_select, multiSelect));

        a.recycle();
    }

    @Override
    public void setSettingsFromBundle(Bundle inState) {
        super.setSettingsFromBundle(inState);
        if (inState != null) {
            if (inState.containsKey(USER_ID_BUNDLE_KEY)) {
                setUserId(inState.getString(USER_ID_BUNDLE_KEY));
            }
            setMultiSelect(inState.getBoolean(MULTI_SELECT_BUNDLE_KEY, multiSelect));
            if (inState.containsKey(FRIEND_PICKER_TYPE_KEY)) {
                try {
                    friendPickerType = FriendPickerType.valueOf(inState.getString(FRIEND_PICKER_TYPE_KEY));
                } catch (Exception e) {
                    // NOOP
                }
            }
        }
    }

    void saveSettingsToBundle(Bundle outState) {
        super.saveSettingsToBundle(outState);

        outState.putString(USER_ID_BUNDLE_KEY, userId);
        outState.putBoolean(MULTI_SELECT_BUNDLE_KEY, multiSelect);
    }

    @Override
    PickerFragmentAdapter createAdapter() {
        PickerFragmentAdapter adapter = new PickerFragmentAdapter(this.getActivity()) {
            @Override
            protected int getGraphObjectRowLayoutId(JSONObject graphObject) {
                return R.layout.picker_list_row;
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
    GraphRequest getRequestForLoadData() {
        if (adapter == null) {
            throw new FacebookException("Can't issue requests until Fragment has been created.");
        }

        String userToFetch = (userId != null) ? userId : "me";
        return createRequest(userToFetch, extraFields);
    }

    @Override
    String getDefaultTitleText() {
        return getResources().getString(R.string.choose_friends);
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
        parameters.putInt("num_friends_picked", getSelection().size());

        logger.logSdkEvent(AnalyticsEvents.EVENT_FRIEND_PICKER_USAGE, null, parameters);
    }

    @Override
    public void loadData(boolean forceReload) {
        super.loadData(forceReload);
        setSelectedGraphObjects(preSelectedFriendIds);
    }

    private GraphRequest createRequest(String userID, Set<String> extraFields) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest request = GraphRequest.newGraphPathRequest(
                accessToken, userID + friendPickerType.getRequestPath(), null);

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

    private class ImmediateLoadingStrategy extends LoadingStrategy {
        @Override
        protected void onLoadFinished(GraphObjectPagingLoader loader, GraphObjectCursor data) {
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
            }
        }

        private void followNextLink() {
            // This may look redundant, but this causes the circle to be alpha-dimmed if we have results.
            displayActivityCircle();

            loader.followNextLink();
        }
    }
}
