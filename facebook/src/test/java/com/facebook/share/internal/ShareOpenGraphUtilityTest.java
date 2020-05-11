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

package com.facebook.share.internal;

import com.facebook.FacebookTestCase;
import com.facebook.TestUtils;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class ShareOpenGraphUtilityTest extends FacebookTestCase {

  private static final String TYPE_KEY = "type";
  private static final String ACTION_JSON_STRING =
      "{\n"
          + "  \"type\": \"myActionType\",\n"
          + "  \"myObject\": {\n"
          + "    \"fbsdk:create_object\":true,\n"
          + "    \"myString\": \"value\",\n"
          + "    \"myInt\": 42,\n"
          + "    \"myBoolean\": true,\n"
          + "    \"myStringArray\": [\n"
          + "      \"string1\",\n"
          + "      \"string2\",\n"
          + "      \"string3\"\n"
          + "    ],\n"
          + "    \"myObject\": {\n"
          + "      \"fbsdk:create_object\":true,\n"
          + "      \"myPi\": 3.14\n"
          + "    }\n"
          + "  }\n"
          + "}";

  @Test
  public void testToJSONObject() throws IOException, JSONException {
    final JSONObject actual = OpenGraphJSONUtility.toJSONObject(this.getAction(), null);
    final JSONObject expected = this.getActionJSONObject();
    TestUtils.assertEquals(expected, actual);
  }

  private static <E> ArrayList<E> createArrayList(E... params) {
    final ArrayList<E> list = new ArrayList<>();
    Collections.addAll(list, params);
    return list;
  }

  private ShareOpenGraphAction getAction() {
    return new ShareOpenGraphAction.Builder()
        .putString(TYPE_KEY, "myActionType")
        .putObject(
            "myObject",
            new ShareOpenGraphObject.Builder()
                .putString("myString", "value")
                .putInt("myInt", 42)
                .putBoolean("myBoolean", true)
                .putStringArrayList(
                    "myStringArray", createArrayList("string1", "string2", "string3"))
                .putObject(
                    "myObject", new ShareOpenGraphObject.Builder().putDouble("myPi", 3.14).build())
                .build())
        .build();
  }

  private JSONObject getActionJSONObject() throws IOException, JSONException {
    return new JSONObject(ACTION_JSON_STRING);
  }
}
