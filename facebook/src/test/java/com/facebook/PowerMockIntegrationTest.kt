/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getClientToken
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

/** This test makes sure PowerMock integration works. */
@PrepareForTest(FacebookSdk::class)
class PowerMockIntegrationTest : FacebookPowerMockTestCase() {
  @Test
  fun testStaticMethodOverrides() {
    mockStatic(FacebookSdk::class.java)
    val applicationId = "1234"

    whenever(getApplicationId()).thenReturn(applicationId)
    assertEquals(applicationId, getApplicationId())

    val clientToken = "clienttoken"
    whenever(getClientToken()).thenReturn(clientToken)
    assertEquals(clientToken, getClientToken())
  }
}
