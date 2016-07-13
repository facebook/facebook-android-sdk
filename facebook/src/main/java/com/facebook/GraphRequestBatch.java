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

package com.facebook;

import android.os.Handler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RequestBatch contains a list of Request objects that can be sent to Facebook in a single
 * round-trip.
 */
public class GraphRequestBatch extends AbstractList<GraphRequest> {
    private static AtomicInteger idGenerator = new AtomicInteger();

    private Handler callbackHandler;
    private List<GraphRequest> requests = new ArrayList<GraphRequest>();
    private int timeoutInMilliseconds = 0;
    private final String id = Integer.valueOf(idGenerator.incrementAndGet()).toString();
    private List<Callback> callbacks = new ArrayList<Callback>();
    private String batchApplicationId;

    /**
     * Constructor. Creates an empty batch.
     */
    public GraphRequestBatch() {
        this.requests = new ArrayList<GraphRequest>();
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public GraphRequestBatch(Collection<GraphRequest> requests) {
        this.requests = new ArrayList<GraphRequest>(requests);
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public GraphRequestBatch(GraphRequest... requests) {
        this.requests = Arrays.asList(requests);
    }

    /**
     * Constructor.
     * @param requests the requests to add to the batch
     */
    public GraphRequestBatch(GraphRequestBatch requests) {
        this.requests = new ArrayList<GraphRequest>(requests);
        this.callbackHandler = requests.callbackHandler;
        this.timeoutInMilliseconds = requests.timeoutInMilliseconds;
        this.callbacks = new ArrayList<Callback>(requests.callbacks);
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

    /**
     * Adds a batch-level callback which will be called when the entire batch has finished
     * executing.
     *
     * @param callback the callback
     */
    public void addCallback(Callback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * Removes a batch-level callback.
     *
     * @param callback the callback
     */
    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }

    @Override
    public final boolean add(GraphRequest request) {
        return requests.add(request);
    }

    @Override
    public final void add(int location, GraphRequest request) {
        requests.add(location, request);
    }

    @Override
    public final void clear() {
        requests.clear();
    }

    @Override
    public final GraphRequest get(int i) {
        return requests.get(i);
    }

    @Override
    public final GraphRequest remove(int location) {
        return requests.remove(location);
    }

    @Override
    public final GraphRequest set(int location, GraphRequest request) {
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

    final List<GraphRequest> getRequests() {
        return requests;
    }

    final List<Callback> getCallbacks() {
        return callbacks;
    }

    /**
     * Getter for the batch application id.
     * @return the batch application id.
     */
    final public String getBatchApplicationId() {
        return batchApplicationId;
    }

    /**
     * Setter for the batch application id.
     * @param batchApplicationId The batch application id.
     */
    final public void setBatchApplicationId(String batchApplicationId) {
        this.batchApplicationId = batchApplicationId;
    }

    /**
     * Executes this batch on the current thread and returns the responses.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws FacebookException        If there was an error in the protocol used to communicate
     *                                  with the service
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException     if the passed in RequestBatch or any of its contents are
     *                                  null
     */
    public final List<GraphResponse> executeAndWait() {
        return executeAndWaitImpl();
    }

    /**
     * Executes this batch asynchronously. This function will return immediately, and the batch will
     * be processed on a separate thread. In order to process results of a request, or determine
     * whether a request succeeded or failed, a callback must be specified (see
     * {@link GraphRequest#setCallback(GraphRequest.Callback)})
     * <p/>
     * This should only be called from the UI thread.
     *
     * @return a RequestAsyncTask that is executing the request
     *
     * @throws IllegalArgumentException if this batch is empty
     * @throws NullPointerException if any of the contents of this batch are null
     */
    public final GraphRequestAsyncTask executeAsync() {
        return executeAsyncImpl();
    }

    /**
     * Specifies the interface that consumers of the RequestBatch class can implement in order to be
     * notified when the entire batch completes execution. It will be called after all per-Request
     * callbacks are called.
     */
    public interface Callback {
        /**
         * The method that will be called when a batch completes.
         *
         * @param batch     the RequestBatch containing the Requests which were executed
         */
        void onBatchCompleted(GraphRequestBatch batch);
    }

    /**
     * Specifies the interface that consumers of the RequestBatch class can implement in order to be
     * notified when the batch makes progress. The frequency of the callbacks can be controlled
     * using {@link FacebookSdk#setOnProgressThreshold(long)}.
     */
    public interface OnProgressCallback extends Callback {
        /**
         * The method that will be called when a batch makes progress.
         *
         * @param batch     the RequestBatch containing the Requests which were executed
         * @param current   the current value of the progress
         * @param max       the max (target) value of the progress
         */
        void onBatchProgress(GraphRequestBatch batch, long current, long max);
    }

    List<GraphResponse> executeAndWaitImpl() {
        return GraphRequest.executeBatchAndWait(this);
    }

    GraphRequestAsyncTask executeAsyncImpl() {
        return GraphRequest.executeBatchAsync(this);
    }
}
