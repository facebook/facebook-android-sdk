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

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.Loader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class GraphObjectPagingLoader<T extends GraphObject> extends Loader<SimpleGraphObjectCursor<T>> {
    private final Class<T> graphObjectClass;
    private boolean skipRoundtripIfCached;
    private Request originalRequest;
    private Request currentRequest;
    private String nextLink;
    private OnErrorListener onErrorListener;
    private SimpleGraphObjectCursor<T> cursor;
    private boolean appendResults = false;
    private boolean loading = false;

    public interface OnErrorListener {
        public void onError(FacebookException error, GraphObjectPagingLoader<?> loader);
    }

    public GraphObjectPagingLoader(Context context, Class<T> graphObjectClass) {
        super(context);

        this.graphObjectClass = graphObjectClass;
    }

    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public SimpleGraphObjectCursor<T> getCursor() {
        return cursor;
    }

    public void clearResults() {
        nextLink = null;
        originalRequest = null;
        currentRequest = null;

        deliverResult(null);
    }

    public boolean isLoading() {
        return loading;
    }

    public void startLoading(Request request, boolean skipRoundtripIfCached) {
        originalRequest = request;
        startLoading(request, skipRoundtripIfCached, 0);
    }

    public void refreshOriginalRequest(long afterDelay) {
        if (originalRequest == null) {
            throw new FacebookException(
                    "refreshOriginalRequest may not be called until after startLoading has been called.");
        }
        startLoading(originalRequest, false, afterDelay);
    }

    public void followNextLink() {
        if (nextLink != null) {
            appendResults = true;
            currentRequest = Request.newGraphPathRequest(originalRequest.getSession(), null, new Request.Callback() {
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
                if (onErrorListener != null) {
                    onErrorListener.onError(new FacebookException(e), this);
                }
                return;
            }

            loading = true;
            RequestBatch batch = putRequestIntoBatch(currentRequest, skipRoundtripIfCached);
            batch.setCacheKey(nextLink.toString());
            Request.executeConnectionAsync(connection, batch);
        }
    }

    @Override
    public void deliverResult(SimpleGraphObjectCursor<T> cursor) {
        SimpleGraphObjectCursor<T> oldCursor = this.cursor;
        this.cursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);

            if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
                oldCursor.close();
            }
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        if (cursor != null) {
            deliverResult(cursor);
        }
    }

    private void startLoading(Request request, boolean skipRoundtripIfCached, long afterDelay) {
        this.skipRoundtripIfCached = skipRoundtripIfCached;
        appendResults = false;
        nextLink = null;
        currentRequest = request;
        currentRequest.setCallback(new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                requestCompleted(response);
            }
        });

        // We are considered loading even if we have a delay.
        loading = true;

        final RequestBatch batch = putRequestIntoBatch(request, skipRoundtripIfCached);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Request.executeBatchAsync(batch);
            }
        };
        if (afterDelay == 0) {
            r.run();
        } else {
            Handler handler = new Handler();
            handler.postDelayed(r, afterDelay);
        }
    }

    private RequestBatch putRequestIntoBatch(Request request, boolean skipRoundtripIfCached) {
        // We just use the request URL as the cache key.
        RequestBatch batch = new RequestBatch(request);
        try {
            batch.setCacheKey(request.getUrlForSingleRequest().toString());
        } catch (MalformedURLException e) {
            throw new FacebookException(e);
        }
        batch.setForceRoundTrip(!skipRoundtripIfCached);
        return batch;
    }

    private void requestCompleted(Response response) {
        Request request = response.getRequest();
        if (request != currentRequest) {
            return;
        }

        loading = false;
        currentRequest = null;

        FacebookException error = response.getError();
        PagedResults result = response.getGraphObjectAs(PagedResults.class);
        if (result == null && error == null) {
            error = new FacebookException("GraphObjectPagingLoader received neither a result nor an error.");
        }

        if (error != null) {
            nextLink = null;

            if (onErrorListener != null) {
                onErrorListener.onError(error, this);
            }
        } else {
            boolean fromCache = response.getIsFromCache();
            addResults(result, fromCache);
            // Once we get any set of results NOT from the cache, stop trying to get any future ones
            // from it.
            if (!fromCache) {
                skipRoundtripIfCached = false;
            }
        }
    }

    private void addResults(PagedResults result, boolean fromCache) {
        SimpleGraphObjectCursor<T> cursorToModify = (cursor == null || !appendResults) ? new SimpleGraphObjectCursor<T>() :
                new SimpleGraphObjectCursor<T>(cursor);

        GraphObjectList<T> data = result.getData().castToListOf(graphObjectClass);
        boolean haveData = data.size() > 0;

        if (haveData) {
            PagingInfo paging = result.getPaging();
            if (nextLink != null && nextLink.equals(paging.getNext())) {
                // We got the same "next" link as we just tried to retrieve. This could happen if cached
                // data is invalid. All we can do in this case is pretend we have finished.
                haveData = false;
            } else {
                nextLink = paging.getNext();

                cursorToModify.addGraphObjects(data, fromCache);
                cursorToModify.setMoreObjectsAvailable(true);
            }
        }
        if (!haveData) {
            cursorToModify.setMoreObjectsAvailable(false);
            cursorToModify.setFromCache(fromCache);

            nextLink = null;
        }

        deliverResult(cursorToModify);
    }

    interface PagingInfo extends GraphObject {
        String getNext();
    }

    interface PagedResults extends GraphObject {
        GraphObjectList<GraphObject> getData();

        PagingInfo getPaging();
    }
}
