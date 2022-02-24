package com.facebook.samples.kotlinsampleapp.appevents

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.facebook.appevents.AppEventsLogger
import com.facebook.samples.kotlinsampleapp.common.MenuItem

@Composable
fun AppEventScreen() {
  val context = LocalContext.current
  val logEvent = { ctx: Context, eventName: String ->
    AppEventsLogger.newLogger(ctx).logEvent(eventName)
  }

  Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
    Text("This screen shows how to emit custom events using FB SDK")
    MenuItem("Log event", onClick = { logEvent(context, "EMIT_EVENT_CLICK") })
  }
}
