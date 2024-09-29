/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.app.Application
import android.content.Intent
import android.content.ServiceConnection
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.internal.AutomaticAnalyticsLogger
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    AutomaticAnalyticsLogger::class,
    InternalAppEventsLogger::class,
    InAppPurchaseEventManager::class,
    InAppPurchaseUtils::class
)
class InAppPurchaseActivityLifecycleTrackerTest : FacebookPowerMockTestCase() {
    private lateinit var applicationContext: Application

    override fun setup() {
        super.setup()
        PowerMockito.whenNew(InternalAppEventsLogger::class.java).withAnyArguments()
            .thenReturn(mock())
        applicationContext = mock<Application>()
        whenever(applicationContext.registerActivityLifecycleCallbacks(any())).thenAnswer {}
        whenever(
            applicationContext.bindService(
                any<Intent>(),
                any<ServiceConnection>(),
                any<Int>()
            )
        )
            .thenReturn(true)
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.isInitialized()).thenReturn(true)
        whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
        whenever(FacebookSdk.getApplicationContext()).thenReturn(applicationContext)
        PowerMockito.mockStatic(AutomaticAnalyticsLogger::class.java)
        PowerMockito.mockStatic(InAppPurchaseEventManager::class.java)

        Whitebox.setInternalState(
            InAppPurchaseActivityLifecycleTracker::class.java, "hasBillingService", null as Boolean?
        )
        Whitebox.setInternalState(
            InAppPurchaseActivityLifecycleTracker::class.java, "isTracking", AtomicBoolean(false)
        )
        PowerMockito.spy(InAppPurchaseActivityLifecycleTracker::class.java)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        PowerMockito.doAnswer { this.javaClass }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any<String>())
    }

    @Test
    fun `test startIapLogging will bind iap intent and lifecycle callback`() {
        val intentCaptor = argumentCaptor<Intent>()
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)
        InAppPurchaseActivityLifecycleTracker.startIapLogging(InAppPurchaseUtils.BillingClientVersion.V1)
        verify(applicationContext).registerActivityLifecycleCallbacks(any())
        verify(applicationContext)
            .bindService(intentCaptor.capture(), any<ServiceConnection>(), any<Int>())
        assertThat(intentCaptor.firstValue.action)
            .isEqualTo("com.android.vending.billing.InAppBillingService.BIND")
        assertThat(intentCaptor.firstValue.`package`).isEqualTo("com.android.vending")
    }

    @Test
    fun `test startIapLogging will only register once`() {
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)
        InAppPurchaseActivityLifecycleTracker.startIapLogging(InAppPurchaseUtils.BillingClientVersion.V1)
        InAppPurchaseActivityLifecycleTracker.startIapLogging(InAppPurchaseUtils.BillingClientVersion.V1)
        verify(applicationContext, times(1)).registerActivityLifecycleCallbacks(any())
    }

    @Test
    fun `test startIapLogging will not register if implicit purchase disabled`() {
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(false)
        InAppPurchaseActivityLifecycleTracker.startIapLogging(InAppPurchaseUtils.BillingClientVersion.V1)
        verify(applicationContext, never()).registerActivityLifecycleCallbacks(any())
    }
}
