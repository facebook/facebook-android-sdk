/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Intent
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.FacebookActivity
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(NativeProtocol::class, Utility::class)
class FacebookDialogFragmentTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: FacebookActivity
  private lateinit var mockDialog: WebDialog
  private lateinit var fragment: FacebookDialogFragment

  @Before
  fun init() {
    mockActivity = mock()
    whenever(mockActivity.intent).thenReturn(Intent())

    PowerMockito.mockStatic(NativeProtocol::class.java)
    PowerMockito.mockStatic(Utility::class.java)

    val mockCompanion: AccessToken.Companion = mock()
    WhiteboxImpl.setInternalState(AccessToken::class.java, "Companion", mockCompanion)
    val mockToken: AccessToken = mock()
    whenever(mockCompanion.getCurrentAccessToken()).thenReturn(mockToken)
    whenever(mockCompanion.isCurrentAccessTokenActive()).thenReturn(true)
    whenever(Utility.getMetadataApplicationId(any())).thenReturn("123")

    mockDialog = mock()
    val mockWebDialogCompanion: WebDialog.Companion = mock()
    WhiteboxImpl.setInternalState(WebDialog::class.java, "Companion", mockWebDialogCompanion)
    whenever(mockWebDialogCompanion.newInstance(any(), any(), any(), any(), any()))
        .thenReturn(mockDialog)

    fragment = spy(FacebookDialogFragment())
    whenever(fragment.activity).thenReturn(mockActivity)
  }

  @Test
  fun `test init dialog`() {
    val params = Bundle()
    params.putString(NativeProtocol.WEB_DIALOG_ACTION, "auth")
    params.putBundle(NativeProtocol.WEB_DIALOG_PARAMS, Bundle())
    whenever(NativeProtocol.getMethodArgumentsFromIntent(any())).thenReturn(params)

    fragment.initDialog()
    val dialog = fragment.innerDialog
    assertThat(dialog).isEqualTo(mockDialog)
  }
}
