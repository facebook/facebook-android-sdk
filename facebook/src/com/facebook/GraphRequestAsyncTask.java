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

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Defines an AsyncTask suitable for executing a Request in the background. May be subclassed
 * by applications having unique threading model needs.
 */
public class GraphRequestAsyncTask extends AsyncTask<Void, Void, List<GraphResponse>> {
    private static final String TAG = GraphRequestAsyncTask.class.getCanonicalName();
    private static Method executeOnExecutorMethod;

    private final HttpURLConnection connection;
    private final GraphRequestBatch requests;

    private Exception exception;

    static {
        for (Method method : AsyncTask.class.getMethods()) {
            if ("executeOnExecutor".equals(method.getName())) {
                Class<?>[] parameters = method.getParameterTypes();
                if ((parameters.length == 2) &&
                        (parameters[0] == Executor.class) && parameters[1].isArray()) {
                    executeOnExecutorMethod = method;
                    break;
                }
            }
        }
    }

    /**
     * Constructor. Serialization of the requests will be done in the background, so any
     * serialization- related errors will be returned via the Response.getException() method.
     *
     * @param requests the requests to execute
     */
    public GraphRequestAsyncTask(GraphRequest... requests) {
        this(null, new GraphRequestBatch(requests));
    }

    /**
     * Constructor. Serialization of the requests will be done in the background, so any
     * serialization- related errors will be returned via the Response.getException() method.
     *
     * @param requests the requests to execute
     */
    public GraphRequestAsyncTask(Collection<GraphRequest> requests) {
        this(null, new GraphRequestBatch(requests));
    }

    /**
     * Constructor. Serialization of the requests will be done in the background, so any
     * serialization- related errors will be returned via the Response.getException() method.
     *
     * @param requests the requests to execute
     */
    public GraphRequestAsyncTask(GraphRequestBatch requests) {
        this(null, requests);
    }

    /**
     * Constructor that allows specification of an HTTP connection to use for executing
     * the requests. No validation is done that the contents of the connection actually
     * reflect the serialized requests, so it is the caller's responsibility to ensure
     * that it will correctly generate the desired responses.
     *
     * @param connection the HTTP connection to use to execute the requests
     * @param requests   the requests to execute
     */
    public GraphRequestAsyncTask(HttpURLConnection connection, GraphRequest... requests) {
        this(connection, new GraphRequestBatch(requests));
    }

    /**
     * Constructor that allows specification of an HTTP connection to use for executing
     * the requests. No validation is done that the contents of the connection actually
     * reflect the serialized requests, so it is the caller's responsibility to ensure
     * that it will correctly generate the desired responses.
     *
     * @param connection the HTTP connection to use to execute the requests
     * @param requests   the requests to execute
     */
    public GraphRequestAsyncTask(HttpURLConnection connection, Collection<GraphRequest> requests) {
        this(connection, new GraphRequestBatch(requests));
    }

    /**
     * Constructor that allows specification of an HTTP connection to use for executing
     * the requests. No validation is done that the contents of the connection actually
     * reflect the serialized requests, so it is the caller's responsibility to ensure
     * that it will correctly generate the desired responses.
     *
     * @param connection the HTTP connection to use to execute the requests
     * @param requests   the requests to execute
     */
    public GraphRequestAsyncTask(HttpURLConnection connection, GraphRequestBatch requests) {
        this.requests = requests;
        this.connection = connection;
    }

    protected final Exception getException() {
        return exception;
    }

    protected final GraphRequestBatch getRequests() {
        return requests;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{RequestAsyncTask: ")
                .append(" connection: ")
                .append(connection)
                .append(", requests: ")
                .append(requests)
                .append("}")
                .toString();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (FacebookSdk.isDebugEnabled()) {
            Log.d(TAG, String.format("execute async task: %s", this));
        }
        if (requests.getCallbackHandler() == null) {
            // We want any callbacks to go to a handler on this thread unless a handler has already
            // been specified.
            requests.setCallbackHandler(new Handler());
        }
    }

    @Override
    protected void onPostExecute(List<GraphResponse> result) {
        super.onPostExecute(result);

        if (exception != null) {
            Log.d(TAG, String.format(
                    "onPostExecute: exception encountered during request: %s",
                    exception.getMessage()));
        }
    }

    @Override
    protected List<GraphResponse> doInBackground(Void... params) {
        try {
            if (connection == null) {
                return requests.executeAndWait();
            } else {
                return GraphRequest.executeConnectionAndWait(connection, requests);
            }
        } catch (Exception e) {
            exception = e;
            return null;
        }
    }

    GraphRequestAsyncTask executeOnSettingsExecutor() {
        if (executeOnExecutorMethod != null) {
            try {
                executeOnExecutorMethod.invoke(this, FacebookSdk.getExecutor(), null);
            } catch (InvocationTargetException e) {
                // fall-through
            } catch (IllegalAccessException e) {
                // fall-through
            }
        } else {
          this.execute();
        }

        return this;
    }
}
