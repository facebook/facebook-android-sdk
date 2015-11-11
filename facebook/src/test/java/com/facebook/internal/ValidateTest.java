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

package com.facebook.internal;

import com.facebook.FacebookTestCase;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;


public class ValidateTest extends FacebookTestCase {
    
    @Test
    public void testNotNullOnNonNull() {
        Validate.notNull("A string", "name");
    }

    @Test
    public void testNotNullOnNull() {
        try {
            Validate.notNull(null, "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testNotEmptyOnNonEmpty() {
        Validate.notEmpty(Arrays.asList(new String[] { "hi" }), "name");
    }

    @Test
    public void testNotEmptylOnEmpty() {
        try {
            Validate.notEmpty(Arrays.asList(new String[] {}), "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testNotNullOrEmptyOnNonEmpty() {
        Validate.notNullOrEmpty("hi", "name");
    }

    @Test
    public void testNotNullOrEmptyOnEmpty() {
        try {
            Validate.notNullOrEmpty("", "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testNotNullOrEmptyOnNull() {
        try {
            Validate.notNullOrEmpty(null, "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testOneOfOnValid() {
        Validate.oneOf("hi", "name", "hi", "there");
    }

    @Test
    public void testOneOfOnInvalid() {
        try {
            Validate.oneOf("hit", "name", "hi", "there");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testOneOfOnValidNull() {
        Validate.oneOf(null, "name", "hi", "there", null);
    }

    @Test
    public void testOneOfOnInvalidNull() {
        try {
            Validate.oneOf(null, "name", "hi", "there");
            fail("expected exception");
        } catch (Exception e) {
        }
    }
}
