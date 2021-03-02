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

import com.facebook.FacebookRequestError
import com.facebook.FacebookTestCase
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FacebookRequestErrorClassificationTest : FacebookTestCase() {
  private val errorClassificationJSON =
      "{" +
          "   \"android_sdk_error_categories\": [" +
          "      {" +
          "         \"name\": \"other\"," +
          "         \"items\": [" +
          "           { \"code\": 102, \"subcodes\": [ 459, 464 ] }," +
          "           { \"code\": 190, \"subcodes\": [ 459, 464 ] }" +
          "         ]" +
          "      }," +
          "      {" +
          "         \"name\": \"login_recoverable\"," +
          "         \"items\": [ { \"code\": 102 }, { \"code\": 190 } ]," +
          "         \"recovery_message\": \"Please log into this app again to reconnect your Facebook account.\"" +
          "      }," +
          "      {" +
          "         \"name\": \"transient\"," +
          "         \"items\": [ { \"code\": 1 }, { \"code\": 2 }, { \"code\": 4 }, { \"code\": 9 }, { \"code\": 17 }, { \"code\": 341 } ]" +
          "      }" +
          "   ]," +
          "   \"id\": \"233936543368280\"" +
          "}"

  @Test
  fun `test create from json`() {
    val serverResponse = JSONObject(errorClassificationJSON)
    val jsonArray = serverResponse.getJSONArray("android_sdk_error_categories")
    val errorClassification = FacebookRequestErrorClassification.createFromJSON(jsonArray)
    assertNotNull(errorClassification)
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER))
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT))
    assertNotNull(
        errorClassification.getRecoveryMessage(FacebookRequestError.Category.LOGIN_RECOVERABLE))
    assertEquals(2, errorClassification.otherErrors.size)
    assertEquals(2, errorClassification.loginRecoverableErrors.size)
    assertEquals(6, errorClassification.transientErrors.size)
    // test subcodes
    assertEquals(2, errorClassification.otherErrors[102]?.size)
    assertNull(errorClassification.loginRecoverableErrors[102])
  }

  @Test
  fun `test classify category `() {
    val serverResponse = JSONObject(errorClassificationJSON)
    val jsonArray = serverResponse.getJSONArray("android_sdk_error_categories")
    val errorClassification = FacebookRequestErrorClassification.createFromJSON(jsonArray)
    assertEquals(FacebookRequestError.Category.TRANSIENT, errorClassification.classify(1, 2, true))
    assertEquals(FacebookRequestError.Category.OTHER, errorClassification.classify(102, 459, false))
    assertEquals(
        FacebookRequestError.Category.LOGIN_RECOVERABLE,
        errorClassification.classify(190, 1, false))
    assertEquals(
        FacebookRequestError.Category.TRANSIENT, errorClassification.classify(341, 1, false))
  }

  @Test
  fun `test get default error classification `() {
    val errorClassification = FacebookRequestErrorClassification.getDefaultErrorClassification()
    assertNotNull(errorClassification)
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER))
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT))
    assertNull(
        errorClassification.getRecoveryMessage(FacebookRequestError.Category.LOGIN_RECOVERABLE))

    assertNull(errorClassification.otherErrors)
    assertNotNull(errorClassification.transientErrors)
    assertNotNull(errorClassification.loginRecoverableErrors)
  }
}
