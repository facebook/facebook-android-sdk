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

import android.annotation.TargetApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashSet;
import java.util.Iterator;
import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONObject;

public class TestUtils {
  public static final double DOUBLE_EQUALS_DELTA = 0.00001;

  public static <E extends Parcelable> E parcelAndUnparcel(final E object) {
    final Parcel writeParcel = Parcel.obtain();
    final Parcel readParcel = Parcel.obtain();
    try {
      writeParcel.writeParcelable(object, 0);
      final byte[] bytes = writeParcel.marshall();
      readParcel.unmarshall(bytes, 0, bytes.length);
      readParcel.setDataPosition(0);
      return readParcel.readParcelable(object.getClass().getClassLoader());
    } finally {
      writeParcel.recycle();
      readParcel.recycle();
    }
  }

  @TargetApi(16)
  public static void assertEquals(final JSONObject expected, final JSONObject actual) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return;
    }
    Assert.failNotEquals("", expected, actual);
  }

  @TargetApi(16)
  public static void assertEquals(final JSONArray expected, final JSONArray actual) {
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
