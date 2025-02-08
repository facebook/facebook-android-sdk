package com.facebook.appevents.gps

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.InternalAppEventsLogger
import kotlin.random.Random

class GpsDebugLogger(context: Context) {
    private val internalAppEventsLogger: InternalAppEventsLogger = InternalAppEventsLogger(context)

    fun log(eventName: String?, parameters: Bundle?) {
        if (shouldLog) internalAppEventsLogger.logEventImplicitly(eventName, parameters)
    }

    companion object {
        private const val LOGGING_SAMPLING_RATE = 0.0001
        private val shouldLog = Random.nextDouble() <= LOGGING_SAMPLING_RATE
    }
}
