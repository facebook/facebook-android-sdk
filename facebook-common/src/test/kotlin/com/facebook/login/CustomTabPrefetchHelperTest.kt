/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.login

import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsSession
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
