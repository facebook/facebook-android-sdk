package com.facebook.appevents.gps

import android.os.Build

object GpsCapabilityChecker {
    @JvmStatic
    fun useOutcomeReceiver(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
