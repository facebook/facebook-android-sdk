/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class AppLinkTest : FacebookPowerMockTestCase() {
  @Test
  fun `test constructor with null targets`() {
    val appLink = AppLink(mock(), null, mock())
    assertThat(appLink.targets).isEmpty()
  }

  @Test(expected = UnsupportedOperationException::class)
  fun `test targets getter return an unmodifiable copy`() {
    val targets = arrayListOf<AppLink.Target>(mock(), mock())
    val appLink = AppLink(mock(), targets, mock())
    val gotTargets = appLink.targets as MutableList<AppLink.Target>
    gotTargets.removeAt(0)
  }
}
