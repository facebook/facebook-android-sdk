/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.facebook.internal.Utility.logd
import java.net.HttpURLConnection

/**
 * Defines an AsyncTask suitable for executing a Request in the background. May be subclassed by
 * applications having unique threading model needs.
 */
open class GraphRequestAsyncTask
/**
 * Constructor that allows specification of an HTTP connection to use for executing the requests. No
 * validation is done that the contents of the connection actually reflect the serialized requests,
 * so it is the caller's responsibility to ensure that it will correctly generate the desired
 * responses.
 *
 * @param connection the HTTP connection to use to execute the requests
 * @param requests the requests to execute
 */
(private val connection: HttpURLConnection?, val requests: GraphRequestBatch) :
    AsyncTask<Void, Void, List<GraphResponse>>() {
  protected var exception: Exception? = null
    private set

  companion object {
    private val TAG = GraphRequestAsyncTask::class.java.canonicalName
  }

  /**
   * Constructor. Serialization of the requests will be done in the background, so any
   * serialization- related errors will be returned via the Response.getException() method.
   *
   * @param requests the requests to execute
   */
  constructor(vararg requests: GraphRequest) : this(null, GraphRequestBatch(*requests))

  /**
   * Constructor. Serialization of the requests will be done in the background, so any
   * serialization- related errors will be returned via the Response.getException() method.
   *
   * @param requests the requests to execute
   */
  constructor(requests: Collection<GraphRequest>) : this(null, GraphRequestBatch(requests))

  /**
   * Constructor. Serialization of the requests will be done in the background, so any
   * serialization- related errors will be returned via the Response.getException() method.
   *
   * @param requests the requests to execute
   */
  constructor(requests: GraphRequestBatch) : this(null, requests)

  /**
   * Constructor that allows specification of an HTTP connection to use for executing the requests.
   * No validation is done that the contents of the connection actually reflect the serialized
   * requests, so it is the caller's responsibility to ensure that it will correctly generate the
   * desired responses.
   *
   * @param connection the HTTP connection to use to execute the requests
   * @param requests the requests to execute
   */
  constructor(
      connection: HttpURLConnection?,
      vararg requests: GraphRequest
  ) : this(connection, GraphRequestBatch(*requests))

  /**
   * Constructor that allows specification of an HTTP connection to use for executing the requests.
   * No validation is done that the contents of the connection actually reflect the serialized
   * requests, so it is the caller's responsibility to ensure that it will correctly generate the
   * desired responses.
   *
   * @param connection the HTTP connection to use to execute the requests
   * @param requests the requests to execute
   */
  constructor(
      connection: HttpURLConnection?,
      requests: Collection<GraphRequest>
  ) : this(connection, GraphRequestBatch(requests))

  override fun toString(): String {
    return StringBuilder()
        .append("{RequestAsyncTask: ")
        .append(" connection: ")
        .append(connection)
        .append(", requests: ")
        .append(requests)
        .append("}")
        .toString()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun onPreExecute() {
    super.onPreExecute()
    if (FacebookSdk.isDebugEnabled()) {
      logd(TAG, String.format("execute async task: %s", this))
    }
    if (requests.callbackHandler == null) {
      // We want any callbacks to go to a handler on this thread unless a handler has already
      // been specified or we are not running on a thread without a looper.
      val handler =
          if (Thread.currentThread() is HandlerThread) {
            Handler()
          } else {
            Handler(Looper.getMainLooper())
          }
      requests.callbackHandler = handler
    }
  }

  override fun onPostExecute(result: List<GraphResponse>) {
    super.onPostExecute(result)
    val exception = this.exception
    if (exception != null) {
      logd(
          TAG,
          String.format(
              "onPostExecute: exception encountered during request: %s", exception.message))
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun doInBackground(vararg params: Void): List<GraphResponse>? {
    return try {
      if (connection == null) {
        requests.executeAndWait()
      } else {
        GraphRequest.executeConnectionAndWait(connection, requests)
      }
    } catch (e: Exception) {
      exception = e
      null
    }
  }
}
