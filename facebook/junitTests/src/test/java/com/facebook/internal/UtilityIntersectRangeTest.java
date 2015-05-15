/**
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

package com.facebook.internal;

import com.facebook.FacebookTestCase;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;


public class UtilityIntersectRangeTest extends FacebookTestCase {

    @Test
    public void testIntersectRangesBothEmpty() {
        int[] range1 = new int[0];
        int[] range2 = new int[0];
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @Test
    public void testIntersectRangesOneEmpty() {
        int[] range1 = new int[0];
        int[] range2 = new int[]{1, 10};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @Test
    public void testIntersectRangesBothSameAndClosed() {
        int[] range1 = new int[]{20, 30};
        int[] range2 = new int[]{20, 30};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30}));
    }

    @Test
    public void testIntersectRangesNoIntersect() {
        int[] range1 = new int[]{20, 30};
        int[] range2 = new int[]{30, 50};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @Test
    public void testIntersectRangesSubsets() {
        int[] range1 = new int[]{20, 100};
        int[] range2 = new int[]{30, 40, 50, 60, 99, 100};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{30, 40, 50, 60, 99, 100}));
    }

    @Test
    public void testIntersectRangesOverlap() {
        int[] range1 = new int[]{20, 40, 60, 80};
        int[] range2 = new int[]{10, 30, 50, 70};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70}));
    }

    @Test
    public void testIntersectRangesDifferentLengthsClosed() {
        int[] range1 = new int[]{20, 40, 60, 80};
        int[] range2 = new int[]{10, 30, 50, 70, 90, 110};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70}));
    }

    @Test
    public void testIntersectRangesDifferentLengthsOneOpen() {
        int[] range1 = new int[]{10, 30, 50, 70, 90, 110};
        int[] range2 = new int[]{20, 40, 60};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70, 90, 110}));
    }

    @Test
    public void testIntersectRangesDifferentLengthsBothOpen() {
        int[] range1 = new int[]{10, 30, 50};
        int[] range2 = new int[]{20, 40, 60};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60}));
    }
}
