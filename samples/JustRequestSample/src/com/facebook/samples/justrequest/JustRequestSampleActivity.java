package com.facebook.samples.justrequest;

import java.util.ArrayList;
import java.util.List;

import com.facebook.FacebookException;
import com.facebook.GraphObject;
import com.facebook.LoggingBehaviors;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Session.StatusCallback;
import com.facebook.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JustRequestSampleActivity extends Activity {
    static final String applicationId = "327064487357152";
    Button buttonRequest;
    EditText editRequests;
    TextView textViewResults;
    Session session;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.buttonRequest = (Button) findViewById(R.id.buttonRequest);
        this.buttonRequest.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                onClickRequest();
            }
        });
        this.editRequests = (EditText) findViewById(R.id.editRequests);
        this.textViewResults = (TextView) findViewById(R.id.textViewResults);

        this.session = createSession();
        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_ACCESS_TOKENS);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.session.onActivityResult(this, requestCode, resultCode, data);
    }

    private void onClickRequest() {
        if (this.session.getIsOpened()) {
            sendRequests();
        } else {
            StatusCallback callback = new StatusCallback() {
                public void call(Session session, SessionState state, Exception exception) {
                    if (state.getIsOpened()) {
                        sendRequests();
                    } else if (exception != null) {
                        AlertDialog alertDialog;
                        alertDialog = new AlertDialog.Builder(JustRequestSampleActivity.this).create();
                        alertDialog.setTitle("Login failed");
                        alertDialog.setMessage(exception.getMessage());
                        alertDialog.show();
                        JustRequestSampleActivity.this.session = createSession();
                    }
                }
            };
            this.session.open(this, callback);
        }
    }

    private void sendRequests() {
        textViewResults.setText("");

        String requestIdsText = editRequests.getText().toString();
        String[] requestIds = requestIdsText.split(",");

        List<Request> requests = new ArrayList<Request>();
        for (final String requestId : requestIds) {
            requests.add(new Request(session, requestId, null, null, new Request.Callback() {
                public void onCompleted(Response response) {
                    GraphObject graphObject = response.getGraphObject();
                    FacebookException error = response.getError();
                    String s = textViewResults.getText().toString();
                    if (graphObject != null) {
                        if (graphObject.get("id") != null) {
                            s = s + String.format("%s: %s\n", graphObject.get("id"), graphObject.get("name"));
                        } else {
                            s = s + String.format("%s: <no such id>\n", requestId); 
                        }
                    } else if (error != null) {
                        s = s + String.format("Error: %s", error.getMessage());
                    }
                    textViewResults.setText(s);
                }
            }));
        }
        Request.executeBatch(requests);
    }

    private Session createSession() {
        ArrayList<String> permissions = new ArrayList<String>();
        return new Session(this, applicationId, permissions, null);
    }
}
