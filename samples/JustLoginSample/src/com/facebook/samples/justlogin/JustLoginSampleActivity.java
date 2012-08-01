package com.facebook.samples.justlogin;

import com.facebook.FacebookActivity;
import com.facebook.LoggingBehaviors;
import com.facebook.SessionState;
import com.facebook.Settings;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class JustLoginSampleActivity extends FacebookActivity {
    static final String URL_PREFIX_FRIENDS = "https://graph.facebook.com/me/friends?access_token=";
    TextView textInstructionsOrLink;
    Button buttonLoginLogout;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.buttonLoginLogout = (Button)findViewById(R.id.buttonLoginLogout);
        this.textInstructionsOrLink = (TextView)findViewById(R.id.instructionsOrLink);

        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_ACCESS_TOKENS);

        this.updateView();
    }

    protected void onSessionStateChange(SessionState state, Exception exception) {
        updateView();
    }

    private void updateView() {
        if (this.isSessionOpen()) {
            this.textInstructionsOrLink.setText(URL_PREFIX_FRIENDS + this.getAccessToken());
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
        this.openSession();
    }

    private void onClickLogout() {
        this.closeSessionAndClearTokenInformation();
    }
}