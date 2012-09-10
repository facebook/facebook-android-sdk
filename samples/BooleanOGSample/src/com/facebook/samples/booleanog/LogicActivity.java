package com.facebook.samples.booleanog;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class LogicActivity extends FragmentActivity {

    private static final String TAG = "TechnoLogic";

    private static final String AND_ACTION = "fb_sample_boolean_og:and";
    private static final String OR_ACTION = "fb_sample_boolean_og:or";
    private static final String POST_AND_ACTION_PATH = "me/" + AND_ACTION;
    private static final String POST_OR_ACTION_PATH = "me/" + OR_ACTION;
    private static final String TRUE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360019783304";
    private static final String FALSE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360256449947";
    private static volatile TruthValueGraphObject TRUE_GRAPH_OBJECT;
    private static volatile TruthValueGraphObject FALSE_GRAPH_OBJECT;
    private static volatile int TRUE_SPINNER_INDEX = -1;

    // Main layout
    private FrameLayout bodyFrame;
    private Button logicButton;
    private Button friendsButton;
    private Button settingsButton;

    // Logic page
    private ViewGroup logicGroup;
    private Spinner leftSpinner;
    private Spinner rightSpinner;
    private Button andButton;
    private Button orButton;
    private TextView resultText;
    private TextView postResultText;

    // Friends page
    private ViewGroup friendsGroup;
    private FriendPickerFragment friendPickerFragment;
    private ListView friendActivityList;
    private RequestAsyncTask pendingRequest;
    private SimpleCursorAdapter friendActivityAdapter;
    private ProgressBar friendActivityProgressBar;

    // Login page
    private ViewGroup settingsGroup;
    private LoginFragment loginFragment;
    private BroadcastReceiver sessionReceiver;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        bodyFrame = (FrameLayout) findViewById(R.id.body_frame);
        logicButton = (Button) findViewById(R.id.logic_button);
        friendsButton = (Button) findViewById(R.id.friends_button);
        settingsButton = (Button) findViewById(R.id.settings_button);

        logicGroup = (ViewGroup) findViewById(R.id.logic_group);
        leftSpinner = (Spinner) findViewById(R.id.left_spinner);
        rightSpinner = (Spinner) findViewById(R.id.right_spinner);
        andButton = (Button) findViewById(R.id.and_button);
        orButton = (Button) findViewById(R.id.or_button);
        resultText = (TextView) findViewById(R.id.result_text);
        postResultText = (TextView) findViewById(R.id.post_result_text);

        friendsGroup = (ViewGroup) findViewById(R.id.friends_group);
        friendActivityList = (ListView) findViewById(R.id.friend_activity_list);
        String[] mapColumnNames = {"date", "action"};
        int[] mapViewIds = {R.id.friend_action_date, R.id.friend_action_data};
        friendActivityAdapter = new SimpleCursorAdapter(this, R.layout.friend_activity_row, createEmptyCursor(),
                mapColumnNames, mapViewIds);
        friendActivityList.setAdapter(friendActivityAdapter);
        friendActivityProgressBar = (ProgressBar) findViewById(R.id.friend_activity_progress_bar);

        settingsGroup = (ViewGroup) findViewById(R.id.settings_group);

        // Fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Log.d(TAG, Boolean.valueOf(fragmentManager != null).toString());
        if (savedInstanceState == null) {
            // First time through, we create our fragment programmatically.
            friendPickerFragment = new FriendPickerFragment();
            loginFragment = new LoginFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.friend_picker_fragment, friendPickerFragment)
                    .add(R.id.login_fragment, loginFragment)
                    .commit();
        } else {
            // Subsequent times, our fragment is recreated by the framework and already has saved and
            // restored its state, so we don't need to specify args again. (In fact, this might be
            // incorrect if the fragment was modified programmatically since it was created.)
            friendPickerFragment = (FriendPickerFragment) fragmentManager.findFragmentById(R.id.friend_picker_fragment);
            loginFragment = (LoginFragment) fragmentManager.findFragmentById(R.id.settings_group);
        }

        // Navigation
        initializeNavigationButton(logicButton);
        initializeNavigationButton(friendsButton);
        initializeNavigationButton(settingsButton);

        // Logic
        ArrayAdapter<CharSequence> truthAdapter = ArrayAdapter
                .createFromResource(this, R.array.truth_values, android.R.layout.simple_spinner_item);
        truthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leftSpinner.setAdapter(truthAdapter);
        rightSpinner.setAdapter(truthAdapter);
        initializeCalculationButton(andButton);
        initializeCalculationButton(orButton);

        // Friends
        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                LogicActivity.this.onError(error);
            }
        });
        friendPickerFragment.setUserId("me");
        friendPickerFragment.setMultiSelect(false);
        friendPickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged() {
                LogicActivity.this.onSelectionChanged();
            }
        });

        // Starting defaults
        onNavigateButtonClick(logicButton);
    }

    // -----------------------------------------------------------------------------------
    // Activity lifecycle

    @Override
    protected void onStart() {
        super.onStart();
        loadIfSessionValid();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadIfSessionValid();
            }
        };

        IntentFilter openedFilter = new IntentFilter(Session.ACTION_ACTIVE_SESSION_OPENED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, openedFilter);
    }

    private void loadIfSessionValid() {
        Session session = Session.getActiveSession();
        if ((session != null) && session.getIsOpened()) {
            friendPickerFragment.loadData(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        friendPickerFragment.setOnErrorListener(null);
        friendPickerFragment.setOnSelectionChangedListener(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginFragment.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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
        logicGroup.setVisibility(getGroupVisibility(source, logicButton));
        friendsGroup.setVisibility(getGroupVisibility(source, friendsButton));
        settingsGroup.setVisibility(getGroupVisibility(source, settingsButton));
    }

    private int getGroupVisibility(Button source, Button groupButton) {
        if (source == groupButton) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    // -----------------------------------------------------------------------------------
    // Logic panel

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

    private boolean getSpinnerBoolean(Spinner spinner) {
        if (TRUE_SPINNER_INDEX < 0) {
            String[] truthValues = getResources().getStringArray(R.array.truth_values);
            TRUE_SPINNER_INDEX = Arrays.asList(truthValues).indexOf(getString(R.string.true_value));
        }

        return spinner.getSelectedItemPosition() == TRUE_SPINNER_INDEX;
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
        Session session = Session.getActiveSession();

        if ((session == null) || !session.getIsOpened()) {
            postResultText.setText("Not logged in, no post generated.");
        } else {
            postResultText.setText("Posting action...");
            LogicAction action = GraphObjectWrapper.createGraphObject(LogicAction.class);

            // For demo purposes, result is just a string, but operands are Open Graph objects
            action.setResult(Boolean.valueOf(result));
            action.setTruthvalue(getTruthValueObject(leftOperand));
            action.setAnothertruthvalue(getTruthValueObject(rightOperand));

            Request.Callback callback = new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    onPostActionResponse(response);
                }
            };
            Request request = new Request(session, actionPath, null, Request.POST_METHOD, callback);
            request.setGraphObject(action);
            RequestAsyncTask task = new RequestAsyncTask(request);

            task.execute();
        }
    }

    private void onPostActionResponse(Response response) {
        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);

        String id = null;
        PostResponse.Body body = null;
        if (postResponse != null) {
            id = postResponse.getId();
            body = postResponse.getBody();
        }

        PostResponse.Error error = null;
        if (body != null) {
            error = body.getError();
        }

        String errorMessage = null;
        if (error != null) {
            errorMessage = error.getMessage();
        }

        if (errorMessage != null) {
            postResultText.setText(errorMessage);
        } else if (response.getError() != null) {
            postResultText.setText(response.getError().getLocalizedMessage());
        } else if (id != null) {
            postResultText.setText("Post id = " + id);
        } else {
            postResultText.setText("");
        }
    }

    private TruthValueGraphObject getTruthValueObject(boolean value) {
        if (value) {
            if (TRUE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObjectWrapper.createGraphObject(TruthValueGraphObject.class);
                object.setUrl(TRUE_GRAPH_OBJECT_URL);
                TRUE_GRAPH_OBJECT = object;
            }
            return TRUE_GRAPH_OBJECT;
        } else {
            if (FALSE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObjectWrapper.createGraphObject(TruthValueGraphObject.class);
                object.setUrl(FALSE_GRAPH_OBJECT_URL);
                FALSE_GRAPH_OBJECT = object;
            }
            return FALSE_GRAPH_OBJECT;
        }
    }

    // -----------------------------------------------------------------------------------
    // Friends panel

    private void onSelectionChanged() {
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
        Request getAnds = new Request(Session.getActiveSession(), andPath, null, Request.GET_METHOD);
        Request getOrs = new Request(Session.getActiveSession(), orPath, null, Request.GET_METHOD);

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

        ArrayList<ActionRow> publishedItems = createActionRows(result);
        updateCursor(publishedItems);
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
            row[0] = Integer.valueOf(id++);
            row[1] = dateFormat.format(item.publishDate);
            row[2] = item.actionText;
            cursor.addRow(row);
        }

        friendActivityAdapter.changeCursor(cursor);
        friendActivityAdapter.notifyDataSetChanged();
    }

    private MatrixCursor createEmptyCursor() {
        String[] cursorColumns = {"_ID", "date", "action"};
        MatrixCursor cursor = new MatrixCursor(cursorColumns);
        return cursor;
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
    // Supporting types

    private class ActionRow implements Comparable<ActionRow> {
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
        Body getBody();

        String getId();

        interface Body extends GraphObject {
            Error getError();
        }

        interface Error extends GraphObject {
            String getMessage();
        }
    }

    // ---------
    // Utility methods

    private void onError(Exception error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(error.getMessage()).setPositiveButton("OK", null);
        builder.show();
    }

    private <T> T chooseOne(Set<T> ts) {
        for (T t : ts) {
            return t;
        }

        return null;
    }
}
