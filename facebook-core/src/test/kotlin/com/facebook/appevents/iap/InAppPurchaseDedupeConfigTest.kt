package com.facebook.appevents.iap

import androidx.core.os.bundleOf
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.OperationalDataEnum
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_DESCRIPTION
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_ID
import com.facebook.appevents.internal.Constants.IAP_PURCHASE_TOKEN
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
import kotlin.test.assertNull

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
    private lateinit var testDedupeParameters: List<Pair<String, List<String>>>
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
        testDedupeParameters = listOf(
            Pair(
                "fb_iap_product_description",
                listOf("fb_description", "fb_iap_product_description")
            ),
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
            testDedupeParameters = testDedupeParameters,
            dedupeWindow = 100L,
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
    fun `get test dedupe parameters`() {
        val dedupParamsWhenNewImplicitPurchase =
            InAppPurchaseDedupeConfig.getTestDedupeParameters(false)
        assertEquals(dedupParamsWhenNewImplicitPurchase, testDedupeParameters)
        val dedupeParamsWhenNewManualPurchase =
            InAppPurchaseDedupeConfig.getTestDedupeParameters(true)
        assertEquals(dedupeParamsWhenNewManualPurchase?.size, 2)

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

    @Test
    fun `add dedupe parameters`() {
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(null, null, null),
            Pair(null, null)
        )
        val originalParams = bundleOf(Pair("key", "value"))
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(null, originalParams, null)
                .first?.getString("key"),
            "value"
        )
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(
                originalParams,
                null,
                null
            ).first?.getString("key"),
            "value"
        )
        var dedupeParams = bundleOf(Pair("key1", "value1"))
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(originalParams, dedupeParams, null)
                .first?.getString("key"),
            "value"
        )
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(originalParams, dedupeParams, null)
                .first?.getString("key1"),
            "value1"
        )
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(dedupeParams, originalParams, null)
                .first?.getString("key"),
            "value"
        )
        assertEquals(
            InAppPurchaseDedupeConfig.addDedupeParameters(dedupeParams, originalParams, null).first
                ?.getString("key1"),
            "value1"
        )
        dedupeParams = bundleOf(
            Pair(IAP_PRODUCT_ID, "product_id"),
            Pair(
                IAP_PURCHASE_TOKEN, "purchase_token"
            ),
            Pair(
                IAP_PRODUCT_DESCRIPTION, "product_description"
            )
        )
        val (newParams, newOperationalData) = InAppPurchaseDedupeConfig.addDedupeParameters(
            dedupeParams,
            null,
            null
        )
        assertEquals(
            newParams
                ?.getString(IAP_PRODUCT_ID),
            "product_id"
        )
        assertEquals(
            newOperationalData
                ?.getParameter(OperationalDataEnum.IAPParameters, IAP_PRODUCT_ID),
            "product_id"
        )
        assertNull(
            newParams
                ?.getString(IAP_PURCHASE_TOKEN)
        )
        assertEquals(
            newOperationalData
                ?.getParameter(OperationalDataEnum.IAPParameters, IAP_PURCHASE_TOKEN),
            "purchase_token"
        )
        assertEquals(
            newParams
                ?.getString(IAP_PRODUCT_DESCRIPTION),
            "product_description"
        )
        assertNull(
            newOperationalData
                ?.getParameter(OperationalDataEnum.IAPParameters, IAP_PRODUCT_DESCRIPTION)
        )
    }
}
