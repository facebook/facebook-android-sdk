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

public class FacebookServiceErrorException extends FacebookException {
    private final int responseCode;

    static final long serialVersionUID = 1;

    public FacebookServiceErrorException(int responseCode) {
        this(responseCode, null, null);
    }

    public FacebookServiceErrorException(int responseCode, String message) {
        this(responseCode, message, null);
    }

    public FacebookServiceErrorException(int responseCode, String message, Throwable throwable) {
        super(message, throwable);

        this.responseCode = responseCode;
    }

    public FacebookServiceErrorException(int responseCode, Throwable throwable) {
        this(responseCode, null, throwable);
    }

    public final int getResponseCode() {
        return this.responseCode;
    }
}
