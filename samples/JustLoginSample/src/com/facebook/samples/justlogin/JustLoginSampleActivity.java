package com.facebook.samples.justlogin;

import java.util.ArrayList;

import com.facebook.LoggingBehaviors;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.SessionStatusCallback;
import com.facebook.Settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class JustLoginSampleActivity extends Activity {
    static final String URL_PREFIX_FRIENDS = "https://graph.facebook.com/me/friends?access_token=";
    static final String applicationId = "380615018626574";
    TextView textInstructionsOrLink;
    Button buttonLoginLogout;
    Session session;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.buttonLoginLogout = (Button)findViewById(R.id.buttonLoginLogout);
        this.textInstructionsOrLink = (TextView)findViewById(R.id.instructionsOrLink);

        this.session = createSession();
        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_ACCESS_TOKENS);

        this.updateView();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.session.onActivityResult(this, requestCode, resultCode, data);
    }

    private void updateView() {
        if (this.session.getIsOpened()) {
            this.textInstructionsOrLink.setText(URL_PREFIX_FRIENDS + this.session.getAccessToken());
            this.buttonLoginLogout.setText(R.string.logout);
            this.buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) { onClickLogout(); }
            });
        } else {
            this.textInstructionsOrLink.setText(R.string.instructions);
            this.buttonLoginLogout.setText(R.string.login);
            this.buttonLoginLogout.setOnClickListener(new OnClickListener() {
                public void onClick(View view) { onClickLogin(); }
            });
        }
    }

    private void onClickLogin() {
        SessionStatusCallback callback = new SessionStatusCallback() {
            public void call(Session session, SessionState state, Exception exception) {
                updateView();
            }
        };
        this.session.open(this, callback);
    }

    private void onClickLogout() {
        this.session.closeAndClearTokenInformation();
        this.session = createSession();
        updateView();
    }
    
    private Session createSession() {
        ArrayList<String> permissions = new ArrayList<String>();
        return new Session(this, applicationId, permissions, null);
    }
}