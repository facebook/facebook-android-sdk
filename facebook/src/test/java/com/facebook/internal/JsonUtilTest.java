/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.facebook.FacebookTestCase;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class JsonUtilTest extends FacebookTestCase {

  @Test
  public void testJsonObjectClear() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put("hello", "world");
    jsonObject.put("hocus", "pocus");

    JsonUtil.jsonObjectClear(jsonObject);
    assertEquals(0, jsonObject.length());
  }

  @Test
  public void testJsonObjectContainsValue() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put("hello", "world");
    jsonObject.put("hocus", "pocus");

    assertTrue(JsonUtil.jsonObjectContainsValue(jsonObject, "pocus"));
    assertFalse(JsonUtil.jsonObjectContainsValue(jsonObject, "Fred"));
  }

  @Test
  public void testJsonObjectEntrySet() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put("hello", "world");
    jsonObject.put("hocus", "pocus");

    Set<Entry<String, Object>> entrySet = JsonUtil.jsonObjectEntrySet(jsonObject);
    assertEquals(2, entrySet.size());
  }

  @Test
  public void testJsonObjectKeySet() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put("hello", "world");
    jsonObject.put("hocus", "pocus");

    Set<String> keySet = JsonUtil.jsonObjectKeySet(jsonObject);
    assertEquals(2, keySet.size());
    assertTrue(keySet.contains("hello"));
    assertFalse(keySet.contains("world"));
  }

  @Test
  public void testJsonObjectPutAll() throws JSONException {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("hello", "world");
    map.put("hocus", "pocus");

    JSONObject jsonObject = new JSONObject();
    JsonUtil.jsonObjectPutAll(jsonObject, map);

    assertEquals("pocus", jsonObject.get("hocus"));
    assertEquals(2, jsonObject.length());
  }

  @Test
  public void testJsonObjectValues() throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("hello", "world");
    jsonObject.put("hocus", "pocus");

    Collection<Object> values = JsonUtil.jsonObjectValues(jsonObject);

    assertEquals(2, values.size());
    assertTrue(values.contains("world"));
  }
}
