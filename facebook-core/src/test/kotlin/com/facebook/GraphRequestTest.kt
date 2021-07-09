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
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.AttributionIdentifiers
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, Log::class)
class GraphRequestTest : FacebookPowerMockTestCase() {
  private val mockAppID = "1234"
  private val mockClientToken = "5678"
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    PowerMockito.`when`(FacebookSdk.getClientToken()).thenReturn(mockClientToken)
    PowerMockito.`when`(FacebookSdk.isDebugEnabled()).thenReturn(false)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testAppendParametersToBaseUrl() {
    val parameters = Bundle()
    parameters.putString("sample_key", "sample_value")
    parameters.putString(GraphRequest.ACCESS_TOKEN_PARAM, "test_access_token")
    val singleGetRequest = GraphRequest(null, "testPath", parameters, HttpMethod.GET, null)
    val singlePostRequest = GraphRequest(null, "testPath", parameters, HttpMethod.POST, null)
    val urlGet = singleGetRequest.urlForSingleRequest
    val urlPost = singlePostRequest.urlForSingleRequest
    val urlBatch = singlePostRequest.relativeUrlForBatchedRequest
    var args = Uri.parse(urlGet).queryParameterNames
    assertThat(args.contains("sample_key")).isTrue
    args = Uri.parse(urlPost).queryParameterNames
    assertThat(args.isEmpty()).isTrue

    // Batch URL should contain parameters
    args = Uri.parse(urlBatch).queryParameterNames
    assertThat(args.contains("sample_key")).isTrue
  }

  @Test
  fun testCreateRequest() {
    val request = GraphRequest()
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.version).isEqualTo(FacebookSdk.getGraphApiVersion())
  }

  @Test
  fun testCreatePostRequest() {
    val graphObject = JSONObject()
    val parameters = Bundle()
    val graphPath = "me/statuses"
    val request1 = GraphRequest.newPostRequest(null, graphPath, graphObject, null)
    assertThat(request1.accessToken).isNull()
    assertThat(request1.httpMethod).isEqualTo(HttpMethod.POST)
    assertThat(request1.graphPath).isEqualTo(graphPath)
    assertThat(request1.graphObject).isEqualTo(graphObject)
    assertThat(request1.callback).isNull()

    val request2 = GraphRequest(null, graphPath, parameters, HttpMethod.POST, null)
    assertThat(request2.accessToken).isNull()
    assertThat(request2.httpMethod).isEqualTo(HttpMethod.POST)
    assertThat(request2.graphPath).isEqualTo(graphPath)
    FacebookTestUtility.assertEqualContentsWithoutOrder(parameters, request2.parameters)
    assertThat(request2.callback).isNull()
  }

  @Test
  fun testCreateMeRequest() {
    val request = GraphRequest.newMeRequest(null, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("me")
  }

  @Test
  fun testCreateMyFriendsRequest() {
    val request = GraphRequest.newMyFriendsRequest(null, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("me/friends")
  }

  @Test
  fun testCreateDeleteRequest() {
    val request = GraphRequest.newDeleteObjectRequest(mock(), "1111", null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.DELETE)
    assertThat(request.graphPath).isEqualTo("1111")
  }

  @Test
  fun testCreateUploadPhotoRequest() {
    val image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8)
    val request = GraphRequest.newUploadPhotoRequest(null, "me/photos", image, null, null, null)
    val parameters = request.parameters
    assertThat(parameters.containsKey("picture")).isTrue
    assertThat(parameters.getParcelable<Bitmap>("picture")).isEqualTo(image)
    assertThat(request.graphPath).isEqualTo("me/photos")
  }

  @Test
  fun testCreatePlacesSearchRequestWithLocation() {
    val location = Location("")
    location.latitude = 47.6204
    location.longitude = -122.3491
    val request = GraphRequest.newPlacesSearchRequest(null, location, 1000, 50, null, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("search")
  }

  @Test
  fun testCreatePlacesSearchRequestWithSearchText() {
    val request = GraphRequest.newPlacesSearchRequest(null, null, 1000, 50, "Starbucks", null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("search")
  }

  @Test(expected = FacebookException::class)
  fun testCreatePlacesSearchRequestRequiresLocationOrSearchText() {
    GraphRequest.newPlacesSearchRequest(null, null, 1000, 50, null, null)
  }

  @Test
  fun testSetHttpMethodToNilGivesDefault() {
    val request = GraphRequest()
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    request.httpMethod = null
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testExecuteBatchWithZeroRequestsThrows() {
    GraphRequest.executeBatchAndWait(*arrayOf())
  }

  @Test(expected = IllegalArgumentException::class)
  fun testToHttpConnectionWithZeroRequestsThrows() {
    GraphRequest.toHttpConnection(*arrayOf())
  }

  @Test
  fun testSingleGetToHttpRequest() {
    val requestMe = GraphRequest(null, "TourEiffel")
    val connection = GraphRequest.toHttpConnection(requestMe)
    assertThat(connection.requestMethod).isEqualTo("GET")
    assertThat(connection.url.path)
        .isEqualTo("/" + FacebookSdk.getGraphApiVersion() + "/TourEiffel")
    assertThat(connection.getRequestProperty("User-Agent")).startsWith("FBAndroidSDK")
    val uri = Uri.parse(connection.url.toString())
    assertThat(uri.getQueryParameter("sdk")).isEqualTo("android")
    assertThat(uri.getQueryParameter("format")).isEqualTo("json")
  }

  @Test
  fun testBuildsClientTokenIfNeeded() {
    val requestMe = GraphRequest(null, "TourEiffel")
    val connection = GraphRequest.toHttpConnection(requestMe)
    val uri = Uri.parse(connection.url.toString())
    val accessToken = uri.getQueryParameter("access_token")
    checkNotNull(accessToken)
    assertThat(accessToken).contains(mockAppID)
    assertThat(accessToken).contains(mockClientToken)
  }

  @Test
  fun testCallback() {
    // Mock http connection response
    val response = mock<GraphResponse>()
    val responses = arrayListOf(response)

    val mockGraphResponseCompanion = mock<GraphResponse.Companion>()
    whenever(mockGraphResponseCompanion.fromHttpConnection(any(), any())).thenReturn(responses)
    Whitebox.setInternalState(GraphResponse::class.java, "Companion", mockGraphResponseCompanion)
    val callback = mock<GraphRequest.Callback>()
    val request = GraphRequest(null, "me/photos", null, null, callback)
    request.executeAndWait()
    verify(callback, times(1)).onCompleted(any())
  }

  @Test
  fun testRequestForCustomAudienceThirdPartyID() {
    val mockAttributionIdentifiersCompanionObject = mock<AttributionIdentifiers.Companion>()
    whenever(mockAttributionIdentifiersCompanionObject.getAttributionIdentifiers(any()))
        .thenReturn(null)
    Whitebox.setInternalState(
        AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifiersCompanionObject)
    PowerMockito.doReturn(false)
        .`when`(FacebookSdk::class.java, "getLimitEventAndDataUsage", any<Context>())
    val expectedRequest =
        GraphRequest(
            null, "mockAppID/custom_audience_third_party_id", Bundle(), HttpMethod.GET, null)
    val request =
        GraphRequest.newCustomAudienceThirdPartyIdRequest(
            mock(), FacebookSdk.getApplicationContext(), "mockAppID", null)
    assertThat(request.graphPath).isEqualTo(expectedRequest.graphPath)
    assertThat(request.httpMethod).isEqualTo(expectedRequest.httpMethod)
    FacebookTestUtility.assertEqualContentsWithoutOrder(
        expectedRequest.parameters, request.parameters)
  }

  @Test
  fun `test GraphRequest raises a warning if no client is set`() {
    PowerMockito.`when`(FacebookSdk.getClientToken()).thenReturn(null)
    PowerMockito.mockStatic(Log::class.java)
    var capturedTag: String? = null
    PowerMockito.`when`(Log.w(any(), any<String>())).thenAnswer {
      capturedTag = it.arguments[0].toString()
      0
    }
    val requestMe = GraphRequest(null, "TourEiffel")
    GraphRequest.toHttpConnection(requestMe)
    assertThat(capturedTag).isEqualTo(GraphRequest.TAG)
  }
}
