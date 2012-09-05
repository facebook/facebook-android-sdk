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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * RequestBatch contains a list of Request objects that can be sent to facebook in a single round-trip.
 */
public class RequestBatch extends AbstractList<Request> {
    private String cacheKey;
    private Handler callbackHandler;
    private boolean forceRoundTrip;
    private ArrayList<Request> requests = new ArrayList<Request>();

    public RequestBatch() {
        this.requests = new ArrayList<Request>();
    }

    public RequestBatch(Collection<Request> requests) {
        this.requests = new ArrayList<Request>(requests);
    }

    public RequestBatch(Request... requests) {
        this.requests = Utility.arrayList(requests);
    }

    public RequestBatch(RequestBatch requests) {
        this.requests = new ArrayList<Request>(requests);
        this.cacheKey = requests.cacheKey;
        this.callbackHandler = requests.callbackHandler;
        this.forceRoundTrip = requests.forceRoundTrip;
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

    final String getCacheKey() {
        return cacheKey;
    }

    final void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    final boolean getForceRoundTrip() {
        return forceRoundTrip;
    }

    final void setForceRoundTrip(boolean forceRoundTrip) {
        this.forceRoundTrip = forceRoundTrip;
    }

    final Handler getCallbackHandler() {
        return callbackHandler;
    }

    final void setCallbackHandler(Handler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
}
