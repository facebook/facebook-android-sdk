/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.aam

import android.content.res.Resources
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.ViewHierarchy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ViewHierarchy::class)
class MetadataMatcherTest : FacebookPowerMockTestCase() {
  private lateinit var mockViewParent: ViewGroup
  private lateinit var mockViewChild1: EditText
  private lateinit var mockViewChild2: TextView
  @Before
  fun init() {
    mockViewParent = mock()
    mockViewChild1 = mock()
    mockViewChild2 = mock()
    PowerMockito.mockStatic(ViewHierarchy::class.java)

    whenever(ViewHierarchy.getHintOfView(mockViewParent)).thenReturn("hint")
    whenever(mockViewParent.tag).thenReturn("tag")
    whenever(mockViewParent.contentDescription).thenReturn("content")
    whenever(mockViewParent.id).thenReturn(0)
    val mockResource = mock<Resources>()
    whenever(mockResource.getResourceName(0)).thenReturn("123/resource")
    whenever(mockViewParent.resources).thenReturn(mockResource)

    whenever(mockViewChild2.text).thenReturn("TEXT")

    whenever(ViewHierarchy.getParentOfView(mockViewChild1)).thenReturn(mockViewParent)
    whenever(ViewHierarchy.getChildrenOfView(mockViewParent))
        .thenReturn(listOf(mockViewChild1, mockViewChild2))
  }

  @Test
  fun testGetCurrentViewIndicators() {
    val indicators = MetadataMatcher.getCurrentViewIndicators(mockViewParent)
    assertThat(indicators).isNotEmpty
    assertThat(indicators).isEqualTo(listOf("hint", "tag", "content", "resource"))
  }

  @Test
  fun testGetAroundViewIndicators() {
    val indicators = MetadataMatcher.getAroundViewIndicators(mockViewChild1)
    assertThat(indicators).isEqualTo(listOf("text"))
  }

  @Test
  fun testMatchIndicator() {
    assertThat(
            MetadataMatcher.matchIndicator(
                listOf("hint", "tag", "content", "resource"), listOf("key")))
        .isFalse
    assertThat(
            MetadataMatcher.matchIndicator(
                listOf("hint", "tag", "content", "resource"), listOf("content")))
        .isTrue
  }

  @Test
  fun testMatchValueWithRegex() {
    assertThat(MetadataMatcher.matchValue("test123", "\\w*\\d*")).isTrue
    assertThat(MetadataMatcher.matchValue("value;", "\\w*\\d*")).isFalse
  }
}
