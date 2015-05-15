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

package com.facebook.scrumptious.picker;

import android.content.Context;
import android.support.v4.content.Loader;
import com.facebook.*;
import org.json.JSONArray;

class GraphObjectPagingLoader extends Loader<GraphObjectCursor> {
    private GraphRequest originalRequest;
    private GraphRequest currentRequest;
    private GraphRequest nextRequest;
    private OnErrorListener onErrorListener;
    private GraphObjectCursor cursor;
    private boolean appendResults = false;
    private boolean loading = false;

    public interface OnErrorListener {
        public void onError(FacebookException error, GraphObjectPagingLoader loader);
    }

    public GraphObjectPagingLoader(Context context) {
        super(context);
    }

    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public GraphObjectCursor getCursor() {
        return cursor;
    }

    public void clearResults() {
        nextRequest = null;
        originalRequest = null;
        currentRequest = null;

        deliverResult(null);
    }

    public boolean isLoading() {
        return loading;
    }

    public void startLoading(GraphRequest request) {
        appendResults = false;
        nextRequest = null;
        currentRequest = request;
        currentRequest.setCallback(new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                requestCompleted(response);
            }
        });

        loading = true;
        final GraphRequestBatch batch = new GraphRequestBatch(request);
        GraphRequest.executeBatchAsync(batch);
    }

    public void followNextLink() {
        if (nextRequest != null) {
            appendResults = true;
            currentRequest = nextRequest;

            currentRequest.setCallback(new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    requestCompleted(response);
                }
            });

            loading = true;
            GraphRequest.executeBatchAsync(new GraphRequestBatch(currentRequest));
        }
    }

    @Override
    public void deliverResult(GraphObjectCursor cursor) {
        GraphObjectCursor oldCursor = this.cursor;
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

    private void requestCompleted(GraphResponse response) {
        GraphRequest request = response.getRequest();
        if (request != currentRequest) {
            return;
        }

        loading = false;
        currentRequest = null;

        FacebookRequestError requestError = response.getError();
        FacebookException exception = (requestError == null) ? null : requestError.getException();
        if (response.getJSONObject() == null && exception == null) {
            exception = new FacebookException("GraphObjectPagingLoader received neither a result nor an error.");
        }

        if (exception != null) {
            nextRequest = null;

            if (onErrorListener != null) {
                onErrorListener.onError(exception, this);
            }
        } else {
            addResults(response);
        }
    }

    private void addResults(GraphResponse response) {
        GraphObjectCursor cursorToModify = (cursor == null || !appendResults) ? new GraphObjectCursor() :
                new GraphObjectCursor(cursor);

        JSONArray data = response.getJSONObject().optJSONArray("data");

        boolean haveData = data.length() > 0;

        if (haveData) {
            nextRequest = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT);
            cursorToModify.addGraphObjects(data);
        } else {
            nextRequest = null;
        }
        cursorToModify.setMoreObjectsAvailable(nextRequest != null);

        deliverResult(cursorToModify);
    }
}
