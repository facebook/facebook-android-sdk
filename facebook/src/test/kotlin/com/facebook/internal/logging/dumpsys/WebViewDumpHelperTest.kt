package com.facebook.internal.logging.dumpsys

import android.content.res.Resources
import android.util.DisplayMetrics
import android.webkit.ValueCallback
import android.webkit.WebView
import com.facebook.FacebookTestCase
import com.nhaarman.mockitokotlin2.any
import java.io.PrintWriter
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.`when`

class WebViewDumpHelperTest : FacebookTestCase() {
  private lateinit var mockWebView: WebView
  private lateinit var mockResources: Resources
  private lateinit var mockDisplayMetrics: DisplayMetrics

  private lateinit var dumpHelper: WebViewDumpHelper

  @Before
  fun before() {
    mockWebView = mock(WebView::class.java)
    mockResources = mock(Resources::class.java)
    mockDisplayMetrics = mock(DisplayMetrics::class.java)
    mockDisplayMetrics.scaledDensity = 1.0f
    `when`(mockResources.displayMetrics).thenReturn(mockDisplayMetrics)
    `when`(mockWebView.resources).thenReturn(mockResources)

    dumpHelper = WebViewDumpHelper()
  }

  @Test
  fun `test dump before handle`() {
    val out = StringWriter()
    val writer = PrintWriter(out)

    dumpHelper.dump(writer)
    writer.flush()
    assertEquals("", out.toString())
  }

  @Test
  fun `test handle and then dump`() {
    var scriptWithOffset: String? = null
    var valueCallback: ValueCallback<String>? = null
    `when`(mockWebView.evaluateJavascript(any<String>(), any())).then {
      scriptWithOffset = it.arguments[0] as String
      valueCallback = it.arguments[1] as ValueCallback<String>
      return@then 0
    }
    dumpHelper.handle(mockWebView)

    assertNotNull(scriptWithOffset)
    assertTrue((scriptWithOffset ?: "").contains("const leftOf = 0"))
    assertTrue((scriptWithOffset ?: "").contains("const topOf = 0"))
    assertTrue((scriptWithOffset ?: "").contains("const density = 1.000000"))

    assertNotNull(valueCallback)
    valueCallback?.onReceiveValue("<html></html>")

    val out = StringWriter()
    val writer = PrintWriter(out)
    dumpHelper.dump(writer)
    writer.flush()
    assertTrue(out.toString().contains("WebView HTML for "))
  }
}
