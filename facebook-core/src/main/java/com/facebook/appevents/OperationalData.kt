package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookException
import com.facebook.appevents.AppEvent.Companion.validateIdentifier
import com.facebook.appevents.internal.Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED
import com.facebook.appevents.internal.Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED
import com.facebook.appevents.internal.Constants.IAP_ACTUAL_DEDUP_KEY_USED
import com.facebook.appevents.internal.Constants.IAP_ACTUAL_DEDUP_RESULT
import com.facebook.appevents.internal.Constants.IAP_AUTOLOG_IMPLEMENTATION
import com.facebook.appevents.internal.Constants.IAP_BASE_PLAN
import com.facebook.appevents.internal.Constants.IAP_BILLING_LIBRARY_VERSION
import com.facebook.appevents.internal.Constants.IAP_FREE_TRIAL_PERIOD
import com.facebook.appevents.internal.Constants.IAP_INTRO_PRICE_AMOUNT_MICROS
import com.facebook.appevents.internal.Constants.IAP_INTRO_PRICE_CYCLES
import com.facebook.appevents.internal.Constants.IAP_NON_DEDUPED_EVENT_TIME
import com.facebook.appevents.internal.Constants.IAP_PACKAGE_NAME
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_ID
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_TYPE
import com.facebook.appevents.internal.Constants.IAP_PURCHASE_TIME
import com.facebook.appevents.internal.Constants.IAP_PURCHASE_TOKEN
import com.facebook.appevents.internal.Constants.IAP_SUBSCRIPTION_AUTORENEWING
import com.facebook.appevents.internal.Constants.IAP_SUBSCRIPTION_PERIOD
import com.facebook.appevents.internal.Constants.IAP_TEST_DEDUP_KEY_USED
import com.facebook.appevents.internal.Constants.IAP_TEST_DEDUP_RESULT
import org.json.JSONObject

class OperationalData {
    private val operationalData: MutableMap<OperationalDataEnum, MutableMap<String, Any>> =
        mutableMapOf()

    fun copy(): OperationalData {
        val copy = OperationalData()
        for (parameterType in operationalData.keys) {
            val keyValPairs = operationalData[parameterType] ?: continue
            for (key in keyValPairs.keys) {
                val value = keyValPairs[key] ?: continue
                copy.addParameter(parameterType, key, value)
            }
        }
        return copy
    }

    fun addParameter(type: OperationalDataEnum, key: String, value: Any) {
        try {
            validateIdentifier(key)
            if (value !is String && value !is Number) {
                throw FacebookException(
                    String.format(
                        "Parameter value '%s' for key '%s' should be a string" + " or a numeric type.",
                        value,
                        key
                    )
                )
            }
            if (type !in operationalData) {
                operationalData[type] = mutableMapOf()
            }
            operationalData[type]?.put(key, value)
        } catch (e: Exception) {
            /* Swallow */
        }
    }

    fun getParameter(type: OperationalDataEnum, key: String): Any? {
        if (type !in operationalData) {
            return null
        }
        return operationalData[type]?.get(key)
    }

    fun toJSON(): JSONObject {
        val json = try {
            val transformedMap = operationalData.mapKeys { entry -> entry.key.value }.toMap()
            JSONObject(transformedMap)
        } catch (e: Exception) {
            null
        }
        return json ?: JSONObject()
    }

    companion object {

        // Parameters that should only be added to operational_parameters
        private val iapOperationalParameters = setOf(
            IAP_PACKAGE_NAME,
            IAP_SUBSCRIPTION_AUTORENEWING,
            IAP_FREE_TRIAL_PERIOD,
            IAP_INTRO_PRICE_AMOUNT_MICROS,
            IAP_INTRO_PRICE_CYCLES,
            IAP_BASE_PLAN,
            EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
            IAP_AUTOLOG_IMPLEMENTATION,
            EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
            IAP_BILLING_LIBRARY_VERSION,
            IAP_SUBSCRIPTION_PERIOD,
            IAP_PURCHASE_TOKEN,
            IAP_NON_DEDUPED_EVENT_TIME,
            IAP_ACTUAL_DEDUP_RESULT,
            IAP_ACTUAL_DEDUP_KEY_USED,
            IAP_TEST_DEDUP_RESULT,
            IAP_TEST_DEDUP_KEY_USED,
        )

        // Parameters that should be added to custom_events and operational_parameters
        private val iapOperationalAndCustomParameters = setOf(
            IAP_PRODUCT_ID,
            IAP_PRODUCT_TYPE,
            IAP_PURCHASE_TIME
        )

        private val parameterClassifications: Map<OperationalDataEnum, Pair<Set<String>, Set<String>>> =
            mapOf(
                OperationalDataEnum.IAPParameters to Pair(
                    iapOperationalParameters, iapOperationalAndCustomParameters
                )
            )

        fun getParameterClassification(
            typeOfParameter: OperationalDataEnum,
            parameter: String
        ): ParameterClassification {
            val operationalParametersForType = parameterClassifications[typeOfParameter]?.first
            val operationalAndCustomParametersForType =
                parameterClassifications[typeOfParameter]?.second
            if (operationalParametersForType != null && operationalParametersForType.contains(
                    parameter
                )
            ) {
                return ParameterClassification.OperationalData

            }
            if (operationalAndCustomParametersForType != null && operationalAndCustomParametersForType.contains(
                    parameter
                )
            ) {
                return ParameterClassification.CustomAndOperationalData
            }
            return ParameterClassification.CustomData
        }

        // Adds parameter to custom_events bundle, operational_parameters, or both
        fun addParameter(
            typeOfParameter: OperationalDataEnum,
            key: String,
            value: String,
            customEventsParams: Bundle,
            operationalData: OperationalData
        ) {
            val classification = getParameterClassification(typeOfParameter, key)
            when (classification) {
                ParameterClassification.CustomData -> customEventsParams.putCharSequence(
                    key,
                    value
                )

                ParameterClassification.OperationalData -> operationalData.addParameter(
                    typeOfParameter,
                    key,
                    value
                )

                ParameterClassification.CustomAndOperationalData -> {
                    operationalData.addParameter(
                        typeOfParameter,
                        key,
                        value
                    )
                    customEventsParams.putCharSequence(
                        key,
                        value
                    )
                }
            }
        }

        fun addParameterAndReturn(
            typeOfParameter: OperationalDataEnum,
            key: String,
            value: String,
            customEventsParams: Bundle?,
            operationalData: OperationalData?
        ): Pair<Bundle?, OperationalData?> {
            val classification = getParameterClassification(typeOfParameter, key)
            var modifiedParams: Bundle? = customEventsParams
            var modifiedOperationalData: OperationalData? = operationalData
            when (classification) {
                ParameterClassification.CustomData -> {
                    if (modifiedParams == null) {
                        modifiedParams = Bundle()
                    }
                    modifiedParams.putCharSequence(
                        key,
                        value
                    )
                }

                ParameterClassification.OperationalData -> {
                    if (modifiedOperationalData == null) {
                        modifiedOperationalData = OperationalData()
                    }
                    modifiedOperationalData.addParameter(
                        typeOfParameter,
                        key,
                        value
                    )
                }

                ParameterClassification.CustomAndOperationalData -> {
                    if (modifiedOperationalData == null) {
                        modifiedOperationalData = OperationalData()
                    }
                    if (modifiedParams == null) {
                        modifiedParams = Bundle()
                    }
                    modifiedOperationalData.addParameter(
                        typeOfParameter,
                        key,
                        value
                    )
                    modifiedParams.putCharSequence(
                        key,
                        value
                    )
                }
            }
            return Pair(modifiedParams, modifiedOperationalData)
        }

        fun getParameter(
            typeOfParameter: OperationalDataEnum,
            key: String,
            params: Bundle?,
            operationalData: OperationalData?
        ): Any? {
            val opValue = operationalData?.getParameter(typeOfParameter, key)
            val eventValue = params?.getCharSequence(key)
            return opValue ?: eventValue
        }
    }
}

enum class OperationalDataEnum(val value: String) {
    IAPParameters("iap_parameters")
}

enum class ParameterClassification(val value: String) {
    CustomData("custom_data"),
    OperationalData("operational_data"),
    CustomAndOperationalData("custom_and_operational_data")
}
