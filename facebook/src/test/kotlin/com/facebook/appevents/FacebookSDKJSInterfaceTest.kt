package com.facebook.appevents

import com.facebook.FacebookPowerMockTestCase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacebookSDKJSInterfaceTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"supports_implicit_sdk_logging\": true,\n" +
          "  \"android_dialog_configs\": [20140701, 20140702, 20140703]\n" +
          "}"
  @Test
  fun `test with valid json`() {
    val result = FacebookSDKJSInterface.jsonStringToBundle(validJson)
    assertNotNull(result["supports_implicit_sdk_logging"])
    assertNotNull(result["android_dialog_configs"])
  }

  @Test
  fun `test nonsense json`() {
    val result = FacebookSDKJSInterface.jsonStringToBundle("anystringwhat")
    assertTrue(result.isEmpty)
  }
}
