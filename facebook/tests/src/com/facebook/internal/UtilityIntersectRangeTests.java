/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Arrays;

public class UtilityIntersectRangeTests extends AndroidTestCase {

    @SmallTest
    public void testIntersectRangesBothEmpty() {
        int[] range1 = new int[0];
        int[] range2 = new int[0];
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @SmallTest
    public void testIntersectRangesOneEmpty() {
        int[] range1 = new int[0];
        int[] range2 = new int[]{1, 10};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @SmallTest
    public void testIntersectRangesBothSameAndClosed() {
        int[] range1 = new int[]{20, 30};
        int[] range2 = new int[]{20, 30};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30}));
    }

    @SmallTest
    public void testIntersectRangesNoIntersect() {
        int[] range1 = new int[]{20, 30};
        int[] range2 = new int[]{30, 50};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{}));
    }

    @SmallTest
    public void testIntersectRangesSubsets() {
        int[] range1 = new int[]{20, 100};
        int[] range2 = new int[]{30, 40, 50, 60, 99, 100};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{30, 40, 50, 60, 99, 100}));
    }

    @SmallTest
    public void testIntersectRangesOverlap() {
        int[] range1 = new int[]{20, 40, 60, 80};
        int[] range2 = new int[]{10, 30, 50, 70};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70}));
    }

    @SmallTest
    public void testIntersectRangesDifferentLengthsClosed() {
        int[] range1 = new int[]{20, 40, 60, 80};
        int[] range2 = new int[]{10, 30, 50, 70, 90, 110};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70}));
    }

    @SmallTest
    public void testIntersectRangesDifferentLengthsOneOpen() {
        int[] range1 = new int[]{10, 30, 50, 70, 90, 110};
        int[] range2 = new int[]{20, 40, 60};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60, 70, 90, 110}));
    }

    @SmallTest
    public void testIntersectRangesDifferentLengthsBothOpen() {
        int[] range1 = new int[]{10, 30, 50};
        int[] range2 = new int[]{20, 40, 60};
        int[] intersectRange = Utility.intersectRanges(range1, range2);
        assertTrue(Arrays.equals(intersectRange, new int[]{20, 30, 60}));
    }
}
