package com.facebook.sdk.tests;

import android.test.AndroidTestCase;
import com.facebook.Session;

public class SessionTests extends AndroidTestCase {
    public void testSessionCreate() {
        Session session = new Session();
        assertFalse(session == null);
    }
}
