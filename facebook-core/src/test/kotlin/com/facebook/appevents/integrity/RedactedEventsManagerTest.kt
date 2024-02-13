/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.integrity.RedactedEventsManager.disable
import com.facebook.appevents.integrity.RedactedEventsManager.enable
import com.facebook.appevents.integrity.RedactedEventsManager.processEventsRedaction
import com.facebook.internal.*
import org.assertj.core.api.Assertions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
        FacebookSdk::class,
        FetchedAppSettingsManager::class)
class RedactedEventsManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockRedactedEventsFromServer: JSONArray
    private lateinit var mockRedactedEvents: JSONArray
    private val mockEventNameNotInRedactedEventsList = "install_app"
    private val mockRedactedString = "FilteredEvent"
    
    private val mockEventNameInRedactedEventsList1 = "redacted_events_1"
    private val mockEventNameInRedactedEventsList2 = "redacted_events_2"
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()
    
    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        mockRedactedEvents = JSONArray()
        
        mockRedactedEvents.put(mockEventNameInRedactedEventsList1)
        mockRedactedEvents.put(mockEventNameInRedactedEventsList2)

        mockRedactedEventsFromServer = JSONArray()
        val jsonObject = JSONObject().apply {
            put("key", mockRedactedString)
            put("value",mockRedactedEvents)
        }
        mockRedactedEventsFromServer.put(jsonObject)
    }

    @After
    fun tearDown() {
        disable()
    }

    private fun initMockFetchedAppSettings(mockRedactedEvents: JSONArray?) {
        val mockFetchedAppSettings = FetchedAppSettings(
                false,
                "",
                false,
                1,
                SmartLoginOption.parseOptions(0),
                emptyMap(),
                false,
                mockFacebookRequestErrorClassification,
                "",
                "",
                false,
                codelessEventsEnabled = false,
                eventBindings = emptyJSONArray,
                sdkUpdateMessage = "",
                trackUninstallEnabled = false,
                monitorViaDialogEnabled = false,
                rawAamRules = "",
                suggestedEventsSetting = "",
                restrictiveDataSetting = "",
                protectedModeStandardParamsSetting = emptyJSONArray,
                MACARuleMatchingSetting = emptyJSONArray,
                migratedAutoLogValues = null,
                blocklistEvents = emptyJSONArray,
                redactedEvents = mockRedactedEvents
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
                .thenReturn(mockFetchedAppSettings)
    }

    @Test
    fun `test fetched redacted events list is null from the server`() {
        initMockFetchedAppSettings(null)
        enable()
        val finalEventName = processEventsRedaction(mockEventNameInRedactedEventsList1)
        Assertions.assertThat(finalEventName).isEqualTo(mockEventNameInRedactedEventsList1)
    }

    @Test
    fun `test fetched redacted events list is empty from the server`() {
        initMockFetchedAppSettings(emptyJSONArray)
        enable()
        val finalEventName = processEventsRedaction(mockEventNameInRedactedEventsList1)
        Assertions.assertThat(finalEventName).isEqualTo(mockEventNameInRedactedEventsList1)
    }

    @Test
    fun `test fetched redacted events list is invalid from the server`() {
        var mockInvalidRedactedEventsFromServer = JSONArray()
        val jsonObject = JSONObject().apply {
            put("not_a_key", mockRedactedString)
            put("not_a_value",mockRedactedEvents)
        }
        mockInvalidRedactedEventsFromServer.put(jsonObject)
        initMockFetchedAppSettings(mockInvalidRedactedEventsFromServer)
        enable()
        val finalEventName = processEventsRedaction(mockEventNameInRedactedEventsList1)
        Assertions.assertThat(finalEventName).isEqualTo(mockEventNameInRedactedEventsList1)
    }

    @Test
    fun `test fetched redacted events list is not null from the server and does not redaction`() {
        initMockFetchedAppSettings(mockRedactedEventsFromServer)
        enable()
        val finalEventName = processEventsRedaction(mockEventNameNotInRedactedEventsList)
        Assertions.assertThat(finalEventName).isEqualTo(mockEventNameNotInRedactedEventsList)
    }
    @Test
    fun `test fetched redacted events list is not null from the server and does redaction`() {
        initMockFetchedAppSettings(mockRedactedEventsFromServer)
        enable()
        val finalEventName = processEventsRedaction(mockEventNameInRedactedEventsList1)
        Assertions.assertThat(finalEventName).isEqualTo(mockRedactedString)
    }
}
