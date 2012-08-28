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

import java.util.Collections;
import java.util.List;

import com.facebook.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

/**
 * A Log In/Log Out button that maintains session state and logs
 * in/out for the app.
 */
public class LoginView extends Button {

    private List<String> permissions = Collections.<String>emptyList();
    private String applicationId = null;
    private SessionTracker sessionTracker;
    private GraphUser user = null;
    private boolean confirmLogout;
    private boolean fetchUserInfo;
    private String loginText;
    private String logoutText;
    
    /**
     * Create the LoginView.
     * 
     * @see View#View(Context)
     */
    public LoginView(Context context) {
        super(context);
    }
    
    /**
     * Create the LoginView by inflating from XML
     * 
     * @see View#View(Context, AttributeSet)
     */
    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs.getStyleAttribute() == 0) {
            // apparently there's no method of setting a default style in xml,
            // so in case the users do not explicitly specify a style, we need 
            // to use sensible defaults.
            this.setBackgroundResource(R.drawable.login_button_blue);
            this.setTextColor(getResources().getColor(R.color.LoginView_textColor));
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.LoginView_textSize));
            this.setPadding(getResources().getDimensionPixelSize(R.dimen.LoginView_paddingLeft),
                            getResources().getDimensionPixelSize(R.dimen.LoginView_paddingTop),
                            getResources().getDimensionPixelSize(R.dimen.LoginView_paddingRight),
                            getResources().getDimensionPixelSize(R.dimen.LoginView_paddingBottom));
            this.setWidth(getResources().getDimensionPixelSize(R.dimen.LoginView_width));
            this.setHeight(getResources().getDimensionPixelSize(R.dimen.LoginView_height));
            this.setGravity(Gravity.CENTER);
        }
        parseAttributes(attrs);
    }

    /**
     * Create the LoginView by inflating from XML and applying a style.
     * 
     * @see View#View(Context, AttributeSet, int)
     */
    public LoginView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(attrs);
    }
    
    /**
     * Set the permissions to use when the session is opened.
     * 
     * @param permissions the permissions to use
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * Set the application ID to be used to open the session.
     * 
     * @param applicationId the application ID to use
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    /**
     * Provides an implementation for {@link Activity#onActivityResult
     * onActivityResult} that updates the Session based on information returned
     * during the authorization flow. The Activity containing this view
     * should forward the resulting onActivityResult call here to
     * update the Session state based on the contents of the resultCode and
     * data.
     * 
     * @param requestCode
     *            The requestCode parameter from the forwarded call. When this
     *            onActivityResult occurs as part of facebook authorization
     *            flow, this value is the activityCode passed to open or
     *            authorize.
     * @param resultCode
     *            An int containing the resultCode parameter from the forwarded
     *            call.
     * @param data
     *            The Intent passed as the data parameter from the forwarded
     *            call.
     * @return A boolean indicating whether the requestCode matched a pending
     *         authorization request for this Session.
     * @see Session#onActivityResult(Activity, int, int, Intent)
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Session session = sessionTracker.getSession();
        if (session != null) {
            return session.onActivityResult((Activity)getContext(), requestCode,
                    resultCode, data);
        } else {
            return false;
        }
    }
    
    /**
     * Set the Session object to use instead of the active Session. Since a Session
     * cannot be reused, if the user logs out from this Session, and tries to
     * log in again, the Active Session will be used instead.
     * 
     * @param newSession the Session object to use
     */
    public void setSession(Session newSession) {
        sessionTracker.setSession(newSession);
        setButtonText();
        fetchUserInfo();
    }
 
    @Override
    public void onFinishInflate() {
        this.sessionTracker = new SessionTracker(new LoginButtonCallback());
        // potential leakage of "this" before construction is complete, but 
        // hopefully since the button's not visible yet, it can't be clicked
        this.setOnClickListener(new LoginClickListener());
        setButtonText();
        fetchUserInfo();
    }
    
    private void parseAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LoginView);
        confirmLogout = a.getBoolean(R.styleable.LoginView_confirmLogout, true);
        fetchUserInfo = a.getBoolean(R.styleable.LoginView_fetchUserInfo, true);
        loginText = a.getString(R.styleable.LoginView_loginText);
        logoutText = a.getString(R.styleable.LoginView_logoutText);
        a.recycle();

    }
   
    private void setButtonText() {
        if (sessionTracker.getOpenSession() != null) {
            setText((logoutText != null) ? logoutText :
                getResources().getString(R.string.LoginView_LogOutButton));
        } else {
            setText((loginText != null) ? loginText : 
                getResources().getString(R.string.LoginView_LogInButton));
        }
    }
    
    private void fetchUserInfo() {
        if (fetchUserInfo) {
            final Session currentSession = sessionTracker.getOpenSession();
            if (currentSession != null) {
                Request request = Request.newMeRequest(currentSession, new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        if (currentSession == sessionTracker.getOpenSession()) {
                            user = response.getGraphObjectAs(GraphUser.class);
                        }
                    }
                });
                Request.executeBatchAsync(request);
            } else {
                user = null;
            }
        }
    }

    private class LoginClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Context context = getContext();
            final Session openSession = sessionTracker.getOpenSession();
            if (openSession != null) {
                // If the Session is currently open, it must mean we need to log out
                if (confirmLogout) {
                    // Create a confirmation dialog
                    String logout = getResources().getString(R.string.LoginView_LogOutAction);
                    String cancel = getResources().getString(R.string.LoginView_CancelAction);
                    String message;
                    if (user != null && user.getName() != null) {
                        message = String.format(getResources().getString(R.string.LoginView_LoggedInAs), user.getName());
                    } else {
                        message = getResources().getString(R.string.LoginView_LoggedInUsingFacebook);
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(message)
                           .setCancelable(true)
                           .setPositiveButton(logout, new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int which) {
                                   openSession.close();
                               }
                           })
                           .setNegativeButton(cancel, null);
                    builder.create().show();
                } else {
                    openSession.close();
                }
            } else {
                if (context instanceof Activity) {
                    Session currentSession = sessionTracker.getSession();
                    if (currentSession != null && !currentSession.getState().getIsClosed()) {
                        currentSession.open((Activity)context, null);
                    } else {
                        sessionTracker.setSession(null);
                        Session.sessionOpen((Activity)context, applicationId, permissions, null);
                    }
                }
            }
        }
    }
    
    private class LoginButtonCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            fetchUserInfo();
            setButtonText();
        }
    };
}
