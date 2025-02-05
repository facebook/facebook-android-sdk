/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.common.R
import com.facebook.internal.ServerProtocol.INSTAGRAM_OAUTH_PATH
import com.facebook.login.CustomTabLoginMethodHandler
import com.facebook.login.LoginTargetApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, WebDialog::class, Utility::class)
class WebDialogTest : FacebookPowerMockTestCase() {
    companion object {
        const val APP_ID = "123456789"
        const val GRAPH_API_VERSION = "v12"
        const val SDK_VERSION = "12.1.0"
        const val FB_DOMAIN = "facebook.com"
        const val IG_DOMAIN = "instagram.com"
        const val ACTION = CustomTabLoginMethodHandler.OAUTH_DIALOG
    }

    private lateinit var mockContext: Context
    private lateinit var listener: WebDialog.OnCompleteListener

    @Before
    fun init() {
        PowerMockito.mockStatic(FacebookSdk::class.java)
        PowerMockito.mockStatic(Utility::class.java)
        whenever(FacebookSdk.isInitialized()).thenReturn(true)
        whenever(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
        whenever(FacebookSdk.getGraphApiVersion()).thenReturn(GRAPH_API_VERSION)
        whenever(FacebookSdk.getSdkVersion()).thenReturn(SDK_VERSION)
        whenever(FacebookSdk.getFacebookDomain()).thenReturn(FB_DOMAIN)
        whenever(FacebookSdk.getInstagramDomain()).thenReturn(IG_DOMAIN)
        whenever(Utility.isChromeOS(any())).thenReturn(true)
        whenever(Utility.buildUri(any(), any(), any())).thenCallRealMethod()

        mockContext = mock()
        val mockPackageManger = mock<PackageManager>()
        val mockApplicationInfo = mock<ApplicationInfo>()
        whenever(mockContext.packageManager).thenReturn(mockPackageManger)
        whenever(mockContext.packageName).thenReturn("com.facebook.internal")
        whenever(mockPackageManger.getApplicationInfo(any<String>(), any<Int>())).thenReturn(
            mockApplicationInfo
        )
        val meta = Bundle()
        meta.putInt(FacebookSdk.WEB_DIALOG_THEME, R.style.com_facebook_auth_dialog)
        mockApplicationInfo.metaData = meta

        listener =
            object : WebDialog.OnCompleteListener {
                override fun onComplete(values: Bundle?, error: FacebookException?) = Unit
            }
    }

    @Test
    fun `test create new auth dialog`() {
        val dialog = WebDialog.newInstance(mockContext, ACTION, Bundle(), 0, listener)
        assertThat(dialog).isNotNull
        assertThat(WebDialog.getWebDialogTheme()).isEqualTo(R.style.com_facebook_auth_dialog)
        val url = Whitebox.getInternalState<String>(dialog, "url")
        assertThat(url)
            .isEqualTo(
                "https://m.$FB_DOMAIN/$GRAPH_API_VERSION/dialog/$ACTION?client_id=$APP_ID&sdk=android-$SDK_VERSION&redirect_uri=fbconnect%3A%2F%2Fchrome_os_success&display=touch"
            )
    }

    @Test
    fun `test create new auth dialog with IG App`() {
        val dialog =
            WebDialog.newInstance(
                mockContext,
                ACTION,
                Bundle(),
                0,
                LoginTargetApp.INSTAGRAM,
                listener
            )
        assertThat(dialog).isNotNull
        assertThat(WebDialog.getWebDialogTheme()).isEqualTo(R.style.com_facebook_auth_dialog)
        val url = Whitebox.getInternalState<String>(dialog, "url")
        assertThat(url)
            .isEqualTo(
                "https://m.$IG_DOMAIN/$INSTAGRAM_OAUTH_PATH?client_id=$APP_ID&sdk=android-$SDK_VERSION&redirect_uri=fbconnect%3A%2F%2Fchrome_os_success&display=touch"
            )
    }
}
