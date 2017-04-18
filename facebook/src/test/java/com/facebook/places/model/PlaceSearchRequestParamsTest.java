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

package com.facebook.places.model;

import com.facebook.FacebookTestCase;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PlaceSearchRequestParamsTest extends FacebookTestCase {
    @Test
    public void testBuilder() {
        PlaceSearchRequestParams.Builder builder = new PlaceSearchRequestParams.Builder();
        builder.setDistance(123);
        builder.setResultsLimit(22);
        builder.setSearchText("search query");
        builder.addCategory("category1");
        builder.addCategory("category2");
        builder.addField("field1");
        builder.addField("field2");
        PlaceSearchRequestParams params = builder.build();

        assertEquals(123, params.getDistance());
        assertEquals(22, params.getResultsLimit());
        assertEquals("search query", params.getSearchText());
        assertSetEqual(new String[]{"category1", "category2"}, params.getCategories());
        assertSetEqual(new String[]{"field1", "field2"}, params.getFields());
    }

    private void assertSetEqual(String[] expectedValues, Set<String> actualValues) {
        assertEquals(expectedValues.length, actualValues.size());
        for (String expectedValue : expectedValues) {
            assertEquals(true, actualValues.contains(expectedValue));
        }
    }
}
