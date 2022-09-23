/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Handler
import java.util.AbstractList
import java.util.concurrent.atomic.AtomicInteger

/**
 * RequestBatch contains a list of Request objects that can be sent to Facebook in a single
 * round-trip.
 */
class GraphRequestBatch : AbstractList<GraphRequest> {
  var callbackHandler: Handler? = null
  private var timeoutInMilliseconds = 0
  val id = Integer.valueOf(idGenerator.incrementAndGet()).toString()
  /** The graph requests in this batch. */
  var requests: MutableList<GraphRequest>
    private set
  /** The callbacks that attached to this batch. */
  var callbacks: MutableList<Callback> = ArrayList()
    private set
  /** The timeout to wait for responses from the server before a timeout error occurs. */
  var timeout: Int
    get() = timeoutInMilliseconds
    set(timeoutInMilliseconds) {
      require(timeoutInMilliseconds >= 0) { "Argument timeoutInMilliseconds must be >= 0." }
      this.timeoutInMilliseconds = timeoutInMilliseconds
    }

  /** Batch application id. */
  var batchApplicationId: String? = null

  /** Constructor. Creates an empty batch. */
  constructor() {
    requests = arrayListOf()
  }

  /**
   * Constructor.
   *
   * @param requests the requests to add to the batch
   */
  constructor(requests: Collection<GraphRequest>) {
    this.requests = ArrayList(requests)
  }

  /**
   * Constructor.
   *
   * @param requests the requests to add to the batch
   */
  constructor(vararg requests: GraphRequest) {
    this.requests = ArrayList(requests.asList())
  }

  /**
   * Constructor.
   *
   * @param requests the requests to add to the batch
   */
  constructor(requests: GraphRequestBatch) {
    this.requests = ArrayList(requests)
    callbackHandler = requests.callbackHandler
    timeoutInMilliseconds = requests.timeoutInMilliseconds
    this.callbacks = ArrayList(requests.callbacks)
  }

  /**
   * Adds a batch-level callback which will be called when the entire batch has finished executing.
   *
   * @param callback the callback
   */
  fun addCallback(callback: Callback) {
    if (!callbacks.contains(callback)) {
      callbacks.add(callback)
    }
  }

  /**
   * Removes a batch-level callback.
   *
   * @param callback the callback
   */
  fun removeCallback(callback: Callback) {
    callbacks.remove(callback)
  }

  override fun add(element: GraphRequest): Boolean {
    return requests.add(element)
  }

  override fun add(index: Int, element: GraphRequest) {
    requests.add(index, element)
  }

  override fun clear() {
    requests.clear()
  }

  override fun get(index: Int): GraphRequest {
    return requests[index]
  }

  override fun removeAt(index: Int): GraphRequest {
    return requests.removeAt(index)
  }

  override fun set(index: Int, element: GraphRequest): GraphRequest {
    return requests.set(index, element)
  }

  override val size: Int
    get() = requests.size

  /**
   * Executes this batch on the current thread and returns the responses.
   *
   * This should only be used if you have transitioned off the UI thread.
   *
   * @return a list of Response objects representing the results of the requests; responses are
   * returned in the same order as the requests were specified.
   * @throws FacebookException If there was an error in the protocol used to communicate with the
   * service
   * @throws IllegalArgumentException if the passed in RequestBatch is empty
   * @throws NullPointerException if the passed in RequestBatch or any of its contents are null
   */
  fun executeAndWait(): List<GraphResponse> {
    return executeAndWaitImpl()
  }

  /**
   * Executes this batch asynchronously. This function will return immediately, and the batch will
   * be processed on a separate thread. In order to process results of a request, or determine
   * whether a request succeeded or failed, a callback must be specified (see [ ]
   * [GraphRequest.setCallback])
   *
   * This should only be called from the UI thread.
   *
   * @return a RequestAsyncTask that is executing the request
   * @throws IllegalArgumentException if this batch is empty
   * @throws NullPointerException if any of the contents of this batch are null
   */
  fun executeAsync(): GraphRequestAsyncTask {
    return executeAsyncImpl()
  }

  /**
   * Specifies the interface that consumers of the RequestBatch class can implement in order to be
   * notified when the entire batch completes execution. It will be called after all per-Request
   * callbacks are called.
   */
  fun interface Callback {
    /**
     * The method that will be called when a batch completes.
     *
     * @param batch the RequestBatch containing the Requests which were executed
     */
    fun onBatchCompleted(batch: GraphRequestBatch)
  }

  /**
   * Specifies the interface that consumers of the RequestBatch class can implement in order to be
   * notified when the batch makes progress. The frequency of the callbacks can be controlled using
   * [FacebookSdk.setOnProgressThreshold].
   */
  interface OnProgressCallback : Callback {
    /**
     * The method that will be called when a batch makes progress.
     *
     * @param batch the RequestBatch containing the Requests which were executed
     * @param current the current value of the progress
     * @param max the max (target) value of the progress
     */
    fun onBatchProgress(batch: GraphRequestBatch, current: Long, max: Long)
  }

  private fun executeAndWaitImpl(): List<GraphResponse> {
    return GraphRequest.executeBatchAndWait(this)
  }

  private fun executeAsyncImpl(): GraphRequestAsyncTask {
    return GraphRequest.executeBatchAsync(this)
  }

  companion object {
    private val idGenerator = AtomicInteger()
  }
}
