package com.facebook.internal

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class ServerProtocolTest : FacebookPowerMockTestCase() {
  private val mockExecutor = FacebookSerialExecutor()

  private val callId = "1337"
  private val version = 420
  private val appId = "9999"

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `no valid keyhash gives null`() {
    PowerMockito.`when`(
            FacebookSdk.getApplicationSignature(ApplicationProvider.getApplicationContext()))
        .thenReturn("")
    val params =
        ServerProtocol.getQueryParamsForPlatformActivityIntentWebFallback(callId, version, Bundle())
    assertNull(params)
  }

  @Test
  fun `all valid ok`() {
    PowerMockito.`when`(
            FacebookSdk.getApplicationSignature(ApplicationProvider.getApplicationContext()))
        .thenReturn("abc123")
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(appId)
    val params =
        ServerProtocol.getQueryParamsForPlatformActivityIntentWebFallback(callId, version, Bundle())
    assertNotNull(params)
    assertNotNull(params.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_KEY_HASH))
    assertNotNull(params.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_APP_ID))
    assertNotNull(params.getInt(ServerProtocol.FALLBACK_DIALOG_PARAM_VERSION))
    assertNotNull(params.getString(ServerProtocol.DIALOG_PARAM_DISPLAY))
    assertNotNull(params.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS))
    assertNotNull(params.getString(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_ARGS))
  }

  @Test
  fun `no valid bundle gives null`() {
    val b = Bundle()
    b.putShort("shortValue", 7.toShort())
    PowerMockito.`when`(
            FacebookSdk.getApplicationSignature(ApplicationProvider.getApplicationContext()))
        .thenReturn("abc123")
    val params =
        ServerProtocol.getQueryParamsForPlatformActivityIntentWebFallback(callId, version, b)
    assertNull(params)
  }
}
