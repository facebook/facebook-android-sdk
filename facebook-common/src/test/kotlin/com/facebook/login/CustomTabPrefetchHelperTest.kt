/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsSession
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.reflect.Whitebox

class CustomTabPrefetchHelperTest : FacebookPowerMockTestCase() {
  private lateinit var mockClient: CustomTabsClient
  private lateinit var mockSession: CustomTabsSession

  override fun setup() {
    super.setup()
    mockClient = mock()
    mockSession = mock()
    whenever(mockClient.newSession(anyOrNull())).thenReturn(mockSession)
    Whitebox.setInternalState(
        CustomTabPrefetchHelper::class.java, "client", null as CustomTabsClient?)
  }

  @Test
  fun `test prefetch workflow`() {
    val helper = CustomTabPrefetchHelper()
    helper.onCustomTabsServiceConnected(mock(), mockClient)
    val mockUrl = mock<Uri>()
    CustomTabPrefetchHelper.mayLaunchUrl(mockUrl)
    val session = CustomTabPrefetchHelper.getPreparedSessionOnce()

    verify(mockSession).mayLaunchUrl(eq(mockUrl), anyOrNull(), anyOrNull())
    verify(mockClient).warmup(any())
    assertThat(session).isEqualTo(mockSession)

    assertThat(CustomTabPrefetchHelper.getPreparedSessionOnce()).isNull()
  }

  @Test
  fun `test prefetch twice won't crash`() {
    val helper = CustomTabPrefetchHelper()
    helper.onCustomTabsServiceConnected(mock(), mockClient)
    val mockUrl = mock<Uri>()
    CustomTabPrefetchHelper.mayLaunchUrl(mockUrl)
    CustomTabPrefetchHelper.getPreparedSessionOnce()
    CustomTabPrefetchHelper.mayLaunchUrl(mockUrl)
    CustomTabPrefetchHelper.getPreparedSessionOnce()
  }

  @Test
  fun `test call mayLaunchUrl directly without a helper instance won't crash`() {
    CustomTabPrefetchHelper.mayLaunchUrl(mock())
    assertThat(CustomTabPrefetchHelper.getPreparedSessionOnce()).isNull()
  }
}
