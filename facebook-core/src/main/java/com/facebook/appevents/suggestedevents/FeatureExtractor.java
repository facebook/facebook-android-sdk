// Copyright 2004-present Facebook. All Rights Reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.facebook.appevents.suggestedevents;

import android.support.annotation.Nullable;
import android.text.InputType;

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.facebook.appevents.internal.ViewHierarchyConstants.*;

@AutoHandleExceptions
final class FeatureExtractor {
    private static final int NUM_OF_FEATURES = 30;

    private static final String REGEX_CR_PASSWORD_FIELD = "password";
    private static final String REGEX_CR_HAS_CONFIRM_PASSWORD_FIELD =
            "(?i)(confirm.*password)|(password.*(confirmation|confirm)|confirmation)";
    private static final String REGEX_CR_HAS_LOG_IN_KEYWORDS = "(?i)(sign in)|login|signIn";
    private static final String REGEX_CR_HAS_SIGN_ON_KEYWORDS = "(?i)(sign.*(up|now)|registration|"
            + "register|(create|apply).*(profile|account)|open.*account|"
            + "account.*(open|creation|application)|enroll|join.*now)";
    private static final String REGEX_ADD_TO_CART_BUTTON_TEXT = "(?i)add to(\\s|\\Z)|update(\\s|\\Z)|cart";
    private static final String REGEX_ADD_TO_CART_PAGE_TITLE = "(?i)add to(\\s|\\Z)|update(\\s|\\Z)|cart|shop|buy";

    private static Map<String, String> languageInfo;
    private static Map<String, String> eventInfo;
    private static Map<String, String> textTypeInfo;
    private static JSONObject rules;
    private static boolean initialized = false;

    static void initialize(File file) {
        try {
            rules = new JSONObject();
            InputStream inputStream = new FileInputStream(file);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            rules = new JSONObject(new String(buffer, "UTF-8"));
        } catch (Exception ex) {
            return;
        }


        // initialize some static fields
        languageInfo = new HashMap<>();
        languageInfo.put(ENGLISH, "1");
        languageInfo.put(GERMAN, "2");
        languageInfo.put(SPANISH, "3");
        languageInfo.put(JAPANESE, "4");

        eventInfo = new HashMap<>();
        eventInfo.put(VIEW_CONTENT, "0");
        eventInfo.put(SEARCH, "1");
        eventInfo.put(ADD_TO_CART, "2");
        eventInfo.put(ADD_TO_WISHLIST, "3");
        eventInfo.put(INITIATE_CHECKOUT, "4");
        eventInfo.put(ADD_PAYMENT_INFO, "5");
        eventInfo.put(PURCHASE, "6");
        eventInfo.put(LEAD, "7");
        eventInfo.put(COMPLETE_REGISTRATION, "8");

        textTypeInfo = new HashMap<>();
        textTypeInfo.put(BUTTON_TEXT, "1");
        textTypeInfo.put(PAGE_TITLE, "2");
        textTypeInfo.put(RESOLVED_DOCUMENT_LINK, "3");
        textTypeInfo.put(BUTTON_ID, "4");

        initialized = true;
    }

    static boolean isInitialized() {
        return initialized;
    }

    static String getTextFeature(String buttonText, String activityName, String appName) {
        // intentionally use "|" and "," to separate to respect how text processed during training
        return (appName + " | " + activityName + ", " + buttonText).toLowerCase();
    }

    @Nullable
    static float[] getDenseFeatures(JSONObject viewHierarchy, String appName) {
        // sanity check
        if (!initialized) {
            return null;
        }
        float[] ret = new float[NUM_OF_FEATURES];
        Arrays.fill(ret, 0);
        try {
            appName = appName.toLowerCase();
            JSONObject viewTree = new JSONObject(
                    viewHierarchy.optJSONObject(VIEW_KEY).toString()); // copy

            String screenName = viewHierarchy.optString(SCREEN_NAME_KEY);
            JSONArray siblings = new JSONArray();

            pruneTree(viewTree, siblings); // viewTree is updated here
            float[] parseResult = parseFeatures(viewTree);
            sum(ret, parseResult);

            JSONObject interactedNode = getInteractedNode(viewTree);
            if (interactedNode == null) {
                return null;
            }
            float[] nonparseFeatures = nonparseFeatures(interactedNode, siblings,
                    screenName, viewTree.toString(), appName);
            sum(ret, nonparseFeatures);
            return ret;
        } catch (JSONException je) {
            /*no op*/
        }

        return ret;
    }

    private static float[] parseFeatures(JSONObject node) {
        float[] densefeat = new float[NUM_OF_FEATURES];
        Arrays.fill(densefeat, 0);
        String text = node.optString(TEXT_KEY).toLowerCase();
        String hint = node.optString(HINT_KEY).toLowerCase();
        String className = node.optString(CLASS_NAME_KEY).toLowerCase();
        int inputType = node.optInt(INPUT_TYPE_KEY, -1);

        String[] textValues = new String[] {text, hint};

        if (matchIndicators(
                new String[] {"$", "amount", "price", "total"}, textValues)) {
            densefeat[0] += 1.0;
        }

        if (matchIndicators(
                new String[] {"password", "pwd"}, textValues)) {
            densefeat[1] += 1.0;
        }

        if (matchIndicators(
                new String[] {"tel", "phone"}, textValues)) {
            densefeat[2] += 1.0;
        }

        if (matchIndicators(
                new String[] {"search"}, textValues)) {
            densefeat[4] += 1.0;
        }

        // input form -- EditText
        if (inputType >= 0) {
            densefeat[5] += 1.0;
        }

        // phone
        if (inputType == InputType.TYPE_CLASS_PHONE || inputType == InputType.TYPE_CLASS_NUMBER) {
            densefeat[6] += 1.0;
        }

        // email
        if (inputType == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                ||  android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            densefeat[7] += 1.0;
        }

        // Check Box
        if (className.contains("checkbox")) {
            densefeat[8] += 1.0;
        }

        if (matchIndicators(
                new String[] {"complete", "confirm", "done", "submit"},
                new String[] {text})) {
            densefeat[10] += 1.0;
        }

        // Radio Button
        if (className.contains("radio") && className.contains("button")) {
            densefeat[12] += 1.0;
        }

        try {
            JSONArray childViews = node.optJSONArray(CHILDREN_VIEW_KEY);
            int len = childViews.length();
            for (int i = 0; i < len; i++) {
                sum(densefeat, parseFeatures(childViews.getJSONObject(i)));
            }
        } catch (JSONException je) {
            /*no op*/
        }

        return densefeat;
    }

    private static float[] nonparseFeatures(JSONObject node,
                                            JSONArray siblings,
                                            String screenName,
                                            String formFieldsJSON,
                                            String appName) {

        float[] densefeat = new float[NUM_OF_FEATURES];
        Arrays.fill(densefeat, 0);

        int siblingLen = siblings.length();
        densefeat[3] = (float) (siblingLen > 1 ? siblingLen - 1 : 0);

        try {
            for (int i = 0; i < siblings.length(); i++) {
                if (isButton(siblings.getJSONObject(i))) {
                    densefeat[9] += 1;
                }
            }
        } catch (JSONException je) {
            /*no op*/
        }

        densefeat[13] = -1;
        densefeat[14] = -1;

        String pageTitle = screenName + '|' + appName;

        StringBuilder hintSB = new StringBuilder();
        StringBuilder textSB = new StringBuilder();
        updateHintAndTextRecursively(node, textSB, hintSB);
        String buttonID = hintSB.toString();
        String buttonText = textSB.toString();

        // [1] CompleteRegistration specific features
        densefeat[15] = regexMatched(ENGLISH, COMPLETE_REGISTRATION, BUTTON_TEXT, buttonText)
                ? 1 : 0;
        densefeat[16] = regexMatched(ENGLISH, COMPLETE_REGISTRATION, PAGE_TITLE, pageTitle)
                ? 1 : 0;
        densefeat[17] = regexMatched(ENGLISH, COMPLETE_REGISTRATION, BUTTON_ID, buttonID)
                ? 1 : 0;

        // TODO: (T54293420) update the logic to include inputtype
        densefeat[18] = formFieldsJSON.contains(REGEX_CR_PASSWORD_FIELD) ? 1 : 0;

        densefeat[19] = regexMatched(REGEX_CR_HAS_CONFIRM_PASSWORD_FIELD, formFieldsJSON)
                ? 1 : 0;
        densefeat[20] = regexMatched(REGEX_CR_HAS_LOG_IN_KEYWORDS, formFieldsJSON)
                ? 1 : 0;
        densefeat[21] = regexMatched(REGEX_CR_HAS_SIGN_ON_KEYWORDS, formFieldsJSON)
                ? 1 : 0;

        // [2] Purchase specific features
        densefeat[22] = regexMatched(ENGLISH, PURCHASE, BUTTON_TEXT, buttonText)
                ? 1 : 0;
        densefeat[24] = regexMatched(ENGLISH, PURCHASE, PAGE_TITLE, pageTitle)
                ? 1 : 0;

        // [3] AddToCart specific features
        densefeat[25] = regexMatched(REGEX_ADD_TO_CART_BUTTON_TEXT, buttonText)
                ? 1 : 0;
        densefeat[27] = regexMatched(REGEX_ADD_TO_CART_PAGE_TITLE, pageTitle)
                ? 1 : 0;

        // [4] Lead specific features
        // TODO: (T54293420) do we need to remove this part?
        densefeat[28] = regexMatched(ENGLISH, LEAD, BUTTON_TEXT, buttonText)
                ? 1 : 0;
        densefeat[29] = regexMatched(ENGLISH, LEAD, PAGE_TITLE, pageTitle)
                ? 1 : 0;

        return densefeat;
    }

    private static boolean regexMatched(String language,
                                        String event, String textType, String matchText) {
        String regex = rules.optJSONObject("rulesForLanguage")
                .optJSONObject(languageInfo.get(language))
                .optJSONObject("rulesForEvent")
                .optJSONObject(eventInfo.get(event))
                .optJSONObject("positiveRules")
                .optString(textTypeInfo.get(textType));
        return regexMatched(regex, matchText);
    }

    private static boolean regexMatched(String pattern, String matchText) {
        return Pattern.compile(pattern).matcher(matchText).find();
    }

    private static boolean matchIndicators(String[] indicators, String[] values) {
        for (String indicator : indicators) {
            for (String value : values) {
                if (value.contains(indicator)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean pruneTree(JSONObject node, JSONArray siblings) {
        try {
            boolean isInteracted = node.optBoolean(IS_INTERACTED_KEY);
            if (isInteracted) {
                return true;
            }

            boolean isChildInteracted = false;
            boolean isDescendantInteracted = false;

            JSONArray childViews = node.optJSONArray(CHILDREN_VIEW_KEY);
            for (int i = 0; i < childViews.length(); i++) {
                JSONObject child = childViews.getJSONObject(i);
                if (child.optBoolean(IS_INTERACTED_KEY)) {
                    isChildInteracted = true;
                    isDescendantInteracted = true;
                    break;
                }
            }

            JSONArray newChildren = new JSONArray();
            if (isChildInteracted) {
                for (int i = 0; i < childViews.length(); i++) {
                    JSONObject child = childViews.getJSONObject(i);
                    siblings.put(child);
                }
            } else {
                for (int i = 0; i < childViews.length(); i++) {
                    JSONObject child = childViews.getJSONObject(i);
                    if (pruneTree(child, siblings)) {
                        isDescendantInteracted = true;
                        newChildren.put(child);
                    }
                }
                node.put(CHILDREN_VIEW_KEY, newChildren);
            }

            return isDescendantInteracted;
        } catch (JSONException je) {
            /*no op*/
        }

        return false;
    }

    private static void sum(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] + b[i];
        }
    }

    private static boolean isButton(JSONObject node) {
        int classTypeBitmask = node.optInt(CLASS_TYPE_BITMASK_KEY);
        return (classTypeBitmask & 1 << CLICKABLE_VIEW_BITMASK) > 0;
    }

    private static void updateHintAndTextRecursively(JSONObject view,
                                          StringBuilder textSB, StringBuilder hintSB) {
        String text = view.optString(TEXT_KEY, "").toLowerCase();
        String hint = view.optString(HINT_KEY, "").toLowerCase();
        if (!text.isEmpty()) {
            textSB.append(text).append(" ");
        }
        if (!hint.isEmpty()) {
            hintSB.append(hint).append(" ");
        }

        JSONArray children = view.optJSONArray(CHILDREN_VIEW_KEY);
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length(); i++) {
            try {
                JSONObject currentChildView = children.getJSONObject(i);
                updateHintAndTextRecursively(currentChildView, textSB, hintSB);
            } catch (JSONException je) {
                /*no opt*/
            }
        }
    }

    @Nullable
    private static JSONObject getInteractedNode(final JSONObject view) {
        try {
            if (view.optBoolean(IS_INTERACTED_KEY)) {
                return view;
            }

            JSONArray children = view.optJSONArray(CHILDREN_VIEW_KEY);
            if (children == null) {
                return null;
            }

            for (int i = 0; i < children.length(); i++) {
                JSONObject sub = getInteractedNode(children.getJSONObject(i));
                if (sub != null) {
                    return sub;
                }
            }
        } catch (JSONException je) {
            /*no op*/
        }

        return null;
    }
}
