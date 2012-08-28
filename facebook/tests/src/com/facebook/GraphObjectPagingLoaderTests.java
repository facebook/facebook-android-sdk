/**
 * Copyright 2012 Facebook
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

package com.facebook;

import android.location.Location;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.List;

public class GraphObjectPagingLoaderTests extends FacebookTestCase {
    @MediumTest
    @LargeTest
    public void testLoaderLoadsAndFollowsNextLinks() throws Exception {
        final GraphObjectPagingLoader<GraphPlace> loader = new GraphObjectPagingLoader<GraphPlace>("TEST",
                GraphObjectPagingLoader.PagingMode.AS_NEEDED, GraphPlace.class);
        CountingCallback<GraphPlace> callback = new CountingCallback<GraphPlace>();
        loader.setCallback(callback);
        TestSession session = openTestSessionWithSharedUser();

        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        final Request request = Request.newPlacesSearchRequest(session, location, 1000, 5, null, null);

        // Need to run this on blocker thread so callbacks are made there.
        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                loader.startLoading(request, false);
            }
        }, false);

        getTestBlocker().waitForSignals(2);
        assertEquals(1, callback.onLoadingCount);
        assertEquals(1, callback.onLoadedCount);
        // We might not get back the exact number we requested because of privacy or other rules on
        // the service side.
        assertTrue(callback.results.size() > 0);

        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                loader.followNextLink();
            }
        }, false);
        getTestBlocker().waitForSignals(2);
        assertEquals(2, callback.onLoadingCount);
        assertEquals(2, callback.onLoadedCount);
    }

    @MediumTest
    @LargeTest
    public void testLoaderContinuesToEndInImmediateMode() throws Exception {
        final GraphObjectPagingLoader<GraphPlace> loader = new GraphObjectPagingLoader<GraphPlace>("TEST",
                GraphObjectPagingLoader.PagingMode.IMMEDIATE, GraphPlace.class);
        CountingCallback<GraphPlace> callback = new CountingCallback<GraphPlace>();
        loader.setCallback(callback);
        TestSession session = openTestSessionWithSharedUser();

        // Issue a request with a radius of 10 meters around the Statue of Liberty.
        Location location = new Location("");
        location.setLatitude(40.689475526529);
        location.setLongitude(-74.045583886723);

        final Request request = Request.newPlacesSearchRequest(session, location, 10, 5, null, null);

        // Need to run this on blocker thread so callbacks are made there.
        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                loader.startLoading(request, false);
            }
        }, false);

        // With such narrow bounds, we expect to get one round-trip with one result, then a second
        // round-trip indicating no data was fetched, plus one call to onFinishedLoadingData.
        getTestBlocker().waitForSignals(5);
        assertEquals(2, callback.onLoadingCount);
        assertEquals(2, callback.onLoadedCount);
        assertEquals(1, callback.onFinishedLoadingDataCount);
        assertEquals(1, callback.results.size());
    }

    @MediumTest
    @LargeTest
    public void testLoaderFinishesImmediatelyOnNoResults() throws Exception {
        final GraphObjectPagingLoader<GraphPlace> loader = new GraphObjectPagingLoader<GraphPlace>("TEST",
                GraphObjectPagingLoader.PagingMode.IMMEDIATE, GraphPlace.class);
        CountingCallback<GraphPlace> callback = new CountingCallback<GraphPlace>();
        loader.setCallback(callback);
        TestSession session = openTestSessionWithSharedUser();

        // Unlikely to ever be a Place here.
        Location location = new Location("");
        location.setLatitude(-1.0);
        location.setLongitude(-1.0);

        final Request request = Request.newPlacesSearchRequest(session, location, 10, 5, null, null);

        // Need to run this on blocker thread so callbacks are made there.
        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                loader.startLoading(request, false);
            }
        }, false);

        getTestBlocker().waitForSignals(3);
        assertEquals(1, callback.onLoadingCount);
        assertEquals(1, callback.onLoadedCount);
        assertEquals(1, callback.onFinishedLoadingDataCount);
        assertEquals(0, callback.results.size());
    }

    private class CountingCallback<T extends GraphObject> implements GraphObjectPagingLoader.Callback<T> {
        public int onLoadingCount;
        public int onLoadedCount;
        public int onFinishedLoadingDataCount;
        public int onErrorCount;
        public final List<T> results = new ArrayList<T>();

        private TestBlocker testBlocker = getTestBlocker();

        @Override
        public void onLoading(String url, GraphObjectPagingLoader loader) {
            ++onLoadingCount;
            testBlocker.signal();
        }

        @Override
        public void onLoaded(GraphObjectList<T> results, GraphObjectPagingLoader loader) {
            ++onLoadedCount;
            this.results.addAll(results);
            testBlocker.signal();
        }

        @Override
        public void onFinishedLoadingData(GraphObjectPagingLoader loader) {
            ++onFinishedLoadingDataCount;
            testBlocker.signal();
        }

        @Override
        public void onError(FacebookException error, GraphObjectPagingLoader loader) {
            ++onErrorCount;
            testBlocker.signal();
        }
    }
}
