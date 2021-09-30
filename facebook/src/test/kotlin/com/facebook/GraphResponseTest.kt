package com.facebook

import java.net.HttpURLConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, HttpURLConnection::class)
class GraphResponseTest : FacebookPowerMockTestCase() {

  private val validJsonSingle = "{\"anything\":\"swag\"}"

  private val validJsonSingleNested =
      "{\n" +
          "  \"data\": [\n" +
          "    {\n" +
          "      \"gatekeepers\": [\n" +
          "        {\n" +
          "          \"key\": \"" +
          "FBSDKFeatureInstrument" +
          "\",\n" +
          "          \"value\": true\n" +
          "        },\n" +
          "        {\n" +
          "          \"key\": \"" +
          "app_events_killswitch" +
          "\",\n" +
          "          \"value\": \"false\"\n" +
          "        }\n" +
          "      ]\n" +
          "    }\n" +
          "  ]\n" +
          "}"

  private val validJson =
      "[\n" +
          "     { \"body\": {\"number\": \"420 12345\" }},\n" +
          "     { \"body\": {\"number\": \"1337 9000\" }}\n" +
          " ]"

  private val validJsonWithErrorCode =
      "[\n" +
          "     { \"body\": {\"number\": \"420 12345\"}," +
          "        \"code\": \"400\" " +
          "      },\n" +
          "     { \"body\": {\"number\": \"1337 9000\"}," +
          "\"code\": \"400\"" +
          "}\n" +
          " ]"

  private val validJsonWithSuccessCode =
      "[\n" +
          "     { \"body\": {\"number\": \"420 12345\"}," +
          "        \"code\": \"200\" " +
          "      },\n" +
          "     { \"body\": {\"number\": \"1337 9000\"}," +
          "\"code\": \"200\"" +
          "}\n" +
          " ]"
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.isFullyInitialized()).thenReturn(true)
  }

  @Test
  fun `test get 400 response from batch`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(400)
    PowerMockito.`when`(connection.errorStream).thenReturn(validJson.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(2, res.size)
    assertEquals("{\"number\":\"420 12345\"}", res[0].rawResponse)
    assertEquals("420 12345", res[0].jsonObject.getString("number"))
    assertEquals("{\"number\":\"1337 9000\"}", res[1].rawResponse)
    assertEquals("1337 9000", res[1].jsonObject.getString("number"))
  }

  @Test
  fun `test get 200 response from batch`() {
    // notice no difference from 400 in terms of still getting parseable response
    // compare to single request
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(200)
    PowerMockito.`when`(connection.inputStream).thenReturn(validJson.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(2, res.size)
    assertEquals("{\"number\":\"420 12345\"}", res[0].rawResponse)
    assertEquals("420 12345", res[0].jsonObject.getString("number"))
    assertEquals("{\"number\":\"1337 9000\"}", res[1].rawResponse)
    assertEquals("1337 9000", res[1].jsonObject.getString("number"))
  }

  @Test
  fun `test get 400 response from batch with 400 code in json`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(400)
    PowerMockito.`when`(connection.errorStream).thenReturn(validJsonWithErrorCode.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(2, res.size)
    for (response in res) {
      assertNotNull(response.error)
      assertNull(response.rawResponse)
      assertEquals(400, response.error.requestStatusCode)
      assertEquals(FacebookRequestError.INVALID_ERROR_CODE, response.error.errorCode)
    }
  }

  @Test
  fun `test get 200 response from batch with 200 code in json`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(200)
    PowerMockito.`when`(connection.inputStream)
        .thenReturn(validJsonWithSuccessCode.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(2, res.size)
    assertEquals("{\"number\":\"420 12345\"}", res[0].rawResponse)
    assertEquals("420 12345", res[0].jsonObject.getString("number"))
    assertEquals("{\"number\":\"1337 9000\"}", res[1].rawResponse)
    assertEquals("1337 9000", res[1].jsonObject.getString("number"))
  }

  @Test
  fun `test get 200 response single request`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(200)
    PowerMockito.`when`(connection.inputStream).thenReturn(validJsonSingle.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(1, res.size)
    assertEquals(validJsonSingle, res[0].rawResponse)
    assertEquals("swag", res[0].jsonObject.getString("anything"))
  }

  @Test
  fun `test get 200 response single request nested`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(200)
    PowerMockito.`when`(connection.inputStream).thenReturn(validJsonSingleNested.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(1, res.size)
    assertEquals(
        "{\"data\":[{\"gatekeepers\":[{\"key\":\"FBSDKFeatureInstrument\",\"value\":true},{\"key\":\"app_events_killswitch\",\"value\":\"false\"}]}]}",
        res[0].rawResponse)
  }

  @Test
  fun `test get 400 response single request`() {
    val connection = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.`when`(connection.responseCode).thenReturn(400)
    PowerMockito.`when`(connection.errorStream).thenReturn(validJsonSingle.byteInputStream())
    val graphRequestBatch = GraphRequestBatch()
    graphRequestBatch.add(GraphRequest())
    val res = GraphResponse.fromHttpConnection(connection, graphRequestBatch)
    assertEquals(1, res.size)
    val response = res[0]
    assertNotNull(response.error)
    assertNull(response.rawResponse)
    assertEquals(400, response.error.requestStatusCode)
    assertEquals(FacebookRequestError.INVALID_ERROR_CODE, response.error.errorCode)
  }
}
