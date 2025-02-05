/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

    @Test
    fun testNotNullOnNonNull() {
        notNull("A string", "name")
    }

    @Test(expected = NullPointerException::class)
    fun testNotNullOnNull() {
        notNull(null, "name")
    }

    @Test
    fun testNotEmptyOnNonEmpty() {
        notEmpty(listOf("hi"), "name")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNotEmptylOnEmpty() {
        notEmpty(listOf<String>(), "name")
    }

    @Test
    fun testNotNullOrEmptyOnNonEmpty() {
        notNullOrEmpty("hi", "name")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNotNullOrEmptyOnEmpty() {

        notNullOrEmpty("", "name")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNotNullOrEmptyOnNull() {
        notNullOrEmpty(null, "name")
    }

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

    @Test(expected = IllegalStateException::class)
    fun testHasNoAppID() {
        whenever(FacebookSdk.getApplicationId()).thenReturn(null)
        hasAppID()
    }

    @Test
    fun testHasClientToken() {
        whenever(FacebookSdk.getClientToken()).thenReturn(clientToken)
        assertThat(hasClientToken()).isEqualTo(clientToken)
    }

    @Test(expected = IllegalStateException::class)
    fun testHasNotClientToken() {
        whenever(FacebookSdk.getClientToken()).thenReturn(null)
        hasClientToken()
    }

    @Test
    fun `test hasCustomTabRedirectActivity will query activities for action view`() {
        val redirectUri = "http://facebook.com"
        Validate.hasCustomTabRedirectActivity(mockContext, redirectUri)

        val intentCaptor = argumentCaptor<Intent>()
        verify(mockPackageManager).queryIntentActivities(intentCaptor.capture(), any<Int>())
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
        whenever(mockPackageManager.queryIntentActivities(any<Intent>(), any<Int>()))
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
        whenever(mockPackageManager.queryIntentActivities(any<Intent>(), any<Int>()))
            .thenReturn(mutableListOf(mockResolveInfo))

        assertThat(Validate.hasCustomTabRedirectActivity(mockContext, redirectUri)).isFalse
    }
}
