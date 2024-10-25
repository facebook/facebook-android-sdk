package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.integrity.BannedParamManager.processFilterBannedParams
import com.facebook.appevents.integrity.BannedParamManager.enable
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import org.json.JSONArray
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class BannedParamsManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockBlockedParamsConfigFromServer: JSONArray
    private val mockBlockedParam1 = "blocked_param_1"
    private val mockBlockedParam2 = "blocked_param_2"
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()

    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
        mockBlockedParamsConfigFromServer = JSONArray()
        mockBlockedParamsConfigFromServer.put(mockBlockedParam1)
        mockBlockedParamsConfigFromServer.put(mockBlockedParam2)
    }

    @After
    fun tearDown() {
        ProtectedModeManager.disable()
    }

    private fun initMockFetchedAppSettings(mockBlockedParams: JSONArray?) {
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
            sensitiveParams = emptyJSONArray,
            schemaRestrictions = emptyJSONArray,
            bannedParams = mockBlockedParams,
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
    fun `test banned param blocked`() {
        initMockFetchedAppSettings(mockBlockedParamsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("blocked_param_1", "block_me")
        }
        val expectedParameters = Bundle().apply {}

        processFilterBannedParams(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test unbanned param passes`() {
        initMockFetchedAppSettings(mockBlockedParamsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("not_blocked", "dont_block_me")
        }
        val expectedParameters = Bundle().apply {
            putString("not_blocked", "dont_block_me")
        }

        processFilterBannedParams(mockParameters)
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
        Assert.assertTrue(isEqual(mockBundle, expectedBundle))
    }
}
