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
 * Represents an error condition specific to the Facebook SDK for Android.
 */
public class FacebookException extends RuntimeException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new FacebookException.
     */
    public FacebookException() {
        super();
    }

    /**
     * Constructs a new FacebookException.
     *
     * @param message the detail message of this exception
     */
    public FacebookException(String message) {
        super(message);
    }

    /**
     * Constructs a new FacebookException.
     *
     * @param format the format string (see {@link java.util.Formatter#format})
     * @param args   the list of arguments passed to the formatter.
     */
    public FacebookException(String format, Object... args) {
        this(String.format(format, args));
    }

    /**
     * Constructs a new FacebookException.
     *
     * @param message   the detail message of this exception
     * @param throwable the cause of this exception
     */
    public FacebookException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new FacebookException.
     *
     * @param throwable the cause of this exception
     */
    public FacebookException(Throwable throwable) {
        super(throwable);
    }

    @Override
    public String toString() {
        // Throwable.toString() returns "FacebookException:{message}". Returning just "{message}"
        // should be fine here.
        return getMessage();
    }
}
