/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.ml.ModelManager
import com.facebook.internal.FetchedAppGateKeepersManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ModelManager::class, FacebookSdk::class, FetchedAppGateKeepersManager::class)
class ProtectedModeManagerTest : FacebookPowerMockTestCase() {
    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn("123")
    }

    @After
    fun tearDown() {
        ProtectedModeManager.disable()
    }

    @Test
    fun `test null as parameters when enable`() {
        ProtectedModeManager.enable()
        val mockParameters = null
        val expectedParameters = null

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertBundleThat(mockParameters, expectedParameters)
    }

    @Test
    fun `test empty parameters when enable`() {
        ProtectedModeManager.enable()
        val mockParameters = Bundle()
        val expectedParameters = Bundle()

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)

        assertThat(mockParameters.size()).isEqualTo(0)
        assertBundleThat(mockParameters, expectedParameters)
    }

    @Test
    fun `test all standard parameters when enable`() {
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)

        assertThat(mockParameters.size()).isEqualTo(2)
        assertBundleThat(mockParameters, expectedParameters)
    }

    @Test
    fun `test filter out non-standard parameters when enable`() {
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("fb_product_price_amount", "0.990")
            putString("quantity", "1")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)

        assertThat(mockParameters.size()).isEqualTo(2)
        assertBundleThat(mockParameters, expectedParameters)
    }

    @Test
    fun `test not filter out non-standard parameters when disable`() {
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("fb_product_price_amount", "0.990")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("fb_product_price_amount", "0.990")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)

        assertThat(mockParameters.size()).isEqualTo(3)
        assertBundleThat(mockParameters, expectedParameters)
    }

    private fun assertBundleThat(mockBundle: Bundle?, expectedBundle: Bundle?)  {
        assertThat(mockBundle.toString()).isEqualTo(expectedBundle.toString())
    }
}
