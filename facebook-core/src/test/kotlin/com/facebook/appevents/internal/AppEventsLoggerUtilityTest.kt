/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.AppEventsLogger
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.Utility
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
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
    whenever(mockContext.packageName).thenReturn("packagename")

    mockCompanion = mock(AppEventsLogger.Companion::class.java)
    WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
  }

  @Test
  fun `test with userid`() {
    whenever(mockCompanion.getUserID()).thenReturn("13379000")
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
    whenever(mockCompanion.getUserID()).thenReturn(null)
    val result =
        AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
            AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
            AttributionIdentifiers(),
            "anonGUID",
            false,
            mockContext)
    assertNotNull(result["event"])
    assertThat(result.has("app_user_id")).isFalse
    assertNotNull(result["application_package_name"])
  }
}
