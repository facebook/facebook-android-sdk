/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class,
    FetchedAppSettingsManager::class
)
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
            sensitiveParams = mockSensitiveParams,
            schemaRestrictions = emptyJSONArray,
            bannedParams = emptyJSONArray,
            currencyDedupeParameters = emptyList(),
            purchaseValueDedupeParameters = emptyList(),
            prodDedupeParameters = emptyList(),
            testDedupeParameters = emptyList(),
            dedupeWindow = 0L
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
        assertEqual(mockInputParams, expectedFinalParamsWithoutChange)
    }

    @Test
    fun `test fetched sensitive params list is empty from the server`() {
        initMockFetchedAppSettings(emptyJSONArray)

        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isFalse
        assertEqual(mockInputParams, expectedFinalParamsWithoutChange)
    }

    @Test
    fun `test fetched sensitive params list is not null from the server and no params need to be filtered`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServer)

        val mockInputParamsWithoutSensitiveParams = Bundle()
        mockInputParamsWithoutSensitiveParams.putString(
            mockNonSensitiveParam,
            mockNonSensitiveParamValue
        )

        enable()
        processFilterSensitiveParams(
            mockInputParamsWithoutSensitiveParams,
            mockEventNameWithoutSensitiveParams
        )

        Assertions.assertThat(mockInputParamsWithoutSensitiveParams.containsKey(filteredParamsKey)).isFalse
        assertEqual(mockInputParamsWithoutSensitiveParams, mockInputParamsWithoutSensitiveParams)
    }

    @Test
    fun `test fetched sensitive params list has only default sensitive params from the server and need to filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerDefaultOnly)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        val filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3)

        val expectedParams = Bundle()
        expectedParams.putString(filteredParamsKey, filteredParams.toString())
        expectedParams.putString(mockNonSensitiveParam, mockNonSensitiveParamValue)
        expectedParams.putString(mockSensitiveParam1, null)
        expectedParams.putString(mockSensitiveParam2, null)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        assertEqual(mockInputParams, expectedParams)
    }

    @Test
    fun `test fetched sensitive params list is not null from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServer)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        val filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3) /* default sensitive param */
        filteredParams.put(mockSensitiveParam1) /* specific sensitive params */
        filteredParams.put(mockSensitiveParam2) /* specific sensitive params */

        val expectedParams = Bundle()
        expectedParams.putString(filteredParamsKey, filteredParams.toString())
        expectedParams.putString(mockNonSensitiveParam, mockNonSensitiveParamValue)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        assertEqual(mockInputParams, expectedParams)
    }

    @Test
    fun `test fetched sensitive params list default only from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerDefaultOnly)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        val filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam3) /* default sensitive param */

        val expectedParams = Bundle()
        expectedParams.putString(filteredParamsKey, filteredParams.toString())
        expectedParams.putString(mockNonSensitiveParam, mockNonSensitiveParamValue)
        expectedParams.putString(mockSensitiveParam1, null)
        expectedParams.putString(mockSensitiveParam2, null)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        assertEqual(mockInputParams, expectedParams)
    }

    @Test
    fun `test fetched sensitive params list specific only from the server and filter the params`() {
        initMockFetchedAppSettings(mockSensitiveParamsFromServerWithoutDefault)
        enable()
        processFilterSensitiveParams(mockInputParams, mockEventWithSensitiveParam)

        val filteredParams = JSONArray()
        filteredParams.put(mockSensitiveParam1) /* specific sensitive params */
        filteredParams.put(mockSensitiveParam2) /* specific sensitive params */

        val expectedParams = Bundle()
        expectedParams.putString(filteredParamsKey, filteredParams.toString())
        expectedParams.putString(mockNonSensitiveParam, mockNonSensitiveParamValue)
        expectedParams.putString(mockSensitiveParam3, null)

        Assertions.assertThat(mockInputParams.containsKey(filteredParamsKey)).isTrue
        assertEqual(mockInputParams, expectedParams)
    }

    private fun isEqual(mockBundle: Bundle?, expectedBundle: Bundle?): Boolean {
        if (mockBundle == null && expectedBundle == null) {
            return true
        }
        val s1 = mockBundle?.keySet() ?: return false
        val s2 = expectedBundle?.keySet() ?: return false

        if (!s1.equals(s2)) {
            return false
        }

        for (s in s1) {
            val v1 = mockBundle.get(s)
            val v2 = expectedBundle.get(s)

            // cant compare serialized lists directly present in _filteredKey
            if (s.equals(filteredParamsKey) && v1.toString().length == v2.toString().length) {
                continue
            }

            if (v1 != v2) {
                return false
            }
        }
        return true
    }

    private fun assertEqual(mockBundle: Bundle?, expectedBundle: Bundle?) {
        Assert.assertTrue(isEqual(mockBundle, expectedBundle))
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

        private lateinit var mockInputParams: Bundle
        private lateinit var expectedFinalParamsWithoutChange: Bundle

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

            mockInputParams = Bundle()
            mockInputParams.putString(mockSensitiveParam1, null)
            mockInputParams.putString(mockSensitiveParam2, null)
            mockInputParams.putString(mockSensitiveParam3, null)
            mockInputParams.putString(mockNonSensitiveParam, mockNonSensitiveParamValue)

            expectedFinalParamsWithoutChange = Bundle()
            expectedFinalParamsWithoutChange.putAll(mockInputParams)

        }
    }
}
