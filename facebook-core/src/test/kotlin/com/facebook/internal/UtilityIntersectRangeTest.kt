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

package com.facebook.internal

import com.facebook.FacebookTestCase
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class UtilityIntersectRangeTest : FacebookTestCase() {
  @Test
  fun testIntersectRangesBothEmpty() {
    val range1 = IntArray(0)
    val range2 = IntArray(0)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf())
  }

  @Test
  fun testIntersectRangesOneEmpty() {
    val range1 = IntArray(0)
    val range2 = intArrayOf(1, 10)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf())
  }

  @Test
  fun testIntersectRangesBothSameAndClosed() {
    val range1 = intArrayOf(20, 30)
    val range2 = intArrayOf(20, 30)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(20, 30))
  }

  @Test
  fun testIntersectRangesNoIntersect() {
    val range1 = intArrayOf(20, 30)
    val range2 = intArrayOf(30, 50)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf())
  }

  @Test
  fun testIntersectRangesSubsets() {
    val range1 = intArrayOf(20, 100)
    val range2 = intArrayOf(30, 40, 50, 60, 99, 100)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(30, 40, 50, 60, 99, 100))
  }

  @Test
  fun testIntersectRangesOverlap() {
    val range1 = intArrayOf(20, 40, 60, 80)
    val range2 = intArrayOf(10, 30, 50, 70)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(20, 30, 60, 70))
  }

  @Test
  fun testIntersectRangesDifferentLengthsClosed() {
    val range1 = intArrayOf(20, 40, 60, 80)
    val range2 = intArrayOf(10, 30, 50, 70, 90, 110)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(20, 30, 60, 70))
  }

  @Test
  fun testIntersectRangesDifferentLengthsOneOpen() {
    val range1 = intArrayOf(10, 30, 50, 70, 90, 110)
    val range2 = intArrayOf(20, 40, 60)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(20, 30, 60, 70, 90, 110))
  }

  @Test
  fun testIntersectRangesDifferentLengthsBothOpen() {
    val range1 = intArrayOf(10, 30, 50)
    val range2 = intArrayOf(20, 40, 60)
    val intersectRange = Utility.intersectRanges(range1, range2)
    assertArrayEquals(intersectRange, intArrayOf(20, 30, 60))
  }
}
