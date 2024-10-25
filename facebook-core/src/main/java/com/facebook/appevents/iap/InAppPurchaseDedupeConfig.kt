package com.facebook.appevents.iap

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.internal.Constants
import com.facebook.internal.FetchedAppSettingsManager
import java.util.Currency
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseDedupeConfig {

    /**
     * Default values when we fail to fetch from the server
     */
    private val defaultCurrencyParameterEquivalents =
        listOf(AppEventsConstants.EVENT_PARAM_CURRENCY)
    private val defaultValueParameterEquivalents =
        listOf(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)
    private val defaultDedupeWindow = TimeUnit.MINUTES.toMillis(1)

    /**
     * Map of parameters we consider in deduplication to their equivalents.
     * i.e. We should consider the case where content_id = product_id when deduplicating.
     * The LHS will be the key that is implicitly logged, and the RHS will be the keys we consider
     * equivalent to the LHS that may be found in the manually logged event.
     */
    private val defaultDedupeParameters =
        listOf(
            Constants.IAP_PRODUCT_ID to listOf(Constants.IAP_PRODUCT_ID),
            Constants.IAP_PRODUCT_DESCRIPTION to listOf(Constants.IAP_PRODUCT_DESCRIPTION),
            Constants.IAP_PRODUCT_TITLE to listOf(Constants.IAP_PRODUCT_TITLE),
            Constants.IAP_PURCHASE_TOKEN to listOf(Constants.IAP_PURCHASE_TOKEN)
        )

    fun getDedupeParameters(dedupingWithImplicitlyLoggedHistory: Boolean): List<Pair<String, List<String>>> {
        val settings =
            FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
        if (settings?.prodDedupeParameters == null || settings.prodDedupeParameters.isEmpty()) {
            return defaultDedupeParameters
        }
        if (!dedupingWithImplicitlyLoggedHistory) {
            return settings.prodDedupeParameters
        }
        // If we are deduping with the implicitly logged purchases, we should let the values be the keys
        val swappedParameters = ArrayList<Pair<String, List<String>>>()
        for (item in settings.prodDedupeParameters) {
            val values = item.second
            for (value in values) {
                swappedParameters.add(Pair(value, listOf(item.first)))
            }
        }
        return swappedParameters
    }

    fun getCurrencyParameterEquivalents(): List<String> {
        val settings =
            FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
        if (settings?.currencyDedupeParameters == null || settings.currencyDedupeParameters.isEmpty()) {
            return defaultCurrencyParameterEquivalents
        }
        return settings.currencyDedupeParameters
    }

    fun getValueParameterEquivalents(): List<String> {
        val settings =
            FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
        if (settings?.purchaseValueDedupeParameters == null || settings.purchaseValueDedupeParameters.isEmpty()) {
            return defaultValueParameterEquivalents
        }
        return settings.purchaseValueDedupeParameters
    }

    fun getCurrencyOfManualEvent(parameters: Bundle?): Currency? {
        val currencyParameters = getCurrencyParameterEquivalents()
        for (equivalent in currencyParameters) {
            try {
                val currencyCode = parameters?.getString(equivalent)
                if (currencyCode.isNullOrEmpty()) {
                    continue
                }
                return Currency.getInstance(currencyCode)
            } catch (e: Exception) {
                /** Swallow invalid currency code */
            }
        }
        return null
    }

    fun getValueOfManualEvent(valueToSum: Double?, parameters: Bundle?): Double? {
        if (valueToSum != null) {
            return valueToSum
        }
        val valueParameters = getValueParameterEquivalents()
        for (equivalent in valueParameters) {
            try {
                val value = parameters?.getDouble(equivalent) ?: continue
                return value
            } catch (e: Exception) {
                /** Swallow failure to parse */
            }
        }
        return null
    }

    fun getDedupeWindow(): Long {
        val settings =
            FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
        if (settings?.dedupeWindow == null || settings.dedupeWindow == 0L) {
            return defaultDedupeWindow
        }
        return settings.dedupeWindow
    }
}
