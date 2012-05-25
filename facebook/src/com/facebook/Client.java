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

import java.util.List;

import org.apache.http.client.HttpClient;

public class Client {
    public Client() {
    }

    public Client(HttpClient client) {
    }

    public final Response execute(Request request) {
    	return null;
    }

    public final Response execute(RequestContext context, Request request) {
    	return null;
    }

    public final List<Response> executeBatch(Request...requests) {
    	return null;
    }

    public final List<Response> executeBatch(RequestContext context, Request...requests) {
    	return null;
    }

    @Override public String toString() {
        return null;
    }
}
