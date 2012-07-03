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

import org.json.JSONObject;

public class FacebookServiceErrorException extends FacebookException {
    public static final int UNKNOWN_ERROR_CODE = -1;

    private final int httpResponseCode;
    private final int facebookErrorCode;
    private final String facebookErrorType;
    private final JSONObject responseBody;

    static final long serialVersionUID = 1;

    public FacebookServiceErrorException(int responseCode) {
        this(responseCode, UNKNOWN_ERROR_CODE, null, null, null);
    }

    public FacebookServiceErrorException(int responseCode, int facebookErrorCode, String facebookErrorType,
            String message, JSONObject responseBody) {
        super(message);
        this.httpResponseCode = responseCode;
        this.facebookErrorCode = facebookErrorCode;
        this.facebookErrorType = facebookErrorType;
        this.responseBody = responseBody;
    }

    public final int getHttpResponseCode() {
        return this.httpResponseCode;
    }

    public final int getFacebookErrorCode() {
        return this.facebookErrorCode;
    }

    public final String getFacebookErrorType() {
        return this.facebookErrorType;
    }

    public final JSONObject getResponseBody() {
        return this.responseBody;
    }

    @Override
    public final String toString() {
        return new StringBuilder().append("{FacebookServiceErrorException: ").append("httpResponseCode: ")
                .append(this.httpResponseCode).append(", facebookErrorCode: ").append(this.facebookErrorCode)
                .append(", facebookErrorType: ").append(this.facebookErrorType).append(", message: ")
                .append(this.getMessage()).append("}").toString();
    }

}
