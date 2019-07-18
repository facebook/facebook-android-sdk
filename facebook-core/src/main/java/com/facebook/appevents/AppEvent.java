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

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.facebook.FacebookException;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.appevents.internal.Constants;
import com.facebook.appevents.internal.RestrictiveDataManager;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final HashSet<String> validatedIdentifiers = new HashSet<String>();

    private final JSONObject jsonObject;
    private final boolean isImplicit;
    private final boolean inBackground;
    private final String name;
    private final String checksum;

    public AppEvent(
            String contextName,
            @NonNull String eventName,
            Double valueToSum,
            Bundle parameters,
            boolean isImplicitlyLogged,
            boolean isInBackground,
            @Nullable final UUID currentSessionId
    ) throws JSONException, FacebookException {
        isImplicit = isImplicitlyLogged;
        inBackground = isInBackground;
        name = eventName;

        jsonObject = getJSONObjectForAppEvent(
                contextName,
                eventName,
                valueToSum,
                parameters,
                currentSessionId);

        checksum = calculateChecksum();
    }

    public String getName() {
        return name;
    }

    private AppEvent(
            String jsonString,
            boolean isImplicit,
            boolean inBackground,
            String checksum) throws JSONException {
        jsonObject = new JSONObject(jsonString);
        this.isImplicit = isImplicit;
        this.name = jsonObject.optString(Constants.EVENT_NAME_EVENT_KEY);
        this.checksum = checksum;
        this.inBackground = inBackground;
    }

    public boolean getIsImplicit() {
        return isImplicit;
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }

    public boolean isChecksumValid() {
        if (this.checksum == null) {
            // for old events we don't have a checksum
            return true;
        }

        return calculateChecksum().equals(checksum);
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

        boolean alreadyValidated;
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

    private JSONObject getJSONObjectForAppEvent(
            String contextName,
            @NonNull String eventName,
            Double valueToSum,
            Bundle parameters,
            @Nullable final UUID currentSessionId
    ) throws JSONException {
        validateIdentifier(eventName);

        JSONObject eventObject = new JSONObject();

        eventObject.put(Constants.EVENT_NAME_EVENT_KEY, eventName);
        eventObject.put(Constants.EVENT_NAME_MD5_EVENT_KEY, md5Checksum(eventName));
        eventObject.put(Constants.LOG_TIME_APP_EVENT_KEY, System.currentTimeMillis() / 1000);
        eventObject.put("_ui", contextName);
        if (currentSessionId != null) {
            eventObject.put("_session_id", currentSessionId);
        }

        if (parameters != null) {
            Map<String, String> processedParam = validateParameters(parameters);
            for (String key : processedParam.keySet()) {
                eventObject.put(key, processedParam.get(key));
            }
        }

        if (valueToSum != null) {
            eventObject.put(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, valueToSum.doubleValue());
        }

        if (inBackground) {
            eventObject.put("_inBackground", "1");
        }

        if (isImplicit) {
            eventObject.put("_implicitlyLogged", "1");
        } else {
            Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                    "Created app event '%s'", eventObject.toString());
        }

        return eventObject;
    }

    private Map<String, String> validateParameters(
            Bundle parameters) throws FacebookException {

        Map<String, String> paramMap = new HashMap<>();
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

            paramMap.put(key, value.toString());
        }

        RestrictiveDataManager.processParameters(paramMap, name);

        return paramMap;
    }

    // OLD VERSION DO NOT USE
    static class SerializationProxyV1 implements Serializable {
        private static final long serialVersionUID = -2488473066578201069L;
        private final String jsonString;
        private final boolean isImplicit;
        private final boolean inBackground;

        private SerializationProxyV1(String jsonString, boolean isImplicit, boolean inBackground) {
            this.jsonString = jsonString;
            this.isImplicit = isImplicit;
            this.inBackground = inBackground;
        }

        private Object readResolve() throws JSONException {
            return new AppEvent(jsonString, isImplicit, inBackground, null);
        }
    }

    static class SerializationProxyV2 implements Serializable {
        private static final long serialVersionUID = 2016_08_03_001L;
        private final String jsonString;
        private final boolean isImplicit;
        private final boolean inBackground;
        private final String checksum;

        private SerializationProxyV2(
                String jsonString,
                boolean isImplicit,
                boolean inBackground,
                String checksum) {
            this.jsonString = jsonString;
            this.isImplicit = isImplicit;
            this.inBackground = inBackground;
            this.checksum = checksum;
        }

        private Object readResolve() throws JSONException {
            return new AppEvent(jsonString, isImplicit, inBackground, checksum);
        }
    }

    private Object writeReplace() {
        return new SerializationProxyV2(jsonObject.toString(), isImplicit, inBackground, checksum);
    }

    @Override
    public String toString() {
        return String.format(
                "\"%s\", implicit: %b, json: %s",
                jsonObject.optString("_eventName"),
                isImplicit,
                jsonObject.toString());
    }

    private String calculateChecksum() {
        // JSONObject.toString() doesn't guarantee order of the keys on KitKat
        // (API Level 19) and below as JSONObject used HashMap internally,
        // starting Android API Level 20+, JSONObject changed to use LinkedHashMap
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return md5Checksum(jsonObject.toString());
        }
        ArrayList<String> keys = new ArrayList<>();
        for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext();) {
            keys.add(iterator.next());
        }
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append(" = ").append(jsonObject.optString(key)).append('\n');
        }
        return md5Checksum(sb.toString());
    }

    private static String md5Checksum(String toHash)
    {
        String hash;
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();
            hash = AppEventUtility.bytesToHex( bytes );
        }
        catch(NoSuchAlgorithmException e )
        {
            Utility.logd("Failed to generate checksum: ", e);
            return "0";
        }
        catch(UnsupportedEncodingException e )
        {
            Utility.logd("Failed to generate checksum: ", e);
            return "1";
        }
        return hash;
    }

}
