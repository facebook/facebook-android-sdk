/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LoginBehaviorTest : FacebookPowerMockTestCase() {

  @Test
  fun `test NATIVE_WITH_FALLBACK set correctly`() {
    val nativeWithFallback = LoginBehavior.NATIVE_WITH_FALLBACK
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isTrue
    assertThat(nativeWithFallback.allowsKatanaAuth()).isTrue
    assertThat(nativeWithFallback.allowsWebViewAuth()).isTrue
    assertThat(nativeWithFallback.allowsDeviceAuth()).isFalse
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isTrue
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isTrue
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isTrue
  }

  @Test
  fun `test NATIVE_ONLY set correctly`() {
    val nativeWithFallback = LoginBehavior.NATIVE_ONLY
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isTrue
    assertThat(nativeWithFallback.allowsKatanaAuth()).isTrue
    assertThat(nativeWithFallback.allowsWebViewAuth()).isFalse
    assertThat(nativeWithFallback.allowsDeviceAuth()).isFalse
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isFalse
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isTrue
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isTrue
  }

  @Test
  fun `test KATANA_ONLY set correctly`() {
    val nativeWithFallback = LoginBehavior.KATANA_ONLY
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isFalse
    assertThat(nativeWithFallback.allowsKatanaAuth()).isTrue
    assertThat(nativeWithFallback.allowsWebViewAuth()).isFalse
    assertThat(nativeWithFallback.allowsDeviceAuth()).isFalse
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isFalse
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isFalse
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isFalse
  }

  @Test
  fun `test WEB_ONLY set correctly`() {
    val nativeWithFallback = LoginBehavior.WEB_ONLY
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isFalse
    assertThat(nativeWithFallback.allowsKatanaAuth()).isFalse
    assertThat(nativeWithFallback.allowsWebViewAuth()).isTrue
    assertThat(nativeWithFallback.allowsDeviceAuth()).isFalse
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isTrue
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isFalse
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isFalse
  }

  @Test
  fun `test DIALOG_ONLY set correctly`() {
    val nativeWithFallback = LoginBehavior.DIALOG_ONLY
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isFalse
    assertThat(nativeWithFallback.allowsKatanaAuth()).isTrue
    assertThat(nativeWithFallback.allowsWebViewAuth()).isTrue
    assertThat(nativeWithFallback.allowsDeviceAuth()).isFalse
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isTrue
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isTrue
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isTrue
  }

  @Test
  fun `test DEVICE_AUTH set correctly`() {
    val nativeWithFallback = LoginBehavior.DEVICE_AUTH
    assertThat(nativeWithFallback.allowsGetTokenAuth()).isFalse
    assertThat(nativeWithFallback.allowsKatanaAuth()).isFalse
    assertThat(nativeWithFallback.allowsWebViewAuth()).isFalse
    assertThat(nativeWithFallback.allowsDeviceAuth()).isTrue
    assertThat(nativeWithFallback.allowsCustomTabAuth()).isFalse
    assertThat(nativeWithFallback.allowsFacebookLiteAuth()).isFalse
    assertThat(nativeWithFallback.allowsInstagramAppAuth()).isFalse
  }
}
