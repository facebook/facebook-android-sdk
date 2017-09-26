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
package com.facebook.internal;

import com.facebook.FacebookRequestError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class FacebookRequestErrorClassification {
    public static final int EC_SERVICE_UNAVAILABLE = 2;
    public static final int EC_APP_TOO_MANY_CALLS = 4;
    public static final int EC_RATE = 9;
    public static final int EC_USER_TOO_MANY_CALLS = 17;
    public static final int EC_INVALID_SESSION = 102;
    public static final int EC_INVALID_TOKEN = 190;
    public static final int EC_TOO_MANY_USER_ACTION_CALLS = 341;

    public static final String KEY_RECOVERY_MESSAGE = "recovery_message";
    public static final String KEY_NAME = "name";
    public static final String KEY_OTHER = "other";
    public static final String KEY_TRANSIENT = "transient";
    public static final String KEY_LOGIN_RECOVERABLE = "login_recoverable";

    // Key is error code, value is the subcodes. Null subcodes means all subcodes are accepted.
    private final Map<Integer, Set<Integer>> otherErrors;
    private final Map<Integer, Set<Integer>> transientErrors;
    private final Map<Integer, Set<Integer>> loginRecoverableErrors;
    private final String otherRecoveryMessage;
    private final String transientRecoveryMessage;
    private final String loginRecoverableRecoveryMessage;

    private static FacebookRequestErrorClassification defaultInstance;

    FacebookRequestErrorClassification(
            Map<Integer, Set<Integer>> otherErrors,
            Map<Integer, Set<Integer>> transientErrors,
            Map<Integer, Set<Integer>> loginRecoverableErrors,
            String otherRecoveryMessage,
            String transientRecoveryMessage,
            String loginRecoverableRecoveryMessage) {
        this.otherErrors = otherErrors;
        this.transientErrors = transientErrors;
        this.loginRecoverableErrors = loginRecoverableErrors;
        this.otherRecoveryMessage = otherRecoveryMessage;
        this.transientRecoveryMessage = transientRecoveryMessage;
        this.loginRecoverableRecoveryMessage = loginRecoverableRecoveryMessage;
    }

    public Map<Integer, Set<Integer>> getOtherErrors() {
        return otherErrors;
    }

    public Map<Integer, Set<Integer>> getTransientErrors() {
        return transientErrors;
    }

    public Map<Integer, Set<Integer>> getLoginRecoverableErrors() {
        return loginRecoverableErrors;
    }

    public String getRecoveryMessage(FacebookRequestError.Category category) {
        switch (category) {
            case OTHER:
                return otherRecoveryMessage;
            case LOGIN_RECOVERABLE:
                return loginRecoverableRecoveryMessage;
            case TRANSIENT:
                return transientRecoveryMessage;
            default:
                return null;
        }
    }

    public FacebookRequestError.Category classify(
            int errorCode,
            int errorSubCode,
            boolean isTransient) {
        if (isTransient) {
            return FacebookRequestError.Category.TRANSIENT;
        }

        if (otherErrors != null && otherErrors.containsKey(errorCode)) {
            Set<Integer> subCodes = otherErrors.get(errorCode);
            if (subCodes == null || subCodes.contains(errorSubCode)) {
                return FacebookRequestError.Category.OTHER;
            }
        }

        if (loginRecoverableErrors != null && loginRecoverableErrors.containsKey(errorCode)) {
            Set<Integer> subCodes = loginRecoverableErrors.get(errorCode);
            if (subCodes == null || subCodes.contains(errorSubCode)) {
                return FacebookRequestError.Category.LOGIN_RECOVERABLE;
            }
        }

        if (transientErrors != null && transientErrors.containsKey(errorCode)) {
            Set<Integer> subCodes = transientErrors.get(errorCode);
            if (subCodes == null || subCodes.contains(errorSubCode)) {
                return FacebookRequestError.Category.TRANSIENT;
            }
        }
        return FacebookRequestError.Category.OTHER;
    }

    public static synchronized FacebookRequestErrorClassification getDefaultErrorClassification() {
        if (defaultInstance == null) {
            defaultInstance = getDefaultErrorClassificationImpl();
        }
        return defaultInstance;
    }

    private static FacebookRequestErrorClassification getDefaultErrorClassificationImpl() {
        Map<Integer, Set<Integer>> transientErrors = new HashMap<Integer, Set<Integer>>() {{
            put(EC_SERVICE_UNAVAILABLE, null);
            put(EC_APP_TOO_MANY_CALLS, null);
            put(EC_RATE, null);
            put(EC_USER_TOO_MANY_CALLS, null);
            put(EC_TOO_MANY_USER_ACTION_CALLS, null);
        }};

        Map<Integer, Set<Integer>> loginRecoverableErrors = new HashMap<Integer, Set<Integer>>() {{
            put(EC_INVALID_SESSION,null);
            put(EC_INVALID_TOKEN,null);
        }};

        return new FacebookRequestErrorClassification(
                null,
                transientErrors,
                loginRecoverableErrors,
                null,
                null,
                null);
    }

    private static Map<Integer, Set<Integer>> parseJSONDefinition(JSONObject definition) {
        JSONArray itemsArray = definition.optJSONArray("items");
        if (itemsArray.length() == 0) {
            return null;
        }

        Map<Integer, Set<Integer>> items = new HashMap<>();
        for (int i = 0; i < itemsArray.length(); i++) {
            JSONObject item = itemsArray.optJSONObject(i);
            if (item == null) {
                continue;
            }
            int code = item.optInt("code");
            if (code == 0) {
                continue;
            }
            Set<Integer> subcodes = null;
            JSONArray subcodesArray = item.optJSONArray("subcodes");
            if (subcodesArray != null && subcodesArray.length() > 0) {
                subcodes = new HashSet<>();
                for (int j = 0; j < subcodesArray.length(); j++) {
                    int subCode = subcodesArray.optInt(j);
                    if (subCode != 0) {
                        subcodes.add(subCode);
                    }
                }
            }
            items.put(code, subcodes);
        }
        return items;
    }

    public static FacebookRequestErrorClassification createFromJSON(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        Map<Integer, Set<Integer>> otherErrors = null;
        Map<Integer, Set<Integer>> transientErrors = null;
        Map<Integer, Set<Integer>> loginRecoverableErrors = null;
        String otherRecoveryMessage = null;
        String transientRecoveryMessage = null;
        String loginRecoverableRecoveryMessage = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject definition = jsonArray.optJSONObject(i);
            if (definition == null) {
                continue;
            }
            String name = definition.optString(KEY_NAME);
            if (name == null) {
                continue;
            }
            if (name.equalsIgnoreCase(KEY_OTHER)) {
                otherRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null);
                otherErrors = parseJSONDefinition(definition);
            } else if (name.equalsIgnoreCase(KEY_TRANSIENT)) {
                transientRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null);
                transientErrors = parseJSONDefinition(definition);
            } else if (name.equalsIgnoreCase(KEY_LOGIN_RECOVERABLE)) {
                loginRecoverableRecoveryMessage = definition.optString(KEY_RECOVERY_MESSAGE, null);
                loginRecoverableErrors = parseJSONDefinition(definition);
            }
        }
        return new FacebookRequestErrorClassification(
            otherErrors,
            transientErrors,
            loginRecoverableErrors,
            otherRecoveryMessage,
            transientRecoveryMessage,
            loginRecoverableRecoveryMessage
        );
    }
}
