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

package com.facebook.internal.instrument;

import android.support.annotation.Nullable;

import org.json.JSONArray;

final class InstrumentUtility {

    /**
     * Get the cause of the raised exception.
     *
     * @param e The Throwable containing the exception that was raised
     * @return The String containing the cause of the raised exception
     */
    @Nullable
    static String getCause(Throwable e) {
        if (e == null) {
            return null;
        }
        if (e.getCause() == null) {
            return e.toString();
        }
        return e.getCause().toString();
    }

    /**
     * Get the iterated call stack traces of the raised exception.
     *
     * @param e The Throwable containing the exception that was raised
     * @return The String containing the stack traces of the raised exception
     */
    @Nullable
    static String getStackTrace(Throwable e) {
        if (e == null) {
            return null;
        }

        // Iterate on causes recursively
        JSONArray array = new JSONArray();
        Throwable previous = null; // Prevent infinite loops
        for (Throwable t = e; t != null && t != previous; t = t.getCause()) {
            for (final StackTraceElement element : t.getStackTrace()) {
                array.put(element.toString());
            }
            previous = t;
        }
        return array.toString();
    }
}
