package com.facebook.internal.logging.dumpsys

import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import java.io.PrintWriter
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(EndToEndDumpsysHelper::class)
class EndToEndDumpsysHelperTest : FacebookPowerMockTestCase() {

  private lateinit var mockWriter: PrintWriter
  private lateinit var mockE2EHelper: EndToEndDumpsysHelper
  private lateinit var mockRootResolver: AndroidRootResolver
  private lateinit var mockWebViewDumpHelper: WebViewDumpHelper
  private val prefix = "pre"
  @Before
  fun init() {
    mockWriter = PowerMockito.mock(PrintWriter::class.java)
    mockRootResolver = PowerMockito.mock(AndroidRootResolver::class.java)
    mockWebViewDumpHelper = PowerMockito.mock(WebViewDumpHelper::class.java)
    mockE2EHelper = spy(EndToEndDumpsysHelper())
    PowerMockito.whenNew(EndToEndDumpsysHelper::class.java)
        .withNoArguments()
        .thenReturn(mockE2EHelper)
    Whitebox.setInternalState(mockE2EHelper, "mRootResolver", mockRootResolver)
    Whitebox.setInternalState(mockE2EHelper, "mWebViewDumpHelper", mockWebViewDumpHelper)
    val mockRoot = PowerMockito.mock(AndroidRootResolver.Root::class.java)
    val mockView = PowerMockito.mock(View::class.java)
    PowerMockito.`when`(mockView.visibility).thenReturn(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
    PowerMockito.`when`(mockRoot.view).thenReturn(mockView)
    PowerMockito.`when`(mockRootResolver.listActiveRoots()).thenReturn(listOf(mockRoot))
  }

  @Test
  fun `test empty or invalid argument`() {
    assertFalse(EndToEndDumpsysHelper.maybeDump(prefix, mockWriter, null))
    assertFalse(EndToEndDumpsysHelper.maybeDump(prefix, mockWriter, emptyArray()))
  }

  @Test
  fun `test valid argument`() {
    assertTrue(EndToEndDumpsysHelper.maybeDump(prefix, mockWriter, arrayOf("e2e")))

    verify(mockWriter).print(eq(prefix))
    verify(mockWriter).println(eq("Top Level Window View Hierarchy:"))
    verify(mockRootResolver).listActiveRoots()
    verify(mockWebViewDumpHelper).dump(any())
  }
}
