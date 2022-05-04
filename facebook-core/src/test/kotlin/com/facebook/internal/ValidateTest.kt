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

package com.facebook.internal

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.Validate.hasAppID
import com.facebook.internal.Validate.hasClientToken
import com.facebook.internal.Validate.notEmpty
import com.facebook.internal.Validate.notNull
import com.facebook.internal.Validate.notNullOrEmpty
import com.facebook.internal.Validate.oneOf
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ValidateTest : FacebookPowerMockTestCase() {
  private val appID = "123"
  private val clientToken = "mockClientToken"
  private val packageName = "com.test"
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    mockContext = mock()
    mockPackageManager = mock()
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn(packageName)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117691280
  @Test
  fun testNotNullOnNonNull() {
    notNull("A string", "name")
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117651873
  @Test(expected = NullPointerException::class)
  fun testNotNullOnNull() {
    notNull(null, "name")
  }

  @Test
  fun testNotEmptyOnNonEmpty() {
    notEmpty(listOf("hi"), "name")
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117690100
  @Test(expected = IllegalArgumentException::class)
  fun testNotEmptylOnEmpty() {
    notEmpty(listOf<String>(), "name")
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117656005
  @Test
  fun testNotNullOrEmptyOnNonEmpty() {
    notNullOrEmpty("hi", "name")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testNotNullOrEmptyOnEmpty() {

    notNullOrEmpty("", "name")
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117651884
  @Test(expected = IllegalArgumentException::class)
  fun testNotNullOrEmptyOnNull() {
    notNullOrEmpty(null, "name")
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117655985
  @Test
  fun testOneOfOnValid() {
    oneOf("hi", "name", "hi", "there")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testOneOfOnInvalid() {

    oneOf("hit", "name", "hi", "there")
  }

  @Test
  fun testOneOfOnValidNull() {
    oneOf(null, "name", "hi", "there", null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testOneOfOnInvalidNull() {

    oneOf(null, "name", "hi", "there")
  }

  @Test
  fun testHasAppID() {
    whenever(FacebookSdk.getApplicationId()).thenReturn(appID)
    assertThat(hasAppID()).isEqualTo(appID)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117650576
  @Test(expected = IllegalStateException::class)
  fun testHasNoAppID() {
    whenever(FacebookSdk.getApplicationId()).thenReturn(null)
    hasAppID()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117650768
  @Test
  fun testHasClientToken() {
    whenever(FacebookSdk.getClientToken()).thenReturn(clientToken)
    assertThat(hasClientToken()).isEqualTo(clientToken)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117653651
  @Test(expected = IllegalStateException::class)
  fun testHasNotClientToken() {
    whenever(FacebookSdk.getClientToken()).thenReturn(null)
    hasClientToken()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117658622
  @Test
  fun `test hasCustomTabRedirectActivity will query activities for action view`() {
    val redirectUri = "http://facebook.com"
    Validate.hasCustomTabRedirectActivity(mockContext, redirectUri)

    val intentCaptor = argumentCaptor<Intent>()
    verify(mockPackageManager).queryIntentActivities(intentCaptor.capture(), any())
    val capturedIntent = intentCaptor.firstValue
    assertThat(capturedIntent.action).isEqualTo(Intent.ACTION_VIEW)
    assertThat(capturedIntent.data.toString()).isEqualTo(redirectUri)
    assertThat(capturedIntent.hasCategory(Intent.CATEGORY_BROWSABLE)).isTrue
  }

  @Test
  fun `test hasCustomTabRedirectActivity returns true if CustomTabActivity is available in the package`() {
    val redirectUri = "http://facebook.com"
    val mockActivityInfo = mock<ActivityInfo>()
    mockActivityInfo.name = "com.facebook.CustomTabActivity"
    mockActivityInfo.packageName = packageName
    val mockResolveInfo = mock<ResolveInfo>()
    mockResolveInfo.activityInfo = mockActivityInfo
    whenever(mockPackageManager.queryIntentActivities(any(), any()))
        .thenReturn(mutableListOf(mockResolveInfo))

    assertThat(Validate.hasCustomTabRedirectActivity(mockContext, redirectUri)).isTrue
  }

  @Test
  fun `test hasCustomTabRedirectActivity returns false if CustomTabActivity is available in another package`() {
    val redirectUri = "http://facebook.com"
    val mockActivityInfo = mock<ActivityInfo>()
    mockActivityInfo.name = "com.facebook.CustomTabActivity"
    mockActivityInfo.packageName = "com.anotherapp"
    val mockResolveInfo = mock<ResolveInfo>()
    mockResolveInfo.activityInfo = mockActivityInfo
    whenever(mockPackageManager.queryIntentActivities(any(), any()))
        .thenReturn(mutableListOf(mockResolveInfo))

    assertThat(Validate.hasCustomTabRedirectActivity(mockContext, redirectUri)).isFalse
  }
}
