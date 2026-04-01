/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.crashshield

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

/**
 * Tests for the debugCrashScheduled flag in CrashShieldHandler.
 *
 * Uses FacebookTestCase (no PowerMock) and Robolectric's ShadowLooper to verify that
 * scheduleCrashInDebug only posts one runnable, preventing repeated crash dialogs.
 */
class CrashShieldHandlerDebugCrashSchedulingTest : FacebookTestCase() {

  @Before
  override fun setUp() {
    super.setUp()
    CrashShieldHandler.reset()
    CrashShieldHandler.isDebug = true
  }

  @After
  fun tearDownCrashShield() {
    CrashShieldHandler.reset()
  }

  @Test
  fun `test first call to scheduleCrashInDebug posts a runnable`() {
    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("test"))

    val shadowLooper = ShadowLooper.shadowMainLooper()
    assertThat(shadowLooper.isIdle).isFalse
  }

  @Test
  fun `test second call to scheduleCrashInDebug does not post another runnable`() {
    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("first"))

    // Consume the first posted runnable
    try {
      ShadowLooper.shadowMainLooper().idle()
    } catch (_: RuntimeException) {
      // Expected — the runnable throws RuntimeException
    }

    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("second"))

    // Main looper should be idle — no second runnable was posted
    assertThat(ShadowLooper.shadowMainLooper().isIdle).isTrue
  }

  @Test
  fun `test reset clears debugCrashScheduled flag`() {
    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("first"))

    try {
      ShadowLooper.shadowMainLooper().idle()
    } catch (_: RuntimeException) {}

    CrashShieldHandler.reset()
    CrashShieldHandler.isDebug = true

    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("after reset"))

    // Should have posted again after reset
    assertThat(ShadowLooper.shadowMainLooper().isIdle).isFalse
  }

  @Test
  fun `test scheduleCrashInDebug does nothing when isDebug is false`() {
    CrashShieldHandler.isDebug = false
    CrashShieldHandler.scheduleCrashInDebug(RuntimeException("test"))

    assertThat(ShadowLooper.shadowMainLooper().isIdle).isTrue
  }
}
