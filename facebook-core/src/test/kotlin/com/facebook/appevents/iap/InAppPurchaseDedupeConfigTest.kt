package com.facebook.appevents.iap

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FetchedAppSettingsManager::class,
    FacebookSdk::class
)
class InAppPurchaseDedupeConfigTest : FacebookPowerMockTestCase() {
    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private val emptyJSONArray = JSONArray()
    private lateinit var purchaseAmountParameters: List<String>
    private lateinit var currencyParameters: List<String>
    private lateinit var dedupeParameters: List<Pair<String, List<String>>>

    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn("123")

        dedupeParameters = listOf(
            Pair(
                "fb_iap_product_id",
                listOf("fb_content_id", "fb_product_item_id", "fb_iap_product_id")
            ),
            Pair(
                "fb_iap_product_title",
                listOf("fb_content_title", "fb_product_title", "fb_iap_product_title")
            ),
            Pair(
                "fb_iap_product_description",
                listOf("fb_description", "fb_iap_product_description")
            ),
            Pair(
                "fb_iap_purchase_token",
                listOf("fb_iap_purchase_token", "fb_transaction_id", "fb_order_id")
            )
        )
        currencyParameters = listOf("fb_currency", "fb_product_price_currency")
        purchaseAmountParameters = listOf("_valueToSum", "fb_product_price_amount")
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
            bannedParams = emptyJSONArray,
            currencyDedupeParameters = currencyParameters,
            purchaseValueDedupeParameters = purchaseAmountParameters,
            prodDedupeParameters = dedupeParameters,
            testDedupeParameters = emptyList(),
            dedupeWindow = 100L
        )
        whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(any())).thenReturn(
            mockFetchedAppSettings
        )
    }

    @Test
    fun `get dedupe parameters`() {
        val dedupeParamsWhenNewImplicitPurchase =
            InAppPurchaseDedupeConfig.getDedupeParameters(false)
        assertEquals(dedupeParamsWhenNewImplicitPurchase, dedupeParameters)
        val dedupeParamsWhenNewManualPurchase = InAppPurchaseDedupeConfig.getDedupeParameters(true)
        assertEquals(dedupeParamsWhenNewManualPurchase.size, 11)
    }

    @Test
    fun `get currency parameters`() {
        val newCurrencyParams = InAppPurchaseDedupeConfig.getCurrencyParameterEquivalents()
        assertEquals(newCurrencyParams, currencyParameters)
    }

    @Test
    fun `get purchase amount parameters`() {
        val newPurchaseAmountParams = InAppPurchaseDedupeConfig.getValueParameterEquivalents()
        assertEquals(newPurchaseAmountParams, purchaseAmountParameters)
    }

    @Test
    fun `get dedupe window`() {
        val dedupeWindow = InAppPurchaseDedupeConfig.getDedupeWindow()
        assertEquals(dedupeWindow, 100L)
    }
}
