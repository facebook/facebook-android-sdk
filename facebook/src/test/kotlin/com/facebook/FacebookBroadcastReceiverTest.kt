package com.facebook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.NativeProtocol
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(NativeProtocol::class, FacebookBroadcastReceiver::class)
class FacebookBroadcastReceiverTest : FacebookPowerMockTestCase() {

  private lateinit var receiver: FacebookBroadcastReceiver
  private lateinit var ctx: Context

  @Before
  fun init() {
    receiver = spy(FacebookBroadcastReceiver())
    ctx = ApplicationProvider.getApplicationContext() as Context
  }

  @Test
  fun `test on receive successful`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, "action")
    mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(isA(Intent::class.java))).thenReturn(false)
    receiver.onReceive(ctx, intent)
    verify(receiver, times(1))
        .onSuccessfulAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
    verify(receiver, never()).onFailedAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
  }

  @Test
  fun `test on receive failedappcall`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, "action")
    mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(isA(Intent::class.java))).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, times(1)).onFailedAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
    verify(receiver, never()).onSuccessfulAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
  }
  @Test
  fun `test on receive never called`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(isA(Intent::class.java))).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, never()).onFailedAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
  }
}
