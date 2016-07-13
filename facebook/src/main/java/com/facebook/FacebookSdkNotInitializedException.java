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
 * An Exception indicating that the Facebook SDK has not been correctly initialized.
 */
public class FacebookSdkNotInitializedException extends FacebookException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a FacebookSdkNotInitializedException with no additional information.
     */
    public FacebookSdkNotInitializedException() {
        super();
    }

    /**
     * Constructs a FacebookSdkNotInitializedException with a message.
     *
     * @param message A String to be returned from getMessage.
     */
    public FacebookSdkNotInitializedException(String message) {
        super(message);
    }

    /**
     * Constructs a FacebookSdkNotInitializedException with a message and inner error.
     *
     * @param message   A String to be returned from getMessage.
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookSdkNotInitializedException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a FacebookSdkNotInitializedException with an inner error.
     *
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookSdkNotInitializedException(Throwable throwable) {
        super(throwable);
    }
}
