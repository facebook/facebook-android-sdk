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

import com.facebook.internal.FacebookRequestErrorClassification;

import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorClassificationTest extends FacebookTestCase {

    @Test
    public void testDefaultErrorClassification() {
        FacebookRequestErrorClassification errorClassification =
                FacebookRequestErrorClassification.getDefaultErrorClassification();
        // Test transient takes precedence
        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_INVALID_TOKEN,
                        0,
                        true)
        );

        assertEquals(
                FacebookRequestError.Category.LOGIN_RECOVERABLE,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_INVALID_SESSION,
                        0,
                        false)
        );

        assertEquals(
                FacebookRequestError.Category.LOGIN_RECOVERABLE,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_INVALID_TOKEN,
                        0,
                        false)
        );

        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_SERVICE_UNAVAILABLE,
                        0,
                        false)
        );

        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_APP_TOO_MANY_CALLS,
                        0,
                        false)
        );


        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_RATE,
                        0,
                        false)
        );

        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_USER_TOO_MANY_CALLS,
                        0,
                        false)
        );


        assertEquals(
                FacebookRequestError.Category.TRANSIENT,
                errorClassification.classify(
                        FacebookRequestErrorClassification.EC_TOO_MANY_USER_ACTION_CALLS,
                        0,
                        false)
        );
    }
}
