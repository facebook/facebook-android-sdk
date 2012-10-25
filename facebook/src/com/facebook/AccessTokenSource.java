package com.facebook;

/**
 * Indicates where a Facebook access token was obtained from.
 */
public enum AccessTokenSource {
    /**
     * Indicates an access token has not been obtained, or is otherwise invalid.
     */
    NONE(false),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * native Facebook app for Android.
     */
    FACEBOOK_APPLICATION(true),
    /**
     * Indicates an access token was obtained by the user logging in through the
     * Web-based dialog.
     */
    WEB_VIEW(false),
    /**
     * Indicates an access token is for a test user rather than an actual
     * Facebook user.
     */
    TEST_USER(true);

    private final boolean canExtendToken;

    AccessTokenSource(boolean canExtendToken) {
        this.canExtendToken = canExtendToken;
    }

    boolean canExtendToken() {
        return canExtendToken;
    }
}