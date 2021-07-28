package com.facebook.appevents.aam

import android.app.Activity
import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class, AttributionIdentifiers::class, FetchedAppSettingsManager::class)
class MetadataIndexerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val VALID_RULES_JSON =
        """{"r1":{"k": "key1,key2","v":"val1"},"r2":{"k":"key1","v":"val2"}}"""
    private const val APP_ID = "123"
  }
  private lateinit var mockActivity: Activity
  private lateinit var mockContext: Context
  private lateinit var mockSettings: FetchedAppSettings
  private lateinit var mockCompanion: MetadataViewObserver.Companion
  @Before
  fun init() {
    mockActivity = mock()
    mockContext = mock()
    mockSettings = mock()
    mockCompanion = mock()

    WhiteboxImpl.setInternalState(MetadataViewObserver::class.java, "Companion", mockCompanion)

    PowerMockito.mockStatic(AttributionIdentifiers::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    PowerMockito.`when`(FacebookSdk.getExecutor()).thenReturn(FacebookSerialExecutor())
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    PowerMockito.`when`(AttributionIdentifiers.isTrackingLimited(mockContext)).thenReturn(false)
    PowerMockito.`when`(FetchedAppSettingsManager.queryAppSettings(APP_ID, false))
        .thenReturn(mockSettings)
    PowerMockito.`when`(mockSettings.rawAamRules).thenReturn(VALID_RULES_JSON)
  }

  @Test
  fun testOnActivityResumedWhenDisable() {
    WhiteboxImpl.setInternalState(MetadataIndexer::class.java, "enabled", false)
    var calledTimes = 0
    PowerMockito.`when`(mockCompanion.startTrackingActivity(mockActivity)).thenAnswer {
      calledTimes++
      Unit
    }
    MetadataIndexer.onActivityResumed(mockActivity)
    assertThat(calledTimes).isEqualTo(0)
  }

  @Test
  fun testOnActivityResumedWhenEnable() {
    var calledTimes = 0
    PowerMockito.`when`(mockCompanion.startTrackingActivity(mockActivity)).thenAnswer {
      calledTimes++
      Unit
    }
    MetadataIndexer.enable()
    MetadataIndexer.onActivityResumed(mockActivity)
    assertThat(calledTimes).isEqualTo(1)
  }
}
