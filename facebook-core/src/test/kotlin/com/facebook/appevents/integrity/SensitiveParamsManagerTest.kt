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
import com.facebook.appevents.integrity.SensitiveParamsManager.disable
import com.facebook.appevents.integrity.SensitiveParamsManager.enable
import com.facebook.appevents.integrity.SensitiveParamsManager.processFilterSensitiveParams
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
class SensitiveParamsManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification

    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        setupTestConfigs()
    }

    @After
    fun tearDown() {
        disable()
    }

    private fun initMockFetchedAppSettings(mockSensitiveParams: JSONArray?) {
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
                redactedEvents = emptyJSONArray,
                sensitiveParams = mockSensitiveParams
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
                .thenReturn(mockFetchedAppSettings)
    }

    @Test
    fun `test fetched sensitive params list is null from the server`() {
        initMockFetchedAppSettings(null)

        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isFalse
        Assertions.assertThat(mockInputParams).isEqualTo(expectedFinalParamsWithoutChange)
    }

    @Test
    fun `test fetched sensitive params list is empty from the server`() {
        initMockFetchedAppSettings(emptyJSONArray)

        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isFalse
        Assertions.assertThat(mockInputParams).isEqualTo(expectedFinalParamsWithoutChange)
    }

    @Test
    fun `test fetched sensitive params list is not null from the server and no params need to be filtered`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServer)

        var mockInputParamsWithoutSensitiveParams = HashMap<String, String?>()
        mockInputParamsWithoutSensitiveParams[mockNonSensitiveParam] = mockNonSensitiveParamValue

        enable()
        processFilterSensitiveParams(mockInputParamsWithoutSensitiveParams, mockEventNameWithoutSensitiveParams)

        var expectedFinalParamsWithoutChange = mockInputParamsWithoutSensitiveParams.toMap()

        Assertions.assertThat(mockInputParamsWithoutSensitiveParams.containsKey(filteredParamsKey)).isFalse
        Assertions.assertThat(mockInputParamsWithoutSensitiveParams).isEqualTo(expectedFinalParamsWithoutChange)
    }

    @Test
    fun `test fetched sensitive params list has only default sensitive params from the server and need to filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerDefaultOnly)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        var expectedParams = HashMap<String, String?>()
        var filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3)
        expectedParams[filteredParamsKey] = filteredParams.toString()
        expectedParams[mockNonSensitiveParam] = mockNonSensitiveParamValue
        expectedParams[mockSensitiveParam1] = null
        expectedParams[mockSensitiveParam2] = null

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        Assertions.assertThat(mockInputParams).isEqualTo(expectedParams)
    }

    @Test
    fun `test fetched sensitive params list is not null from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServer)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        var expectedParams = HashMap<String, String?>()
        var filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3) /* default sensitive param */
        filteredParams.put(mockSensitiveParam1) /* specific sensitive params */
        filteredParams.put(mockSensitiveParam2) /* specific sensitive params */
        expectedParams[filteredParamsKey] = filteredParams.toString()
        expectedParams[mockNonSensitiveParam] = mockNonSensitiveParamValue

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        Assertions.assertThat(mockInputParams).isEqualTo(expectedParams)
    }

    @Test
    fun `test fetched sensitive params list default only from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerDefaultOnly)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        var expectedParams = HashMap<String, String?>()
        var filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3) /* default sensitive param */
        expectedParams[filteredParamsKey] = filteredParams.toString()
        expectedParams[mockNonSensitiveParam] = mockNonSensitiveParamValue
        expectedParams[mockSensitiveParam1] = null
        expectedParams[mockSensitiveParam2] = null

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        Assertions.assertThat(mockInputParams).isEqualTo(expectedParams)
    }

    @Test
    fun `test fetched sensitive params list specific only from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerWithoutDefault)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        var expectedParams = HashMap<String, String?>()
        var filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam1) /* specific sensitive params */
        filteredParams.put(mockSensitiveParam2) /* specific sensitive params */

        expectedParams[filteredParamsKey] = filteredParams.toString()
        expectedParams[mockNonSensitiveParam] = mockNonSensitiveParamValue
        expectedParams[mockSensitiveParam3] = null

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        Assertions.assertThat(mockInputParams).isEqualTo(expectedParams)
    }

    companion object {
        private const val mockAppID = "123"

        private const val configKey = "key"
        private const val configValue = "value"

        private const val filteredParamsKey = "_filteredKey"
        private const val defaultSensitiveParametersKey = "_MTSDK_Default_"

        private const val mockEventNameWithoutSensitiveParams = "install_app"
        private const val mockEventWithSensitiveParam = "sensitive_event_1"
        private const val mockSensitiveParam1 = "sensitive_param_1"
        private const val mockSensitiveParam2 = "sensitive_param_2"
        private const val mockSensitiveParam3 = "sensitive_param_3"
        private const val mockNonSensitiveParam = "non_sensitive_param"
        private const val mockNonSensitiveParamValue = "param_value"

        private lateinit var emptyJSONArray: JSONArray

        /* mock sensitive params with event name */
        private lateinit var mockDefaultSensitiveParams: JSONObject /* default sensitive params */
        private lateinit var mockSpecificSensitiveParams: JSONObject /* non default sensitive params */

        /* mock config fetched from server */
        private lateinit var mockSensitiveParamsFromServerDefaultOnly: JSONArray /* default only */
        private lateinit var mockSensitiveParamsFromServerWithoutDefault: JSONArray /* specific sensitive params only */
        private lateinit var mockSensitiveParamsFromServer: JSONArray /* specific sensitive params and default */

        private lateinit var mockInputParams: HashMap<String, String?>
        private lateinit var expectedFinalParamsWithoutChange: Map<String, String?>

        fun setupTestConfigs() {
            emptyJSONArray = JSONArray()

            var sensitiveParams = JSONArray()
            sensitiveParams.put(mockSensitiveParam1)
            sensitiveParams.put(mockSensitiveParam2)
            mockSpecificSensitiveParams = JSONObject().apply {
                put(configKey, mockEventWithSensitiveParam)
                put(configValue, sensitiveParams)
            }
            mockSensitiveParamsFromServerWithoutDefault = JSONArray()
            mockSensitiveParamsFromServerWithoutDefault.put(mockSpecificSensitiveParams)

            sensitiveParams = JSONArray()
            sensitiveParams.put(mockSensitiveParam3)
            mockDefaultSensitiveParams = JSONObject().apply {
                put("key", defaultSensitiveParametersKey)
                put("value", sensitiveParams)
            }
            mockSensitiveParamsFromServerDefaultOnly = JSONArray()
            mockSensitiveParamsFromServerDefaultOnly.put(mockDefaultSensitiveParams)

            mockSensitiveParamsFromServer = JSONArray()
            mockSensitiveParamsFromServer.put(mockSpecificSensitiveParams)
            mockSensitiveParamsFromServer.put(mockDefaultSensitiveParams)

            mockInputParams = HashMap()
            mockInputParams[mockSensitiveParam1] = null
            mockInputParams[mockSensitiveParam2] = null
            mockInputParams[mockSensitiveParam3] = null
            mockInputParams[mockNonSensitiveParam] = mockNonSensitiveParamValue

            expectedFinalParamsWithoutChange = mockInputParams.toMap()
        }
    }
}
