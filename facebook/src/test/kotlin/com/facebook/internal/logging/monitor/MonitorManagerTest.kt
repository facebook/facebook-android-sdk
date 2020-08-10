package com.facebook.internal.logging.monitor

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_APP_ID
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class MonitorManagerTest : FacebookPowerMockTestCase() {
  private val mockExecutor = FacebookSerialExecutor()
  private lateinit var mockMonitorCreator: MonitorManager.MonitorCreator
  private lateinit var mockSettings: FetchedAppSettings

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    whenCalled(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationId", TEST_APP_ID)
    whenCalled(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenCalled(FacebookSdk.getApplicationId()).thenReturn(TEST_APP_ID)
    mockMonitorCreator = mock(MonitorManager.MonitorCreator::class.java)
    MonitorManager.setMonitorCreator(mockMonitorCreator)
    mockSettings = mock(FetchedAppSettings::class.java)
    mockStatic(FetchedAppSettingsManager::class.java)
    whenCalled(FetchedAppSettingsManager.getAppSettingsWithoutQuery(TEST_APP_ID))
        .thenReturn(mockSettings)
  }

  @Test
  fun `test start monitor not enabled from manifest and app settings from dialog is null`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(false)
    whenCalled(FetchedAppSettingsManager.getAppSettingsWithoutQuery(TEST_APP_ID)).thenReturn(null)
    MonitorManager.start()
    verify(mockMonitorCreator, never()).enable()
  }

  @Test
  fun `test start monitor not enabled from manifest and not enabled from dialog`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(false)
    whenCalled(mockSettings.monitorViaDialogEnabled).thenReturn(false)
    MonitorManager.start()
    verify(mockMonitorCreator, never()).enable()
  }

  @Test
  fun `test start monitor not enabled from manifest and enabled from dialog`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(false)
    whenCalled(mockSettings.monitorViaDialogEnabled).thenReturn(true)
    MonitorManager.start()
    verify(mockMonitorCreator, never()).enable()
  }

  @Test
  fun `test start monitor enabled from manifest and app settings from dialog is null`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(true)
    whenCalled(FetchedAppSettingsManager.getAppSettingsWithoutQuery(TEST_APP_ID)).thenReturn(null)
    MonitorManager.start()
    verify(mockMonitorCreator, never()).enable()
  }

  @Test
  fun `test start monitor enabled from manifest and not enabled from dialog`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(true)
    whenCalled(mockSettings.monitorViaDialogEnabled).thenReturn(false)
    MonitorManager.start()
    verify(mockMonitorCreator, never()).enable()
  }

  @Test
  fun `test start monitor enabled from manifest and enabled from dialog`() {
    whenCalled(FacebookSdk.getMonitorEnabled()).thenReturn(true)
    whenCalled(mockSettings.monitorViaDialogEnabled).thenReturn(true)
    MonitorManager.start()
    verify(mockMonitorCreator).enable()
  }
}
