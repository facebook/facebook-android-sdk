package com.facebook;

import android.test.AndroidTestCase;

public class SessionTests extends AndroidTestCase {

    public void testFailNullArguments() {
        try {
            new Session(null, null, null, null);
            
            // Should not get here
            assertFalse(true);
        } catch (NullPointerException e) {
            // got expected exception
        }
    }
}
