/*
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

package com.facebook.appevents.aam;

import android.support.annotation.RestrictTo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final class MetadataRule {
    private static final String TAG = MetadataRule.class.getCanonicalName();
    private static List<MetadataRule> rules = new ArrayList<>();
    private static final String FIELD_K = "k";
    private static final String FIELD_V = "v";
    private static final String FILED_K_DELIMITER = ",";
    private String name;
    private List<String> keyRules;
    private String valRule;

    private MetadataRule(String name, List<String> keyRules, String valRule) {
        this.name = name;
        this.keyRules = keyRules;
        this.valRule = valRule;
    }

    static List<MetadataRule> getRules() {
        return new ArrayList<>(rules);
    }

    String getName() {
        return name;
    }

    List<String> getKeyRules() {
        return new ArrayList<>(keyRules);
    }

    String getValRule() {
        return valRule;
    }

    static void updateRules(String rulesFromServer) {
        try {
            rules.clear();
            JSONObject jsonObject = new JSONObject(rulesFromServer);
            constructRules(jsonObject);
        } catch (JSONException e) {

        }
    }

    private static void constructRules(JSONObject jsonObject) {
        try {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!(jsonObject.get(key) instanceof JSONObject)) {
                    continue;
                }
                JSONObject ruleJson = jsonObject.getJSONObject(key);
                if (!ruleJson.has(FIELD_K)
                        || !ruleJson.has(FIELD_V)
                        || ruleJson.getString(FIELD_K).isEmpty()
                        || ruleJson.getString(FIELD_V).isEmpty()) {
                    continue;
                }

                rules.add(new MetadataRule(
                        key,
                        Arrays.asList(ruleJson.getString(FIELD_K).split(FILED_K_DELIMITER)),
                        ruleJson.getString(FIELD_V)));
            }
        } catch (JSONException e) {

        }
    }
}
