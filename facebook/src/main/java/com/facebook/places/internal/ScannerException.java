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

package com.facebook.places.internal;

/**
 * com.facebook.places.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class ScannerException extends Exception {

    public enum Type {
        NOT_SUPPORTED,
        PERMISSION_DENIED,
        DISABLED,
        SCAN_ALREADY_IN_PROGRESS,
        UNKNOWN_ERROR,
        TIMEOUT,
    }

    public Type type;

    public ScannerException(Type type) {
        super("Type: " + type.name());
        this.type = type;
    }

    public ScannerException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public ScannerException(Type type, Exception ex) {
        super("Type: " + type.name(), ex);
        this.type = type;
    }
}
