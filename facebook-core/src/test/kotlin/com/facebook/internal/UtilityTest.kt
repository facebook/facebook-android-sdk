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

package com.facebook.internal

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.collections.HashMap
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    AccessToken::class,
    FacebookSdk::class,
    GraphRequest::class,
    FeatureManager::class,
    GoogleApiAvailability::class,
)
class UtilityTest : FacebookPowerMockTestCase() {

  private val mockTokenString = "A token of my esteem"
  private val mockAppID = "1234"
  private val mockClientToken = "5678"

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    whenever(FacebookSdk.getClientToken()).thenReturn(mockClientToken)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getGraphDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getGraphApiVersion()).thenCallRealMethod()
    PowerMockito.mockStatic(FeatureManager::class.java)
  }

  @Test
  fun testGetGraphMeRequestWithCacheAsyncNoGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(null, "id,name,first_name,middle_name,last_name")
  }

  @Test
  fun testGetGraphMeRequestWithCacheAsyncNullGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(
        "facebook", "id,name,first_name,middle_name,last_name")
  }

  @Test
  fun testGetGraphMeRequestWithCacheAsyncGGGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(
        "gaming", "id,name,first_name,middle_name,last_name")
  }

  @Test
  fun testGetGraphMeRequestWithCacheAsyncIGGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync("instagram", "id,name,profile_picture")
  }

  @Test
  fun testGetGraphDomainFromTokenDomain() {
    var graphDomain = Utility.getGraphDomainFromTokenDomain("facebook")
    assertThat(graphDomain).isEqualTo("facebook.com")

    graphDomain = Utility.getGraphDomainFromTokenDomain("gaming")
    assertThat(graphDomain).isEqualTo("fb.gg")

    graphDomain = Utility.getGraphDomainFromTokenDomain("instagram")
    assertThat(graphDomain).isEqualTo("instagram.com")
  }

  fun createTestForGetGraphMeRequestWithCacheAsync(graphDomain: String?, expectedFields: String) {
    val mockGraphRequestCompanion = mock<GraphRequest.Companion>()
    val mockGraphRequest = mock<GraphRequest>()
    whenever(mockGraphRequestCompanion.newMeRequest(anyOrNull(), anyOrNull()))
        .thenReturn(mockGraphRequest)
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)

    val accessToken = mock<AccessToken>()
    whenever(accessToken.graphDomain).thenReturn(graphDomain)
    val mockTokenCompanionObject = mock<AccessToken.Companion>()
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockTokenCompanionObject)
    whenever(mockTokenCompanionObject.getCurrentAccessToken()).thenReturn(accessToken)

    val bundleArgumentCaptor = argumentCaptor<Bundle>()
    Utility.getGraphMeRequestWithCacheAsync(mockTokenString, mock())

    verify(mockGraphRequestCompanion).newMeRequest(isNull(), isNull())
    verify(mockGraphRequest).parameters = bundleArgumentCaptor.capture()
    val parameters = bundleArgumentCaptor.firstValue
    assertThat(parameters.getString(GraphRequest.FIELDS_PARAM)).isEqualTo(expectedFields)
    assertThat(parameters.getString("access_token")).isEqualTo(mockTokenString)
  }

  @Test
  fun testStringMapToParcel() {
    // Test null
    assertNull(roundtrip(null))
    val map = HashMap<String?, String?>()

    // Test empty
    assertEquals(0, roundtrip(map)?.size)

    // Test regular
    map["a"] = "100"
    map["b"] = null
    map["c"] = "hello"
    val result = roundtrip(map)
    assertEquals(3, result?.size)
    assertEquals(map, result)
    assertEquals("100", result?.get("a"))
    assertNull(result?.get("b"))
    assertEquals("hello", result?.get("c"))
  }

  private fun roundtrip(input: Map<String?, String?>?): Map<String?, String?>? {
    val parcel = Parcel.obtain()
    return try {
      Utility.writeStringMapToParcel(parcel, input)
      parcel.setDataPosition(0)
      Utility.readStringMapFromParcel(parcel)
    } finally {
      parcel.recycle()
    }
  }

  @Test
  fun testUriBuild() {
    val path = "v10.0/dialog/access"

    val parameters = Bundle()
    parameters.putString("f1", "v1")
    parameters.putString("f2", "v2")
    parameters.putString("f3", "www.facebook.com")

    var uri = Utility.buildUri(ServerProtocol.getDialogAuthority(), path, null)
    assertEquals("https://m.facebook.com/v10.0/dialog/access", uri.toString())

    uri = Utility.buildUri(ServerProtocol.getDialogAuthority(), path, parameters)
    assertEquals(
        "https://m.facebook.com/v10.0/dialog/access?f1=v1&f2=v2&f3=www.facebook.com",
        uri.toString())

    // Test parseUrlQueryString
    val query = Utility.parseUrlQueryString(uri.query)
    assertEquals(parameters.toString(), query.toString())
  }

  @Test
  fun testBuildBundle() {
    val parameters = Bundle()
    Utility.putCommaSeparatedStringList(parameters, "k1", listOf("12", "34", "56", "78"))

    assertEquals("12,34,56,78", parameters.get("k1"))

    val barray = arrayOf(true, false).toBooleanArray()
    Utility.putJSONValueInBundle(parameters, "k2", barray)
    assertArrayEquals(barray, parameters.get("k2") as BooleanArray)
  }

  @Test
  fun testConvertJSONObjectToStringMap() {
    val validJson = "{\"k1\": true, \"k2\": \"value\"}"
    val result = Utility.convertJSONObjectToStringMap(JSONObject(validJson))
    assertEquals(mapOf("k1" to "true", "k2" to "value"), result)
  }

  @Test
  fun testSetAppEventAttributionParametersWithoutServiceUpdateCompliance() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ServiceUpdateCompliance))
        .thenReturn(false)

    val params: JSONObject = JSONObject()
    val mockAnonId = "fb_mock_anonID"
    val mockAttributionID = "fb_mock_attributionID"
    val mockContext = mock<Context>()
    val mockIdentifiers = PowerMockito.mock(AttributionIdentifiers::class.java)
    whenever(mockIdentifiers.attributionId).thenReturn(mockAttributionID)
    Utility.setAppEventAttributionParameters(
        params, mockIdentifiers, mockAnonId, false, mockContext)
    assertEquals(params["anon_id"], mockAnonId)
    assertEquals(params["attribution"], mockAttributionID)
  }

  @Test
  fun testSetAppEventAttributionParametersWithServiceUpdateCompliance() {
    if (Build.VERSION.SDK_INT < 31) {
      // Skipping test, SDK version is lower than 31
      return
    }

    whenever(FeatureManager.isEnabled(FeatureManager.Feature.ServiceUpdateCompliance))
        .thenReturn(true)

    val googleApiAvailability = PowerMockito.mock(GoogleApiAvailability::class.java)
    mockStatic(GoogleApiAvailability::class.java)
    whenever(GoogleApiAvailability.getInstance()).thenReturn(googleApiAvailability)
    whenever(googleApiAvailability.isGooglePlayServicesAvailable(any()))
        .thenReturn(ConnectionResult.SUCCESS)

    val params: JSONObject = JSONObject()
    val mockAnonId = "fb_mock_anonID"
    val mockAttributionID = "fb_mock_attributionID"
    val mockContext = mock<Context>()
    val mockIdentifiers = PowerMockito.mock(AttributionIdentifiers::class.java)
    whenever(mockIdentifiers.attributionId).thenReturn(mockAttributionID)
    Utility.setAppEventAttributionParameters(
        params, mockIdentifiers, mockAnonId, false, mockContext)
    assertNull(params["anon_id"])
    assertNull(params["attribution"])
  }
}
