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

package com.facebook.share.model;

import java.util.Arrays;
import java.util.List;
import junit.framework.Assert;

public class ShareModelTestUtility {

  public static <E> void assertEquals(final E a, final E b) {
    if (a == null) {
      // if a is null, b should be null
      Assert.assertNull(b);
    } else if ((a instanceof boolean[]) && (b instanceof boolean[])) {
      // check for arrays of all of the primitive types, then arrays of Object, and route
      // those through Arrays equals
      Assert.assertTrue(Arrays.equals((boolean[]) a, (boolean[]) b));
    } else if ((a instanceof byte[]) && (b instanceof byte[])) {
      Assert.assertTrue(Arrays.equals((byte[]) a, (byte[]) b));
    } else if ((a instanceof char[]) && (b instanceof char[])) {
      Assert.assertTrue(Arrays.equals((char[]) a, (char[]) b));
    } else if ((a instanceof double[]) && (b instanceof double[])) {
      Assert.assertTrue(Arrays.equals((double[]) a, (double[]) b));
    } else if ((a instanceof float[]) && (b instanceof float[])) {
      Assert.assertTrue(Arrays.equals((float[]) a, (float[]) b));
    } else if ((a instanceof int[]) && (b instanceof int[])) {
      Assert.assertTrue(Arrays.equals((int[]) a, (int[]) b));
    } else if ((a instanceof long[]) && (b instanceof long[])) {
      Assert.assertTrue(Arrays.equals((long[]) a, (long[]) b));
    } else if ((a instanceof short[]) && (b instanceof short[])) {
      Assert.assertTrue(Arrays.equals((short[]) a, (short[]) b));
    } else if ((a instanceof Object[]) && (b instanceof Object[])) {
      Assert.assertTrue(Arrays.deepEquals((Object[]) a, (Object[]) b));
    } else if ((a instanceof List) && (b instanceof List)) {
      // check for Lists
      assertEquals((List) a, (List) b);
    } else if ((a instanceof ShareModel) && (b instanceof ShareModel)) {
      // check for ShareModels
      assertEquals((ShareModel) a, (ShareModel) b);
    } else {
      // now use Object.equals
      Assert.assertTrue(a.equals(b));
    }
  }

  public static void assertEquals(final List a, final List b) {
    final int size = a.size();
    Assert.assertEquals(size, b.size());
    for (int i = 0; i < size; ++i) {
      assertEquals(a.get(i), b.get(i));
    }
  }

  public static <E extends ShareModel> void assertEquals(final E a, final E b) {
    if ((a instanceof SharePhoto) && (b instanceof SharePhoto)) {
      assertEquals((SharePhoto) a, (SharePhoto) b);
    } else if ((a instanceof SharePhotoContent) && (b instanceof SharePhotoContent)) {
      assertEquals((SharePhotoContent) a, (SharePhotoContent) b);
    } else if ((a instanceof ShareLinkContent) && (b instanceof ShareLinkContent)) {
      assertEquals((ShareLinkContent) a, (ShareLinkContent) b);
    } else if ((a instanceof ShareVideo) && (b instanceof ShareVideo)) {
      assertEquals((ShareVideo) a, (ShareVideo) b);
    } else if ((a instanceof ShareVideoContent) && (b instanceof ShareVideoContent)) {
      assertEquals((ShareVideoContent) a, (ShareVideoContent) b);
    } else {
      Assert.fail(a.getClass().toString() + " models do not have an equality test");
    }
  }

  public static void assertEquals(final SharePhoto a, final SharePhoto b) {
    Assert.assertEquals(a.getBitmap(), b.getBitmap());
    Assert.assertEquals(a.getImageUrl(), b.getImageUrl());
    Assert.assertEquals(a.getUserGenerated(), b.getUserGenerated());
  }

  public static void assertEquals(final SharePhotoContent a, final SharePhotoContent b) {
    assertEquals(a.getPhotos(), b.getPhotos());
    assertContentEquals(a, b);
  }

  public static void assertEquals(final ShareLinkContent a, final ShareLinkContent b) {
    assertContentEquals(a, b);
  }

  public static void assertEquals(final ShareVideo a, final ShareVideo b) {
    Assert.assertEquals(a.getLocalUrl(), b.getLocalUrl());
  }

  public static void assertEquals(final ShareVideoContent a, final ShareVideoContent b) {
    assertEquals(a.getPreviewPhoto(), b.getPreviewPhoto());
    assertEquals(a.getVideo(), b.getVideo());
    assertContentEquals(a, b);
  }

  private static void assertContentEquals(final ShareContent a, final ShareContent b) {
    Assert.assertEquals(a.getContentUrl(), b.getContentUrl());
    Assert.assertEquals(a.getPeopleIds(), b.getPeopleIds());
    Assert.assertEquals(a.getPlaceId(), b.getPlaceId());
    Assert.assertEquals(a.getRef(), b.getRef());
  }

  private ShareModelTestUtility() {}
}
