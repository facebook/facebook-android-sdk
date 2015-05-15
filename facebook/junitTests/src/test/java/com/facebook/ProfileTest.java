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

import android.net.Uri;
import android.os.Parcel;

import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.junit.Assert.*;

public final class ProfileTest extends FacebookTestCase {
    static final String ID = "ID";
    static final String ANOTHER_ID = "ANOTHER_ID";
    static final String FIRST_NAME = "FIRST_NAME";
    static final String MIDDLE_NAME = "MIDDLE_NAME";
    static final String LAST_NAME = "LAST_NAME";
    static final String NAME = "NAME";
    static final Uri LINK_URI = Uri.parse("https://www.facebook.com/name");

    public static Profile createDefaultProfile() {
        return new Profile(
                ID,
                FIRST_NAME,
                MIDDLE_NAME,
                LAST_NAME,
                NAME,
                LINK_URI
        );
    }

    static void assertDefaultObjectGetters(Profile profile) {
        assertEquals(ID, profile.getId());
        assertEquals(FIRST_NAME, profile.getFirstName());
        assertEquals(MIDDLE_NAME, profile.getMiddleName());
        assertEquals(LAST_NAME, profile.getLastName());
        assertEquals(NAME, profile.getName());
        assertEquals(LINK_URI, profile.getLinkUri());
    }

    static Profile createMostlyNullsProfile() {
        return new Profile(ANOTHER_ID, null, null, null, null, null);
    }

    static void assertMostlyNullsObjectGetters(Profile profile) {
        assertEquals(ANOTHER_ID, profile.getId());
        assertNull(profile.getFirstName());
        assertNull(profile.getMiddleName());
        assertNull(profile.getLastName());
        assertNull(profile.getName());
        assertNull(profile.getLinkUri());
    }


    @Test
    public void testProfileCtorAndGetters() {
        Profile profile = createDefaultProfile();
        assertDefaultObjectGetters(profile);

        profile = createMostlyNullsProfile();
        assertMostlyNullsObjectGetters(profile);
    }

    @Test
    public void testHashCode() {
        Profile profile1 = createDefaultProfile();
        Profile profile2 = createDefaultProfile();
        assertEquals(profile1.hashCode(), profile2.hashCode());

        Profile profile3 = createMostlyNullsProfile();
        assertNotEquals(profile1.hashCode(), profile3.hashCode());
    }

    @Test
    public void testEquals() {
        Profile profile1 = createDefaultProfile();
        Profile profile2 = createDefaultProfile();
        assertEquals(profile1, profile2);

        Profile profile3 = createMostlyNullsProfile();
        assertNotEquals(profile1, profile3);
    }

    @Test
    public void testJsonSerialization() {
        Profile profile1 = createDefaultProfile();
        JSONObject jsonObject = profile1.toJSONObject();
        Profile profile2 = new Profile(jsonObject);
        assertDefaultObjectGetters(profile2);
        assertEquals(profile1, profile2);

        // Check with nulls
        profile1 = createMostlyNullsProfile();
        jsonObject = profile1.toJSONObject();
        profile2 = new Profile(jsonObject);
        assertMostlyNullsObjectGetters(profile2);
        assertEquals(profile1, profile2);
    }

    @Test
    public void testParcelSerialization() {
        Profile profile1 = createDefaultProfile();
        Profile profile2 = TestUtils.parcelAndUnparcel(profile1);

        assertDefaultObjectGetters(profile2);
        assertEquals(profile1, profile2);

        // Check with nulls
        profile1 = createMostlyNullsProfile();
        profile2 = TestUtils.parcelAndUnparcel(profile1);
        assertMostlyNullsObjectGetters(profile2);
        assertEquals(profile1, profile2);
    }

    @Test
    public void testGetSetCurrentProfile() {
        FacebookSdk.sdkInitialize(Robolectric.application);
        Profile profile1 = createDefaultProfile();
        Profile.setCurrentProfile(profile1);
        assertEquals(ProfileManager.getInstance().getCurrentProfile(), profile1);
        assertEquals(profile1, Profile.getCurrentProfile());

        Profile.setCurrentProfile(null);
        assertNull(ProfileManager.getInstance().getCurrentProfile());
        assertNull(Profile.getCurrentProfile());
    }
}
