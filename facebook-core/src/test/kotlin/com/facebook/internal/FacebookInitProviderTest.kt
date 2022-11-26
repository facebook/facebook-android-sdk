/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class)
class FacebookInitProviderTest : FacebookPowerMockTestCase() {
  private lateinit var provider: FacebookInitProvider
  private lateinit var mockContext: Context
  @Before
  fun init() {
    mockContext = PowerMockito.mock(Context::class.java)
    provider = FacebookInitProvider()
    WhiteboxImpl.setInternalState(provider, "mContext", mockContext as Any)
    PowerMockito.mockStatic(FacebookSdk::class.java)
  }

  @Test
  fun `test onCreate call FacebookSdk initialization`() {
    var capturedContext: Context? = null
    whenever(FacebookSdk.sdkInitialize(any())).thenAnswer {
      capturedContext = it.arguments[0] as Context
      Unit
    }
    provider.onCreate()
    Assert.assertNotNull(capturedContext)
    Assert.assertEquals(mockContext, capturedContext)
  }
}
