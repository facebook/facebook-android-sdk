/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
    var errorClassification = FacebookRequestErrorClassification.createFromJSON(jsonArray)
    assertNotNull(errorClassification)
    errorClassification = checkNotNull(errorClassification)
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER))
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT))
    assertNotNull(
        errorClassification.getRecoveryMessage(FacebookRequestError.Category.LOGIN_RECOVERABLE))
    assertEquals(2, errorClassification.otherErrors?.size)
    assertEquals(2, errorClassification.loginRecoverableErrors?.size)
    assertEquals(6, errorClassification.transientErrors?.size)
    // test subcodes
    assertEquals(2, errorClassification.otherErrors?.get(102)?.size)
    assertNull(errorClassification.loginRecoverableErrors?.get(102))
  }

  @Test
  fun `test classify category `() {
    val serverResponse = JSONObject(errorClassificationJSON)
    val jsonArray = serverResponse.getJSONArray("android_sdk_error_categories")
    var errorClassification = FacebookRequestErrorClassification.createFromJSON(jsonArray)
    errorClassification = checkNotNull(errorClassification)
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
    val errorClassification = FacebookRequestErrorClassification.defaultErrorClassification
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER))
    assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT))
    assertNull(
        errorClassification.getRecoveryMessage(FacebookRequestError.Category.LOGIN_RECOVERABLE))

    assertNull(errorClassification.otherErrors)
    assertNotNull(errorClassification.transientErrors)
    assertNotNull(errorClassification.loginRecoverableErrors)
  }
}
