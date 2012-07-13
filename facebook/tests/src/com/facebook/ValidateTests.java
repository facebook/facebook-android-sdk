package com.facebook;

import java.util.Arrays;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class ValidateTests extends AndroidTestCase {
    
    @SmallTest
    public void testNotNullOnNonNull() {
        Validate.notNull("A string", "name");
    }

    @SmallTest
    public void testNotNullOnNull() {
        try {
            Validate.notNull(null, "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @SmallTest
    public void testNotEmptyOnNonEmpty() {
        Validate.notEmpty(Arrays.asList(new String[] { "hi" }), "name");
    }

    @SmallTest
    public void testNotEmptylOnEmpty() {
        try {
            Validate.notEmpty(Arrays.asList(new String[] {}), "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @SmallTest
    public void testNotNullOrEmptyOnNonEmpty() {
        Validate.notNullOrEmpty("hi", "name");
    }

    @SmallTest
    public void testNotNullOrEmptyOnEmpty() {
        try {
            Validate.notNullOrEmpty("", "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @SmallTest
    public void testNotNullOrEmptyOnNull() {
        try {
            Validate.notNullOrEmpty(null, "name");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @SmallTest
    public void testOneOfOnValid() {
        Validate.oneOf("hi", "name", "hi", "there");
    }

    @SmallTest
    public void testOneOfOnInvalid() {
        try {
            Validate.oneOf("hit", "name", "hi", "there");
            fail("expected exception");
        } catch (Exception e) {
        }
    }

    @SmallTest
    public void testOneOfOnValidNull() {
        Validate.oneOf(null, "name", "hi", "there", null);
    }

    @SmallTest
    public void testOneOfOnInvalidNull() {
        try {
            Validate.oneOf(null, "name", "hi", "there");
            fail("expected exception");
        } catch (Exception e) {
        }
    }
}
