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

package com.facebook.appevents;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.facebook.FacebookException;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.internal.Constants;
import com.facebook.internal.Logger;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

class AppEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final HashSet<String> validatedIdentifiers = new HashSet<String>();

    private final JSONObject jsonObject;
    private final boolean isImplicit;
    private final String name;

    public AppEvent(
            String contextName,
            String eventName,
            Double valueToSum,
            Bundle parameters,
            boolean isImplicitlyLogged,
            @Nullable final UUID currentSessionId
    ) throws JSONException, FacebookException {
        jsonObject = getJSONObjectForAppEvent(
                contextName,
                eventName,
                valueToSum,
                parameters,
                isImplicitlyLogged,
                currentSessionId);
        isImplicit = isImplicitlyLogged;
        name = eventName;
    }

    public String getName() {
        return name;
    }

    private AppEvent(String jsonString, boolean isImplicit) throws JSONException {
        jsonObject = new JSONObject(jsonString);
        this.isImplicit = isImplicit;
        this.name = jsonObject.optString(Constants.EVENT_NAME_EVENT_KEY);
    }

    public boolean getIsImplicit() {
        return isImplicit;
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }

    // throw exception if not valid.
    private static void validateIdentifier(String identifier) throws FacebookException {

        // Identifier should be 40 chars or less, and only have 0-9A-Za-z, underscore, hyphen,
        // and space (but no hyphen or space in the first position).
        final String regex = "^[0-9a-zA-Z_]+[0-9a-zA-Z _-]*$";

        final int MAX_IDENTIFIER_LENGTH = 40;
        if (identifier == null
                || identifier.length() == 0
                || identifier.length() > MAX_IDENTIFIER_LENGTH) {
            if (identifier == null) {
                identifier = "<None Provided>";
            }
            throw new FacebookException(
                    String.format(
                            Locale.ROOT,
                            "Identifier '%s' must be less than %d characters",
                            identifier,
                            MAX_IDENTIFIER_LENGTH)
            );
        }

        boolean alreadyValidated = false;
        synchronized (validatedIdentifiers) {
            alreadyValidated = validatedIdentifiers.contains(identifier);
        }

        if (!alreadyValidated) {
            if (identifier.matches(regex)) {
                synchronized (validatedIdentifiers) {
                    validatedIdentifiers.add(identifier);
                }
            } else {
                throw new FacebookException(
                        String.format(
                                "Skipping event named '%s' due to illegal name - must be " +
                                        "under 40 chars and alphanumeric, _, - or space, and " +
                                        "not start with a space or hyphen.",
                                identifier
                        )
                );
            }
        }
    }

    private static JSONObject getJSONObjectForAppEvent(
            String contextName,
            String eventName,
            Double valueToSum,
            Bundle parameters,
            boolean isImplicitlyLogged,
            @Nullable final UUID currentSessionId
    ) throws FacebookException, JSONException{
        validateIdentifier(eventName);

        JSONObject eventObject = new JSONObject();

        eventObject.put(Constants.EVENT_NAME_EVENT_KEY, eventName);
        eventObject.put(Constants.LOG_TIME_APP_EVENT_KEY, System.currentTimeMillis() / 1000);
        eventObject.put("_ui", contextName);
        if (currentSessionId != null) {
            eventObject.put("_session_id", currentSessionId);
        }

        if (valueToSum != null) {
            eventObject.put("_valueToSum", valueToSum.doubleValue());
        }

        if (isImplicitlyLogged) {
            eventObject.put("_implicitlyLogged", "1");
        }

        if (parameters != null) {
            for (String key : parameters.keySet()) {

                validateIdentifier(key);

                Object value = parameters.get(key);
                if (!(value instanceof String) && !(value instanceof Number)) {
                    throw new FacebookException(
                            String.format(
                                    "Parameter value '%s' for key '%s' should be a string" +
                                            " or a numeric type.",
                                    value,
                                    key)
                    );
                }

                eventObject.put(key, value.toString());
            }
        }

        if (!isImplicitlyLogged) {
            Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                    "Created app event '%s'", eventObject.toString());
        }

        return eventObject;
    }

    static class SerializationProxyV1 implements Serializable {
        private static final long serialVersionUID = -2488473066578201069L;
        private final String jsonString;
        private final boolean isImplicit;

        private SerializationProxyV1(String jsonString, boolean isImplicit) {
            this.jsonString = jsonString;
            this.isImplicit = isImplicit;
        }

        private Object readResolve() throws JSONException {
            return new AppEvent(jsonString, isImplicit);
        }
    }

    private Object writeReplace() {
        return new SerializationProxyV1(jsonObject.toString(), isImplicit);
    }

    @Override
    public String toString() {
        return String.format(
                "\"%s\", implicit: %b, json: %s",
                jsonObject.optString("_eventName"),
                isImplicit,
                jsonObject.toString());
    }
}
