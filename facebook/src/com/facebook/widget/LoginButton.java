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

package com.facebook.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import com.facebook.*;
import com.facebook.android.R;
import com.facebook.model.GraphUser;
import com.facebook.internal.SessionAuthorizationType;
import com.facebook.internal.SessionTracker;
import com.facebook.internal.Utility;

import java.util.Collections;
import java.util.List;

/**
 * A Log In/Log Out button that maintains session state and logs
 * in/out for the app.
 * <p/>
 * This control will create and use the active session upon construction
 * if it has the available data (if the app ID is specified in the manifest).
 * It will also open the active session if it does not require user interaction
 * (i.e. if the session is in the {@link com.facebook.SessionState#CREATED_TOKEN_LOADED} state.
 * Developers can override the use of the active session by calling
 * the {@link #setSession(com.facebook.Session)} method.
 */
public class LoginButton extends Button {

    private static final String TAG = LoginButton.class.getName();
    private String applicationId = null;
    private SessionTracker sessionTracker;
    private GraphUser user = null;
    private Session userInfoSession = null; // the Session used to fetch the current user info
    private boolean confirmLogout;
    private boolean fetchUserInfo;
    private String loginText;
    private String logoutText;
    private UserInfoChangedCallback userInfoChangedCallback;
    private Fragment parentFragment;
    private LoginButtonProperties properties = new LoginButtonProperties();

    static class LoginButtonProperties {
        private SessionDefaultAudience defaultAudience = SessionDefaultAudience.FRIENDS;
        private List<String> permissions = Collections.<String>emptyList();
        private SessionAuthorizationType authorizationType = null;
        private OnErrorListener onErrorListener;
        private SessionLoginBehavior loginBehavior = SessionLoginBehavior.SSO_WITH_FALLBACK;
        private Session.StatusCallback sessionStatusCallback;

        public void setOnErrorListener(OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
        }

        public OnErrorListener getOnErrorListener() {
            return onErrorListener;
        }

        public void setDefaultAudience(SessionDefaultAudience defaultAudience) {
            this.defaultAudience = defaultAudience;
        }

        public SessionDefaultAudience getDefaultAudience() {
            return defaultAudience;
        }

        public void setReadPermissions(List<String> permissions, Session session) {
            if (SessionAuthorizationType.PUBLISH.equals(authorizationType)) {
                throw new UnsupportedOperationException(
                        "Cannot call setReadPermissions after setPublishPermissions has been called.");
            }
            if (validatePermissions(permissions, SessionAuthorizationType.READ, session)) {
                this.permissions = permissions;
                authorizationType = SessionAuthorizationType.READ;
            }
        }

        public void setPublishPermissions(List<String> permissions, Session session) {
            if (SessionAuthorizationType.READ.equals(authorizationType)) {
                throw new UnsupportedOperationException(
                        "Cannot call setPublishPermissions after setReadPermissions has been called.");
            }
            if (validatePermissions(permissions, SessionAuthorizationType.PUBLISH, session)) {
                this.permissions = permissions;
                authorizationType = SessionAuthorizationType.PUBLISH;
            }
        }

        private boolean validatePermissions(List<String> permissions,
                SessionAuthorizationType authType, Session currentSession) {
            if (SessionAuthorizationType.PUBLISH.equals(authType)) {
                if (Utility.isNullOrEmpty(permissions)) {
                    throw new IllegalArgumentException("Permissions for publish actions cannot be null or empty.");
                }
            }
            if (currentSession != null && currentSession.isOpened()) {
                if (!Utility.isSubset(permissions, currentSession.getPermissions())) {
                    Log.e(TAG, "Cannot set additional permissions when session is already open.");
                    return false;
                }
            }
            return true;
        }

        List<String> getPermissions() {
            return permissions;
        }

        public void clearPermissions() {
            permissions = null;
            authorizationType = null;
        }

        public void setLoginBehavior(SessionLoginBehavior loginBehavior) {
            this.loginBehavior = loginBehavior;
        }

        public SessionLoginBehavior getLoginBehavior() {
            return loginBehavior;
        }

        public void setSessionStatusCallback(Session.StatusCallback callback) {
            this.sessionStatusCallback = callback;
        }

        public Session.StatusCallback getSessionStatusCallback() {
            return sessionStatusCallback;
        }
    }

    /**
     * Specifies a callback interface that will be called when the button's notion of the current
     * user changes (if the fetch_user_info attribute is true for this control).
     */
    public interface UserInfoChangedCallback {
        /**
         * Called when the current user changes.
         * @param user  the current user, or null if there is no user
         */
        void onUserInfoFetched(GraphUser user);
    }

    /**
     * Callback interface that will be called when a network or other error is encountered
     * while logging in.
     */
    public interface OnErrorListener {
        /**
         * Called when a network or other error is encountered.
         * @param error     a FacebookException representing the error that was encountered.
         */
        void onError(FacebookException error);
    }

    /**
     * Create the LoginButton.
     *
     * @see View#View(Context)
     */
    public LoginButton(Context context) {
        super(context);
        initializeActiveSessionWithCachedToken(context);
        // since onFinishInflate won't be called, we need to finish initialization ourselves
        finishInit();
    }

    /**
     * Create the LoginButton by inflating from XML
     *
     * @see View#View(Context, AttributeSet)
     */
    public LoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs.getStyleAttribute() == 0) {
            // apparently there's no method of setting a default style in xml,
            // so in case the users do not explicitly specify a style, we need
            // to use sensible defaults.
            this.setTextColor(getResources().getColor(R.color.com_facebook_loginview_text_color));
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.com_facebook_loginview_text_size));
            this.setPadding(getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_padding_left),
                    getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_padding_top),
                    getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_padding_right),
                    getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_padding_bottom));
            this.setWidth(getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_width));
            this.setHeight(getResources().getDimensionPixelSize(R.dimen.com_facebook_loginview_height));
            this.setGravity(Gravity.CENTER);
            if (isInEditMode()) {
                // cannot use a drawable in edit mode, so setting the background color instead
                // of a background resource.
                this.setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
                // hardcoding in edit mode as getResources().getString() doesn't seem to work in IntelliJ
                loginText = "Log in";
            } else {
                this.setBackgroundResource(R.drawable.com_facebook_loginbutton_blue);
            }
        }
        parseAttributes(attrs);
        if (!isInEditMode()) {
            initializeActiveSessionWithCachedToken(context);
        }        
    }

    /**
     * Create the LoginButton by inflating from XML and applying a style.
     *
     * @see View#View(Context, AttributeSet, int)
     */
    public LoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(attrs);
        initializeActiveSessionWithCachedToken(context);
    }

    /**
     * Sets an OnErrorListener for this instance of LoginButton to call into when
     * certain exceptions occur.
     *
     * @param onErrorListener The listener object to set
     */
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        properties.setOnErrorListener(onErrorListener);
    }

    /**
     * Returns the current OnErrorListener for this instance of LoginButton.
     *
     * @return The OnErrorListener
     */
    public OnErrorListener getOnErrorListener() {
        return properties.getOnErrorListener();
    }

    /**
     * Sets the default audience to use when the session is opened.
     * This value is only useful when specifying write permissions for the native
     * login dialog.
     *
     * @param defaultAudience the default audience value to use
     */
    public void setDefaultAudience(SessionDefaultAudience defaultAudience) {
        properties.setDefaultAudience(defaultAudience);
    }

    /**
     * Gets the default audience to use when the session is opened.
     * This value is only useful when specifying write permissions for the native
     * login dialog.
     *
     * @return the default audience value to use
     */
    public SessionDefaultAudience getDefaultAudience() {
        return properties.getDefaultAudience();
    }

    /**
     * Set the permissions to use when the session is opened. The permissions here
     * can only be read permissions. If any publish permissions are included, the login
     * attempt by the user will fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the session is open. If this is called
     * after the session is opened, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * Since the session can be automatically opened when the LoginButton is constructed,
     * it's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by managing the session explicitly).
     *
     * @param permissions the read permissions to use
     *
     * @throws UnsupportedOperationException if setPublishPermissions has been called
     */
    public void setReadPermissions(List<String> permissions) {
        properties.setReadPermissions(permissions, sessionTracker.getSession());
    }

    /**
     * Set the permissions to use when the session is opened. The permissions here
     * should only be publish permissions. If any read permissions are included, the login
     * attempt by the user may fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the session is open. If this is called
     * after the session is opened, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * Since the session can be automatically opened when the LoginButton is constructed,
     * it's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by managing the session explicitly).
     *
     * @param permissions the read permissions to use
     *
     * @throws UnsupportedOperationException if setReadPermissions has been called
     * @throws IllegalArgumentException if permissions is null or empty
     */
    public void setPublishPermissions(List<String> permissions) {
        properties.setPublishPermissions(permissions, sessionTracker.getSession());
    }


    /**
     * Clears the permissions currently associated with this LoginButton.
     */
    public void clearPermissions() {
        properties.clearPermissions();
    }

    /**
     * Sets the login behavior for the session that will be opened. If null is specified,
     * the default ({@link SessionLoginBehavior SessionLoginBehavior.SSO_WITH_FALLBACK}
     * will be used.
     *
     * @param loginBehavior The {@link SessionLoginBehavior SessionLoginBehavior} that
     *                      specifies what behaviors should be attempted during
     *                      authorization.
     */
    public void setLoginBehavior(SessionLoginBehavior loginBehavior) {
        properties.setLoginBehavior(loginBehavior);
    }

    /**
     * Gets the login behavior for the session that will be opened. If null is returned,
     * the default ({@link SessionLoginBehavior SessionLoginBehavior.SSO_WITH_FALLBACK}
     * will be used.
     *
     * @return loginBehavior The {@link SessionLoginBehavior SessionLoginBehavior} that
     *                      specifies what behaviors should be attempted during
     *                      authorization.
     */
    public SessionLoginBehavior getLoginBehavior() {
        return properties.getLoginBehavior();
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
     * Gets the callback interface that will be called when the current user changes.
     * @return the callback interface
     */
    public UserInfoChangedCallback getUserInfoChangedCallback() {
        return userInfoChangedCallback;
    }

    /**
     * Sets the callback interface that will be called when the current user changes.
     *
     * @param userInfoChangedCallback   the callback interface
     */
    public void setUserInfoChangedCallback(UserInfoChangedCallback userInfoChangedCallback) {
        this.userInfoChangedCallback = userInfoChangedCallback;
    }

    /**
     * Sets the callback interface that will be called whenever the status of the Session
     * associated with this LoginButton changes. Note that updates will only be sent to the
     * callback while the LoginButton is actually attached to a window.
     *
     * @param callback the callback interface
     */
    public void setSessionStatusCallback(Session.StatusCallback callback) {
        properties.setSessionStatusCallback(callback);
    }

    /**
     * Sets the callback interface that will be called whenever the status of the Session
     * associated with this LoginButton changes.

     * @return the callback interface
     */
    public Session.StatusCallback getSessionStatusCallback() {
        return properties.getSessionStatusCallback();
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
     *            onActivityResult occurs as part of Facebook authorization
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
     * log in again, a new Active Session will be used instead.
     * <p/>
     * If the passed in session is currently opened, this method will also attempt to
     * load some user information for display (if needed).
     *
     * @param newSession the Session object to use
     * @throws FacebookException if errors occur during the loading of user information
     */
    public void setSession(Session newSession) {
        sessionTracker.setSession(newSession);
        fetchUserInfo();
        setButtonText();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        finishInit();
    }

    private void finishInit() {
        setOnClickListener(new LoginClickListener());
        setButtonText();
        if (!isInEditMode()) {
            sessionTracker = new SessionTracker(getContext(), new LoginButtonCallback(), null, false);
            fetchUserInfo();
        }
    }

    /**
     * Sets the fragment that contains this control. This allows the LoginButton to be
     * embedded inside a Fragment, and will allow the fragment to receive the
     * {@link Fragment#onActivityResult(int, int, android.content.Intent) onActivityResult}
     * call rather than the Activity.
     *
     * @param fragment the fragment that contains this control
     */
    public void setFragment(Fragment fragment) {
        parentFragment = fragment;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sessionTracker != null && !sessionTracker.isTracking()) {
            sessionTracker.startTracking();
            fetchUserInfo();
            setButtonText();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sessionTracker != null) {
            sessionTracker.stopTracking();
        }
    }

    // For testing purposes only
    List<String> getPermissions() {
        return properties.getPermissions();
    }

    void setProperties(LoginButtonProperties properties) {
        this.properties = properties;
    }

    private void parseAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.com_facebook_login_view);
        confirmLogout = a.getBoolean(R.styleable.com_facebook_login_view_confirm_logout, true);
        fetchUserInfo = a.getBoolean(R.styleable.com_facebook_login_view_fetch_user_info, true);
        loginText = a.getString(R.styleable.com_facebook_login_view_login_text);
        logoutText = a.getString(R.styleable.com_facebook_login_view_logout_text);
        a.recycle();
    }

    private void setButtonText() {
        if (sessionTracker != null && sessionTracker.getOpenSession() != null) {
            setText((logoutText != null) ? logoutText :
                    getResources().getString(R.string.com_facebook_loginview_log_out_button));
        } else {
            setText((loginText != null) ? loginText :
                    getResources().getString(R.string.com_facebook_loginview_log_in_button));
        }
    }

    private boolean initializeActiveSessionWithCachedToken(Context context) {
        if (context == null) {
            return false;
        }

        Session session = Session.getActiveSession();
        if (session != null) {
            return session.isOpened();
        }

        String applicationId = Utility.getMetadataApplicationId(context);
        if (applicationId == null) {
            return false;
        }

        return Session.openActiveSessionFromCache(context) != null;
    }

    private void fetchUserInfo() {
        if (fetchUserInfo) {
            final Session currentSession = sessionTracker.getOpenSession();
            if (currentSession != null) {
                if (currentSession != userInfoSession) {
                    Request request = Request.newMeRequest(currentSession, new Request.GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser me,  Response response) {
                            if (currentSession == sessionTracker.getOpenSession()) {
                                user = me;
                                if (userInfoChangedCallback != null) {
                                    userInfoChangedCallback.onUserInfoFetched(user);
                                }
                            }
                            if (response.getError() != null) {
                                handleError(response.getError().getException());
                            }
                        }
                    });
                    Request.executeBatchAsync(request);
                    userInfoSession = currentSession;
                }
            } else {
                user = null;
                if (userInfoChangedCallback != null) {
                    userInfoChangedCallback.onUserInfoFetched(user);
                }
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
                    String logout = getResources().getString(R.string.com_facebook_loginview_log_out_action);
                    String cancel = getResources().getString(R.string.com_facebook_loginview_cancel_action);
                    String message;
                    if (user != null && user.getName() != null) {
                        message = String.format(getResources().getString(R.string.com_facebook_loginview_logged_in_as), user.getName());
                    } else {
                        message = getResources().getString(R.string.com_facebook_loginview_logged_in_using_facebook);
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(message)
                           .setCancelable(true)
                           .setPositiveButton(logout, new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int which) {
                                   openSession.closeAndClearTokenInformation();
                               }
                           })
                           .setNegativeButton(cancel, null);
                    builder.create().show();
                } else {
                    openSession.closeAndClearTokenInformation();
                }
            } else {
                Session currentSession = sessionTracker.getSession();
                if (currentSession == null || currentSession.getState().isClosed()) {
                    sessionTracker.setSession(null);
                    Session session = new Session.Builder(context).setApplicationId(applicationId).build();
                    Session.setActiveSession(session);
                    currentSession = session;
                }
                if (!currentSession.isOpened()) {
                    Session.OpenRequest openRequest = null;
                    if (parentFragment != null) {
                        openRequest = new Session.OpenRequest(parentFragment);
                    } else if (context instanceof Activity) {
                        openRequest = new Session.OpenRequest((Activity)context);
                    }

                    if (openRequest != null) {
                        openRequest.setDefaultAudience(properties.defaultAudience);
                        openRequest.setPermissions(properties.permissions);
                        openRequest.setLoginBehavior(properties.loginBehavior);

                        if (SessionAuthorizationType.PUBLISH.equals(properties.authorizationType)) {
                            currentSession.openForPublish(openRequest);
                        } else {
                            currentSession.openForRead(openRequest);
                        }
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
            if (exception != null) {
                handleError(exception);
            }

            if (properties.sessionStatusCallback != null) {
                properties.sessionStatusCallback.call(session, state, exception);
            }
        }
    };

    void handleError(Exception exception) {
        if (properties.onErrorListener != null) {
            if (exception instanceof FacebookException) {
                properties.onErrorListener.onError((FacebookException)exception);
            } else {
                properties.onErrorListener.onError(new FacebookException(exception));
            }
        }
    }
}
