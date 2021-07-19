package com.facebook.appevents

import android.content.Context
import android.preference.PreferenceManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.appevents.internal.AppEventUtility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class, PreferenceManager::class, AppEventUtility::class)
class AnalyticsUserIDStoreTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  private val mockPreference = MockSharedPreference()
  private val userID = "123"
  private val userID2 = "456"
  @Before
  fun init() {
    val mockCompanion: InternalAppEventsLogger.Companion = mock()
    WhiteboxImpl.setInternalState(InternalAppEventsLogger::class.java, "Companion", mockCompanion)
    PowerMockito.`when`(mockCompanion.getAnalyticsExecutor()).thenReturn(mockExecutor)

    val mockContext: Context = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)

    PowerMockito.mockStatic(AppEventUtility::class.java)
    PowerMockito.`when`(AppEventUtility.assertIsMainThread()).then {}

    mockPreference.edit().putString("com.facebook.appevents.AnalyticsUserIDStore.userID", userID)
    PowerMockito.mockStatic(PreferenceManager::class.java)
    PowerMockito.`when`(PreferenceManager.getDefaultSharedPreferences(any()))
        .thenReturn(mockPreference)
  }

  @Test
  fun testInitStore() {
    AnalyticsUserIDStore.initStore()
    assertThat(AnalyticsUserIDStore.getUserID()).isEqualTo(userID)

    AnalyticsUserIDStore.setUserID(userID2)
    assertThat(AnalyticsUserIDStore.getUserID()).isEqualTo(userID2)
  }
}
