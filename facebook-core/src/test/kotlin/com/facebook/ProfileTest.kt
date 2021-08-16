/*
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
package com.facebook

import android.content.Context
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.ImageRequest
import com.facebook.util.common.ProfileTestHelper.PICTURE_URI
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import com.facebook.util.common.ProfileTestHelper.createProfileWithPictureUri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, ImageRequest::class, LocalBroadcastManager::class)
class ProfileTest : FacebookPowerMockTestCase() {

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(Matchers.isA(Context::class.java)))
        .thenReturn(mockLocalBroadcastManager)
  }

  @Test
  fun testProfileCtorAndGetters() {
    var profile = createDefaultProfile()
    assertDefaultObjectGetters(profile)
    profile = createMostlyNullsProfile()
    assertMostlyNullsObjectGetters(profile)
  }

  @Test
  fun testHashCode() {
    val profile1 = createDefaultProfile()
    val profile2 = createDefaultProfile()
    Assert.assertEquals(profile1.hashCode().toLong(), profile2.hashCode().toLong())
    val profile3 = createMostlyNullsProfile()
    Assert.assertNotEquals(profile1.hashCode().toLong(), profile3.hashCode().toLong())

    val profile4 = createProfileWithPictureUri()
    val profile5 = createProfileWithPictureUri()
    Assert.assertEquals(profile4.hashCode().toLong(), profile5.hashCode().toLong())
    Assert.assertNotEquals(profile4.hashCode().toLong(), profile3.hashCode().toLong())
  }

  @Test
  fun testEquals() {
    val profile1 = createDefaultProfile()
    val profile2 = createDefaultProfile()
    Assert.assertEquals(profile1, profile2)
    val profile3 = createMostlyNullsProfile()
    Assert.assertNotEquals(profile1, profile3)
    Assert.assertNotEquals(profile3, profile1)
  }

  @Test
  fun testJsonSerialization() {
    var profile1 = createDefaultProfile()
    var jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    var profile2 = Profile(jsonObject)
    assertDefaultObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)

    // Check with nulls
    profile1 = createMostlyNullsProfile()
    jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    profile2 = Profile(jsonObject)
    assertMostlyNullsObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)

    // Check with picture_uri field
    profile1 = createProfileWithPictureUri()
    jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    profile2 = Profile(jsonObject)
    assertDefaultObjectGetters(profile2)
    Assert.assertEquals(Uri.parse(PICTURE_URI), profile2.pictureUri)
    Assert.assertEquals(profile1, profile2)
  }

  @Test
  fun testParcelSerialization() {
    var profile1 = createDefaultProfile()
    var profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertDefaultObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)

    // Check with nulls
    profile1 = createMostlyNullsProfile()
    profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertMostlyNullsObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)

    // Check with picture_uri field
    profile1 = createProfileWithPictureUri()
    profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertDefaultObjectGetters(profile2)
    Assert.assertEquals(Uri.parse(PICTURE_URI), profile2?.pictureUri)
    Assert.assertEquals(profile1, profile2)
  }

  @Test
  fun testGetSetCurrentProfile() {
    val profile1 = createDefaultProfile()
    Profile.setCurrentProfile(profile1)
    Assert.assertEquals(ProfileManager.getInstance().currentProfile, profile1)
    Assert.assertEquals(profile1, Profile.getCurrentProfile())
    Profile.setCurrentProfile(null)
    Assert.assertNull(ProfileManager.getInstance().currentProfile)
    Assert.assertNull(Profile.getCurrentProfile())
  }

  @Test
  fun testGetProfilePictureUri() {
    val testFacebookImageUri = Uri.parse("https://scontent.xx.fbcdn.net/")

    val mockImageRequestCompanion = mock<ImageRequest.Companion>()
    Whitebox.setInternalState(ImageRequest::class.java, "Companion", mockImageRequestCompanion)
    whenever(mockImageRequestCompanion.getProfilePictureUri(any(), any(), any(), any()))
        .thenReturn(testFacebookImageUri)
    val instagramProfile = createProfileWithPictureUri()
    Profile.setCurrentProfile(instagramProfile)

    Assert.assertEquals(instagramProfile.pictureUri, Uri.parse(PICTURE_URI))
    Assert.assertEquals(instagramProfile.getProfilePictureUri(100, 100), Uri.parse(PICTURE_URI))

    val facebookProfile = createDefaultProfile()
    Assert.assertNull(facebookProfile.pictureUri)
    Assert.assertEquals(facebookProfile.getProfilePictureUri(100, 100), testFacebookImageUri)
  }
}
