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
 * An Exception indicating that an operation was canceled before it completed.
 */
public class FacebookOperationCanceledException extends FacebookException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a FacebookOperationCanceledException with no additional information.
     */
    public FacebookOperationCanceledException() {
        super();
    }

    /**
     * Constructs a FacebookOperationCanceledException with a message.
     *
     * @param message A String to be returned from getMessage.
     */
    public FacebookOperationCanceledException(String message) {
        super(message);
    }

    /**
     * Constructs a FacebookOperationCanceledException with a message and inner error.
     *
     * @param message   A String to be returned from getMessage.
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookOperationCanceledException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a FacebookOperationCanceledException with an inner error.
     *
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookOperationCanceledException(Throwable throwable) {
        super(throwable);
    }
}
