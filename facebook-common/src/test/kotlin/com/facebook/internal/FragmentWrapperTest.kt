/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Intent
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FragmentWrapperTest : FacebookPowerMockTestCase() {
  @Test
  fun `test native fragment`() {
    val mockFragment = mock<android.app.Fragment>()
    val mockActivity = mock<android.app.Activity>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    val fragmentWrapper = FragmentWrapper(mockFragment)
    val mockIntent = mock<Intent>()
    fragmentWrapper.startActivityForResult(mockIntent, 0xface)
    verify(mockFragment).startActivityForResult(mockIntent, 0xface)
    assertThat(fragmentWrapper.activity).isSameAs(mockActivity)
  }

  @Test
  fun `test androidx fragment`() {
    val mockFragment = mock<androidx.fragment.app.Fragment>()
    val mockActivity = mock<androidx.fragment.app.FragmentActivity>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    val fragmentWrapper = FragmentWrapper(mockFragment)
    val mockIntent = mock<Intent>()
    fragmentWrapper.startActivityForResult(mockIntent, 0xface)
    verify(mockFragment).startActivityForResult(mockIntent, 0xface)
    assertThat(fragmentWrapper.activity).isSameAs(mockActivity)
  }
}
