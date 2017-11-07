/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.login;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class FacebookLogin {

    private String code = null;
    private String userCode = null;
    private String verificationUrl = FacebookAssistantConstants.DEFAULT_VERIFICATION_URL;
    private int expiresIn = 0;
    private int refreshInterval = 0;

    private LocalDateTime requestTime = LocalDateTime.MIN;
    private long lastRefreshTime = 0;

    private String accessToken = null;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> loginChecker = null;

    private final List<FacebookLoginEventListener> eventListeners;

    private final Preferences preferences;

    private static class LazyHolder {
        static final FacebookLogin INSTANCE = new FacebookLogin();
    }

    @Contract(pure = true)
    public static FacebookLogin getInstance() {
        return LazyHolder.INSTANCE;
    }

    private FacebookLogin() {
        executor = Executors.newSingleThreadScheduledExecutor();
        eventListeners = new CopyOnWriteArrayList<>();
        preferences = loadPreferences();

        if (accessToken != null) {
            fireEvent(FacebookLoginEventType.LoggedIn);
        }
    }

    public void addEventListener(final FacebookLoginEventListener eventListener) {
        if ((eventListener != null) && !eventListeners.contains(eventListener)) {
            eventListeners.add(eventListener);
        }
    }

    public void removeEventListener(final FacebookLoginEventListener eventListener) {
        if ((eventListener != null) && eventListeners.contains(eventListener)) {
            eventListeners.remove(eventListener);
        }
    }

    private Preferences loadPreferences() {
        final Preferences preferences = Preferences.userRoot()
                .node(FacebookAssistantConstants.PREFERENCES_PATH);

        code = preferences.get(FacebookAssistantConstants.PARAM_CODE, null);
        accessToken = preferences.get(FacebookAssistantConstants.PARAM_ACCESS_TOKEN, null);

        return preferences;
    }

    private void savePreferences() {
        if (code == null) {
            preferences.remove(FacebookAssistantConstants.PARAM_CODE);
        } else {
            preferences.put(FacebookAssistantConstants.PARAM_CODE, code);
        }

        if (accessToken == null) {
            preferences.remove(FacebookAssistantConstants.PARAM_ACCESS_TOKEN);
        } else {
            preferences.put(FacebookAssistantConstants.PARAM_ACCESS_TOKEN, accessToken);
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            // Do not save anything
        }
    }

    private void clearPreferences() {
        code = null;
        accessToken = null;
        savePreferences();
    }

    @Nullable
    public String getUserCode() {
        return userCode;
    }

    public URI getVerificationUrl() {
        String uri = verificationUrl;
        if (userCode != null) {
            uri += new Formatter()
                    .format(FacebookAssistantConstants.DEVICE_CODE_QUERY, userCode)
                    .toString();
        }

        return URI.create(uri);
    }

    public LocalDateTime getExpiryTime() {
        return requestTime.plusMinutes(expiresIn);
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public JsonObject getProfile() {
        if (getAccessToken() == null) {
            return null;
        }

        try {
            final URI requestPath = new URIBuilder(
                    FacebookAssistantConstants.GRAPH_API_URL +
                            FacebookAssistantConstants.PROFILE_PATH)
                    .addParameter(
                            FacebookAssistantConstants.PARAM_FIELDS,
                            FacebookAssistantConstants.PROFILE_FIELDS)
                    .addParameter(
                            FacebookAssistantConstants.PARAM_ACCESS_TOKEN,
                            accessToken)
                    .build();

            final String response = Request
                    .Get(requestPath)
                    .execute()
                    .returnContent()
                    .asString();

            return new JsonParser()
                    .parse(response)
                    .getAsJsonObject();

        } catch (IOException e) {
            // Error contacting /me endpoint. Probably a bad access token
            accessToken = null;
            clearPreferences();
            fireEvent(FacebookLoginEventType.LoginExpired);

        } catch (URISyntaxException | JsonSyntaxException e) {
            // URI or JSON errors. Pretend like we're not logged in
        }

        return null;
    }

    /**
     * Starts a login attempt and returns immediately. The event listener is invoked when
     * login process finishes.
     *
     * @param eventListener Optional event listener to receive callbacks on login events
     */
    public synchronized void logIn(@Nullable final FacebookLoginEventListener eventListener) {

        addEventListener(eventListener);

        if (getAccessToken() != null) {
            // Already logged in
            if (eventListener != null) {
                eventListener.handleEvent(FacebookLoginEventType.LoggedIn);
            }

            return;
        }

        addEventListener(new FacebookAuthSpinner());
        startLoginAsync();
    }

    /**
     * Logs out the current user (if present) and clears all the access tokens and background data
     */
    public synchronized void logOut() {
        cancelLoginChecker();

        code = null;
        userCode = null;

        if (getAccessToken() != null) {
            accessToken = null;
            fireEvent(FacebookLoginEventType.LoggedOut);
        }

        clearPreferences();
    }

    private void fireEvent(final FacebookLoginEventType eventType) {
        for (FacebookLoginEventListener listener : eventListeners) {
            SwingUtilities.invokeLater(() -> listener.handleEvent(eventType));
        }
    }

    void startLoginAsync() {

        try {
            // Request a login
            final String response = Request
                    .Post(
                            FacebookAssistantConstants.GRAPH_API_URL +
                                    FacebookAssistantConstants.LOGIN_REQUEST_PATH)
                    .setHeader(
                            FacebookAssistantConstants.HEADER_NAME,
                            FacebookAssistantConstants.HEADER_VALUE)
                    .bodyForm(
                            new BasicNameValuePair(
                                    FacebookAssistantConstants.PARAM_ACCESS_TOKEN,
                                    new Formatter()
                                            .format(
                                                    FacebookAssistantConstants.ACCESS_TOKEN_FORMAT,
                                                    FacebookAssistantConstants.APP_ID,
                                                    FacebookAssistantConstants.CLIENT_TOKEN)
                                            .toString()),
                            /*
                            new BasicNameValuePair(
                                    FacebookAssistantConstants.PARAM_REDIRECT_URI,
                                    FacebookAssistantConstants.REDIRECT_URL),
                            */
                            new BasicNameValuePair(
                                    FacebookAssistantConstants.PARAM_SCOPE,
                                    FacebookAssistantConstants.LOGIN_SCOPES))
                    .execute()
                    .returnContent()
                    .asString();

            final JsonObject json = new JsonParser()
                    .parse(response)
                    .getAsJsonObject();

            code =
                    json.get(FacebookAssistantConstants.RESPONSE_CODE).getAsString();
            userCode =
                    json.get(FacebookAssistantConstants.RESPONSE_USER_CODE).getAsString();
            verificationUrl =
                    json.get(FacebookAssistantConstants.RESPONSE_VERIFICATION_URL).getAsString();
            expiresIn =
                    json.get(FacebookAssistantConstants.RESPONSE_EXPIRES_IN).getAsInt();
            refreshInterval =
                    json.get(FacebookAssistantConstants.RESPONSE_INTERVAL).getAsInt();

        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            // HTTP request / JSON errors
            clearPreferences();
            fireEvent(FacebookLoginEventType.LoginFailed);
            return;
        }

        // Reset all timers and tasks
        requestTime = LocalDateTime.now();
        lastRefreshTime = System.currentTimeMillis();
        accessToken = null;

        cancelLoginChecker();
        savePreferences();

        fireEvent(FacebookLoginEventType.LoginInitiated);
    }

    void waitForLoginAsync() {

        cancelLoginChecker();

        if ((code == null) || (accessToken != null)) {
            return;
        }

        final Runnable taskLoginChecker = () -> {
            try {
                if (checkLoginStatus()) {
                    cancelLoginChecker();

                    if (accessToken != null) {
                        savePreferences();
                        fireEvent(FacebookLoginEventType.LoggedIn);
                    } else {
                        clearPreferences();
                        fireEvent(FacebookLoginEventType.LoginFailed);
                    }
                }
            } catch (IOException e) {
                cancelLoginChecker();
                clearPreferences();
                fireEvent(FacebookLoginEventType.LoginFailed);
            }
        };

        loginChecker = executor.scheduleAtFixedRate(
                taskLoginChecker,
                refreshInterval,
                refreshInterval,
                TimeUnit.SECONDS);
    }

    private void cancelLoginChecker() {
        if (loginChecker != null) {
            loginChecker.cancel(true);
            loginChecker = null;
        }
    }

    private boolean checkLoginStatus()
            throws IOException {

        // Too soon to check. Back off and try later
        if (System.currentTimeMillis() - lastRefreshTime < refreshInterval * 1000) {
            return false;
        }

        // Code is invalid or expired. Signal that we need to stop checking
        if ((code == null) ||
                LocalDateTime.now().isAfter(getExpiryTime())) {
            return true;
        }

        // Check login status
        final InputStream response = Request
                .Post(
                        FacebookAssistantConstants.GRAPH_API_URL +
                                FacebookAssistantConstants.LOGIN_STATUS_PATH)
                .setHeader(
                        FacebookAssistantConstants.HEADER_NAME,
                        FacebookAssistantConstants.HEADER_VALUE)
                .bodyForm(
                        new BasicNameValuePair(
                                FacebookAssistantConstants.PARAM_ACCESS_TOKEN,
                                new Formatter()
                                        .format(
                                                FacebookAssistantConstants.ACCESS_TOKEN_FORMAT,
                                                FacebookAssistantConstants.APP_ID,
                                                FacebookAssistantConstants.CLIENT_TOKEN)
                                        .toString()),
                        new BasicNameValuePair(
                                FacebookAssistantConstants.PARAM_CODE,
                                code))
                .execute()
                .returnResponse()
                .getEntity()
                .getContent();

        try {
            final JsonObject json = new JsonParser()
                .parse(new JsonReader(new InputStreamReader(response)))
                .getAsJsonObject();

            if (json.has(FacebookAssistantConstants.RESPONSE_ACCESS_TOKEN)) {
                accessToken =
                        json.get(FacebookAssistantConstants.RESPONSE_ACCESS_TOKEN).getAsString();
            }
            if (accessToken != null) {
                // Successful login!
                return true;
            }

            if (json.has(FacebookAssistantConstants.RESPONSE_ERROR)) {
                final int errorSubCode = json
                        .get(FacebookAssistantConstants.RESPONSE_ERROR)
                        .getAsJsonObject()
                        .get(FacebookAssistantConstants.RESPONSE_ERROR_SUBCODE)
                        .getAsInt();

                switch (errorSubCode) {
                    case FacebookAssistantConstants.ERROR_SUBCODE_NOT_AUTHORIZED:
                        // Has not approved login yet. Do not stop polling
                        return false;

                    case FacebookAssistantConstants.ERROR_SUBCODE_TOO_FREQUENT:
                        // Checking status too frequently. Back off and try later
                        return false;

                    case FacebookAssistantConstants.ERROR_SUBCODE_LOGIN_EXPIRED:
                        // Login expired. Have to stop and start over
                        code = null;
                        return true;

                    default:
                        // Unknown error code. Probably network issue?
                        return false;
                }
            }

        } catch (RuntimeException re) {
            // A bunch of json parsing methods just throw subtypes of RuntimeException.
            // We want to deal with these errors gracefully.
            throw new IOException(re);
        }

        // Signal that we should try again later because we didn't get any response
        return false;
    }
}
