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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

  public abstract void functionToTest(JSONObject inputJson);

  @ParameterizedRobolectricTestRunner.Parameters
  public static Collection<Object[]> getParameters() {
    // Call getParametersForJSONString(<your_json>) instead
    throw new IllegalStateException("getParameters() is not specified for the test");
  }

  private static Object[] createTestCase(
      JSONObject validJson, String nameToReplace, Object valueToReplace) throws JSONException {
    JSONObject value = new JSONObject();
    value.put("value", valueToReplace);
    return new Object[] {validJson.toString(), nameToReplace, value.toString()};
  }

  // Values for replacement. Variety of different types to try to catch some edge case.
  private static final List<Object> possibleValues =
      new ArrayList<Object>() {
        {
          add(null);
          add(new JSONArray());
          add(new JSONObject());
          add("1");
          add("100000000000000000");
          add("");
          add("///////////////////:://////////::");
          add(1);
          add(0.1);
          add(VALUE_FOR_NEW_KEY);
        }
      };

  protected static Collection<Object[]> getParametersForJSONString(String validJsonString) {
    ArrayList<Object[]> result = new ArrayList<>();
    try {
      JSONObject validJson = new JSONObject(validJsonString);
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
        if (validJsonString.charAt(secondPosition + 1) == ':') {
          String nameToReplace = validJsonString.substring(firstPosition + 1, secondPosition);
          for (Object value : possibleValues) {
            result.add(createTestCase(validJson, nameToReplace, value));
          }
        }
        offset = secondPosition + 1;
      }
    } catch (JSONException e) {
      fail("Failed to parse test JSON");
    }

    return result;
  }

  public JSONObject inputJson;
  public String nameToReplace;
  public Object valueToReplace;

  protected FacebookFuzzyInputPowerMockTestCase(
      String inputJson, String nameToReplace, String valueToReplaceEncoded) {
    // Fun fact, Robolectric 3 explodes if you pass JSON types or Object. So we have to pass
    //  everything as a String
    Object valueToReplace = null;
    try {
      this.inputJson = new JSONObject(inputJson);
      valueToReplace = new JSONObject(valueToReplaceEncoded).opt("value");
    } catch (JSONException e) {
      fail(e.getMessage());
    }
    this.nameToReplace = nameToReplace;
    this.valueToReplace = valueToReplace;
  }

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

  @Test
  public void testGenFuzzy() {
    try {
      assertTrue(replaceJSONObjectOrArrayKey(inputJson, nameToReplace, valueToReplace));
    } catch (JSONException e) {
      fail();
    }
    functionToTest(inputJson);
  }
}
