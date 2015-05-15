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

/**
 * Represents an error returned from the Facebook service in response to a request.
 */
public class FacebookServiceException extends FacebookException {

    private final FacebookRequestError error;

    private static final long serialVersionUID = 1;

    /**
     * Constructs a new FacebookServiceException.
     *
     * @param error the error from the request
     */
    public FacebookServiceException(FacebookRequestError error, String errorMessage) {
        super(errorMessage);
        this.error = error;
    }

    /**
     * Returns an object that encapsulates complete information representing the error returned by Facebook.
     *
     * @return complete information representing the error.
     */
    public final FacebookRequestError getRequestError() {
        return error;
    }

    @Override
    public final String toString() {
        return new StringBuilder()
                .append("{FacebookServiceException: ")
                .append("httpResponseCode: ")
                .append(error.getRequestStatusCode())
                .append(", facebookErrorCode: ")
                .append(error.getErrorCode())
                .append(", facebookErrorType: ")
                .append(error.getErrorType())
                .append(", message: ")
                .append(error.getErrorMessage())
                .append("}")
                .toString();
    }

}
