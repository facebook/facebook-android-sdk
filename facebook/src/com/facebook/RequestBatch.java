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

import android.os.Handler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RequestBatch contains a list of Request objects that can be sent to Facebook in a single round-trip.
 */
public class RequestBatch extends AbstractList<Request> {
    private static AtomicInteger idGenerator = new AtomicInteger();

    private Handler callbackHandler;
    private List<Request> requests = new ArrayList<Request>();
    private int timeoutInMilliseconds = 0;
    private final String id = Integer.valueOf(idGenerator.incrementAndGet()).toString();

    /**
     * Constructor. Creates an empty batch.
     */
    public RequestBatch() {
        this.requests = new ArrayList<Request>();
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public RequestBatch(Collection<Request> requests) {
        this.requests = new ArrayList<Request>(requests);
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public RequestBatch(Request... requests) {
        this.requests = Arrays.asList(requests);
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public RequestBatch(RequestBatch requests) {
        this.requests = new ArrayList<Request>(requests);
        this.callbackHandler = requests.callbackHandler;
        this.timeoutInMilliseconds = requests.timeoutInMilliseconds;
    }

    /**
     * Gets the timeout to wait for responses from the server before a timeout error occurs.
     * @return the timeout, in milliseconds; 0 (the default) means do not timeout
     */
    public int getTimeout() {
        return timeoutInMilliseconds;
    }

    /**
     * Sets the timeout to wait for responses from the server before a timeout error occurs.
     * @param timeoutInMilliseconds the timeout, in milliseconds; 0 means do not timeout
     */
    public void setTimeout(int timeoutInMilliseconds) {
        if (timeoutInMilliseconds < 0) {
            throw new IllegalArgumentException("Argument timeoutInMilliseconds must be >= 0.");
        }
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }

    @Override
    public final boolean add(Request request) {
        return requests.add(request);
    }

    @Override
    public final void add(int location, Request request) {
        requests.add(location, request);
    }

    @Override
    public final void clear() {
        requests.clear();
    }

    @Override
    public final Request get(int i) {
        return requests.get(i);
    }

    @Override
    public final Request remove(int location) {
        return requests.remove(location);
    }

    @Override
    public final Request set(int location, Request request) {
        return requests.set(location, request);
    }

    @Override
    public final int size() {
        return requests.size();
    }

    final String getId() {
        return id;
    }

    final Handler getCallbackHandler() {
        return callbackHandler;
    }

    final void setCallbackHandler(Handler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    final List<Request> getRequests() {
        return requests;
    }

    /**
     * Executes this batch on the current thread and returns the responses.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @return a list of Response objects representing the results of the requests; responses are returned in the same
     *         order as the requests were specified.
     *
     * @throws FacebookException
     *            If there was an error in the protocol used to communicate with the service
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException if the passed in RequestBatch or any of its contents are null
     */
    public final List<Response> executeAndWait() {
        return Request.executeBatchAndWait(this);
    }

    /**
     * Executes this batch asynchronously. This function will return immediately, and the batch will
     * be processed on a separate thread. In order to process results of a request, or determine
     * whether a request succeeded or failed, a callback must be specified (see
     * {@link Request#setCallback(com.facebook.Request.Callback)})
     * <p/>
     * This should only be called from the UI thread.
     *
     * @return a RequestAsyncTask that is executing the request
     *
     * @throws IllegalArgumentException if this batch is empty
     * @throws NullPointerException if any of the contents of this batch are null
     */
    public final RequestAsyncTask executeAsync() {
        return Request.executeBatchAsync(this);
    }
}
