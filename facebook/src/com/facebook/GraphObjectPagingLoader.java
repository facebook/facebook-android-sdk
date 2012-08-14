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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class GraphObjectPagingLoader<T extends GraphObject> {
    private final String cacheIdentity;
    private final PagingMode pagingMode;
    private final Class<? extends GraphObject> graphObjectClass;
    private boolean skipRoundtripIfCached;
    private Request currentRequest;
    private Session sessionOfOriginalRequest;
    private String nextLink;

    private Callback callback;

    public enum PagingMode {
        // Will immediately follow "next" links, as long as UI is present to display results.
        IMMEDIATE,
        // Will immediately follow "next" links regardless of whether UI is present.
        IMMEDIATE_BACKGROUND,
        // Will only follow "next" links when asked to.
        AS_NEEDED
    }

    public interface Callback<T> {
        public void onLoading(String url, GraphObjectPagingLoader loader);

        public void onLoaded(GraphObjectList<T> results, GraphObjectPagingLoader loader);

        public void onFinishedLoadingData(GraphObjectPagingLoader loader);

        public void onError(FacebookException error, GraphObjectPagingLoader loader);
    }

    public GraphObjectPagingLoader(String cacheIdentity, PagingMode pagingMode,
            Class<T> graphObjectClass) {
        this.cacheIdentity = cacheIdentity;
        this.pagingMode = pagingMode;
        this.graphObjectClass = graphObjectClass;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }


    public void startLoading(Request request, boolean skipRoundtripIfCached) {
        this.skipRoundtripIfCached = skipRoundtripIfCached;
        sessionOfOriginalRequest = request.getSession();

        currentRequest = request;
        currentRequest.setCallback(new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                requestCompleted(response);
            }
        });

        if (callback != null) {
            callback.onLoading(nextLink, this);
        }

        // TODO port: caching
        Request.executeBatchAsync(currentRequest);
    }

    private void requestCompleted(Response response) {
        Request request = response.getRequest();
        if (request != currentRequest) {
            return;
        }
        // TODO port: set isCached flag
        currentRequest = null;

        FacebookException error = response.getError();
        PagedResults result = response.getGraphObjectAs(PagedResults.class);
        if (result == null && error == null) {
            // TODO port: create protocol mismatch error
        }

        if (error != null) {
            if (callback != null) {
                callback.onError(error, this);
            }
        } else {
            addResults(result);
        }
    }

    private void addResults(PagedResults result) {
        GraphObjectList<? extends GraphObject> data = result.getData().castToListOf(graphObjectClass);
        if (data.size() == 0) {
            nextLink = null;
            if (callback != null) {
                callback.onFinishedLoadingData(this);
            }
        } else {
            PagingInfo paging = result.getPaging();
            nextLink = paging.getNext();
        }

        if (callback != null) {
            callback.onLoaded(data, this);
        }

        // TODO don't page in immediate if we have no table UI present to put UI into
        if (pagingMode == PagingMode.IMMEDIATE || pagingMode == PagingMode.IMMEDIATE_BACKGROUND) {
            followNextLink();
        }
    }

    private void followNextLink() {
        if (nextLink != null) {
            currentRequest = Request.newGraphPathRequest(sessionOfOriginalRequest, null, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    requestCompleted(response);
                }
            });

            // We rely on the "next" link returned to us being in the right format to return the results we expect.
            HttpURLConnection connection = null;
            try {
                connection = Request.createConnection(new URL(nextLink));
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError(new FacebookException(e), this);
                }
                return;
            }

            if (callback != null) {
                callback.onLoading(nextLink, this);
            }

            // TODO caching
            Request.executeConnectionAsync(connection, currentRequest);
        }
    }

    interface PagingInfo extends GraphObject {
        String getNext();
    }

    interface PagedResults extends GraphObject {
        GraphObjectList<GraphObject> getData();

        PagingInfo getPaging();
    }
}
