package com.facebook.appevents.iap

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.internal.Constants
import java.util.Currency
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseDedupeConfig {
    val dedupeWindow = TimeUnit.MINUTES.toMillis(1)

    /**
     * Default values when we fail to fetch from the server
     */

    private val defaultCurrencyParameterEquivalents =
        listOf(AppEventsConstants.EVENT_PARAM_CURRENCY)
    private val defaultValueParameterEquivalents =
        listOf(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)

    /**
     * Map of parameters we consider in deduplication to their equivalents.
     * i.e. We should consider the case where content_id = product_id when deduplicating.
     */
    private val defaultDedupeParameters =
        listOf(
            Constants.IAP_PRODUCT_ID to listOf(Constants.IAP_PRODUCT_ID),
            Constants.IAP_PRODUCT_DESCRIPTION to listOf(Constants.IAP_PRODUCT_DESCRIPTION),
            Constants.IAP_PRODUCT_TITLE to listOf(Constants.IAP_PRODUCT_TITLE),
            Constants.IAP_PURCHASE_TOKEN to listOf(Constants.IAP_PURCHASE_TOKEN)
        )

    fun getDedupeParameters(dedupingWithImplicitlyLoggedHistory: Boolean): List<Pair<String, List<String>>> {
        // TODO: Fetch non-default parameters from server
        return defaultDedupeParameters
    }

    private fun getCurrencyParameterEquivalents(): List<String> {
        // TODO: Fetch non-default parameters from server
        return defaultCurrencyParameterEquivalents
    }

    private fun getValueParameterEquivalents(): List<String> {
        // TODO: Fetch non-default parameters from server
        return defaultValueParameterEquivalents
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
}
