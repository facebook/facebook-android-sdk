/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.facebook.internal.Utility;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONObject;

public class TestUtils {

  @TargetApi(16)
  public static void assertEquals(final JSONObject expected, final JSONObject actual) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return;
    }
    Assert.failNotEquals("", expected, actual);
  }

  private static boolean areEqual(final JSONObject expected, final JSONObject actual) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (expected == actual) {
      return true;
    }
    if ((expected == null) || (actual == null)) {
      return false;
    }

    final Iterator<String> expectedKeysIterator = expected.keys();
    final HashSet<String> expectedKeys = new HashSet<String>();
    while (expectedKeysIterator.hasNext()) {
      expectedKeys.add(expectedKeysIterator.next());
    }

    final Iterator<String> actualKeysIterator = actual.keys();
    while (actualKeysIterator.hasNext()) {
      final String key = actualKeysIterator.next();
      if (!areEqual(expected.opt(key), actual.opt(key))) {
        return false;
      }
      expectedKeys.remove(key);
    }
    return expectedKeys.size() == 0;
  }

  private static boolean areEqual(final JSONArray expected, final JSONArray actual) {
    // JSONObject.equals does not do an order-independent comparison, so we need to check values
    // that are JSONObject
    // manually
    if (expected == actual) {
      return true;
    }
    if ((expected == null) || (actual == null)) {
      return false;
    }
    if (expected.length() != actual.length()) {
      return false;
    }

    final int length = expected.length();
    for (int i = 0; i < length; ++i) {
      if (!areEqual(expected.opt(i), actual.opt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean areEqual(final Object expected, final Object actual) {
    if (expected == actual) {
      return true;
    }
    if ((expected == null) || (actual == null)) {
      return false;
    }
    if ((expected instanceof JSONObject) && (actual instanceof JSONObject)) {
      return areEqual((JSONObject) expected, (JSONObject) actual);
    }
    if ((expected instanceof JSONArray) && (actual instanceof JSONArray)) {
      return areEqual((JSONArray) expected, (JSONArray) actual);
    }
    return expected.equals(actual);
  }
}
