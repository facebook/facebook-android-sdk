/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ondeviceprocessing

import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.ServiceResult
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.isServiceAvailable
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendCustomEvents
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendInstallEvent
import com.facebook.internal.FacebookSignatureValidator
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.ppml.receiver.IReceiverService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    FacebookSignatureValidator::class,
    IReceiverService.Stub::class,
    FetchedAppSettingsManager::class,
    AppEventUtility::class)
class RemoteServiceWrapperTest : FacebookPowerMockTestCase() {
  private val applicationId = "app_id"
  private val appEvents: List<AppEvent> =
      listOf(AppEvent("context_name", "test_event", 0.0, null, false, false, null))
  private lateinit var mockContext: Context

  override fun setup() {
    super.setup()
    mockContext = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)

    // Disable AppEventUtility.isMainThread since executor now runs in main thread
    PowerMockito.spy(AppEventUtility::class.java)
    PowerMockito.doReturn(false).`when`(AppEventUtility::class.java, "isMainThread")

    // Reset internal state
    val value: Boolean? = null
    Whitebox.setInternalState(RemoteServiceWrapper::class.java, "isServiceAvailable", value)
  }

  @Test
  fun testSendCustomEvents_RemoteServiceNotAvailable() {
    // Arrange
    val mockRemoteService = mockRemoteService(null, false, false, true)

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.SERVICE_NOT_AVAILABLE)
    verify(mockRemoteService, never()).sendEvents(any())
  }

  @Test
  fun testSendCustomEvents_RemoteServiceAvailableButSignatureMismatch() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), false, false, true)

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.SERVICE_NOT_AVAILABLE)
    verify(mockRemoteService, never()).sendEvents(any())
  }

  @Test
  fun testSendCustomEvents_RemoteServiceAvailableButFailedToBind() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), true, false, true)

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.SERVICE_ERROR)
    verify(mockRemoteService, never()).sendEvents(any())
  }

  @Test
  fun testSendCustomEvents_RemoteServiceAvailableButBinderIsNull() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), true, true, true)

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.SERVICE_NOT_AVAILABLE)
    verify(mockRemoteService, never()).sendEvents(any())
  }

  @Test
  fun testSendCustomEvents_RemoteServiceAvailableButThrowsException() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), true, true, false)
    doThrow(RemoteException::class).`when`(mockRemoteService).sendEvents(any())

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.SERVICE_ERROR)
    verify(mockRemoteService).sendEvents(any())
  }

  @Test
  fun testSendCustomEvents_RemoteServiceAvailable() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), true, true, false)

    // Act
    val serviceResult = sendCustomEvents(applicationId, appEvents)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.OPERATION_SUCCESS)
    val captor = ArgumentCaptor.forClass(Bundle::class.java)
    verify(mockRemoteService).sendEvents(captor.capture())
    assertThat(
            captor.value.getString("event"),
        )
        .isEqualTo(RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS.toString())
  }

  @Test
  fun testSendInstallEvent_RemoteServiceAvailable() {
    // Arrange
    val mockRemoteService = mockRemoteService(mock(), true, true, false)

    // Act
    val serviceResult = sendInstallEvent(applicationId)

    // Assert
    assertThat(serviceResult).isEqualTo(ServiceResult.OPERATION_SUCCESS)
    val captor = ArgumentCaptor.forClass(Bundle::class.java)
    verify(mockRemoteService).sendEvents(captor.capture())
    assertThat(captor.value.getString("event"))
        .isEqualTo(RemoteServiceWrapper.EventType.MOBILE_APP_INSTALL.toString())
    assertThat(captor.value.getString("custom_events")).isNull()
  }

  @Test
  fun testIsServiceAvailable_RemoteServiceNotAvailable() {
    // Arrange
    mockRemoteService(null, false, false, true)

    // Act
    val serviceAvailable = isServiceAvailable()

    // Assert
    assertThat(serviceAvailable).isFalse()
  }

  @Test
  fun testIsServiceAvailable_RemoteServiceAvailable() {
    // Arrange
    mockRemoteService(mock(), true, false, true)

    // Act
    val serviceAvailable = isServiceAvailable()

    // Assert
    assertThat(serviceAvailable).isTrue()
  }

  private fun mockRemoteService(
      serviceResolveInfo: ResolveInfo?,
      isSignatureValid: Boolean,
      isServiceBindSuccessful: Boolean,
      isBinderNull: Boolean
  ): IReceiverService {
    // Mock PackageManager
    val mockPackageManager: PackageManager = mock()
    whenever(mockPackageManager.resolveService(any(), any())).thenReturn(serviceResolveInfo)
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)

    // Mock FacebookSignatureValidator
    PowerMockito.mockStatic(FacebookSignatureValidator::class.java)
    whenever(validateSignature(any(), any())).thenReturn(isSignatureValid)

    // Mock FetchedAppSettings
    val mockAppSettings: FetchedAppSettings = mock()
    whenever(mockAppSettings.supportsImplicitLogging()).thenReturn(false)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(queryAppSettings(any(), any())).thenReturn(mockAppSettings)

    // Mock remote service binder
    val mockBinder: IBinder? = if (isBinderNull) null else mock()
    // Mock Context.bindService
    whenever(mockContext.bindService(any(), any(), any())).thenAnswer {
      val connection = it.arguments[1] as ServiceConnection
      if (!isBinderNull) {
        connection.onServiceConnected(mock(), mockBinder)
      }
      return@thenAnswer isServiceBindSuccessful
    }

    val mockRemoteService: IReceiverService = mock()
    PowerMockito.mockStatic(IReceiverService.Stub::class.java)
    whenever(IReceiverService.Stub.asInterface(mockBinder)).thenReturn(mockRemoteService)
    return mockRemoteService
  }
}
