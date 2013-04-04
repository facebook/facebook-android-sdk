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

package com.facebook.samples.booleanog;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;
import com.facebook.model.*;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.UserSettingsFragment;
import com.facebook.widget.PickerFragment;

import java.text.SimpleDateFormat;
import java.util.*;

public class LogicActivity extends FragmentActivity {

    private static final String TAG = "BooleanOpenGraphSample";

    private static final String SAVE_ACTIVE_TAB = TAG + ".SAVE_ACTIVE_TAB";
    private static final String SAVE_CONTENT_SELECTION = TAG + ".SAVE_CONTENT_SELECTION";
    private static final String SAVE_LEFT_OPERAND_SELECTION = TAG + ".SAVE_LEFT_OPERAND_SELECTION";
    private static final String SAVE_RIGHT_OPERAND_SELECTION = TAG + ".SAVE_RIGHT_OPERAND_SELECTION";
    private static final String SAVE_RESULT_TEXT = TAG + ".SAVE_RESULT_TEXT";
    private static final String SAVE_POST_RESULT_TEXT = TAG + ".SAVE_POST_RESULT_TEXT";
    private static final String SAVE_PENDING = TAG + ".SAVE_PENDING";
    private static final String SAVE_FRIEND_ACTIONS = TAG + ".SAVE_FRIEND_ACTIONS";
    private static final String PENDING_POST_PATH = "PENDING_POST_PATH";
    private static final String PENDING_POST_LEFT = "PENDING_POST_LEFT";
    private static final String PENDING_POST_RIGHT = "PENDING_POST_RIGHT";
    private static final String PENDING_POST_RESULT = "PENDING_POST_RESULT";

    private static final String AND_ACTION = "fb_sample_boolean_og:and";
    private static final String OR_ACTION = "fb_sample_boolean_og:or";
    private static final String POST_AND_ACTION_PATH = "me/" + AND_ACTION;
    private static final String POST_OR_ACTION_PATH = "me/" + OR_ACTION;
    private static final String TRUE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360019783304";
    private static final String FALSE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360256449947";
    private static final String INSTALLED = "installed";
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static volatile TruthValueGraphObject TRUE_GRAPH_OBJECT;
    private static volatile TruthValueGraphObject FALSE_GRAPH_OBJECT;
    private static volatile int TRUE_SPINNER_INDEX = -1;
    private static volatile int FALSE_SPINNER_INDEX = -1;

    // Main layout
    private Button logicButton;
    private Button friendsButton;
    private Button settingsButton;
    private Button contentButton;
    private String activeTab;

    // Logic group
    private ViewGroup logicGroup;
    private Spinner leftSpinner;
    private Spinner rightSpinner;
    private Button andButton;
    private Button orButton;
    private TextView resultText;
    private TextView postResultText;
    private Bundle pendingPost;

    // Friends group
    private ViewGroup friendsGroup;
    private FriendPickerFragment friendPickerFragment;
    private RequestAsyncTask pendingRequest;
    private SimpleCursorAdapter friendActivityAdapter;
    private ProgressBar friendActivityProgressBar;
    private ArrayList<ActionRow> friendActionList;

    // Login group
    private ViewGroup settingsGroup;
    private UserSettingsFragment userSettingsFragment;

    // Content group
    private ViewGroup contentGroup;
    private ImageView contentImage;
    private Spinner contentSpinner;

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (exception != null) {
                pendingPost = null;
            } else if (state == SessionState.OPENED) {
                friendPickerFragment.loadData(false);
            } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
                sendPendingPost();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Views
        logicButton = (Button) findViewById(R.id.logic_button);
        friendsButton = (Button) findViewById(R.id.friends_button);
        settingsButton = (Button) findViewById(R.id.settings_button);
        contentButton = (Button) findViewById(R.id.content_button);

        logicGroup = (ViewGroup) findViewById(R.id.logic_group);
        leftSpinner = (Spinner) findViewById(R.id.left_spinner);
        rightSpinner = (Spinner) findViewById(R.id.right_spinner);
        andButton = (Button) findViewById(R.id.and_button);
        orButton = (Button) findViewById(R.id.or_button);
        resultText = (TextView) findViewById(R.id.result_text);
        postResultText = (TextView) findViewById(R.id.post_result_text);

        friendsGroup = (ViewGroup) findViewById(R.id.friends_group);
        ListView friendActivityList = (ListView) findViewById(R.id.friend_activity_list);
        String[] mapColumnNames = {"date", "action"};
        int[] mapViewIds = {R.id.friend_action_date, R.id.friend_action_data};
        friendActivityAdapter = new SimpleCursorAdapter(this, R.layout.friend_activity_row, createEmptyCursor(),
                mapColumnNames, mapViewIds);
        friendActivityList.setAdapter(friendActivityAdapter);
        friendActivityProgressBar = (ProgressBar) findViewById(R.id.friend_activity_progress_bar);

        settingsGroup = (ViewGroup) findViewById(R.id.settings_group);

        contentGroup = (ViewGroup) findViewById(R.id.content_group);
        contentImage = (ImageView) findViewById(R.id.content_image);
        contentSpinner = (Spinner) findViewById(R.id.content_spinner);

        // Fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        friendPickerFragment = (FriendPickerFragment) fragmentManager.findFragmentById(R.id.friend_picker_fragment);
        if (friendPickerFragment == null) {
            Bundle args = new Bundle();
            args.putBoolean(FriendPickerFragment.SHOW_TITLE_BAR_BUNDLE_KEY, false);
            friendPickerFragment = new FriendPickerFragment(args);
            transaction.add(R.id.friend_picker_fragment, friendPickerFragment);
        }

        userSettingsFragment = (UserSettingsFragment) fragmentManager.findFragmentById(R.id.login_fragment);
        if (userSettingsFragment == null) {
            userSettingsFragment = new UserSettingsFragment();
            transaction.add(R.id.login_fragment, userSettingsFragment);
        }

        transaction.commit();

        // Spinners
        ArrayAdapter<CharSequence> truthAdapter = ArrayAdapter
                .createFromResource(this, R.array.truth_values, android.R.layout.simple_spinner_item);
        truthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leftSpinner.setAdapter(truthAdapter);
        rightSpinner.setAdapter(truthAdapter);
        contentSpinner.setAdapter(truthAdapter);
        leftSpinner.setSelection(0);
        rightSpinner.setSelection(0);

        // Navigation
        for (Button button : Arrays.asList(logicButton, friendsButton, settingsButton, contentButton)) {
            initializeNavigationButton(button);
        }

        // Logic
        initializeCalculationButton(andButton);
        initializeCalculationButton(orButton);

        // Friends
        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> fragment, FacebookException error) {
                LogicActivity.this.onError(error);
            }
        });
        friendPickerFragment.setUserId("me");
        friendPickerFragment.setMultiSelect(false);
        friendPickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(PickerFragment<?> fragment) {
                LogicActivity.this.onFriendSelectionChanged();
            }
        });
        friendPickerFragment.setExtraFields(Arrays.asList(INSTALLED));
        friendPickerFragment.setFilter(new PickerFragment.GraphObjectFilter<GraphUser>() {
            @Override
            public boolean includeItem(GraphUser graphObject) {
                Boolean installed = graphObject.cast(GraphUserWithInstalled.class).getInstalled();
                return (installed != null) && installed.booleanValue();
            }
        });

        // Content
        contentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                LogicActivity.this.onContentSelectionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                LogicActivity.this.onContentSelectionChanged();
            }
        });

        // Restore saved state
        Button startButton = logicButton;

        if (savedInstanceState != null) {
            leftSpinner.setSelection(savedInstanceState.getInt(SAVE_LEFT_OPERAND_SELECTION));
            rightSpinner.setSelection(savedInstanceState.getInt(SAVE_RIGHT_OPERAND_SELECTION));
            contentSpinner.setSelection(savedInstanceState.getInt(SAVE_CONTENT_SELECTION));
            resultText.setText(savedInstanceState.getString(SAVE_RESULT_TEXT));
            postResultText.setText(savedInstanceState.getString(SAVE_POST_RESULT_TEXT));
            activeTab = savedInstanceState.getString(SAVE_ACTIVE_TAB);
            pendingPost = savedInstanceState.getBundle(SAVE_PENDING);

            friendActionList = savedInstanceState.getParcelableArrayList(SAVE_FRIEND_ACTIONS);
            if ((friendActionList != null) && (friendActionList.size() > 0)) {
                updateCursor(friendActionList);
            }

            if (getString(R.string.navigate_friends).equals(activeTab)) {
                startButton = friendsButton;
            } else if (getString(R.string.navigate_content).equals(activeTab)) {
                startButton = contentButton;
            } else if (getString(R.string.navigate_settings).equals(activeTab)) {
                startButton = settingsButton;
            }
        }

        if (!handleNativeLink()) {
            onNavigateButtonClick(startButton);
        }
    }

    // -----------------------------------------------------------------------------------
    // Activity lifecycle

    @Override
    public void onStart() {
        super.onStart();
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            friendPickerFragment.loadData(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        outState.putInt(SAVE_LEFT_OPERAND_SELECTION, leftSpinner.getSelectedItemPosition());
        outState.putInt(SAVE_RIGHT_OPERAND_SELECTION, rightSpinner.getSelectedItemPosition());
        outState.putInt(SAVE_CONTENT_SELECTION, contentSpinner.getSelectedItemPosition());
        outState.putString(SAVE_RESULT_TEXT, resultText.getText().toString());
        outState.putString(SAVE_POST_RESULT_TEXT, postResultText.getText().toString());
        outState.putString(SAVE_ACTIVE_TAB, activeTab);
        outState.putBundle(SAVE_PENDING, pendingPost);
        outState.putParcelableArrayList(SAVE_FRIEND_ACTIONS, friendActionList);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();

        friendPickerFragment.setOnErrorListener(null);
        friendPickerFragment.setOnSelectionChangedListener(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    // -----------------------------------------------------------------------------------
    // Navigation

    private void initializeNavigationButton(Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNavigateButtonClick((Button) view);
            }
        });
    }

    private void onNavigateButtonClick(Button source) {
        activeTab = source.getText().toString();

        logicGroup.setVisibility(getGroupVisibility(source, logicButton));
        friendsGroup.setVisibility(getGroupVisibility(source, friendsButton));
        settingsGroup.setVisibility(getGroupVisibility(source, settingsButton));
        contentGroup.setVisibility(getGroupVisibility(source, contentButton));

        // Show an error if viewing friends and there is no logged in user.
        if (source == friendsButton) {
            Session session = Session.getActiveSession();
            if ((session == null) || !session.isOpened()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.feature_requires_login_title)
                        .setMessage(R.string.feature_requires_login_message)
                        .setPositiveButton(R.string.ok_button, null)
                        .show();
            }
        }
    }

    private int getGroupVisibility(Button source, Button groupButton) {
        if (source == groupButton) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    // -----------------------------------------------------------------------------------
    // Logic group

    private void initializeCalculationButton(Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOperationButtonClick(view);
            }
        });
    }

    private void onOperationButtonClick(View view) {
        if (view == andButton) {
            onAndButtonClick();
        } else if (view == orButton) {
            onOrButtonClick();
        } else {
            assert false;
        }
    }

    private void onAndButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand && rightOperand;

        resultText.setText(getLogicText(getString(R.string.and_operation), leftOperand, rightOperand, result));
        postAction(POST_AND_ACTION_PATH, leftOperand, rightOperand, result);
    }

    private void onOrButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand || rightOperand;

        resultText.setText(getLogicText(getString(R.string.or_operation), leftOperand, rightOperand, result));
        postAction(POST_OR_ACTION_PATH, leftOperand, rightOperand, result);
    }

    private String getLogicText(String op, boolean leftOperand, boolean rightOperand, boolean result) {
        String trueString = getString(R.string.true_value);
        String falseString = getString(R.string.false_value);
        String arg0String = leftOperand ? trueString : falseString;
        String arg1String = rightOperand ? trueString : falseString;
        String resultString = result ? trueString : falseString;

        return String.format("%s %s %s = %s", arg0String, op, arg1String, resultString);
    }

    private void postAction(final String actionPath, final boolean leftOperand, final boolean rightOperand,
            final boolean result) {
        Bundle post = new Bundle();
        post.putString(PENDING_POST_PATH, actionPath);
        post.putBoolean(PENDING_POST_LEFT, leftOperand);
        post.putBoolean(PENDING_POST_RIGHT, rightOperand);
        post.putBoolean(PENDING_POST_RESULT, result);
        pendingPost = post;

        sendPendingPost();
    }

    private void sendPendingPost() {
        if (pendingPost == null) {
            return;
        }

        Session session = Session.getActiveSession();
        if ((session == null) || !session.isOpened()) {
            postResultText.setText("Not logged in, no post generated.");
            pendingPost = null;
            return;
        }

        List<String> permissions = session.getPermissions();
        if (!permissions.containsAll(PERMISSIONS)) {
            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS);
            session.requestNewPublishPermissions(newPermissionsRequest);
            return;
        }

        postResultText.setText("Posting action...");

        // For demo purposes, result is just a boolean, but operands are Open Graph objects
        String actionPath = pendingPost.getString(PENDING_POST_PATH);
        boolean leftOperand = pendingPost.getBoolean(PENDING_POST_LEFT);
        boolean rightOperand = pendingPost.getBoolean(PENDING_POST_RIGHT);
        boolean result = pendingPost.getBoolean(PENDING_POST_RESULT);

        LogicAction action = GraphObject.Factory.create(LogicAction.class);
        action.setResult(result);
        action.setTruthvalue(getTruthValueObject(leftOperand));
        action.setAnothertruthvalue(getTruthValueObject(rightOperand));

        Request.Callback callback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                onPostActionResponse(response);
            }
        };
        Request request = new Request(session, actionPath, null, HttpMethod.POST,
                callback);
        request.setGraphObject(action);
        RequestAsyncTask task = new RequestAsyncTask(request);

        task.execute();
    }

    private void onPostActionResponse(Response response) {
        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);
        if (postResponse != null && postResponse.getId() != null) {
            postResultText.setText("Post id = " + postResponse.getId());
        } else if (response.getError() != null) {
            postResultText.setText(response.getError().getErrorMessage());
        } else {
            postResultText.setText("");
        }
    }

    private TruthValueGraphObject getTruthValueObject(boolean value) {
        if (value) {
            if (TRUE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObject.Factory
                        .create(TruthValueGraphObject.class);
                object.setUrl(TRUE_GRAPH_OBJECT_URL);
                TRUE_GRAPH_OBJECT = object;
            }
            return TRUE_GRAPH_OBJECT;
        } else {
            if (FALSE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObject.Factory
                        .create(TruthValueGraphObject.class);
                object.setUrl(FALSE_GRAPH_OBJECT_URL);
                FALSE_GRAPH_OBJECT = object;
            }
            return FALSE_GRAPH_OBJECT;
        }
    }

    // -----------------------------------------------------------------------------------
    // Friends group

    private void onFriendSelectionChanged() {
        GraphUser user = chooseOne(friendPickerFragment.getSelection());
        if (user != null) {
            onChooseFriend(user.getId());
        } else {
            friendActivityAdapter.changeCursor(createEmptyCursor());
        }
    }

    private void onChooseFriend(String friendId) {
        friendActivityProgressBar.setVisibility(View.VISIBLE);

        String andPath = String.format("%s/%s", friendId, AND_ACTION);
        String orPath = String.format("%s/%s", friendId, OR_ACTION);
        Request getAnds = new Request(Session.getActiveSession(), andPath, null, HttpMethod.GET);
        Request getOrs = new Request(Session.getActiveSession(), orPath, null, HttpMethod.GET);

        RequestBatch batch = new RequestBatch(getAnds, getOrs);

        if (pendingRequest != null) {
            pendingRequest.cancel(true);
        }

        pendingRequest = new RequestAsyncTask(batch) {
            @Override
            protected void onPostExecute(List<Response> result) {
                if (pendingRequest == this) {
                    pendingRequest = null;

                    LogicActivity.this.onPostExecute(result);
                }
            }
        };

        pendingRequest.execute();
    }

    private void onPostExecute(List<Response> result) {
        friendActivityProgressBar.setVisibility(View.GONE);

        friendActionList = createActionRows(result);
        updateCursor(friendActionList);
    }

    private ArrayList<ActionRow> createActionRows(List<Response> result) {
        ArrayList<ActionRow> publishedItems = new ArrayList<ActionRow>();

        for (Response response : result) {
            if (response.getError() != null) {
                continue;
            }

            GraphMultiResult list = response.getGraphObjectAs(GraphMultiResult.class);
            List<PublishedLogicAction> listData = list.getData().castToListOf(PublishedLogicAction.class);

            for (PublishedLogicAction action : listData) {
                publishedItems.add(createActionRow(action));
            }
        }

        Collections.sort(publishedItems);
        return publishedItems;
    }

    private void updateCursor(Iterable<ActionRow> publishedItems) {
        MatrixCursor cursor = createEmptyCursor();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int id = 0;
        for (ActionRow item : publishedItems) {
            Object[] row = new Object[3];
            row[0] = id++;
            row[1] = dateFormat.format(item.publishDate);
            row[2] = item.actionText;
            cursor.addRow(row);
        }

        friendActivityAdapter.changeCursor(cursor);
        friendActivityAdapter.notifyDataSetChanged();
    }

    private MatrixCursor createEmptyCursor() {
        String[] cursorColumns = {"_ID", "date", "action"};
        return new MatrixCursor(cursorColumns);
    }

    private ActionRow createActionRow(PublishedLogicAction action) {
        String actionText = getActionText(action);
        Date publishDate = action.getPublishTime();

        return new ActionRow(actionText, publishDate);
    }

    private String getActionText(PublishedLogicAction action) {
        LogicAction actionData = action.getData();
        if (actionData == null) {
            return "";
        }

        TruthValueGraphObject left = actionData.getTruthvalue();
        TruthValueGraphObject right = actionData.getAnothertruthvalue();
        Boolean actionResult = actionData.getResult();

        String verb = action.getType();
        if (AND_ACTION.equals(verb)) {
            verb = getString(R.string.and_operation);
        } else if (OR_ACTION.equals(verb)) {
            verb = getString(R.string.or_operation);
        }

        if ((left == null) || (right == null) || (actionResult == null) || (verb == null)) {
            return "";
        }

        return String.format("%s %s %s = %s", left.getTitle(), verb, right.getTitle(), actionResult.toString());
    }

    // -----------------------------------------------------------------------------------
    // Content group

    private Boolean getDeepLinkContent(Uri deepLinkUri) {
        if (deepLinkUri != null) {
            String deepLink = deepLinkUri.toString();

            if (deepLink.startsWith(TRUE_GRAPH_OBJECT_URL)) {
                return Boolean.TRUE;
            } else if (deepLink.startsWith(FALSE_GRAPH_OBJECT_URL)) {
                return Boolean.FALSE;
            }
        }

        return null;
    }

    private void onContentSelectionChanged() {
        Boolean spinnerBoolean = getSpinnerBoolean(contentSpinner);
        if (Boolean.TRUE.equals(spinnerBoolean)) {
            contentImage.setVisibility(View.VISIBLE);
            contentImage.setImageResource(R.drawable.true_content);
        } else if (Boolean.FALSE.equals(spinnerBoolean)) {
            contentImage.setVisibility(View.VISIBLE);
            contentImage.setImageResource(R.drawable.false_content);
        } else {
            contentImage.setImageResource(View.INVISIBLE);
        }
    }

    // -----------------------------------------------------------------------------------
    // Utility methods

    private boolean handleNativeLink() {
        Session existingSession = Session.getActiveSession();
        // If we have a valid existing session, we'll use it; if not, open one using the provided Intent
        // but do not cache the token (we don't want to use the same user identity the next time the
        // app is run).
        if (existingSession == null || !existingSession.isOpened()) {
            AccessToken accessToken = AccessToken.createFromNativeLinkingIntent(getIntent());
            if (accessToken != null) {
                Session newSession = new Session.Builder(this).setTokenCachingStrategy(new NonCachingTokenCachingStrategy())
                        .build();
                newSession.open(accessToken, null);

                Session.setActiveSession(newSession);
            }
        }
        // See if we have a deep link in addition.
        Boolean deepLinkContent = getDeepLinkContent(getIntent().getData());
        if (deepLinkContent != null) {
            onNavigateButtonClick(contentButton);
            contentSpinner.setSelection(getSpinnerPosition(deepLinkContent));
            return true;
        }

        return false;
    }

    private int getSpinnerPosition(Boolean value) {
        initializeSpinnerIndexes();

        if (Boolean.TRUE.equals(value)) {
            return TRUE_SPINNER_INDEX;
        } else if (Boolean.FALSE.equals(value)) {
            return FALSE_SPINNER_INDEX;
        } else {
            return -1;
        }
    }

    private Boolean getSpinnerBoolean(Spinner spinner) {
        initializeSpinnerIndexes();

        int position = spinner.getSelectedItemPosition();
        if (position == TRUE_SPINNER_INDEX) {
            return Boolean.TRUE;
        } else if (position == FALSE_SPINNER_INDEX) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    private void initializeSpinnerIndexes() {
        if ((TRUE_SPINNER_INDEX < 0) || (FALSE_SPINNER_INDEX < 0)) {
            String[] truthArray = getResources().getStringArray(R.array.truth_values);
            List<String> truthList = Arrays.asList(truthArray);
            TRUE_SPINNER_INDEX = truthList.indexOf(getString(R.string.true_value));
            FALSE_SPINNER_INDEX = truthList.indexOf(getString(R.string.false_value));
        }
    }

    private void onError(Exception error) {
        showErrorMessage(error.getMessage());
    }

    private void showErrorMessage(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok_button, null)
                .show();
    }

    private <T> T chooseOne(List<T> ts) {
        for (T t : ts) {
            return t;
        }

        return null;
    }

    // -----------------------------------------------------------------------------------
    // Supporting types

    private interface GraphUserWithInstalled extends GraphUser {
        Boolean getInstalled();
    }

    private static class ActionRow implements Comparable<ActionRow>, Parcelable {
        final String actionText;
        final Date publishDate;

        ActionRow(String actionText, Date publishDate) {
            this.actionText = actionText;
            this.publishDate = publishDate;
        }

        @Override
        public int compareTo(ActionRow other) {
            if (other == null) {
                return 1;
            } else {
                return publishDate.compareTo(other.publishDate);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(actionText);
            parcel.writeLong(publishDate.getTime());
        }

        @SuppressWarnings("unused")
        public final Creator<ActionRow> CREATOR = new Creator<ActionRow>() {
            @Override
            public ActionRow createFromParcel(Parcel parcel) {
                String actionText = parcel.readString();
                Date publishDate = new Date(parcel.readLong());
                return new ActionRow(actionText, publishDate);
            }

            @Override
            public ActionRow[] newArray(int size) {
                return new ActionRow[size];
            }
        };
    }

    /**
     * Used to create and consume TruthValue open graph objects.
     */
    private interface TruthValueGraphObject extends GraphObject {
        void setUrl(String url);

        String getTitle();
    }

    /**
     * Used to create and consume And an Or open graph actions
     */
    private interface LogicAction extends OpenGraphAction {
        Boolean getResult();

        void setResult(Boolean result);

        TruthValueGraphObject getTruthvalue();

        void setTruthvalue(TruthValueGraphObject truthvalue);

        TruthValueGraphObject getAnothertruthvalue();

        void setAnothertruthvalue(TruthValueGraphObject anothertruthvalue);
    }

    /**
     * Used to consume published And and Or open graph actions.
     */
    private interface PublishedLogicAction extends OpenGraphAction {
        LogicAction getData();

        String getType();
    }

    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }
}
