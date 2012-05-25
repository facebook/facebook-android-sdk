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

import org.apache.http.HttpResponse;

public class Response {
    private Response() {
    }

    public final Exception getError() {
		return null;
    }

    public final GraphObject getGraphObject() {
		return null;
    }

    public final HttpResponse getHttpResponse() {
		return null;
    }

    public static List<Response> fromHttpResponse(RequestContext context, HttpResponse response) {
		return null;
    }

    @Override public String toString() {
        return null;
    }
}
