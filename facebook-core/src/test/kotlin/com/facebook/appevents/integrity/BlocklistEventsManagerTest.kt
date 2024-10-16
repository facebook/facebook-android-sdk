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
import com.facebook.internal.*
import org.junit.Assert.assertTrue
import org.json.JSONArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import kotlin.test.assertFalse

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class BlocklistEventsManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockBlocklistEventsFromServer: JSONArray
    private val mockEventNameNotInBlocklist = "install_app"
    private val mockEventNameInBlocklist1 = "blocklist_events_1"
    private val mockEventNameInBlocklist2 = "blocklist_events_2"
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()


    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        mockBlocklistEventsFromServer = JSONArray()
        mockBlocklistEventsFromServer.put(mockEventNameInBlocklist1)
        mockBlocklistEventsFromServer.put(mockEventNameInBlocklist2)
    }

    @After
    fun tearDown() {
        BlocklistEventsManager.disable()
    }

    fun initMockFetchedAppSettings(mockBlocklistEvents:JSONArray?) {
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
            blocklistEvents = mockBlocklistEvents,
            redactedEvents = emptyJSONArray,
            sensitiveParams = emptyJSONArray,
            schemaRestrictions = emptyJSONArray,
            bannedParams = emptyJSONArray
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
            .thenReturn(mockFetchedAppSettings)
    }

    @Test
    fun `test fetched blocklist is null from the server`() {
        initMockFetchedAppSettings(emptyJSONArray)
        BlocklistEventsManager.enable()
        assertFalse(BlocklistEventsManager.isInBlocklist(mockEventNameInBlocklist1))
    }

    @Test
    fun `test fetched blocklist is not null from the server and return true from isInBlocklist()`() {
        initMockFetchedAppSettings(mockBlocklistEventsFromServer)
        BlocklistEventsManager.enable()
        assertTrue(BlocklistEventsManager.isInBlocklist(mockEventNameInBlocklist1))
    }
    @Test
    fun `test fetched blocklist is not null from the server and return false from isInBlocklist()`() {
        initMockFetchedAppSettings(mockBlocklistEventsFromServer)
        BlocklistEventsManager.enable()
        assertFalse(BlocklistEventsManager.isInBlocklist(mockEventNameNotInBlocklist))
    }
}
