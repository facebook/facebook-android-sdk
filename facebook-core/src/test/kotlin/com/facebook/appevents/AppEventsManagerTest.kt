package com.facebook.appevents

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.mock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class AppEventsManagerTest : FacebookPowerMockTestCase() {
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
  }

  @Test
  fun testApplicationIdIsEmpty() {
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("")
    AppEventsManager.start()
    val state =
        Whitebox.getInternalState<AtomicReference<FetchedAppSettingsManager.FetchAppSettingState>>(
            FetchedAppSettingsManager::class.java, "loadingState")
    assertThat(state.get()).isEqualTo(FetchedAppSettingsManager.FetchAppSettingState.ERROR)
  }

  @Test
  fun testFetchedAppSettingsContainsApplicationId() {
    val appID = "123"
    val mockAppSettings: FetchedAppSettings = mock()
    val fetchedAppSettings = ConcurrentHashMap<String, FetchedAppSettings>()
    fetchedAppSettings[appID] = mockAppSettings
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(appID)
    Whitebox.setInternalState(
        FetchedAppSettingsManager::class.java, "fetchedAppSettings", fetchedAppSettings)
    AppEventsManager.start()
    val state =
        Whitebox.getInternalState<AtomicReference<FetchedAppSettingsManager.FetchAppSettingState>>(
            FetchedAppSettingsManager::class.java, "loadingState")
    assertThat(state.get()).isEqualTo(FetchedAppSettingsManager.FetchAppSettingState.SUCCESS)
  }
}
