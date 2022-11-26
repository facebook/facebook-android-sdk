/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class CurrentAccessTokenExpirationBroadcastReceiverTest : FacebookPowerMockTestCase() {
  private lateinit var mockAccessTokenManager: AccessTokenManager
  override fun setup() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    mockAccessTokenManager = mock()
    val mockAccessTokenManagerCompanion: AccessTokenManager.Companion = mock()
    whenever(mockAccessTokenManagerCompanion.getInstance()).thenReturn(mockAccessTokenManager)
    Whitebox.setInternalState(
        AccessTokenManager::class.java, "Companion", mockAccessTokenManagerCompanion)
  }

  @Test
  fun `test on receiving current access token changed intent`() {
    val intent = Intent(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED)
    val receiver = CurrentAccessTokenExpirationBroadcastReceiver()
    receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
    verify(mockAccessTokenManager).currentAccessTokenChanged()
  }

  @Test
  fun `test on receiving irrelevant intent`() {
    val intent = Intent("irrelevant action")
    val receiver = CurrentAccessTokenExpirationBroadcastReceiver()
    receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
    verify(mockAccessTokenManager, never()).currentAccessTokenChanged()
  }
}
