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

@PrepareForTest(ProtectedModeManager::class, FacebookSdk::class, FetchedAppSettingsManager::class)
class ProtectedModeManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockStandardParamsFromServer: JSONArray
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()


    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        mockStandardParamsFromServer = JSONArray()
        mockStandardParamsFromServer.put("standard_param_from_server_1")
        mockStandardParamsFromServer.put("standard_param_from_server_2")
    }

    @After
    fun tearDown() {
        ProtectedModeManager.disable()
    }

    fun initMockFetchedAppSettings(mockStandardParams:JSONArray?) {
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
            protectedModeStandardParamsSetting = mockStandardParams,
            MACARuleMatchingSetting = emptyJSONArray,
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
            .thenReturn(mockFetchedAppSettings)
    }

    @Test
    fun `test null as parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = null
        val expectedParameters = null

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test empty parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle()
        val expectedParameters = Bundle()

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test all standard parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("pm", "1")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test filter out non-standard parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("non_standard_param_1", "value_1")
            putString("non_standard_param_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("pm", "1")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    // This should not happen in the current design, the server will drop empty standard params.
    // Adding this test case to ensure empty standard params from server will not crash the App in
    // case.
    fun `test filter out non-standard parameters when enable and server return empty standard params list`() {
        initMockFetchedAppSettings(emptyJSONArray)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("fb_product_price_amount", "0.990")
            putString("quantity", "1")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("pm", "1")
        }

        // We use static standard params list defined in FB SDK.
        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test filter out non-standard parameters when enable and server do not return standard params`() {
        initMockFetchedAppSettings(null)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("pm", "1")
        }

        // We use static standard params list defined in FB SDK.
        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test not filter out non-standard parameters when disable`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
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
            val v1 = mockBundle.get(s) ?: return false
            val v2 = expectedBundle.get(s) ?: return false
            if (v1 != v2) {
                return false
            }
        }
        return true
    }

    private fun assertEqual(mockBundle: Bundle?, expectedBundle: Bundle?) {
        assertTrue(isEqual(mockBundle,expectedBundle ))
    }
}
