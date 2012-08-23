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
    public void testLoaderLoads() throws Exception {
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
