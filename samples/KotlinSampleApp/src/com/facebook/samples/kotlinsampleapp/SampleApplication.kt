package com.facebook.samples.kotlinsampleapp

import android.app.Application
import com.facebook.appevents.AppEventsLogger

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    AppEventsLogger.activateApp(this)
  }
}
