package com.facebook.appevents.aam

import android.content.res.Resources
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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

    PowerMockito.`when`(ViewHierarchy.getHintOfView(mockViewParent)).thenReturn("hint")
    PowerMockito.`when`(mockViewParent.tag).thenReturn("tag")
    PowerMockito.`when`(mockViewParent.contentDescription).thenReturn("content")
    PowerMockito.`when`(mockViewParent.id).thenReturn(0)
    val mockResource = mock<Resources>()
    PowerMockito.`when`(mockResource.getResourceName(0)).thenReturn("123/resource")
    PowerMockito.`when`(mockViewParent.resources).thenReturn(mockResource)

    PowerMockito.`when`(mockViewChild2.text).thenReturn("TEXT")

    PowerMockito.`when`(ViewHierarchy.getParentOfView(mockViewChild1)).thenReturn(mockViewParent)
    PowerMockito.`when`(ViewHierarchy.getChildrenOfView(mockViewParent))
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
}
