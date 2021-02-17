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

package com.facebook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

/** Tests that passing wrong type in any part of SDK settings doesn't cause SDK to throw */
@RunWith(ParameterizedRobolectricTestRunner.class)
public abstract class FacebookFuzzyInputPowerMockTestCase extends FacebookPowerMockTestCase {
  private static String VALUE_FOR_NEW_KEY = "NEW_KEY";
  private static final int POSSIBLE_VALUES_COUNT = 10;

  public abstract void functionToTest(JSONObject inputJson);

  @ParameterizedRobolectricTestRunner.Parameters
  public static Collection<Object[]> getParameters() {
    // Call getParametersForJSONString(<your_json>) instead
    throw new IllegalStateException("getParameters() is not specified for the test");
  }

  private static Object[] createTestCase(
      String validJson, String nameToReplace, Integer valueToReplaceIndex) {
    return new Object[] {validJson, nameToReplace, valueToReplaceIndex};
  }

  protected static Collection<Object[]> getParametersForJSONString(String validJsonString) {
    ArrayList<Object[]> result = new ArrayList<>();
    // All of the code below is just a sad attempt to change value in JSON for tests.
    // JSON package is unmocked(and unavailable here). There's probably a way to make JSON
    //  work, but I didn't come up with it in a reasonable time.
    int offset = 0;
    while (true) {
      int firstPosition = validJsonString.indexOf("\"", offset);
      if (firstPosition == -1) {
        break;
      }
      int secondPosition = validJsonString.indexOf("\"", firstPosition + 1);
      if (secondPosition == -1) {
        break;
      }
      if (validJsonString.codePointAt(secondPosition + 1) == ':') {
        String nameToReplace = validJsonString.substring(firstPosition + 1, secondPosition);
        for (int i = 0; i < POSSIBLE_VALUES_COUNT; i++) {
          result.add(createTestCase(validJsonString, nameToReplace, i));
        }
      }
      offset = secondPosition + 1;
    }
    return result;
  }

  @ParameterizedRobolectricTestRunner.Parameter(0)
  @Nullable
  public String inputJson;

  @ParameterizedRobolectricTestRunner.Parameter(1)
  @Nullable
  public String nameToReplace;

  @ParameterizedRobolectricTestRunner.Parameter(2)
  public int valueToReplaceIndex;

  private static boolean replaceJSONObjectOrArrayKey(
      Object json, String keyToReplace, Object valueToReplace) throws JSONException {
    if (json instanceof JSONObject) {
      if (replaceJSONObjectKey((JSONObject) json, keyToReplace, valueToReplace)) {
        return true;
      }
    }
    if (json instanceof JSONArray) {
      if (replaceJSONArrayKey((JSONArray) json, keyToReplace, valueToReplace)) {
        return true;
      }
    }
    return false;
  }

  private static boolean replaceJSONObjectKey(
      JSONObject json, String keyToReplace, Object valueToReplace) throws JSONException {
    if (json.has(keyToReplace)) {
      if (VALUE_FOR_NEW_KEY.equals(valueToReplace)
          && json.get(keyToReplace) instanceof JSONObject) {
        JSONObject child = (JSONObject) json.get(keyToReplace);
        child.put("new_key", "new_value");
      } else {
        json.put(keyToReplace, valueToReplace);
      }
      return true;
    }
    for (Iterator it = json.keys(); it.hasNext(); ) {
      String key = (String) it.next();
      Object value = json.get(key);
      if (replaceJSONObjectOrArrayKey(value, keyToReplace, valueToReplace)) {
        return true;
      }
    }
    return false;
  }

  private static boolean replaceJSONArrayKey(
      JSONArray json, String keyToReplace, Object valueToReplace) throws JSONException {
    for (int i = 0; i < json.length(); i++) {
      Object value = json.get(i);
      if (replaceJSONObjectOrArrayKey(value, keyToReplace, valueToReplace)) {
        return true;
      }
    }
    return false;
  }

  @Test(timeout = 1000) // 1 sec
  public void testGenFuzzy() {
    // Values for replacement. Variety of different types to try to catch some edge case.
    List<Object> possibleValues =
        new ArrayList<Object>() {
          {
            add(null);
            add(new JSONObject());
            add(new JSONArray());
            add("1");
            add("100000000000000000");
            add("");
            add("///////////////////:://////////::");
            add(1);
            add(0.1);
            add(VALUE_FOR_NEW_KEY);
          }
        };
    assertEquals(possibleValues.size(), POSSIBLE_VALUES_COUNT);

    if (nameToReplace == null || inputJson == null) {
      fail("At least one of the test arguments is null");
      return; // To help with type inference
    }

    Object valueToReplace = possibleValues.get(valueToReplaceIndex);
    JSONObject json = new JSONObject();
    try {
      json = new JSONObject(inputJson);
      assertTrue(replaceJSONObjectOrArrayKey(json, nameToReplace, valueToReplace));
    } catch (JSONException e) {
      fail();
    }
    functionToTest(json);
  }
}
