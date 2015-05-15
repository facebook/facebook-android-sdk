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

package com.facebook.share.model;

import com.facebook.FacebookTestCase;
import com.facebook.TestUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ShareOpenGraphActionBuilderTest extends FacebookTestCase {
    @Test
    public void testVideoBuilder() {
        final ShareOpenGraphAction action =
                ShareModelTestUtility.getOpenGraphActionBuilder().build();
        assertEquals(ShareModelTestUtility.OPEN_GRAPH_ACTION_TYPE, action.getActionType());
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_VALUE,
                action.getBoolean(
                        ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_VALUE_KEY,
                        !ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_VALUE));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_VALUE,
                action.getBoolean(
                        ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY,
                        ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_VALUE));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_ARRAY,
                action.getBooleanArray(ShareModelTestUtility.OPEN_GRAPH_BOOLEAN_ARRAY_KEY));
        assertNull(action.getBooleanArray(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_DOUBLE_VALUE,
                action.getDouble(ShareModelTestUtility.OPEN_GRAPH_DOUBLE_VALUE_KEY, 0),
                TestUtils.DOUBLE_EQUALS_DELTA);
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_DOUBLE_VALUE,
                action.getDouble(
                        ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY,
                        ShareModelTestUtility.OPEN_GRAPH_DOUBLE_VALUE),
                TestUtils.DOUBLE_EQUALS_DELTA);
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_DOUBLE_ARRAY,
                action.getDoubleArray(ShareModelTestUtility.OPEN_GRAPH_DOUBLE_ARRAY_KEY));
        assertNull(action.getDoubleArray(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_INT_VALUE,
                action.getInt(ShareModelTestUtility.OPEN_GRAPH_INT_VALUE_KEY, 0));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_INT_VALUE,
                action.getInt(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY,
                        ShareModelTestUtility.OPEN_GRAPH_INT_VALUE));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_INT_ARRAY,
                action.getIntArray(ShareModelTestUtility.OPEN_GRAPH_INT_ARRAY_KEY));
        assertNull(action.getIntArray(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_LONG_VALUE,
                action.getLong(ShareModelTestUtility.OPEN_GRAPH_LONG_VALUE_KEY, 0));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_LONG_VALUE,
                action.getLong(
                        ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY,
                        ShareModelTestUtility.OPEN_GRAPH_LONG_VALUE));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_LONG_ARRAY,
                action.getLongArray(ShareModelTestUtility.OPEN_GRAPH_LONG_ARRAY_KEY));
        assertNull(action.getLongArray(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_STRING,
                action.getString(ShareModelTestUtility.OPEN_GRAPH_STRING_KEY));
        assertNull(action.getString(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        assertEquals(
                ShareModelTestUtility.OPEN_GRAPH_STRING_ARRAY_LIST,
                action.getStringArrayList(ShareModelTestUtility.OPEN_GRAPH_STRING_ARRAY_LIST_KEY));
        assertNull(action.getStringArrayList(ShareModelTestUtility.OPEN_GRAPH_UNUSED_KEY));
        ShareModelTestUtility.assertEquals(action, TestUtils.parcelAndUnparcel(action));
    }
}
