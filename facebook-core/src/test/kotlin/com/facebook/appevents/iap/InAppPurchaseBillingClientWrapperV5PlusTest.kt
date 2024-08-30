/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, InAppPurchaseUtils::class)
class InAppPurchaseBillingClientWrapperV5PlusTest : FacebookPowerMockTestCase() {
    private val exampleClassName =
        "com.facebook.appevents.iap.InAppPurchaseBillingClientWrapperV5PlusTest"
    private lateinit var inAppPurchaseBillingClientWrapperV5Plus: InAppPurchaseBillingClientWrapperV5Plus

    @Before
    override fun setup() {
        super.setup()
        val mockContext: Context = mock()
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        whenever(getClass(anyOrNull())).thenReturn(Class.forName(exampleClassName))
        inAppPurchaseBillingClientWrapperV5Plus =
            InAppPurchaseBillingClientWrapperV5Plus.getOrCreateInstance(mockContext)!!
    }

    @Test
    fun testHelperClassCanSuccessfullyCreateWrapper() {
        Assertions.assertThat(inAppPurchaseBillingClientWrapperV5Plus).isNotNull()
    }
}
