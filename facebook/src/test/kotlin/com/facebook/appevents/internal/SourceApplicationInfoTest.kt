package com.facebook.appevents.internal

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, PreferenceManager::class)
class SourceApplicationInfoTest : FacebookPowerMockTestCase() {
  private lateinit var mockSharedPreferences: MockSharedPreference
  private lateinit var mockContext: Context

  private val callingApplicationPackage = "com.facebook.testapp"
  private val newCallingApplicationPackage = "com.facebook.newApp"
  private val openByAppLink = true
  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    mockStatic(PreferenceManager::class.java)
    mockContext = mock(Context::class.java)
    mockSharedPreferences = MockSharedPreference()
    `when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    `when`(FacebookSdk.isInitialized()).thenReturn(true)
    `when`(PreferenceManager.getDefaultSharedPreferences(mockContext))
        .thenReturn(mockSharedPreferences)
    mockSharedPreferences
        .edit()
        .putString(
            "com.facebook.appevents.SourceApplicationInfo.callingApplicationPackage",
            callingApplicationPackage)
        .putBoolean("com.facebook.appevents.SourceApplicationInfo.openedByApplink", openByAppLink)
        .apply()
  }

  @Test
  fun `test get a new stored source application info`() {
    val sourceAppInfo = SourceApplicationInfo.getStoredSourceApplicatioInfo()
    assertEquals(callingApplicationPackage, sourceAppInfo.callingApplicationPackage)
    assertEquals(openByAppLink, sourceAppInfo.isOpenedByAppLink)
    assertEquals("Applink(com.facebook.testapp)", sourceAppInfo.toString())
  }

  @Test
  fun `test clear stored source application info from disk`() {
    SourceApplicationInfo.clearSavedSourceApplicationInfoFromDisk()
    val sourceAppInfo = SourceApplicationInfo.getStoredSourceApplicatioInfo()
    assertNull(sourceAppInfo)
  }

  @Test
  fun `test build source application info and write to disk`() {
    val mockActivity = mock(Activity::class.java)
    val mockComponentName = mock(ComponentName::class.java)
    val mockIntent = mock(Intent::class.java)

    `when`(mockComponentName.packageName).thenReturn(newCallingApplicationPackage)
    `when`(mockActivity.packageName).thenReturn(callingApplicationPackage)
    `when`(mockActivity.getCallingActivity()).thenReturn(mockComponentName)
    `when`(mockActivity.getIntent()).thenReturn(mockIntent)
    var sourceAppInfo = SourceApplicationInfo.Factory.create(mockActivity)
    assertEquals(false, sourceAppInfo.isOpenedByAppLink)
    assertEquals(newCallingApplicationPackage, sourceAppInfo.callingApplicationPackage)

    sourceAppInfo.writeSourceApplicationInfoToDisk()
    sourceAppInfo = SourceApplicationInfo.getStoredSourceApplicatioInfo()
    assertEquals(newCallingApplicationPackage, sourceAppInfo.callingApplicationPackage)
    assertEquals(false, sourceAppInfo.isOpenedByAppLink)
  }
}
