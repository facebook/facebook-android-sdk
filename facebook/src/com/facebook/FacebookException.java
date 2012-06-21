/**
 * Copyright 2010 Facebook, Inc.
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

public class FacebookException extends RuntimeException {
	static final long serialVersionUID = 1;

    public FacebookException(String message) {
        super(message);
    }

    public FacebookException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public FacebookException(String message, int httpStatusCode, Object parsedJsonResponse) {
        super(message);
    }

    public boolean hasHttpStatusCode() {
        return false;
    }

    public int getHttpStatusCode() {
        return 200;
    }

    public Object getParsedJsonResponse() {
        return null;
    }
}
