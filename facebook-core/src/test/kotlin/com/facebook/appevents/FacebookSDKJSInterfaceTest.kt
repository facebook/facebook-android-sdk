package com.facebook.appevents

import android.content.Context
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class FacebookSDKJSInterfaceTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"supports_implicit_sdk_logging\": true,\n" +
          "  \"android_dialog_configs\": [20140701, 20140702, 20140703]\n" +
          "}"
  private lateinit var mockContext: Context
  private lateinit var mockLogger: InternalAppEventsLogger
  private lateinit var sdkInterface: FacebookSDKJSInterface
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    mockContext = mock()
    mockLogger = mock()

    val mockInternalAppEventsLoggerCompanion = mock<InternalAppEventsLogger.Companion>()
    whenever(mockInternalAppEventsLoggerCompanion.createInstance(anyOrNull(), anyOrNull()))
        .thenReturn(mockLogger)
    Whitebox.setInternalState(
        InternalAppEventsLogger::class.java, "Companion", mockInternalAppEventsLoggerCompanion)
    sdkInterface = FacebookSDKJSInterface(mockContext)
  }

  @Test
  fun `test with valid json`() {
    var captureParameters: Bundle? = null
    whenever(mockLogger.logEvent(any(), any())).thenAnswer {
      captureParameters = it.arguments[1] as Bundle
      Unit
    }

    sdkInterface.sendEvent("pixel_123", "event_name", validJson)
    assertThat(captureParameters).isNotNull
    assertThat(captureParameters?.size()).isEqualTo(3)
    assertThat(captureParameters?.get("_fb_pixel_referral_id")).isEqualTo("pixel_123")
    assertThat(captureParameters?.get("supports_implicit_sdk_logging")).isNotNull
    assertThat(captureParameters?.get("android_dialog_configs")).isNotNull
  }

  @Test
  fun `test nonsense json`() {
    var captureParameters: Bundle? = null
    whenever(mockLogger.logEvent(any(), any())).thenAnswer {
      captureParameters = it.arguments[1] as Bundle
      Unit
    }
    sdkInterface.sendEvent("pixel_123", "event_name", "anystringwhat")
    assertThat(captureParameters).isNotNull
    assertThat(captureParameters?.size()).isEqualTo(1)
    assertThat(captureParameters?.get("_fb_pixel_referral_id")).isEqualTo("pixel_123")
  }
}
