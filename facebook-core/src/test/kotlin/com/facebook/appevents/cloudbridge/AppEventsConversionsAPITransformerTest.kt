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

package com.facebook.appevents.cloudbridge

import com.facebook.FacebookTestUtility.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppEventsConversionsAPITransformerTest {

  private val customEventsAppData =
      """[{"_eventName":"fb_mobile_add_to_cart","_logTime":12345,"_ui":"unknown","_inBackground":"1"}, {"_eventName":"new_event","_logTime":67890, "fb_content_type":"product", "_valueToSum":21.97, "fb_currency":"GBP"}]"""

  private val userData = "{\"fn\":\"1234567890\", \"em\":\"ABCDE\"}"

  private val restOfData1 =
      mutableMapOf<String, Any>(
          AppEventsConversionsAPITransformer.DataProcessingParameterName.OPTIONS.rawValue to "[]",
          AppEventsConversionsAPITransformer.DataProcessingParameterName.STATE.rawValue to 0)

  private val transformedAppData1 =
      mutableMapOf<String, Any>(
          AppEventUserAndAppDataField.ADV_TE.rawValue to 1,
          AppEventUserAndAppDataField.EXT_INFO.rawValue to arrayListOf("i2"))

  private val transformedUserData1 =
      mutableMapOf<String, Any>(
          ConversionsAPIUserAndAppDataField.MAD_ID.rawValue to "ABCDE-12345",
          "fn" to "1234567890",
          "em" to "ABCDE")

  private val transformedCustomEvent1 =
      mutableMapOf<String, Any>(
          ConversionsAPICustomEventField.EVENT_NAME.rawValue to
              ConversionsAPIEventName.ADDED_TO_CART.rawValue,
          ConversionsAPICustomEventField.EVENT_TIME.rawValue to 12345)

  private val transformedCustomEvent2 =
      mutableMapOf(
          ConversionsAPICustomEventField.EVENT_NAME.rawValue to "new_event",
          ConversionsAPICustomEventField.EVENT_TIME.rawValue to 67890,
          ConversionsAPISection.CUSTOM_DATA.rawValue to
              mapOf(
                  ConversionsAPICustomEventField.CONTENT_TYPE.rawValue to "product",
                  ConversionsAPICustomEventField.VALUE_TO_SUM.rawValue to 21.97,
                  ConversionsAPICustomEventField.CURRENCY.rawValue to "GBP"))

  private fun checkIfCommonFieldsAreEqual(parameters: Map<String, Any>) {
    val userData = parameters[ConversionsAPISection.USER_DATA.rawValue] as Map<*, *>
    assertThat(userData).isEqualTo(transformedUserData1)

    val appData = parameters[ConversionsAPISection.APP_DATA.rawValue] as Map<*, *>

    val ate = appData[ConversionsAPIUserAndAppDataField.ADV_TE.rawValue]
    val extInfo = appData[ConversionsAPIUserAndAppDataField.EXT_INFO.rawValue] as ArrayList<*>

    assertThat(ate).isEqualTo(1)
    assertNotNull(extInfo)
    assertThat(extInfo.size).isEqualTo(ate)
    assertThat(extInfo[0] as String).isEqualTo("i2")

    val actionSource = parameters[OtherEventConstants.ACTION_SOURCE.rawValue] as? String
    assertThat(OtherEventConstants.APP.rawValue).isEqualTo(actionSource)

    val options =
        parameters[
            AppEventsConversionsAPITransformer.DataProcessingParameterName.OPTIONS.rawValue] as?
            String
    val state =
        parameters[
            AppEventsConversionsAPITransformer.DataProcessingParameterName.STATE.rawValue] as?
            Int
    assertThat(options).isEqualTo("[]")
    assertThat(state).isEqualTo(0)
  }

  private fun checkIfMAIEventIsEqual(parameters: Map<String, Any>) {
    val eventName = parameters[ConversionsAPICustomEventField.EVENT_NAME.rawValue] as? String
    val eventTime = parameters[ConversionsAPICustomEventField.EVENT_TIME.rawValue] as? Int

    assertThat(eventName).isEqualTo(OtherEventConstants.MOBILE_APP_INSTALL.rawValue)
    assertThat(eventTime).isEqualTo(23456)
  }

  private fun checkIfCustomEvent1IsEqual(parameters: Map<String, Any>) {
    val eventName = parameters[ConversionsAPICustomEventField.EVENT_NAME.rawValue] as? String
    val eventTime = parameters[ConversionsAPICustomEventField.EVENT_TIME.rawValue] as? Int

    assertThat(eventName).isEqualTo("AddToCart")
    assertThat(eventTime).isEqualTo(12345)
  }

  private fun checkIfCustomEvent2IsEqual(parameters: Map<String, Any>) {
    val eventName = parameters[ConversionsAPICustomEventField.EVENT_NAME.rawValue] as? String
    val eventTime = parameters[ConversionsAPICustomEventField.EVENT_TIME.rawValue] as? Int

    val customData1 = parameters[ConversionsAPISection.CUSTOM_DATA.rawValue] as Map<*, *>

    val contentType = customData1[ConversionsAPICustomEventField.CONTENT_TYPE.rawValue] as? String
    val valueToSum = customData1[ConversionsAPICustomEventField.VALUE_TO_SUM.rawValue] as? Double
    val currency = customData1[ConversionsAPICustomEventField.CURRENCY.rawValue] as? String

    assertThat(eventName).isEqualTo("new_event")
    assertThat(eventTime).isEqualTo(67890)
    assertThat(contentType).isEqualTo("product")
    assertThat(valueToSum).isEqualTo(21.97)
    assertThat(currency).isEqualTo("GBP")
  }

  @Test
  fun testTransformEvents() {

    val events = """[{"_eventName":"fb_mobile_add_to_cart","_logTime":12345}]"""
    var transformedEvents = AppEventsConversionsAPITransformer.transformEvents(events)
    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.count()).isEqualTo(1)

    val firstEvent = transformedEvents[0]
    checkIfCustomEvent1IsEqual(firstEvent)
  }

  @Test
  fun testTransformMultipleEvents() {

    var transformedEvents = AppEventsConversionsAPITransformer.transformEvents(customEventsAppData)
    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.count()).isEqualTo(2)

    val firstEvent = transformedEvents[0]
    checkIfCustomEvent1IsEqual(firstEvent)

    val secondEvent = transformedEvents[1]
    checkIfCustomEvent2IsEqual(secondEvent)
  }

  @Test
  fun testTransformAndUpdateAppAndUserData() {
    val userData = mutableMapOf<String, Any>()
    val appData = mutableMapOf<String, Any>()

    AppEventsConversionsAPITransformer.transformAndUpdateAppAndUserData(
        userData, appData, AppEventUserAndAppDataField.ADVERTISER_ID, value = "ABCDE-12345")
    AppEventsConversionsAPITransformer.transformAndUpdateAppAndUserData(
        userData,
        appData,
        AppEventUserAndAppDataField.USER_DATA,
        value = "{\"fn\":\"1234567890\", \"em\":\"ABCDE\"}")
    AppEventsConversionsAPITransformer.transformAndUpdateAppAndUserData(
        userData, appData, AppEventUserAndAppDataField.ADV_TE, value = 1)
    AppEventsConversionsAPITransformer.transformAndUpdateAppAndUserData(
        userData, appData, AppEventUserAndAppDataField.EXT_INFO, value = listOf("i2"))

    val madid = userData[ConversionsAPIUserAndAppDataField.MAD_ID.rawValue] as String
    val fn = userData["fn"] as String
    val em = userData["em"] as String

    val ate = appData[ConversionsAPIUserAndAppDataField.ADV_TE.rawValue] as Int
    val extInfo = appData[ConversionsAPIUserAndAppDataField.EXT_INFO.rawValue] as List<*>

    assertThat(ate).isEqualTo(1)
    assertThat(extInfo.size).isEqualTo(1)
    assertThat(extInfo[0] as? String).isEqualTo("i2")

    assertThat(madid).isEqualTo("ABCDE-12345")
    assertThat(fn).isEqualTo("1234567890")
    assertThat(em).isEqualTo("ABCDE")
  }

  @Test
  fun testTransformValueContents() {
    val transformedValue =
        AppEventsConversionsAPITransformer.transformValue(
            CustomEventField.CONTENTS.rawValue,
            "[{\"id\": \"1234\", \"quantity\": 1},{\"id\":\"5678\", \"quantity\": 2}]") as
            ArrayList<*>

    assertThat(transformedValue.size).isEqualTo(2)

    var dict = transformedValue[0] as Map<*, *>
    assertThat(dict["id"] as? String).isEqualTo("1234")
    assertThat(dict["quantity"] as? Int).isEqualTo(1)

    dict = transformedValue[1] as Map<*, *>
    assertThat(dict["id"] as? String).isEqualTo("5678")
    assertThat(dict["quantity"] as? Int).isEqualTo(2)
  }

  @Test
  fun testTransformValueATE() {
    val ateTransformedValue =
        AppEventsConversionsAPITransformer.transformValue(
            AppEventUserAndAppDataField.ADV_TE.rawValue, "1") as?
            Boolean

    assertThat(ateTransformedValue).isEqualTo(true)
  }

  @Test
  fun testTransformValueExtinfo() {
    val transformedValue =
        AppEventsConversionsAPITransformer.transformValue(
            AppEventUserAndAppDataField.EXT_INFO.rawValue, "[\"i0\", [\"i1\"], [\"i2\"]]") as
            ArrayList<*>

    assertThat(transformedValue.size).isEqualTo(3)
    assertThat(transformedValue[0] as? String).isEqualTo("i0")
  }

  @Test
  fun testTransformValueEventTime() {

    val dataProviderInputToExpected = mapOf(12345 to 12345, "12345" to 12345, "abcd" to null)

    dataProviderInputToExpected.forEach { pair ->
      val actual =
          AppEventsConversionsAPITransformer.transformValue(
              CustomEventField.EVENT_TIME.rawValue, pair.key) as?
              Int
      val expected = pair.value

      assertThat(actual).isEqualTo(expected)
    }
  }

  @Test
  fun testCombineCommonFields() {

    val combinedFields =
        AppEventsConversionsAPITransformer.combineCommonFields(
            transformedUserData1, transformedAppData1, restOfData1)

    checkIfCommonFieldsAreEqual(combinedFields)
  }

  @Test
  fun testCombineAllTransformedData1() {
    var transformedEvents =
        AppEventsConversionsAPITransformer.combineAllTransformedData(
            AppEventType.MOBILE_APP_INSTALL,
            transformedUserData1,
            transformedAppData1,
            restOfData1,
            listOf(), // listOf<Map<String,Any>>()
            23456)

    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.size).isEqualTo(1)

    val firstEvent = transformedEvents[0]
    checkIfCommonFieldsAreEqual(firstEvent)
    checkIfMAIEventIsEqual(firstEvent)
  }

  @Test
  fun testCombineAllTransformedData2() {
    val customEvents = arrayListOf(transformedCustomEvent1, transformedCustomEvent2)

    var transformedEvents =
        AppEventsConversionsAPITransformer.combineAllTransformedData(
            AppEventType.CUSTOM,
            transformedUserData1,
            transformedAppData1,
            restOfData1,
            customEvents,
            null)

    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.size).isEqualTo(2)

    val firstEvent = transformedEvents[0]
    checkIfCommonFieldsAreEqual(firstEvent)
    checkIfCustomEvent1IsEqual(firstEvent)

    val secondEvent = transformedEvents[1]
    checkIfCommonFieldsAreEqual(secondEvent)
    checkIfCustomEvent2IsEqual(secondEvent)
  }

  @Test
  fun testCombineAllTransformedDataCheckForNulls() {
    val customEvents = arrayListOf(transformedCustomEvent1, transformedCustomEvent2)

    assertThat(
            AppEventsConversionsAPITransformer.combineAllTransformedData(
                AppEventType.OTHER,
                transformedUserData1,
                transformedAppData1,
                restOfData1,
                customEvents,
                null))
        .isNull()

    assertThat(
            AppEventsConversionsAPITransformer.combineAllTransformedData(
                AppEventType.CUSTOM,
                transformedUserData1,
                transformedAppData1,
                restOfData1,
                listOf(), // listOf<Map<String,Any>>()
                null))
        .isNull()

    assertThat(
            AppEventsConversionsAPITransformer.combineAllTransformedData(
                AppEventType.MOBILE_APP_INSTALL,
                transformedUserData1,
                transformedAppData1,
                restOfData1,
                listOf(), // listOf<Map<String,Any>>(),
                null))
        .isNull()
  }

  @Test
  fun testConversionsAPICompatibleEvent1() {
    val parameters =
        mutableMapOf<String, Any>(
            "event" to "MOBILE_APP_INSTALL",
            "advertiser_id" to "ABCDE-12345",
            "advertiser_tracking_enabled" to 1,
            "ud" to userData,
            "extInfo" to arrayListOf("i2"),
            "install_timestamp" to 23456)

    parameters.putAll(restOfData1)

    var transformedEvents =
        AppEventsConversionsAPITransformer.conversionsAPICompatibleEvent(parameters)

    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.size).isEqualTo(1)

    val firstEvent = transformedEvents[0]
    checkIfCommonFieldsAreEqual(firstEvent)
    checkIfMAIEventIsEqual(firstEvent)
  }

  @Test
  fun testConversionsAPICompatibleEvent2() {
    val parameters =
        mutableMapOf<String, Any>(
            "event" to "CUSTOM_APP_EVENTS",
            "advertiser_id" to "ABCDE-12345",
            "advertiser_tracking_enabled" to 1,
            "ud" to userData,
            "extInfo" to arrayListOf("i2"),
            "custom_events" to
                customEventsAppData // parameters is the equivalent of the request.graphObject,
            // custom_events the equivalent of request.tag
            )

    parameters.putAll(restOfData1)

    var transformedEvents =
        AppEventsConversionsAPITransformer.conversionsAPICompatibleEvent(parameters)
    transformedEvents = assertNotNull(transformedEvents)
    assertThat(transformedEvents.size).isEqualTo(2)

    val secondEvent = transformedEvents[1]
    checkIfCommonFieldsAreEqual(secondEvent)
    checkIfCustomEvent2IsEqual(secondEvent)
  }
}
