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
import com.facebook.internal.Utility
import com.facebook.util.common.ProfileTestHelper.PICTURE_URI
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import com.facebook.util.common.ProfileTestHelper.createProfileWithPictureUri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class, ImageRequest::class, LocalBroadcastManager::class, Utility::class)
class ProfileTest : FacebookPowerMockTestCase() {
  private lateinit var mockAccessTokenCompanion: AccessToken.Companion
  private lateinit var mockProfileManagerCompanion: ProfileManager.Companion
  private lateinit var mockProfileManager: ProfileManager
  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any<Context>()))
        .thenReturn(mockLocalBroadcastManager)
    PowerMockito.mockStatic(Utility::class.java)
    mockAccessTokenCompanion = mock()
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)
    mockProfileManagerCompanion = mock()
    Whitebox.setInternalState(ProfileManager::class.java, "Companion", mockProfileManagerCompanion)
    mockProfileManager = mock()
    whenever(mockProfileManagerCompanion.getInstance()).thenReturn(mockProfileManager)
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
    assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode())
    val profile3 = createMostlyNullsProfile()
    assertThat(profile1.hashCode()).isNotEqualTo(profile3.hashCode())

    val profile4 = createProfileWithPictureUri()
    val profile5 = createProfileWithPictureUri()
    assertThat(profile4.hashCode()).isEqualTo(profile5.hashCode())
    assertThat(profile4.hashCode()).isNotEqualTo(profile3.hashCode())
  }

  @Test
  fun testEquals() {
    val profile1 = createDefaultProfile()
    val profile2 = createDefaultProfile()
    assertThat(profile1).isEqualTo(profile2)
    val profile3 = createMostlyNullsProfile()
    assertThat(profile1).isNotEqualTo(profile3)
    assertThat(profile3).isNotEqualTo(profile2)
  }

  @Test
  fun testJsonSerialization() {
    var profile1 = createDefaultProfile()
    var jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    var profile2 = Profile(jsonObject)
    assertDefaultObjectGetters(profile2)
    assertThat(profile1).isEqualTo(profile2)

    // Check with nulls
    profile1 = createMostlyNullsProfile()
    jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    profile2 = Profile(jsonObject)
    assertMostlyNullsObjectGetters(profile2)
    assertThat(profile1).isEqualTo(profile2)

    // Check with picture_uri field
    profile1 = createProfileWithPictureUri()
    jsonObject = profile1.toJSONObject()
    jsonObject = checkNotNull(jsonObject)
    profile2 = Profile(jsonObject)
    assertDefaultObjectGetters(profile2)
    assertThat(profile2.pictureUri).isEqualTo(Uri.parse(PICTURE_URI))
    assertThat(profile1).isEqualTo(profile2)
  }

  @Test
  fun testParcelSerialization() {
    var profile1 = createDefaultProfile()
    var profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertDefaultObjectGetters(profile2)
    assertThat(profile1).isEqualTo(profile2)

    // Check with nulls
    profile1 = createMostlyNullsProfile()
    profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertMostlyNullsObjectGetters(profile2)
    assertThat(profile1).isEqualTo(profile2)

    // Check with picture_uri field
    profile1 = createProfileWithPictureUri()
    profile2 = FacebookTestUtility.parcelAndUnparcel(profile1)
    assertDefaultObjectGetters(profile2)
    assertThat(profile2?.pictureUri).isEqualTo(Uri.parse(PICTURE_URI))
    assertThat(profile1).isEqualTo(profile2)
  }

  @Test
  fun testSetCurrentProfile() {
    val profile1 = createDefaultProfile()
    Profile.setCurrentProfile(profile1)
    Profile.setCurrentProfile(null)
    val profileCaptor = argumentCaptor<Profile>()
    verify(mockProfileManager, times(2)).currentProfile = profileCaptor.capture()
    assertThat(profileCaptor.firstValue).isEqualTo(profile1)
    assertThat(profileCaptor.secondValue).isNull()
  }

  @Test
  fun testGetCurrentProfile() {
    val profile1 = createDefaultProfile()
    whenever(mockProfileManager.currentProfile).thenReturn(profile1)
    assertThat(Profile.getCurrentProfile()).isEqualTo(profile1)
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

    assertThat(instagramProfile.pictureUri).isEqualTo(Uri.parse(PICTURE_URI))
    assertThat(instagramProfile.getProfilePictureUri(100, 100)).isEqualTo(Uri.parse(PICTURE_URI))

    val facebookProfile = createDefaultProfile()
    assertThat(facebookProfile.pictureUri).isNull()
    assertThat(facebookProfile.getProfilePictureUri(100, 100)).isEqualTo(testFacebookImageUri)
  }

  @Test
  fun `test fetch profile for an expired access token`() {
    val mockAccessToken = mock<AccessToken>()
    whenever(mockAccessToken.isExpired).thenReturn(true)
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mockAccessToken)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(false)
    Profile.fetchProfileForCurrentAccessToken()
    var getGraphMeRequestWithCacheAsyncIsCalled = false
    PowerMockito.`when`(Utility.getGraphMeRequestWithCacheAsync(any(), any())).thenAnswer {
      getGraphMeRequestWithCacheAsyncIsCalled = true
      Unit
    }
    assertThat(getGraphMeRequestWithCacheAsyncIsCalled).isFalse
  }

  @Test
  fun `test fetch profile for an valid access token`() {
    val mockAccessToken = mock<AccessToken>()
    whenever(mockAccessToken.isExpired).thenReturn(false)
    whenever(mockAccessToken.token).thenReturn(TEST_TOKEN)
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(mockAccessToken)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(true)

    var capturedToken: String? = null
    var capturedCallback: Utility.GraphMeRequestWithCacheCallback? = null
    PowerMockito.`when`(Utility.getGraphMeRequestWithCacheAsync(any(), any())).thenAnswer {
      capturedToken = it.arguments[0] as String
      capturedCallback = it.arguments[1] as Utility.GraphMeRequestWithCacheCallback
      Unit
    }
    Profile.fetchProfileForCurrentAccessToken()

    assertThat(capturedToken).isEqualTo(TEST_TOKEN)
    assertThat(capturedCallback).isNotNull

    val profileCaptor = argumentCaptor<Profile>()

    capturedCallback?.onSuccess(JSONObject(mapOf("id" to TEST_USER_ID)))
    verify(mockProfileManager).currentProfile = profileCaptor.capture()
    val capturedProfile = profileCaptor.lastValue
    assertThat(capturedProfile.id).isEqualTo(TEST_USER_ID)
    assertThat(capturedProfile.name).isEqualTo("")
    assertThat(capturedProfile.firstName).isEqualTo("")
    assertThat(capturedProfile.middleName).isEqualTo("")
    assertThat(capturedProfile.lastName).isEqualTo("")
    assertThat(capturedProfile.pictureUri).isNull()
  }

  companion object {
    const val TEST_TOKEN = "123456789"
    const val TEST_USER_ID = "987654321"
  }
}
