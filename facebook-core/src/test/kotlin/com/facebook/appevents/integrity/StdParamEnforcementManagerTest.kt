package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.integrity.StdParamsEnforcementManager.enable
import com.facebook.appevents.integrity.StdParamsEnforcementManager.processFilterParamSchemaBlocking
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
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
    StdParamsEnforcementManager::class,
    FacebookSdk::class,
    FetchedAppSettingsManager::class
)
class StdParamEnforcementManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private val requireExactMatchKey = "require_exact_match"
    private val potentialMatchesKey = "potential_matches"
    private val currencyKey = "fb_currency"
    private val currencyRegexRestriction = "^[a-zA-Z]{3}$"
    private val valueKey = "fb_value"
    private val valueRegexRestriction = "^-?\\d+(?:\\.\\d+)?$"
    private val orderIdKey = "fb_order_id"
    private val orderIdRegexRestriction = "^.{0,1000}$"
    private val contentIdsKey = "fb_content_ids"
    private val contentIdsRegexRestriction = "^.{0,1000}$"
    private lateinit var mockSchemaRestrictionsConfigFromServer: JSONArray
    private lateinit var currencyRestrictions: JSONArray
    private lateinit var valueRestrictions: JSONArray
    private lateinit var orderIdRestrictions: JSONArray
    private lateinit var currencyRegexRestrictions: JSONArray
    private lateinit var valueRegexRestrictions: JSONArray
    private lateinit var orderIdRegexRestrictions: JSONArray
    private lateinit var contentIdsRegexRestrictions: JSONArray
    private lateinit var currencyEnumRestrictions: JSONArray
    private lateinit var contentIdsRestrictions: JSONArray
    private lateinit var blockedParamRestrictions: JSONArray
    private val currencyEnumRestriction = "USDP"
    private val blockedParamKey = "blocked_param"
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()

    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        mockSchemaRestrictionsConfigFromServer = JSONArray()

        currencyRestrictions = JSONArray()

        currencyRegexRestrictions = JSONArray()
        currencyRegexRestrictions.put(currencyRegexRestriction)

        currencyEnumRestrictions = JSONArray()
        currencyEnumRestrictions.put(currencyEnumRestriction)

        currencyRestrictions.put(
            JSONObject().apply {
                put(requireExactMatchKey, "false")
                put(potentialMatchesKey, currencyRegexRestrictions)
            },
        )

        currencyRestrictions.put(
            JSONObject().apply {
                put(requireExactMatchKey, "true")
                put(potentialMatchesKey, currencyEnumRestrictions)
            },
        )

        mockSchemaRestrictionsConfigFromServer.put(
            JSONObject().apply {
                put("key", currencyKey)
                put("value", currencyRestrictions)
            }
        )

        valueRegexRestrictions = JSONArray()
        valueRegexRestrictions.put(valueRegexRestriction)

        valueRestrictions = JSONArray()
        valueRestrictions.put(JSONObject().apply {
            put(requireExactMatchKey, "false")
            put(potentialMatchesKey, valueRegexRestrictions)
        })

        mockSchemaRestrictionsConfigFromServer.put(
            JSONObject().apply {
                put("key", valueKey)
                put("value", valueRestrictions)
            }
        )


        orderIdRegexRestrictions = JSONArray()
        orderIdRegexRestrictions.put(orderIdRegexRestriction)
        orderIdRestrictions = JSONArray()
        orderIdRestrictions.put(JSONObject().apply {
            put(requireExactMatchKey, "false")
            put(potentialMatchesKey, orderIdRegexRestrictions)
        })

        mockSchemaRestrictionsConfigFromServer.put(
            JSONObject().apply {
                put("key", orderIdKey)
                put("value", orderIdRestrictions)
            }
        )

        contentIdsRegexRestrictions = JSONArray()
        contentIdsRegexRestrictions.put(contentIdsRegexRestriction)
        contentIdsRestrictions = JSONArray()
        contentIdsRestrictions.put(JSONObject().apply {
            put(requireExactMatchKey, "false")
            put(potentialMatchesKey, contentIdsRegexRestrictions)
        })

        mockSchemaRestrictionsConfigFromServer.put(
            JSONObject().apply {
                put("key", contentIdsKey)
                put("value", contentIdsRestrictions)
            }
        )

        blockedParamRestrictions = JSONArray()
        blockedParamRestrictions.put(JSONObject().apply {
            put(requireExactMatchKey, "true")
            put(potentialMatchesKey, JSONArray())
        })

        mockSchemaRestrictionsConfigFromServer.put(JSONObject().apply {
            put("key", blockedParamKey)
            put("value", blockedParamRestrictions)
        })

    }

    @After
    fun tearDown() {
        ProtectedModeManager.disable()
    }

    private fun initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer: JSONArray?) {
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
            schemaRestrictions = mockSchemaRestrictionsConfigFromServer,
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
    fun `test enforce schematize enum rule passes`() {
        initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("fb_currency", "USDP")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USDP")
        }

        processFilterParamSchemaBlocking(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test enforce schematize enum rule fails`() {
        initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("fb_currency", "ABCD")
        }
        val expectedParameters = Bundle()

        processFilterParamSchemaBlocking(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test enforce schematize regex rule passes, enum rule fails, param gets through`() {
        initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("fb_order_id", "random_test_string")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_order_id", "random_test_string")
        }

        processFilterParamSchemaBlocking(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test enforce schematize regex rules`() {
        initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("fb_currency", "ABC")
            putString("fb_value", "23a")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "ABC")
        }

        processFilterParamSchemaBlocking(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `mock blocking`() {
        // blocking happens if possible value set is empty
        initMockFetchedAppSettings(mockSchemaRestrictionsConfigFromServer)

        enable()

        val mockParameters = Bundle().apply {
            putString("blocked_param", "random_test_string")
        }
        val expectedParameters = Bundle().apply {}

        processFilterParamSchemaBlocking(mockParameters)
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
