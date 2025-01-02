package com.facebook.appevents

import com.facebook.FacebookException
import com.facebook.appevents.AppEvent.Companion.validateIdentifier
import org.json.JSONObject

class OperationalData {
    private val operationalData: MutableMap<String, MutableMap<String, Any>> =
        mutableMapOf()

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
            if (type.value !in operationalData) {
                operationalData[type.value] = mutableMapOf()
            }
            operationalData[type.value]?.put(key, value)
        } catch (e: Exception) {
            /* Swallow */
        }
    }

    fun getParameter(type: OperationalDataEnum, key: String): Any? {
        if (type.value !in operationalData) {
            return null
        }
        return operationalData[type.value]?.get(key)
    }

    fun toJSON(): JSONObject {
        val json = try {
            (operationalData as Map<*, *>?)?.let { JSONObject(it) }
        } catch (e: Exception) {
            null
        }
        return json ?: JSONObject()
    }
}

enum class OperationalDataEnum(val value: String) {
    IAPParameters("iap_parameters")
}
