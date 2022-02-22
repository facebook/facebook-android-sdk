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
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.Logger
import com.facebook.internal.ServerProtocol
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(AccessToken::class, FacebookSdk::class, Log::class)
class GraphRequestTest : FacebookPowerMockTestCase() {
  private val facebookGraphUrl = "graph.facebook.com"
  private val gamingGraphUrl = "graph.fb.gg"
  private val instagramGraphUrl = "graph.instagram.com"
  private val mockAppID = "1234"
  private val mockClientToken = "5678"
  private val mockTokenString = "EAAasdf"
  private val mockGamingTokenString = "GGasdf"
  private val mockInstagramTokenString = "IGasdf"
  private val mockAppTokenString = mockAppID + "|" + mockClientToken
  private val mockUserID = "1000"
  private lateinit var mockHttpURLConnection: HttpURLConnection
  private lateinit var mockHttpURLConnectionDataStream: PipedInputStream

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    whenever(FacebookSdk.getClientToken()).thenReturn(mockClientToken)
    whenever(FacebookSdk.isDebugEnabled()).thenReturn(false)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getGraphDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getGraphApiVersion()).thenCallRealMethod()
    mockLoggedInWithTokenDomain("facebook")

    mockHttpURLConnectionDataStream = PipedInputStream()
    mockHttpURLConnection = mock()
    val pipedOutputStreamForMockConnection = PipedOutputStream(mockHttpURLConnectionDataStream)
    whenever(mockHttpURLConnection.outputStream).thenReturn(pipedOutputStreamForMockConnection)
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
  fun testBaseUrlCreationForInstagram() {
    mockLoggedInWithTokenDomain("instagram")
    val igGraphRequest = GraphRequest.newMeRequest(null, null)

    val singleRequestUrl = igGraphRequest.urlForSingleRequest
    val batchRequestUrl = igGraphRequest.relativeUrlForBatchedRequest

    assertThat(singleRequestUrl).contains("graph.instagram.com")
    assertThat(singleRequestUrl.contains(FacebookSdk.getGraphApiVersion())).isTrue
    assertThat(batchRequestUrl.contains(FacebookSdk.getGraphApiVersion())).isTrue
  }

  @Test
  fun testApplicationRequestRoutingByTokenDomain() {
    // Application requests should always go to the facebook.com or fb.gg domains
    val appRequest = GraphRequest(null, mockAppID, null, HttpMethod.GET, null)
    val appActivitiesRequest =
        GraphRequest(null, mockAppID + "/activities", null, HttpMethod.GET, null)

    // With no access token set
    assertThat(appRequest.urlForSingleRequest).contains(facebookGraphUrl)
    assertThat(appActivitiesRequest.urlForSingleRequest).contains(facebookGraphUrl)

    mockLoggedInWithTokenDomain("facebook")

    // User is logged in with facebook
    assertThat(appRequest.urlForSingleRequest).contains(facebookGraphUrl)
    assertThat(appActivitiesRequest.urlForSingleRequest).contains(facebookGraphUrl)

    mockLoggedInWithTokenDomain("gaming")

    // User is logged in with gaming
    assertThat(appRequest.urlForSingleRequest).contains(gamingGraphUrl)
    assertThat(appActivitiesRequest.urlForSingleRequest).contains(gamingGraphUrl)

    mockLoggedInWithTokenDomain("instagram")

    // User is logged in with instagram
    assertThat(appRequest.urlForSingleRequest).contains(facebookGraphUrl)
    assertThat(appActivitiesRequest.urlForSingleRequest).contains(facebookGraphUrl)
  }

  @Test
  fun testMeRequestRoutingByTokenDomain() {
    val meRequest = GraphRequest.newMeRequest(null, null)
    val mePermissionsRequest = GraphRequest(null, "/me/permissions", null, HttpMethod.GET, null)

    // With no access token set
    assertThat(meRequest.urlForSingleRequest).contains(facebookGraphUrl)
    assertThat(mePermissionsRequest.urlForSingleRequest).contains(facebookGraphUrl)

    mockLoggedInWithTokenDomain("facebook")

    // User is logged in with facebook
    assertThat(meRequest.urlForSingleRequest).contains(facebookGraphUrl)
    assertThat(mePermissionsRequest.urlForSingleRequest).contains(facebookGraphUrl)

    mockLoggedInWithTokenDomain("gaming")

    // User is logged in with gaming
    assertThat(meRequest.urlForSingleRequest).contains(gamingGraphUrl)
    assertThat(mePermissionsRequest.urlForSingleRequest).contains(gamingGraphUrl)

    mockLoggedInWithTokenDomain("instagram")

    // User is logged in with instagram
    assertThat(meRequest.urlForSingleRequest).contains(instagramGraphUrl)
    assertThat(mePermissionsRequest.urlForSingleRequest).contains(instagramGraphUrl)
  }

  @Test
  fun testRequestShouldUseClientTokenWhenNoTokenProvided() {
    val request = GraphRequest(null, "testPath", Bundle(), HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockAppTokenString)
  }

  @Test
  fun testFBRequestShouldUseFBTokenWhenPassedAsTokenParam() {
    mockLoggedInWithTokenDomain("facebook")
    val accessToken = createAccessTokenForDomain("facebook")

    val request = GraphRequest(accessToken, "testPath", Bundle(), HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockTokenString)
  }

  @Test
  fun testFBRequestShouldUseFBTokenWhenPassedAsBundleParam() {
    val parameters = Bundle()
    parameters.putString("access_token", mockTokenString)

    val request = GraphRequest(null, "testPath", parameters, HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockTokenString)
  }

  @Test
  fun testIGRequestShouldUseClientTokenWhenRerouted() {
    mockLoggedInWithTokenDomain("instagram")
    val accessToken = createAccessTokenForDomain("instagram")

    val request = GraphRequest(accessToken, mockAppID, Bundle(), HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockAppTokenString)
  }

  @Test
  fun testIGRequestShouldUseIGTokenWhenPassedAsTokenParam() {
    mockLoggedInWithTokenDomain("instagram")
    val accessToken = createAccessTokenForDomain("instagram")

    val request = GraphRequest(accessToken, "me", Bundle(), HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockInstagramTokenString)
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
  fun testCreateIGMeRequest() {
    mockLoggedInWithTokenDomain("instagram")

    val request = GraphRequest.newMeRequest(null, null)

    assertThat(request).isNotNull
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("me")

    val requestUrl = request.urlForSingleRequest
    assertThat(requestUrl).contains("graph.instagram.com")
    assertThat(requestUrl.contains(FacebookSdk.getGraphApiVersion())).isTrue
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

  @Test(expected = FileNotFoundException::class)
  fun `test create upload photo request with an invalid file`() {
    val testFile = File("no_exist")
    GraphRequest.newUploadPhotoRequest(null, "me/photos", testFile, null, null, null)
  }

  @Test
  fun `test create upload photo request with a valid file`() {
    val testFileContent = byteArrayOf(1, 2, 3, 4)
    val testFilename = UUID.randomUUID().toString()
    val testFile = File(testFilename)
    testFile.deleteOnExit()
    testFile.createNewFile()
    testFile.writeBytes(testFileContent)

    val request =
        GraphRequest.newUploadPhotoRequest(null, "me/photos", testFile, "test caption", null, null)

    val parameters = request.parameters
    assertThat(parameters.containsKey("picture")).isTrue
    val fileDescriptor = parameters.getParcelable<ParcelFileDescriptor>("picture")
    val fileInputStream = ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor)
    val readContent = fileInputStream.readBytes()
    assertThat(readContent).isEqualTo(testFileContent)
    assertThat(parameters.getString("caption")).isEqualTo("test caption")
    assertThat(request.graphPath).isEqualTo("me/photos")
  }

  @Test(expected = FacebookException::class)
  fun `test create upload photo request with an invalid uri`() {
    val httpUri = Uri.parse("https://facebook.com")
    GraphRequest.newUploadPhotoRequest(null, "me/photos", httpUri, null, null, null)
  }

  @Test(expected = FileNotFoundException::class)
  fun `test create upload photo request with an invalid file uri`() {
    val fileUri = Uri.parse("file://no-exists")
    GraphRequest.newUploadPhotoRequest(null, "me/photos", fileUri, null, null, null)
  }

  @Test
  fun `test create upload photo request with a content uri`() {
    val contentUri = Uri.parse("content://test_content_uri")

    val request =
        GraphRequest.newUploadPhotoRequest(null, "me/photos", contentUri, null, null, null)

    val parameters = request.parameters
    assertThat(parameters.containsKey("picture")).isTrue
    assertThat(parameters.getParcelable<Uri>("picture")).isEqualTo(contentUri)
    assertThat(request.graphPath).isEqualTo("me/photos")
  }

  @Test
  fun testCreatePlacesSearchRequestWithLocation() {
    val location = Location("")
    location.latitude = 47.6_204
    location.longitude = -122.3_491
    val request = GraphRequest.newPlacesSearchRequest(null, location, 1_000, 50, null, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("search")
  }

  @Test
  fun testCreatePlacesSearchRequestWithSearchText() {
    val request = GraphRequest.newPlacesSearchRequest(null, null, 1_000, 50, "Starbucks", null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.GET)
    assertThat(request.graphPath).isEqualTo("search")
  }

  @Test(expected = FacebookException::class)
  fun testCreatePlacesSearchRequestRequiresLocationOrSearchText() {
    GraphRequest.newPlacesSearchRequest(null, null, 1_000, 50, null, null)
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
    whenever(FacebookSdk.getLimitEventAndDataUsage(any<Context>())).thenReturn(false)
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
  fun testRoutingNoTokenFacebookDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "facebook", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingNoTokenFacebookDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "facebook", "me", "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingNoTokenGamingDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "gaming", mockAppID, "graph.fb.gg", mockAppTokenString)
  }

  @Test
  fun testRoutingNoTokenGamingDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "gaming", "me", "graph.fb.gg", mockAppTokenString)
  }

  @Test
  fun testRoutingNoTokenInstagramDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "instagram", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingNoTokenInstagramDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, null, "instagram", "me", "graph.instagram.com", mockAppTokenString)
  }

  @Test
  fun testRoutingFacebookTokenFacebookDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", mockAppID, "graph.facebook.com", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenFacebookDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", "me", "graph.facebook.com", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringFacebookDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "facebook", mockAppID, "graph.facebook.com", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringFacebookDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "facebook", "me", "graph.facebook.com", mockTokenString)
  }

  @Test
  fun testRoutingGamingTokenGamingDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("gaming")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "gaming", mockAppID, "graph.fb.gg", mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenGamingDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("gaming")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "gaming", "me", "graph.fb.gg", mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenStringGamingDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockGamingTokenString, "gaming", mockAppID, "graph.fb.gg", mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenStringGamingDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockGamingTokenString, "gaming", "me", "graph.fb.gg", mockGamingTokenString)
  }

  @Test
  fun testRoutingInstagramTokenInstagramDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("instagram")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "instagram", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingInstagramTokenInstagramDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("instagram")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "instagram", "me", "graph.instagram.com", mockInstagramTokenString)
  }

  @Test
  fun testRoutingInstagramTokenStringInstagramDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null,
        mockInstagramTokenString,
        "instagram",
        mockAppID,
        "graph.facebook.com",
        mockAppTokenString)
  }

  @Test
  fun testRoutingInstagramTokenStringInstagramDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null,
        mockInstagramTokenString,
        "instagram",
        "me",
        "graph.instagram.com",
        mockInstagramTokenString)
  }

  @Test
  fun testRoutingFacebookTokenGamingDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "gaming", mockAppID, "graph.fb.gg", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenGamingDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "gaming", "me", "graph.fb.gg", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringGamingDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "gaming", mockAppID, "graph.fb.gg", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringGamingDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "gaming", "me", "graph.fb.gg", mockTokenString)
  }

  @Test
  fun testRoutingGamingTokenFacebookDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("gaming")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", mockAppID, "graph.facebook.com", mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenFacebookDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("gaming")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", "me", "graph.facebook.com", mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenStringFacebookDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null,
        mockGamingTokenString,
        "facebook",
        mockAppID,
        "graph.facebook.com",
        mockGamingTokenString)
  }

  @Test
  fun testRoutingGamingTokenStringFacebookDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockGamingTokenString, "facebook", "me", "graph.facebook.com", mockGamingTokenString)
  }

  @Test
  fun testRoutingInstagramTokenFacebookDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("instagram")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingInstagramTokenFacebookDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("instagram")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "facebook", "me", "graph.facebook.com", mockInstagramTokenString)
  }

  @Test
  fun testRoutingInstagramTokenStringFacebookDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null,
        mockInstagramTokenString,
        "facebook",
        mockAppID,
        "graph.facebook.com",
        mockAppTokenString)
  }

  @Test
  fun testRoutingInstagramTokenStringFacebookDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null,
        mockInstagramTokenString,
        "facebook",
        "me",
        "graph.facebook.com",
        mockInstagramTokenString)
  }

  @Test
  fun testRoutingFacebookTokenInstagramDomainApplicationRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "instagram", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingFacebookTokenInstagramDomainMeRequest() {
    val accessToken = createAccessTokenForDomain("facebook")
    createTestCaseForDomainRoutingAndTokenType(
        accessToken, null, "instagram", "me", "graph.instagram.com", mockTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringInstagramDomainApplicationRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "instagram", mockAppID, "graph.facebook.com", mockAppTokenString)
  }

  @Test
  fun testRoutingFacebookTokenStringInstagramDomainMeRequest() {
    createTestCaseForDomainRoutingAndTokenType(
        null, mockTokenString, "instagram", "me", "graph.instagram.com", mockTokenString)
  }

  @Test
  fun testPrefersTokenStringOverAccessTokenObj() {
    val accessToken = createAccessTokenForDomain("instagram")
    val parameters = Bundle()
    parameters.putString("access_token", mockTokenString)
    val request = GraphRequest(accessToken, "me", parameters, HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockTokenString)
  }

  @Test
  fun testForceApplicationRequestWithProvidedClientToken() {
    mockLoggedInWithTokenDomain("instagram")
    val customClientToken = "0000|0000"
    val parameters = Bundle()
    parameters.putString("access_token", customClientToken)
    val request = GraphRequest(null, "", parameters, HttpMethod.GET, null)
    request.setForceApplicationRequest(true)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getHost()).isEqualTo("graph.facebook.com")
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(customClientToken)
  }

  @Test
  fun testForceApplicationRequestWithNoToken() {
    mockLoggedInWithTokenDomain("instagram")
    val parameters = Bundle()
    val request = GraphRequest(null, "", parameters, HttpMethod.GET, null)
    request.setForceApplicationRequest(true)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getHost()).isEqualTo("graph.facebook.com")
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockAppTokenString)
  }

  @Test
  fun testForceApplicationRequestWithInstagramUserToken() {
    mockLoggedInWithTokenDomain("instagram")
    val parameters = Bundle()
    parameters.putString("access_token", mockInstagramTokenString)
    val request = GraphRequest(null, "", parameters, HttpMethod.GET, null)
    request.setForceApplicationRequest(true)
    val requestUri = Uri.parse(request.urlForSingleRequest)
    assertThat(requestUri.getHost()).isEqualTo("graph.facebook.com")
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(mockAppTokenString)
  }

  @Test
  fun `test serializing request batch to url connection`() {
    val mockConnection = mockHttpURLConnection
    whenever(mockConnection.url).thenReturn(URL("https", "graph.facebook.com", 443, "testfile"))
    whenever(mockConnection.requestMethod).thenReturn(HttpMethod.POST.name)
    whenever(mockConnection.getRequestProperty(any())).thenReturn("test property value")
    val requestBatch =
        GraphRequestBatch(
            GraphRequest.newPostRequest(
                null, "testPostPath", JSONObject("{\"key\":\"value\"}"), null),
            GraphRequest.newGraphPathRequest(null, "testGetPath", null))
    GraphRequest.serializeToUrlConnection(requestBatch, mockConnection)
    verify(mockConnection).requestMethod = HttpMethod.POST.name
    val dataInputStream = BufferedInputStream(GZIPInputStream(mockHttpURLConnectionDataStream))
    val data = dataInputStream.reader().readText()
    val decodeData = URLDecoder.decode(data, "UTF-8")
    // check requests are in the decoded data
    assertThat(decodeData).contains("batch_app_id=$mockAppID")
    assertThat(decodeData)
        .contains("\"relative_url\":\"\\/${ServerProtocol.getDefaultAPIVersion()}\\/testPostPath")
    assertThat(decodeData)
        .contains("\"relative_url\":\"\\/${ServerProtocol.getDefaultAPIVersion()}\\/testGetPath")
    assertThat(decodeData).contains("\"body\":\"key=value\"")
  }

  @Test
  fun `test serializing bytearray attachment to url connection`() {
    val mockConnection = mockHttpURLConnection
    whenever(mockConnection.url).thenReturn(URL("https", "graph.facebook.com", 443, "testfile"))
    whenever(mockConnection.requestMethod).thenReturn(HttpMethod.POST.name)
    whenever(mockConnection.getRequestProperty(any())).thenReturn("test property value")
    val request = GraphRequest.newPostRequest(null, "testPath", null, null)
    request.parameters.putByteArray("attachment", "test attachment data".toByteArray())
    val requestBatch = GraphRequestBatch(request)
    GraphRequest.serializeToUrlConnection(requestBatch, mockConnection)
    verify(mockConnection).requestMethod = HttpMethod.POST.name
    val dataInputStream = BufferedInputStream((mockHttpURLConnectionDataStream))
    val data = dataInputStream.reader().readText()
    val decodeData = URLDecoder.decode(data, "UTF-8")
    // check requests are in the decoded data
    assertThat(decodeData).contains("test attachment data")
  }

  @Test
  fun `test serializing graph object to url connection`() {
    val mockConnection = mockHttpURLConnection
    whenever(mockConnection.url).thenReturn(URL("https", "graph.facebook.com", 443, "testfile"))
    whenever(mockConnection.requestMethod).thenReturn(HttpMethod.POST.name)
    whenever(mockConnection.getRequestProperty(any())).thenReturn("test property value")

    val graphObjectToSerialized =
        JSONObject("""
      {"v1":1,"v2":"test","v3":true,"v4":[1,2,3]}
      """.trim())
    val request = GraphRequest.newPostRequest(null, "testPath", graphObjectToSerialized, null)
    GraphRequest.serializeToUrlConnection(GraphRequestBatch(request), mockConnection)

    val dataInputStream = BufferedInputStream(GZIPInputStream(mockHttpURLConnectionDataStream))
    val data = dataInputStream.reader().readText()
    val decodeData = URLDecoder.decode(data, "UTF-8")
    val dataItems = decodeData.split("&")
    assertThat(dataItems)
        .containsAll(listOf("v1=1", "v2=test", "v3=true", "v4[0]=1", "v4[1]=2", "v4[2]=3"))
  }

  @Test
  fun `test serializing graph object of id reference to url connection`() {
    val mockConnection = mockHttpURLConnection
    whenever(mockConnection.url).thenReturn(URL("https", "graph.facebook.com", 443, "testfile"))
    whenever(mockConnection.requestMethod).thenReturn(HttpMethod.POST.name)
    whenever(mockConnection.getRequestProperty(any())).thenReturn("test property value")

    val graphObjectToSerialized = JSONObject("""
      {"obj":{"id":"12345"}}
      """.trim())
    val request = GraphRequest.newPostRequest(null, "testPath", graphObjectToSerialized, null)
    GraphRequest.serializeToUrlConnection(GraphRequestBatch(request), mockConnection)

    val dataInputStream = BufferedInputStream(GZIPInputStream(mockHttpURLConnectionDataStream))
    val data = dataInputStream.reader().readText()
    val decodeData = URLDecoder.decode(data, "UTF-8")
    val dataItems = decodeData.split("&")
    assertThat(dataItems).contains("obj=12345")
  }

  @Test
  fun `test serializing graph object of url reference to url connection`() {
    val mockConnection = mockHttpURLConnection
    whenever(mockConnection.url).thenReturn(URL("https", "graph.facebook.com", 443, "testfile"))
    whenever(mockConnection.requestMethod).thenReturn(HttpMethod.POST.name)
    whenever(mockConnection.getRequestProperty(any())).thenReturn("test property value")

    val graphObjectToSerialized =
        JSONObject("""
      {"obj":{"url":"http://facebook.com/"}}
      """.trim())
    val request = GraphRequest.newPostRequest(null, "testPath", graphObjectToSerialized, null)
    GraphRequest.serializeToUrlConnection(GraphRequestBatch(request), mockConnection)

    val dataInputStream = BufferedInputStream(GZIPInputStream(mockHttpURLConnectionDataStream))
    val data = dataInputStream.reader().readText()
    val decodeData = URLDecoder.decode(data, "UTF-8")
    val dataItems = decodeData.split("&")
    assertThat(dataItems).contains("obj=http://facebook.com/")
  }

  @Test
  fun `test validating get requests with null or empty fields params will generate an error log`() {
    val mockLoggerCompanion = mock<Logger.Companion>()
    Whitebox.setInternalState(Logger::class.java, "Companion", mockLoggerCompanion)
    val graphRequestWithNullFieldsParams = GraphRequest.newGraphPathRequest(null, "/test/", null)
    graphRequestWithNullFieldsParams.parameters.remove(GraphRequest.FIELDS_PARAM)
    val graphRequestWithEmptyFieldsParams = GraphRequest.newGraphPathRequest(null, "/test/", null)
    graphRequestWithEmptyFieldsParams.parameters.putString(GraphRequest.FIELDS_PARAM, "")
    GraphRequest.validateFieldsParamForGetRequests(
        GraphRequestBatch(graphRequestWithNullFieldsParams, graphRequestWithEmptyFieldsParams))
    verify(mockLoggerCompanion, times(2))
        .log(eq(LoggingBehavior.DEVELOPER_ERRORS), eq(Log.WARN), any(), any())
  }

  @Test
  fun `test validating get requests with correct fields params`() {
    val mockLoggerCompanion = mock<Logger.Companion>()
    Whitebox.setInternalState(Logger::class.java, "Companion", mockLoggerCompanion)
    val graphRequest = GraphRequest.newGraphPathRequest(null, "/test/", null)
    graphRequest.parameters.putString(GraphRequest.FIELDS_PARAM, "id,name")
    GraphRequest.validateFieldsParamForGetRequests(GraphRequestBatch(graphRequest))
    verify(mockLoggerCompanion, never())
        .log(eq(LoggingBehavior.DEVELOPER_ERRORS), eq(Log.WARN), any(), any())
  }

  fun createAccessTokenForDomain(domain: String): AccessToken {
    var tokenString: String? = null
    if (domain == "gaming") {
      tokenString = mockGamingTokenString
    } else if (domain == "instagram") {
      tokenString = mockInstagramTokenString
    } else {
      tokenString = mockTokenString
    }
    return AccessToken(
        tokenString, mockAppID, mockUserID, null, null, null, null, null, null, null, domain)
  }

  fun createTestCaseForDomainRoutingAndTokenType(
      accessToken: AccessToken?,
      tokenString: String?,
      currentTokenDomain: String,
      graphPath: String,
      expectedHost: String,
      expectedTokenToUse: String
  ) {
    mockLoggedInWithTokenDomain(currentTokenDomain)

    val parameters = Bundle()
    if (tokenString != null) {
      parameters.putString("access_token", tokenString)
    }
    val request = GraphRequest(accessToken, graphPath, parameters, HttpMethod.GET, null)
    val requestUri = Uri.parse(request.urlForSingleRequest)

    assertThat(requestUri.getHost()).isEqualTo(expectedHost)
    assertThat(requestUri.getQueryParameter("access_token")).isEqualTo(expectedTokenToUse)
  }

  fun mockLoggedInWithTokenDomain(tokenDomain: String) {
    val mockAccessToken = createAccessTokenForDomain(tokenDomain)
    val mockTokenCompanionObject = mock<AccessToken.Companion>()
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockTokenCompanionObject)
    whenever(mockTokenCompanionObject.getCurrentAccessToken()).thenReturn(mockAccessToken)
  }
}
