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
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(AppEventsLogger::class, Utility::class)
class AppEventsLoggerUtilityTest : FacebookPowerMockTestCase() {

  private lateinit var mockContext: Context
  @Before
  fun init() {
    mockStatic(AppEventsLogger::class.java)
    mockStatic(Utility::class.java)
    mockContext = mock(Context::class.java)
    `when`(mockContext.packageName).thenReturn("packagename")
  }

  @Test
  fun `test with userid`() {
    `when`(AppEventsLogger.getUserID()).thenReturn("13379000")
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
    `when`(AppEventsLogger.getUserID()).thenReturn(null)
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
