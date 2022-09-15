package com.facebook.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.common.R
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookWebFallbackDialog::class, FacebookSdk::class)
class FacebookWebFallbackDialogTest : FacebookPowerMockTestCase() {
  private lateinit var fallbackDialog: FacebookWebFallbackDialog
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    val mockContext: Context = mock()
    val mockPackageManger = mock<PackageManager>()
    val mockApplicationInfo = mock<ApplicationInfo>()

    whenever(mockContext.packageManager).thenReturn(mockPackageManger)
    whenever(mockContext.packageName).thenReturn("com.facebook.internal")
    whenever(mockPackageManger.getApplicationInfo(any(), any())).thenReturn(mockApplicationInfo)
    val meta = Bundle()
    meta.putInt(FacebookSdk.WEB_DIALOG_THEME, R.style.com_facebook_auth_dialog)
    mockApplicationInfo.metaData = meta

    fallbackDialog = FacebookWebFallbackDialog.newInstance(mockContext, "", "")
  }

  @Test
  fun `test parse response url`() {
    val bundle = fallbackDialog.parseResponseUri("$BASE_URL${buildParamStr(params)}")
    params.forEach { (k, v) -> assertThat(bundle[k]).isEqualTo(v) }
    assertThat(bundle[NativeProtocol.EXTRA_PROTOCOL_VERSION])
        .isEqualTo(NativeProtocol.getLatestKnownVersion())
  }

  @Test
  fun `test parse response url when has bridge arguments`() {
    val obj = JSONObject(mapOf("a" to "b"))
    val bridgeParams = "${ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS}=${obj}"
    val bundle = fallbackDialog.parseResponseUri("$BASE_URL${buildParamStr(params)}&$bridgeParams")

    params.forEach { (k, v) -> assertThat(bundle[k]).isEqualTo(v) }
    assertThat(bundle[NativeProtocol.EXTRA_PROTOCOL_VERSION])
        .isEqualTo(NativeProtocol.getLatestKnownVersion())

    assertThat(bundle.containsKey(ServerProtocol.FALLBACK_DIALOG_PARAM_BRIDGE_ARGS)).isFalse
    assertThat(bundle[NativeProtocol.EXTRA_PROTOCOL_BRIDGE_ARGS].toString())
        .isEqualTo(BundleJSONConverter.convertToBundle(obj).toString())
  }

  @Test
  fun `test parse response url when has method result`() {
    val obj = JSONObject(mapOf("a" to "b"))
    val methodResultParams = "${ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS}=${obj}"
    val bundle =
        fallbackDialog.parseResponseUri("$BASE_URL${buildParamStr(params)}&$methodResultParams")

    params.forEach { (k, v) -> assertThat(bundle[k]).isEqualTo(v) }
    assertThat(bundle[NativeProtocol.EXTRA_PROTOCOL_VERSION])
        .isEqualTo(NativeProtocol.getLatestKnownVersion())

    assertThat(bundle.containsKey(ServerProtocol.FALLBACK_DIALOG_PARAM_METHOD_RESULTS)).isFalse
    assertThat(bundle[NativeProtocol.EXTRA_PROTOCOL_METHOD_RESULTS].toString())
        .isEqualTo(BundleJSONConverter.convertToBundle(obj).toString())
  }

  @Test
  fun `test cancel`() {
    val mockWebView: WebView = mock()
    whenever(mockWebView.isShown).thenReturn(true)
    WhiteboxImpl.setInternalState(fallbackDialog, "webView", mockWebView)
    WhiteboxImpl.setInternalState(fallbackDialog, "isPageFinished", true)
    WhiteboxImpl.setInternalState(fallbackDialog, "waitingForDialogToClose", false)

    fallbackDialog.cancel()
    verify(mockWebView).loadUrl(any())
  }

  companion object {
    const val BASE_URL = "https://m.facebook.com/v12.0/dialog/oauth?"
    private val params = mapOf("client_id" to "123", "sdk" to "android-12.0", "scope" to "openid")
    fun buildParamStr(params: Map<String, String>): String {
      return params.map { (k, v) -> "${k}=${v}" }.joinToString("&")
    }
  }
}
