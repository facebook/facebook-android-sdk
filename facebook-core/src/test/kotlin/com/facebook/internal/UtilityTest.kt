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

import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlin.collections.HashMap
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    AccessToken::class,
    FacebookSdk::class,
    GraphRequest::class,
    Utility::class,
)
class UtilityTest : FacebookPowerMockTestCase() {

  private val mockTokenString = "A token of my esteem"
  private val mockAppID = "1234"
  private val mockClientToken = "5678"

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    PowerMockito.`when`(FacebookSdk.getClientToken()).thenReturn(mockClientToken)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.`when`(FacebookSdk.getGraphDomain()).thenCallRealMethod()
    PowerMockito.`when`(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
    PowerMockito.`when`(FacebookSdk.getGraphApiVersion()).thenCallRealMethod()
  }

  @Test
  @Throws(Exception::class)
  fun testGetGraphMeRequestWithCacheAsyncNoGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(null, "id,name,first_name,middle_name,last_name")
  }

  @Test
  @Throws(Exception::class)
  fun testGetGraphMeRequestWithCacheAsyncNullGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(
        "facebook", "id,name,first_name,middle_name,last_name")
  }

  @Test
  @Throws(Exception::class)
  fun testGetGraphMeRequestWithCacheAsyncGGGraphDomain() {
    createTestForGetGraphMeRequestWithCacheAsync(
        "gaming", "id,name,first_name,middle_name,last_name")
  }

  @Test
  @Throws(Exception::class)
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

  @Throws(Exception::class)
  fun createTestForGetGraphMeRequestWithCacheAsync(graphDomain: String?, expectedFields: String) {
    PowerMockito.whenNew(GraphRequest::class.java)
        .withAnyArguments()
        .thenReturn(mock<GraphRequest>())

    val accessToken = mock<AccessToken>()
    whenever(accessToken.graphDomain).thenReturn(graphDomain)
    val mockTokenCompanionObject = mock<AccessToken.Companion>()
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockTokenCompanionObject)
    whenever(mockTokenCompanionObject.getCurrentAccessToken()).thenReturn(accessToken)

    val bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle::class.java)
    Utility.getGraphMeRequestWithCacheAsync(
        mockTokenString, mock<Utility.GraphMeRequestWithCacheCallback>())

    PowerMockito.verifyNew(GraphRequest::class.java)
        .withArguments(
            isNull(),
            eq<String>("me"),
            bundleArgumentCaptor.capture(),
            eq(HttpMethod.GET),
            isNull(),
            isNull(),
            any<Int>(),
            isNull()) // @JvmOverloads adds extra arguments
    val parameters = bundleArgumentCaptor.getValue()
    assertThat(parameters.getString("fields")).isEqualTo(expectedFields)
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
  fun testUnmodifiableCollection() {
    val result = Utility.unmodifiableCollection("t1", "t2")
    assertArrayEquals(arrayOf("t1", "t2"), result.toTypedArray())
  }

  @Test
  fun testConvertJSONObjectToStringMap() {
    val validJson = "{\"k1\": true, \"k2\": \"value\"}"
    val result = Utility.convertJSONObjectToStringMap(JSONObject(validJson))
    assertEquals(mapOf("k1" to "true", "k2" to "value"), result)
  }
}
