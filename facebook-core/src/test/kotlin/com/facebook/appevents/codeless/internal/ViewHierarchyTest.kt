/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.codeless.internal

import android.content.Context
import android.view.View
import android.view.ViewParent
import android.widget.Adapter
import android.widget.AdapterView
import androidx.core.view.NestedScrollingChild
import com.facebook.appevents.codeless.CodelessTestBase
import com.facebook.appevents.codeless.internal.ViewHierarchy.getDictionaryOfView
import com.facebook.internal.Utility.sha256hash
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ViewHierarchy::class)
class ViewHierarchyTest : CodelessTestBase() {
  private lateinit var mockView: View
  private lateinit var mockTestNestedScrollingChild: TestNestedScrollingChild
  private lateinit var mockTestAdapterView: TestAdapterView
  private lateinit var mockViewParent: ViewParent

  @Before
  override fun setup() {
    super.setup()
    mockView = mock()
    mockTestNestedScrollingChild = mock()
    mockTestAdapterView = mock()
    mockViewParent = mock()
  }

  @Test
  fun testGetDictionaryOfView() {
    val dict = getDictionaryOfView(root)
    val outerText = dict.getJSONArray("childviews").getJSONObject(0).getString("text")
    Assert.assertTrue(outerText.equals(sha256hash("Outer Label"), ignoreCase = true))
    val innerText =
        dict.getJSONArray("childviews")
            .getJSONObject(1)
            .getJSONArray("childviews")
            .getJSONObject(0)
            .getString("text")
    Assert.assertTrue(innerText.equals(sha256hash("Inner Label"), ignoreCase = true))
  }

  abstract class TestAdapterView(context: Context?) : AdapterView<Adapter>(context), ViewParent
  abstract class TestNestedScrollingChild : ViewParent, NestedScrollingChild

  @Test
  fun testIsAdapterViewItem() {
    PowerMockito.spy(ViewHierarchy::class.java)
    val isAdapterViewItem =
        ViewHierarchy::class.java.getDeclaredMethod("isAdapterViewItem", View::class.java)
    isAdapterViewItem.isAccessible = true

    // mock NestedScrollingChild -> true
    whenever(mockView.parent).thenReturn(mockTestNestedScrollingChild)
    Assert.assertTrue(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean)

    // mock AdapterView -> true
    whenever(mockView.parent).thenReturn(mockTestAdapterView)
    Assert.assertTrue(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean)

    // mock other cases -> false
    whenever(mockView.parent).thenReturn(mockViewParent)
    Assert.assertFalse(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean)
  }
}
