package com.facebook.appevents.gps

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.InternalAppEventsLogger
import kotlin.random.Random

class GpsDebugLogger(context: Context) {
    private val internalAppEventsLogger: InternalAppEventsLogger = InternalAppEventsLogger(context)

    fun log(eventName: String?, parameters: Bundle?) {
        if (shouldLog && isGPSDebugEvent(eventName)) internalAppEventsLogger.logEventImplicitly(eventName, parameters)
    }

    private fun isGPSDebugEvent(eventName: String?): Boolean {
        return eventName?.contains(GPS_PREFIX) ?: false
    }

    companion object {
        private const val LOGGING_SAMPLING_RATE = 0.0001
        private const val GPS_PREFIX = "gps"
        private val shouldLog = Random.nextDouble() <= LOGGING_SAMPLING_RATE
    }
}
