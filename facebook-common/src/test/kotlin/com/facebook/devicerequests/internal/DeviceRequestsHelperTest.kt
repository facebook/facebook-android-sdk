/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.devicerequests.internal

import android.content.Context
import android.graphics.Color
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.EnumSet
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FetchedAppSettingsManager::class, FacebookSdk::class)
class DeviceRequestsHelperTest : FacebookPowerMockTestCase() {
  private lateinit var mockApplicationContext: Context
  private lateinit var mockNsdManager: NsdManager

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    whenever(FacebookSdk.getSdkVersion()).thenReturn(TEST_VERSION_NUMBER)

    mockApplicationContext = mock()
    mockNsdManager = mock()
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.getSystemService(Context.NSD_SERVICE))
        .thenReturn(mockNsdManager)
  }

  @Test
  fun `test getting device info`() {
    val deviceInfoStr = DeviceRequestsHelper.getDeviceInfo()
    val deviceInfo = JSONObject(deviceInfoStr)
    assertThat(deviceInfo.getString(DeviceRequestsHelper.DEVICE_INFO_MODEL)).isEqualTo(Build.MODEL)
    assertThat(deviceInfo.getString(DeviceRequestsHelper.DEVICE_INFO_DEVICE))
        .isEqualTo(Build.DEVICE)
  }

  @Test
  fun `test getting device info associated with other information`() {
    val extraInfo = mutableMapOf("key" to "value")
    val deviceInfoStr = DeviceRequestsHelper.getDeviceInfo(extraInfo)
    val deviceInfo = JSONObject(deviceInfoStr)
    assertThat(deviceInfo.getString(DeviceRequestsHelper.DEVICE_INFO_MODEL)).isEqualTo(Build.MODEL)
    assertThat(deviceInfo.getString(DeviceRequestsHelper.DEVICE_INFO_DEVICE))
        .isEqualTo(Build.DEVICE)
    assertThat(deviceInfo.getString("key")).isEqualTo("value")
    assertThat(extraInfo[DeviceRequestsHelper.DEVICE_INFO_DEVICE]).isEqualTo(Build.DEVICE)
    assertThat(extraInfo[DeviceRequestsHelper.DEVICE_INFO_MODEL]).isEqualTo(Build.MODEL)
  }

  @Test
  fun `test obtaining smart login option availability when it's enabled`() {
    val mockFetchedAppSettings = mock<FetchedAppSettings>()
    whenever(mockFetchedAppSettings.smartLoginOptions)
        .thenReturn(EnumSet.of(SmartLoginOption.Enabled))
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyOrNull()))
        .thenReturn(mockFetchedAppSettings)

    assertThat(DeviceRequestsHelper.isAvailable()).isTrue
  }

  @Test
  fun `test obtaining smart login option when it's disabled`() {
    val mockFetchedAppSettings = mock<FetchedAppSettings>()
    whenever(mockFetchedAppSettings.smartLoginOptions).thenReturn(EnumSet.of(SmartLoginOption.None))
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyOrNull()))
        .thenReturn(mockFetchedAppSettings)

    assertThat(DeviceRequestsHelper.isAvailable()).isFalse
  }

  @Test
  fun `test start and clean up advertisement service`() {
    val mockFetchedAppSettings = mock<FetchedAppSettings>()
    whenever(mockFetchedAppSettings.smartLoginOptions)
        .thenReturn(EnumSet.of(SmartLoginOption.Enabled))
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyOrNull()))
        .thenReturn(mockFetchedAppSettings)

    DeviceRequestsHelper.startAdvertisementService(TEST_USER_CODE)

    val nsdServiceInfoCaptor = argumentCaptor<NsdServiceInfo>()
    val nsdListenerCaptor = argumentCaptor<NsdManager.RegistrationListener>()
    verify(mockNsdManager)
        .registerService(nsdServiceInfoCaptor.capture(), any(), nsdListenerCaptor.capture())
    val capturedNsdServiceInfo = nsdServiceInfoCaptor.firstValue
    // verify the service name
    assertThat(capturedNsdServiceInfo.serviceName)
        .isEqualTo("fbsdk_android-12|34|567_$TEST_USER_CODE")

    DeviceRequestsHelper.cleanUpAdvertisementService(TEST_USER_CODE)
    verify(mockNsdManager).unregisterService(nsdListenerCaptor.firstValue)
  }

  @Test
  fun `test generated QR code only black and white`() {
    val encodedURL = "https://facebook.com/test"
    val image = DeviceRequestsHelper.generateQRCode(encodedURL)
    val pixels = IntArray(200 * 200)
    checkNotNull(image)
    image.getPixels(pixels, 0, 200, 0, 0, 200, 200)
    assertThat(pixels).containsOnly(Color.BLACK, Color.WHITE)
  }

  companion object {
    private const val TEST_USER_CODE = "123456"
    private const val APP_ID = "123456789"
    private const val TEST_VERSION_NUMBER = "12.34.567"
  }
}
