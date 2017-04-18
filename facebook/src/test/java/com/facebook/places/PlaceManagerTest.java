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

package com.facebook.places;

import android.location.Location;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.places.model.CurrentPlaceFeedbackRequestParams;
import com.facebook.places.model.PlaceInfoRequestParams;
import com.facebook.places.model.PlaceSearchRequestParams;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import static org.junit.Assert.assertEquals;

import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({
        AccessToken.class})
public class PlaceManagerTest extends FacebookPowerMockTestCase {

    @Before
    public void setup() {
        mockStatic(AccessToken.class);
        PowerMockito.when(AccessToken.getCurrentAccessToken()).thenReturn(null);
    }

    @Test
    public void testSearchPlaceForLocationRequest() {
        PlaceSearchRequestParams.Builder builder = new PlaceSearchRequestParams.Builder();
        builder.setSearchText("search text");
        builder.setResultsLimit(5);
        builder.addCategory("category1");
        builder.addCategory("category2");
        builder.addField("field1");
        builder.addField("field2");
        builder.setDistance(500);

        PlaceSearchRequestParams params = builder.build();
        Location location = new Location("dummy");
        location.setLatitude(1);
        location.setLongitude(2);

        GraphRequest request =
                PlaceManager.newPlaceSearchRequestForLocation(params, location);

        assertEquals("search", request.getGraphPath());
        assertEquals(HttpMethod.GET, request.getHttpMethod());

        Bundle requestParams = request.getParameters();

        assertEquals("search text", requestParams.get("q"));
        assertEquals(500, requestParams.get("distance"));
        assertEquals(5, requestParams.get("limit"));
        assertEquals("1.000000,2.000000", requestParams.get("center"));
        assertEquals("field1,field2", requestParams.get("fields"));
        assertEquals("place", requestParams.get("type"));
        assertEquals("[\"category2\",\"category1\"]", requestParams.get("categories"));
    }

    @Test
    public void testPlaceInfoRequest() {
        PlaceInfoRequestParams.Builder builder = new PlaceInfoRequestParams.Builder();
        builder.setPlaceId("12345");
        builder.addField("field1");
        builder.addFields(new String[]{"field2", "field3"});
        PlaceInfoRequestParams params = builder.build();

        GraphRequest request = PlaceManager.newPlaceInfoRequest(params);

        assertEquals("12345", request.getGraphPath());
        assertEquals(HttpMethod.GET, request.getHttpMethod());

        Bundle requestParams = request.getParameters();
        assertEquals("field1,field3,field2", requestParams.get("fields"));
    }

    @Test
    public void testCurrentPlaceFeedbackRequest() {
        CurrentPlaceFeedbackRequestParams.Builder builder =
                new CurrentPlaceFeedbackRequestParams.Builder();
        builder.setPlaceId("12345");
        builder.setTracking("trackingid");
        builder.setWasHere(true);
        CurrentPlaceFeedbackRequestParams params = builder.build();

        GraphRequest request = PlaceManager.newCurrentPlaceFeedbackRequest(params);

        assertEquals("current_place/feedback", request.getGraphPath());
        assertEquals(HttpMethod.POST, request.getHttpMethod());

        Bundle requestParams = request.getParameters();
        assertEquals("12345", requestParams.get("place_id"));
        assertEquals("trackingid", requestParams.get("tracking"));
        assertEquals(true, requestParams.get("was_here"));
    }
}
