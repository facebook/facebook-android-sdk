/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock

class GetTokenClientTest : FacebookPowerMockTestCase() {

  @Test
  fun `test constructor make sure it can be created with correct input`() {
    val contextMock = mock(Context::class.java)
    val request =
        LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            null,
            DefaultAudience.FRIENDS,
            "rerequest",
            "1234",
            "5678",
            null,
            AuthenticationTokenTestUtil.NONCE,
            null,
            null,
            null)
    val getTokenClient = GetTokenClient(contextMock, request)
    assertThat(getTokenClient).isNotNull
    assertThat(getTokenClient.nonce).isEqualTo(AuthenticationTokenTestUtil.NONCE)
  }
}
