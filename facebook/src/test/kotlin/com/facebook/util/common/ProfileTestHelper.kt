package com.facebook.util.common

import android.net.Uri
import com.facebook.Profile
import org.junit.Assert

object ProfileTestHelper {
  const val ID = "ID"
  const val ANOTHER_ID = "ANOTHER_ID"
  const val FIRST_NAME = "FIRST_NAME"
  const val MIDDLE_NAME = "MIDDLE_NAME"
  const val LAST_NAME = "LAST_NAME"
  const val NAME = "NAME"
  const val LINK_URI = "https://www.facebook.com/name"
  const val PICTURE_URI = "https://scontent.cdninstagram.com"
  @JvmStatic
  fun createDefaultProfile(): Profile {
    return Profile(ID, FIRST_NAME, MIDDLE_NAME, LAST_NAME, NAME, Uri.parse(LINK_URI))
  }

  @JvmStatic
  fun assertDefaultObjectGetters(profile: Profile) {
    Assert.assertEquals(ID, profile.id)
    Assert.assertEquals(FIRST_NAME, profile.firstName)
    Assert.assertEquals(MIDDLE_NAME, profile.middleName)
    Assert.assertEquals(LAST_NAME, profile.lastName)
    Assert.assertEquals(NAME, profile.name)
    Assert.assertEquals(Uri.parse(LINK_URI), profile.linkUri)
  }

  @JvmStatic
  fun createMostlyNullsProfile(): Profile {
    return Profile(ANOTHER_ID, null, null, null, null, null)
  }

  @JvmStatic
  fun assertMostlyNullsObjectGetters(profile: Profile) {
    Assert.assertEquals(ANOTHER_ID, profile.id)
    Assert.assertNull(profile.firstName)
    Assert.assertNull(profile.middleName)
    Assert.assertNull(profile.lastName)
    Assert.assertNull(profile.name)
    Assert.assertNull(profile.linkUri)
  }
}
