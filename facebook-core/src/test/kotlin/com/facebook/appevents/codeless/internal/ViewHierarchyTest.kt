/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito

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
    assertThat(outerText.equals(sha256hash("Outer Label"), ignoreCase = true)).isTrue
    val innerText =
        dict
            .getJSONArray("childviews")
            .getJSONObject(1)
            .getJSONArray("childviews")
            .getJSONObject(0)
            .getString("text")
    assertThat(innerText.equals(sha256hash("Inner Label"), ignoreCase = true)).isTrue
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
    assertThat(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean).isTrue

    // mock AdapterView -> true
    whenever(mockView.parent).thenReturn(mockTestAdapterView)
    assertThat(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean).isTrue

    // mock other cases -> false
    whenever(mockView.parent).thenReturn(mockViewParent)
    assertThat(isAdapterViewItem.invoke(ViewHierarchy::class.java, mockView) as Boolean).isFalse
  }
}
