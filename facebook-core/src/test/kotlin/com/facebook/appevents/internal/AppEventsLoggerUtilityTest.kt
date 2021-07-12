package com.facebook.appevents.internal

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.AppEventsLogger
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.Utility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(Utility::class)
class AppEventsLoggerUtilityTest : FacebookPowerMockTestCase() {

  private lateinit var mockContext: Context
  private lateinit var mockCompanion: AppEventsLogger.Companion
  @Before
  fun init() {
    mockStatic(Utility::class.java)
    mockContext = mock(Context::class.java)
    `when`(mockContext.packageName).thenReturn("packagename")

    mockCompanion = mock(AppEventsLogger.Companion::class.java)
    WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
  }

  @Test
  fun `test with userid`() {
    `when`(mockCompanion.getUserID()).thenReturn("13379000")
    val result =
        AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
            AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
            AttributionIdentifiers(),
            "anonGUID",
            false,
            mockContext)
    assertNotNull(result["event"])
    assertNotNull(result["app_user_id"])
    assertNotNull(result["application_package_name"])
  }

  @Test
  fun `test without userid`() {
    `when`(mockCompanion.getUserID()).thenReturn(null)
    val result =
        AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
            AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
            AttributionIdentifiers(),
            "anonGUID",
            false,
            mockContext)
    assertNotNull(result["event"])
    assertFalse(result.has("app_user_id"))
    assertNotNull(result["application_package_name"])
  }
}
