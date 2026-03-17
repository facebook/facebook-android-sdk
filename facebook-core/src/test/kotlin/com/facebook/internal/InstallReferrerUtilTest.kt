/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.facebook.FacebookSdk
import com.facebook.FacebookTestCase
import com.facebook.MockSharedPreference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class InstallReferrerUtilTest : FacebookTestCase() {
    private lateinit var mockApplicationContext: Context
    private lateinit var mockInstallReferrerClient: InstallReferrerClient
    private lateinit var mockReferrerDetails: ReferrerDetails
    private lateinit var mockSharedPreference: MockSharedPreference
    private val emptyCallback =
        object : InstallReferrerUtil.Callback {
            override fun onReceiveReferrerUrl(s: String?) = Unit
        }

    @Before
    fun init() {
        mockSharedPreference = MockSharedPreference()

        mockkStatic(FacebookSdk::class)
        every { FacebookSdk.isFullyInitialized() } returns true
        mockApplicationContext = mockk(relaxed = true)
        every {
            mockApplicationContext.getSharedPreferences(
                FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE
            )
        } returns mockSharedPreference
        every { FacebookSdk.getApplicationContext() } returns mockApplicationContext

        mockReferrerDetails = mockk(relaxed = true)
        mockInstallReferrerClient = mockk(relaxed = true)
        every { mockInstallReferrerClient.installReferrer } returns mockReferrerDetails

        val mockBuilder = mockk<InstallReferrerClient.Builder>()
        every { mockBuilder.build() } returns mockInstallReferrerClient
        mockkStatic(InstallReferrerClient::class)
        every { InstallReferrerClient.newBuilder(mockApplicationContext) } returns mockBuilder
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test connection with fb referer return`() {
        var connectionCounter = 0
        var didReceivedReferrerUrl = false
        val referrerUrl = "facebook.com"
        every { mockReferrerDetails.installReferrer } returns referrerUrl
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            connectionCounter += 1
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.OK
            )
        }

        val callback =
            object : InstallReferrerUtil.Callback {
                override fun onReceiveReferrerUrl(s: String?) {
                    assertEquals(s, referrerUrl)
                    didReceivedReferrerUrl = true
                }
            }

        InstallReferrerUtil.tryUpdateReferrerInfo(callback)
        assertEquals(
            1,
            connectionCounter,
        )
        assertThat(didReceivedReferrerUrl).isTrue
    }

    @Test
    fun `test connection twice`() {
        var connectionCounter = 0
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            connectionCounter += 1
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.OK
            )
        }
        InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback)
        InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback)
        assertEquals(1, connectionCounter)
    }

    @Test
    fun `test service unavailable`() {
        var connectionCounter = 0
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            connectionCounter += 1
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE
            )
        }
        repeat(3) { InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback) }
        assertEquals(3, connectionCounter)
    }

    @Test
    fun `test blocking happy path returns referrer url`() {
        val referrerUrl = "facebook.com/test"
        every { mockReferrerDetails.installReferrer } returns referrerUrl
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.OK
            )
        }

        var receivedUrl: String? = null
        val callback = object : InstallReferrerUtil.Callback {
            override fun onReceiveReferrerUrl(s: String?) {
                receivedUrl = s
            }
        }

        InstallReferrerUtil.tryUpdateReferrerInfoBlocking(callback)
        assertThat(receivedUrl).isEqualTo(referrerUrl)
    }

    @Test
    fun `test blocking returns immediately when listener invoked synchronously`() {
        // startConnection immediately invokes the listener with OK
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.OK
            )
        }

        val startTime = System.nanoTime()
        InstallReferrerUtil.tryUpdateReferrerInfoBlocking(emptyCallback)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

        // Should return nearly instantly, not wait for the full timeout
        assertThat(elapsedMs).isLessThan(1000)
    }

    @Test
    fun `test blocking skips connection when already updated`() {
        // First, mark as updated by performing a successful connection
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.OK
            )
        }
        InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback)

        // Now the blocking call should skip startConnection
        var connectionCounter = 0
        every { mockInstallReferrerClient.startConnection(any()) } answers {
            connectionCounter += 1
        }

        InstallReferrerUtil.tryUpdateReferrerInfoBlocking(emptyCallback)
        assertEquals(0, connectionCounter)
    }

    @Test
    fun `test blocking service unavailable releases latch`() {
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE
            )
        }

        var callbackInvoked = false
        val callback = object : InstallReferrerUtil.Callback {
            override fun onReceiveReferrerUrl(s: String?) {
                callbackInvoked = true
            }
        }

        // Should return quickly since latch is released by the finally block
        val startTime = System.nanoTime()
        InstallReferrerUtil.tryUpdateReferrerInfoBlocking(callback)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

        assertFalse(callbackInvoked)
        assertThat(elapsedMs).isLessThan(1000)
    }

    @Test
    fun `test blocking feature not supported releases latch`() {
        val listenerSlot = slot<InstallReferrerStateListener>()
        every { mockInstallReferrerClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED
            )
        }

        var callbackInvoked = false
        val callback = object : InstallReferrerUtil.Callback {
            override fun onReceiveReferrerUrl(s: String?) {
                callbackInvoked = true
            }
        }

        val startTime = System.nanoTime()
        InstallReferrerUtil.tryUpdateReferrerInfoBlocking(callback)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

        assertFalse(callbackInvoked)
        assertThat(elapsedMs).isLessThan(1000)
    }
}
