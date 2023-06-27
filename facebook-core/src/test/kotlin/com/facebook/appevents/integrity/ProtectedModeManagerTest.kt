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
import org.mockito.kotlin.mock
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
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test empty parameters when enable`() {
        ProtectedModeManager.enable()
        val mockParameters = Bundle()
        val expectedParameters = Bundle()

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)

        assertEqual(mockParameters, expectedParameters)
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

        assertEqual(mockParameters, expectedParameters)
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

        assertEqual(mockParameters, expectedParameters)
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

        assertEqual(mockParameters, expectedParameters)
    }

    private fun assertEqual(mockBundle: Bundle?, expectedBundle: Bundle?): Boolean {
        val s1 = mockBundle?.keySet() ?: return false
        val s2 = expectedBundle?.keySet() ?: return false

        if (!s1.equals(s2)) {
            return false
        }

        for (s in s1) {
            val v1 = mockBundle.get(s) ?: return false
            val v2 = expectedBundle.get(s) ?: return false
            if (v1 != v2) {
                return false
            }
        }
        return true
    }
}
