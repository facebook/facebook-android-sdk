package com.facebook.internal

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.FacebookActivity
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
    val dialog = WhiteboxImpl.getInternalState<Dialog>(fragment, "dialog")
    assertThat(dialog).isEqualTo(mockDialog)
  }
}
