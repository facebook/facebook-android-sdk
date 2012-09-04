package com.facebook.samples.booleanog;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;

import java.util.Arrays;

public class LogicActivity extends FragmentActivity {

    private static final String TAG = "TechnoLogic";

    private static final String AND_ACTION_PATH = "me/fb_sample_boolean_og:and";
    private static final String OR_ACTION_PATH = "me/fb_sample_boolean_og:or";
    private static final String TRUE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360019783304";
    private static final String FALSE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360256449947";
    private static volatile TruthValueGraphObject TRUE_GRAPH_OBJECT;
    private static volatile TruthValueGraphObject FALSE_GRAPH_OBJECT;
    private static volatile int TRUE_SPINNER_INDEX = -1;


    // Main layout
    FrameLayout bodyFrame;
    Button logicButton;
    Button friendsButton;
    Button settingsButton;

    // Logic page
    ViewGroup logicGroup;
    Spinner leftSpinner;
    Spinner rightSpinner;
    Button andButton;
    Button orButton;
    TextView resultText;
    TextView postResultText;

    // Friends page
    ViewGroup friendsGroup;
    ListView friendActivityList;
    FriendPickerFragment friendPickerFragment;

    // Login page
    ViewGroup settingsGroup;
    LoginFragment loginFragment;

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
        //friendActivityList = (ListView) findViewById(R.id.friend_activity_list);

        settingsGroup = (ViewGroup) findViewById(R.id.settings_group);

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

        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                LogicActivity.this.onError(error);
            }
        });

        // Navigation
        initializeNavigationButton(logicButton);
        initializeNavigationButton(friendsButton);
        initializeNavigationButton(settingsButton);

        // Logic
        ArrayAdapter<CharSequence> truthAdapter = ArrayAdapter.createFromResource(this, R.array.truth_values, android.R.layout.simple_spinner_item);
        truthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leftSpinner.setAdapter(truthAdapter);
        rightSpinner.setAdapter(truthAdapter);
        initializeCalculationButton(andButton);
        initializeCalculationButton(orButton);

        // Friends

        // Starting defaults
        onNavigateButtonClick(logicButton);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginFragment.onActivityResult(requestCode, resultCode, data);
    }

    private void initializeCalculationButton(Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == andButton) {
                    onAndButtonClick();
                } else if (view == orButton) {
                    onOrButtonClick();
                } else {
                    assert false;
                }
            }
        });
    }

    private void onAndButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand && rightOperand;

        resultText.setText(getLogicText(getString(R.string.and_operation), leftOperand, rightOperand, result));
        postAction(AND_ACTION_PATH, leftOperand, rightOperand, result);
    }

    private void onOrButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand || rightOperand;

        resultText.setText(getLogicText(getString(R.string.or_operation), leftOperand, rightOperand, result));
        postAction(OR_ACTION_PATH, leftOperand, rightOperand, result);
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
            BooleanOpenGraphAction action = GraphObjectWrapper.createGraphObject(BooleanOpenGraphAction.class);

            // For demo purposes, result is just a string, but operands are Open Graph objects
            action.setResult(result ? "1" : "0");
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
        PostResponseBody body = null;
        if (postResponse != null) {
            id = postResponse.getId();
            body = postResponse.getBody();
        }

        PostResponseError error = null;
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
            postResultText.setText("Post id = "+ id);
        } else {
            postResultText.setText("");
        }
    }

    private interface PostResponse extends GraphObject {
        PostResponseBody getBody();
        String getId();
    }

    private interface PostResponseBody extends GraphObject {
        PostResponseError getError();
    }

    private interface PostResponseError extends GraphObject {
        String getMessage();
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

    /**
     * Used to create and consume TruthValue open graph objects.
     */
    private interface TruthValueGraphObject extends GraphObject {
        String getId();
        void setId(String id);

        String getUrl();
        void setUrl(String url);

        String getTitle();
        void setTitle(String title);
    }

    /**
     * Used to create and consume And an Or open graph actions
     */
    private interface BooleanOpenGraphAction extends OpenGraphAction {
        String getResult();
        void setResult(String result);

        TruthValueGraphObject getTruthvalue();
        void setTruthvalue(TruthValueGraphObject truthvalue);

        TruthValueGraphObject getAnothertruthvalue();
        void setAnothertruthvalue(TruthValueGraphObject anothertruthvalue);
    }

    /**
     * Used to consume published And and Or open graph actions.
     */
    private interface PublishedBooleanOpenGraphAction extends OpenGraphAction {
        BooleanOpenGraphAction getData();

        String getVerb();
    }

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

    private void onError(Exception error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(error.getMessage()).setPositiveButton("OK", null);
        builder.show();
    }
}
