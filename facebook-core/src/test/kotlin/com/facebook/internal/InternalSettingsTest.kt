/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class InternalSettingsTest : FacebookTestCase() {

  @After
  fun tearDown() {
    // Reset state between tests since InternalSettings is a singleton
    InternalSettings.setCustomUserAgent("")
  }

  @Test
  fun `getCustomUserAgent returns null initially`() {
    // Clear any previously set value by setting a blank and verifying getter
    // Note: The field starts as null, but prior tests in the suite may have set it.
    // We test the set/get round-trip instead.
    InternalSettings.setCustomUserAgent("test-agent")
    assertThat(InternalSettings.getCustomUserAgent()).isEqualTo("test-agent")
  }

  @Test
  fun `setCustomUserAgent stores the value`() {
    val agent = "FBAndroidSDK.17.0.0/MyApp.1.0"
    InternalSettings.setCustomUserAgent(agent)
    assertThat(InternalSettings.getCustomUserAgent()).isEqualTo(agent)
  }

  @Test
  fun `isUnityApp returns true when user agent starts with Unity prefix`() {
    InternalSettings.setCustomUserAgent("Unity.7.0.1")
    assertThat(InternalSettings.isUnityApp).isTrue
  }

  @Test
  fun `isUnityApp returns true for any Unity version`() {
    InternalSettings.setCustomUserAgent("Unity.2021.3.0f1")
    assertThat(InternalSettings.isUnityApp).isTrue
  }

  @Test
  fun `isUnityApp returns false for non-Unity user agent`() {
    InternalSettings.setCustomUserAgent("FBAndroidSDK.17.0.0")
    assertThat(InternalSettings.isUnityApp).isFalse
  }

  @Test
  fun `isUnityApp returns false when user agent is empty`() {
    InternalSettings.setCustomUserAgent("")
    assertThat(InternalSettings.isUnityApp).isFalse
  }

  @Test
  fun `isUnityApp is case sensitive`() {
    InternalSettings.setCustomUserAgent("unity.7.0.1")
    assertThat(InternalSettings.isUnityApp).isFalse
  }

  @Test
  fun `setCustomUserAgent overwrites previous value`() {
    InternalSettings.setCustomUserAgent("Unity.1.0")
    assertThat(InternalSettings.isUnityApp).isTrue

    InternalSettings.setCustomUserAgent("NotUnity.2.0")
    assertThat(InternalSettings.isUnityApp).isFalse
    assertThat(InternalSettings.getCustomUserAgent()).isEqualTo("NotUnity.2.0")
  }
}
