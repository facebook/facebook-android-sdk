package com.facebook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.NativeProtocol
import com.facebook.util.common.anyObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(NativeProtocol::class)
class FacebookBroadcastReceiverTest : FacebookPowerMockTestCase() {
  // This class will make the callback methods public, which allows verifying interactions
  private class TestFacebookBroadcastReceiver : FacebookBroadcastReceiver() {
    public override fun onSuccessfulAppCall(appCallId: String, action: String, extras: Bundle) {
      // Do nothing. Only for testing.
    }

    public override fun onFailedAppCall(appCallId: String, action: String, extras: Bundle) {
      // Do nothing. Only for testing.
    }
  }

  private lateinit var receiver: TestFacebookBroadcastReceiver
  private lateinit var ctx: Context

  @Before
  fun init() {
    receiver = mock()
    whenever(receiver.onReceive(any(), any())).thenCallRealMethod()
    ctx = ApplicationProvider.getApplicationContext() as Context
  }

  @Test
  fun `test on receive successful`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, "action")
    PowerMockito.mockStatic(NativeProtocol::class.java)
    whenever(NativeProtocol.isErrorResult(any())).thenReturn(false)
    receiver.onReceive(ctx, intent)
    verify(receiver, times(1)).onSuccessfulAppCall(eq("1337"), eq("action"), any())
    verify(receiver, never()).onFailedAppCall(eq("1337"), eq("action"), any())
  }

  @Test
  fun `test on receive failed app call`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, "action")
    PowerMockito.mockStatic(NativeProtocol::class.java)
    whenever(NativeProtocol.isErrorResult(anyObject())).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, times(1)).onFailedAppCall(eq("1337"), eq("action"), any())
    verify(receiver, never()).onSuccessfulAppCall(eq("1337"), eq("action"), any())
  }

  @Test
  fun `test on receive never called`() {
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID, "1337")
    PowerMockito.mockStatic(NativeProtocol::class.java)
    whenever(NativeProtocol.isErrorResult(anyObject())).thenReturn(true)
    receiver.onReceive(ctx, intent)
    verify(receiver, never()).onFailedAppCall(eq("1337"), eq("action"), any())
  }
}
