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

package com.facebook.login.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.facebook.*;
import com.facebook.R;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.LoginAuthorizationType;
import com.facebook.internal.Utility;
import com.facebook.internal.Utility.FetchedAppSettings;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A Log In/Log Out button that maintains login state and logs in/out for the app.
 * <p/>
 * This control requires the app ID to be specified in the AndroidManifest.xml.
 */
public class LoginButton extends FacebookButtonBase {
    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();

    // ***
    // Keep all the enum values in sync with attrs.xml
    // ***

    /**
     * The display modes for the login button tool tip.
     */
    public static enum ToolTipMode {
        /**
         * Default display mode. A server query will determine if the tool tip should be displayed
         * and, if so, what the string shown to the user should be.
         */
        AUTOMATIC("automatic", 0),

        /**
         * Display the tool tip with a local string--regardless of what the server returns
         */
        DISPLAY_ALWAYS("display_always", 1),

        /**
         * Never display the tool tip--regardless of what the server says
         */
        NEVER_DISPLAY("never_display", 2);

        public static ToolTipMode DEFAULT = AUTOMATIC;

        public static ToolTipMode fromInt(int enumValue) {
            for (ToolTipMode mode : values()) {
                if (mode.getValue() == enumValue) {
                    return mode;
                }
            }

            return null;
        }

        private String stringValue;
        private int intValue;
        private ToolTipMode(String stringValue, int value) {
            this.stringValue = stringValue;
            this.intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int getValue() {
            return intValue;
        }
    }

    private static final String TAG = LoginButton.class.getName();
    private boolean confirmLogout;
    private String loginText;
    private String logoutText;
    private LoginButtonProperties properties = new LoginButtonProperties();
    private String loginLogoutEventName = AnalyticsEvents.EVENT_LOGIN_VIEW_USAGE;
    private boolean toolTipChecked;
    private ToolTipPopup.Style toolTipStyle = ToolTipPopup.Style.BLUE;
    private ToolTipMode toolTipMode;
    private long toolTipDisplayTime = ToolTipPopup.DEFAULT_POPUP_DISPLAY_TIME;
    private ToolTipPopup toolTipPopup;
    private AccessTokenTracker accessTokenTracker;
    private LoginManager loginManager;

    static class LoginButtonProperties {
        private DefaultAudience defaultAudience = DefaultAudience.FRIENDS;
        private List<String> permissions = Collections.<String>emptyList();
        private LoginAuthorizationType authorizationType = null;
        private LoginBehavior loginBehavior = LoginBehavior.SSO_WITH_FALLBACK;

        public void setDefaultAudience(DefaultAudience defaultAudience) {
            this.defaultAudience = defaultAudience;
        }

        public DefaultAudience getDefaultAudience() {
            return defaultAudience;
        }

        public void setReadPermissions(List<String> permissions) {

            if (LoginAuthorizationType.PUBLISH.equals(authorizationType)) {
                throw new UnsupportedOperationException("Cannot call setReadPermissions after " +
                        "setPublishPermissions has been called.");
            }
            if (validatePermissions(permissions, LoginAuthorizationType.READ)) {
                this.permissions = permissions;
                authorizationType = LoginAuthorizationType.READ;
            }
        }

        public void setPublishPermissions(List<String> permissions) {

            if (LoginAuthorizationType.READ.equals(authorizationType)) {
                throw new UnsupportedOperationException("Cannot call setPublishPermissions after " +
                        "setReadPermissions has been called.");
            }
            if (validatePermissions(permissions, LoginAuthorizationType.PUBLISH)) {
                this.permissions = permissions;
                authorizationType = LoginAuthorizationType.PUBLISH;
            }
        }

        private boolean validatePermissions(List<String> permissions,
                                            LoginAuthorizationType authType) {

            if (LoginAuthorizationType.PUBLISH.equals(authType)) {
                if (Utility.isNullOrEmpty(permissions)) {
                    throw new IllegalArgumentException(
                            "Permissions for publish actions cannot be null or empty.");
                }
            }
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            if (accessToken != null) {
                if (!Utility.isSubset(permissions, accessToken.getPermissions())) {
                    Log.e(TAG, "Cannot set additional permissions with existing AccessToken.");
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

        public void setLoginBehavior(LoginBehavior loginBehavior) {
            this.loginBehavior = loginBehavior;
        }

        public LoginBehavior getLoginBehavior() {
            return loginBehavior;
        }
    }

    /**
     * Create the LoginButton by inflating from XML
     *
     * @see View#View(Context, AttributeSet)
     */
    public LoginButton(Context context) {
        super(
                context,
                null,
                0,
                0,
                AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
                DEFAULT_REQUEST_CODE);
    }

    /**
     * Create the LoginButton by inflating from XML
     *
     * @see View#View(Context, AttributeSet)
     */
    public LoginButton(Context context, AttributeSet attrs) {
        super(
                context,
                attrs,
                0,
                0,
                AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
                DEFAULT_REQUEST_CODE);
    }

    /**
     * Create the LoginButton by inflating from XML and applying a style.
     *
     * @see View#View(Context, AttributeSet, int)
     */
    public LoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(
                context,
                attrs,
                defStyle,
                0,
                AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
                DEFAULT_REQUEST_CODE);
    }

    /**
     * Sets the default audience to use when the user logs in.
     * This value is only useful when specifying publish permissions for the native
     * login dialog.
     *
     * @param defaultAudience the default audience value to use
     */
    public void setDefaultAudience(DefaultAudience defaultAudience) {
        properties.setDefaultAudience(defaultAudience);
    }

    /**
     * Gets the default audience to use when the user logs in.
     * This value is only useful when specifying publish permissions for the native
     * login dialog.
     *
     * @return the default audience value to use
     */
    public DefaultAudience getDefaultAudience() {
        return properties.getDefaultAudience();
    }

    /**
     * Set the permissions to use when the user logs in. The permissions here
     * can only be read permissions. If any publish permissions are included, the login
     * attempt by the user will fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the user logs in. If this is called
     * after login, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * It's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by using the LoginManager explicitly).
     *
     * @param permissions the read permissions to use
     * @throws UnsupportedOperationException if setPublishPermissions has been called
     */
    public void setReadPermissions(List<String> permissions) {
        properties.setReadPermissions(permissions);
    }

    /**
     * Set the permissions to use when the user logs in. The permissions here
     * can only be read permissions. If any publish permissions are included, the login
     * attempt by the user will fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the user logs in. If this is called
     * after login, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * It's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by using the LoginManager explicitly).
     *
     * @param permissions the read permissions to use
     * @throws UnsupportedOperationException if setPublishPermissions has been called
     */
    public void setReadPermissions(String... permissions) {
        properties.setReadPermissions(Arrays.asList(permissions));
    }


    /**
     * Set the permissions to use when the user logs in. The permissions here
     * should only be publish permissions. If any read permissions are included, the login
     * attempt by the user may fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the user logs in. If this is called
     * after login, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * It's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by using the LoginManager explicitly).
     *
     * @param permissions the publish permissions to use
     * @throws UnsupportedOperationException if setReadPermissions has been called
     * @throws IllegalArgumentException      if permissions is null or empty
     */
    public void setPublishPermissions(List<String> permissions) {
        properties.setPublishPermissions(permissions);
    }

    /**
     * Set the permissions to use when the user logs in. The permissions here
     * should only be publish permissions. If any read permissions are included, the login
     * attempt by the user may fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the user logs in. If this is called
     * after login, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * It's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by using the LoginManager explicitly).
     *
     * @param permissions the publish permissions to use
     * @throws UnsupportedOperationException if setReadPermissions has been called
     * @throws IllegalArgumentException      if permissions is null or empty
     */
    public void setPublishPermissions(String... permissions) {
        properties.setPublishPermissions(Arrays.asList(permissions));
    }


    /**
     * Clears the permissions currently associated with this LoginButton.
     */
    public void clearPermissions() {
        properties.clearPermissions();
    }

    /**
     * Sets the login behavior during authorization. If null is specified, the default
     * ({@link com.facebook.login.LoginBehavior LoginBehavior.SSO_WITH_FALLBACK}
     * will be used.
     *
     * @param loginBehavior The {@link com.facebook.login.LoginBehavior LoginBehavior} that
     *                      specifies what behaviors should be attempted during
     *                      authorization.
     */
    public void setLoginBehavior(LoginBehavior loginBehavior) {
        properties.setLoginBehavior(loginBehavior);
    }

    /**
     * Gets the login behavior during authorization. If null is returned, the default
     * ({@link com.facebook.login.LoginBehavior LoginBehavior.SSO_WITH_FALLBACK}
     * will be used.
     *
     * @return loginBehavior The {@link com.facebook.login.LoginBehavior LoginBehavior} that
     * specifies what behaviors should be attempted during
     * authorization.
     */
    public LoginBehavior getLoginBehavior() {
        return properties.getLoginBehavior();
    }

    /**
     * Sets the style (background) of the Tool Tip popup. Currently a blue style and a black
     * style are supported. Blue is default
     *
     * @param toolTipStyle The style of the tool tip popup.
     */
    public void setToolTipStyle(ToolTipPopup.Style toolTipStyle) {
        this.toolTipStyle = toolTipStyle;
    }

    /**
     * Sets the mode of the Tool Tip popup. Currently supported modes are default (normal
     * behavior), always_on (popup remains up until forcibly dismissed), and always_off (popup
     * doesn't show)
     *
     * @param toolTipMode The new mode for the tool tip
     */
    public void setToolTipMode(ToolTipMode toolTipMode) {
        this.toolTipMode = toolTipMode;
    }

    /**
     * Return the current {@link ToolTipMode} for this LoginButton
     *
     * @return The {@link ToolTipMode}
     */
    public ToolTipMode getToolTipMode() {
        return toolTipMode;
    }

    /**
     * Sets the amount of time (in milliseconds) that the tool tip will be shown to the user. The
     * default is {@value ToolTipPopup#DEFAULT_POPUP_DISPLAY_TIME}. Any value that is less than or
     * equal to zero will cause the tool tip to be displayed indefinitely.
     *
     * @param displayTime The amount of time (in milliseconds) that the tool tip will be displayed
     *                    to the user
     */
    public void setToolTipDisplayTime(long displayTime) {
        this.toolTipDisplayTime = displayTime;
    }

    /**
     * Gets the current amount of time (in ms) that the tool tip will be displayed to the user.
     *
     * @return The current amount of time (in ms) that the tool tip will be displayed.
     */
    public long getToolTipDisplayTime() {
        return toolTipDisplayTime;
    }

    /**
     * Dismisses the Tooltip if it is currently visible
     */
    public void dismissToolTip() {
        if (toolTipPopup != null) {
            toolTipPopup.dismiss();
            toolTipPopup = null;
        }
    }

    /**
     * Registers a login callback to the given callback manager.
     *
     * @param callbackManager The callback manager that will encapsulate the callback.
     * @param callback        The login callback that will be called on login completion.
     */
    public void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<LoginResult> callback) {
        getLoginManager().registerCallback(callbackManager, callback);
    }

    /**
     * Registers a login callback to the given callback manager.
     *
     * @param callbackManager The callback manager that will encapsulate the callback.
     * @param callback        The login callback that will be called on login completion.
     * @param requestCode     The request code to use, this should be outside of the range of those
     *                        reserved for the Facebook SDK
     *                        {@link com.facebook.FacebookSdk#isFacebookRequestCode(int)}.
     */
    public void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<LoginResult> callback,
            final int requestCode) {
        setRequestCode(requestCode);
        registerCallback(callbackManager, callback);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (accessTokenTracker != null && !accessTokenTracker.isTracking()) {
            accessTokenTracker.startTracking();
            setButtonText();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!toolTipChecked && !isInEditMode()) {
            toolTipChecked = true;
            checkToolTipSettings();
        }
    }

    private void showToolTipPerSettings(FetchedAppSettings settings) {
        if (settings != null && settings.getNuxEnabled() && getVisibility() == View.VISIBLE) {
            String toolTipString = settings.getNuxContent();
            displayToolTip(toolTipString);
        }
    }

    private void displayToolTip(String toolTipString) {
        toolTipPopup = new ToolTipPopup(toolTipString, this);
        toolTipPopup.setStyle(toolTipStyle);
        toolTipPopup.setNuxDisplayTime(toolTipDisplayTime);
        toolTipPopup.show();
    }

    private void checkToolTipSettings() {
        switch (toolTipMode) {
            case AUTOMATIC:
                // kick off an async request
                final String appId = Utility.getMetadataApplicationId(getContext());
                FacebookSdk.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final FetchedAppSettings settings = Utility.queryAppSettings(appId, false);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToolTipPerSettings(settings);
                            }
                        });
                    }
                });
                break;
            case DISPLAY_ALWAYS:
                String toolTipString = getResources().getString(
                        R.string.com_facebook_tooltip_default);
                displayToolTip(toolTipString);
                break;
            case NEVER_DISPLAY:
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setButtonText();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (accessTokenTracker != null) {
            accessTokenTracker.stopTracking();
        }
        dismissToolTip();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        // If the visibility is not VISIBLE, we want to dismiss the tooltip if it is there
        if (visibility != VISIBLE) {
            dismissToolTip();
        }
    }

    // For testing purposes only
    List<String> getPermissions() {
        return properties.getPermissions();
    }

    void setProperties(LoginButtonProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configureButton(
            final Context context,
            final AttributeSet attrs,
            final int defStyleAttr,
            final int defStyleRes) {
        super.configureButton(context, attrs, defStyleAttr, defStyleRes);
        setInternalOnClickListener(new LoginClickListener());

        parseLoginButtonAttributes(context, attrs, defStyleAttr, defStyleRes);

        if (isInEditMode()) {
            // cannot use a drawable in edit mode, so setting the background color instead
            // of a background resource.
            setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
            // hardcoding in edit mode as getResources().getString() doesn't seem to work in
            // IntelliJ
            loginText = "Log in with Facebook";
        } else {
            accessTokenTracker = new AccessTokenTracker() {
                @Override
                protected void onCurrentAccessTokenChanged(
                        AccessToken oldAccessToken,
                        AccessToken currentAccessToken) {
                    setButtonText();
                }
            };
        }

        setButtonText();
    }

    @Override
    protected int getDefaultStyleResource() {
        return R.style.com_facebook_loginview_default_style;
    }

    private void parseLoginButtonAttributes(
            final Context context,
            final AttributeSet attrs,
            final int defStyleAttr,
            final int defStyleRes) {
        this.toolTipMode = ToolTipMode.DEFAULT;
        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.com_facebook_login_view,
                defStyleAttr,
                defStyleRes);
        try {
            confirmLogout = a.getBoolean(R.styleable.com_facebook_login_view_confirm_logout, true);
            loginText = a.getString(R.styleable.com_facebook_login_view_login_text);
            logoutText = a.getString(R.styleable.com_facebook_login_view_logout_text);
            toolTipMode = ToolTipMode.fromInt(a.getInt(
                    R.styleable.com_facebook_login_view_tooltip_mode,
                    ToolTipMode.DEFAULT.getValue()));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        int height = (getCompoundPaddingTop() +
                (int)Math.ceil(Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom)) +
                getCompoundPaddingBottom());

        final Resources resources = getResources();
        String text = loginText;
        int logInWidth;
        int width;
        if (text == null) {
            text = resources.getString(R.string.com_facebook_loginview_log_in_button_long);
            logInWidth = measureButtonWidth(text);
            width = resolveSize(logInWidth, widthMeasureSpec);
            if (width < logInWidth) {
                text = resources.getString(R.string.com_facebook_loginview_log_in_button);
            }
        }
        logInWidth = measureButtonWidth(text);

        text = logoutText;
        if (text == null) {
            text = resources.getString(R.string.com_facebook_loginview_log_out_button);
        }
        int logOutWidth = measureButtonWidth(text);

        width = resolveSize(Math.max(logInWidth, logOutWidth), widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureButtonWidth(final String text) {
        int textWidth = measureTextWidth(text);
        int width = (getCompoundPaddingLeft() +
                getCompoundDrawablePadding() +
                textWidth +
                getCompoundPaddingRight());
        return width;
    }

    private void setButtonText() {
        final Resources resources = getResources();
        if (AccessToken.getCurrentAccessToken() != null) {
            setText((logoutText != null) ?
                    logoutText :
                    resources.getString(R.string.com_facebook_loginview_log_out_button));
        } else {
            if (loginText != null) {
                setText(loginText);
            } else {
                String text = resources.getString(
                        R.string.com_facebook_loginview_log_in_button_long);
                int width = getWidth();
                // if the width is 0, we are going to measure size, so use the long text
                if (width != 0) {
                    // we have a specific width, check if the long text fits
                    int measuredWidth = measureButtonWidth(text);
                    if (measuredWidth > width) {
                        // it doesn't fit, use the shorter text
                        text = resources.getString(R.string.com_facebook_loginview_log_in_button);
                    }
                }
                setText(text);
            }
        }
    }

    private class LoginClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Context context = getContext();

            AccessToken accessToken = AccessToken.getCurrentAccessToken();

            if (accessToken != null) {
                // Log out
                if (confirmLogout) {
                    // Create a confirmation dialog
                    String logout = getResources().getString(
                            R.string.com_facebook_loginview_log_out_action);
                    String cancel = getResources().getString(
                            R.string.com_facebook_loginview_cancel_action);
                    String message;
                    Profile profile = Profile.getCurrentProfile();
                    if (profile != null && profile.getName() != null) {
                        message = String.format(
                                getResources().getString(
                                        R.string.com_facebook_loginview_logged_in_as),
                                profile.getName());
                    } else {
                        message = getResources().getString(
                                R.string.com_facebook_loginview_logged_in_using_facebook);
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(message)
                            .setCancelable(true)
                            .setPositiveButton(logout, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    getLoginManager().logOut();
                                }
                            })
                            .setNegativeButton(cancel, null);
                    builder.create().show();
                } else {
                    getLoginManager().logOut();
                }
            } else {
                LoginManager loginManager = getLoginManager();
                loginManager.setDefaultAudience(getDefaultAudience());
                loginManager.setLoginBehavior(getLoginBehavior());

                if (LoginAuthorizationType.PUBLISH.equals(properties.authorizationType)) {
                    if (LoginButton.this.getFragment() != null) {
                        loginManager.logInWithPublishPermissions(
                                LoginButton.this.getFragment(),
                                properties.permissions);
                    } else {
                        loginManager.logInWithPublishPermissions(
                                LoginButton.this.getActivity(),
                                properties.permissions);
                    }
                } else {
                    if (LoginButton.this.getFragment() != null) {
                        loginManager.logInWithReadPermissions(
                                LoginButton.this.getFragment(),
                                properties.permissions);
                    } else {
                        loginManager.logInWithReadPermissions(
                                LoginButton.this.getActivity(),
                                properties.permissions);
                    }
                }
            }

            AppEventsLogger logger = AppEventsLogger.newLogger(getContext());

            Bundle parameters = new Bundle();
            parameters.putInt("logging_in", (accessToken != null) ? 0 : 1);

            logger.logSdkEvent(loginLogoutEventName, null, parameters);

            callExternalOnClickListener(v);
        }
    }

    LoginManager getLoginManager() {
        if (loginManager == null) {
            loginManager = LoginManager.getInstance();
        }
        return loginManager;
    }

    void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }
}
