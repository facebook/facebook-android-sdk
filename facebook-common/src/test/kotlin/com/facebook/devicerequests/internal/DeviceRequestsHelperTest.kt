/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import java.util.EnumSet
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
