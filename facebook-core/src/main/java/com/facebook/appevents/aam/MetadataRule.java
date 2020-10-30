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

import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.json.JSONException;
import org.json.JSONObject;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class MetadataRule {
  private static final String TAG = MetadataRule.class.getCanonicalName();
  private static final Set<MetadataRule> rules = new CopyOnWriteArraySet<>();
  private static final String FIELD_K = "k";
  private static final String FIELD_V = "v";
  private static final String FIELD_K_DELIMITER = ",";
  private String name;
  private List<String> keyRules;
  private String valRule;

  private MetadataRule(String name, List<String> keyRules, String valRule) {
    this.name = name;
    this.keyRules = keyRules;
    this.valRule = valRule;
  }

  static Set<MetadataRule> getRules() {
    return new HashSet<>(rules);
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
    Iterator<String> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      JSONObject rule = jsonObject.optJSONObject(key);
      if (rule == null) {
        continue;
      }
      String k = rule.optString(FIELD_K);
      String v = rule.optString(FIELD_V);
      if (k.isEmpty()) {
        continue;
      }
      rules.add(new MetadataRule(key, Arrays.asList(k.split(FIELD_K_DELIMITER)), v));
    }
  }

  public static Set<String> getEnabledRuleNames() {
    Set<String> ruleNames = new HashSet<>();
    for (MetadataRule r : rules) {
      ruleNames.add(r.getName());
    }
    return ruleNames;
  }
}
