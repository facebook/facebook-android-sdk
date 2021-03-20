package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.NativeProtocol
import com.facebook.util.common.anyObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.isA
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(NativeProtocol::class, FacebookBroadcastReceiver::class, BroadcastReceiver::class)
class FacebookBroadcastReceiverTest : FacebookPowerMockTestCase() {

  private lateinit var receiver: FacebookBroadcastReceiver
  private lateinit var ctx: Context

  @Before
  fun init() {
    receiver = PowerMockito.mock(FacebookBroadcastReceiver::class.java)
    PowerMockito.`when`(receiver.onReceive(isA(Context::class.java), isA(Intent::class.java)))
        .thenCallRealMethod()
    ctx = ApplicationProvider.getApplicationContext() as Context
  }

  @Test
  fun `test on receive successful`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, "action")
    PowerMockito.mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(anyObject())).thenReturn(false)
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
    PowerMockito.mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(anyObject())).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, times(1)).onFailedAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
    verify(receiver, never()).onSuccessfulAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
  }
  @Test
  fun `test on receive never called`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    PowerMockito.mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(NativeProtocol.isErrorResult(anyObject())).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, never()).onFailedAppCall(eq("1337"), eq("action"), any(Bundle::class.java))
  }
}
