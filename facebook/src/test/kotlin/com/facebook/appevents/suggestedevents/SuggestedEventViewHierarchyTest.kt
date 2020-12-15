package com.facebook.appevents.suggestedevents

import android.app.Activity
import android.view.View
import android.widget.EditText
import android.widget.Switch
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.ViewHierarchy
import java.util.Collections.emptyList
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.Robolectric

@PrepareForTest(ViewHierarchy::class)
class SuggestedEventViewHierarchyTest : FacebookPowerMockTestCase() {
  @Test
  fun `test get all clickable views`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()

    val blacklistView = Switch(context)
    assertEquals(emptyList<View>(), SuggestedEventViewHierarchy.getAllClickableViews(blacklistView))

    val clickableView = View(context)
    val mockListener: View.OnClickListener = mock(View.OnClickListener::class.java)
    clickableView.setOnClickListener(mockListener)
    assertEquals(
        listOf<View>(clickableView),
        SuggestedEventViewHierarchy.getAllClickableViews(clickableView))

    // Test with inner children views
    val view1 = View(context)
    val view2 = EditText(context)
    val childrenViews = listOf<View>(view1, view2)
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    PowerMockito.`when`(ViewHierarchy.getChildrenOfView(clickableView)).thenReturn(childrenViews)
    assertEquals(
        listOf<View>(clickableView),
        SuggestedEventViewHierarchy.getAllClickableViews(clickableView))

    // Test with inner children views, and one child is clickable view
    view1.setOnClickListener(mockListener)
    assertEquals(
        listOf<View>(clickableView, view1),
        SuggestedEventViewHierarchy.getAllClickableViews(clickableView))
  }

  @Test
  fun `test get dictionary Of view`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()

    val view = View(context)
    val clickableView = View(context)
    val mockListener: View.OnClickListener = mock(View.OnClickListener::class.java)
    clickableView.setOnClickListener(mockListener)

    val obj1 = SuggestedEventViewHierarchy.getDictionaryOfView(view, clickableView)
    assertEquals(
        "{\"classname\":\"View\",\"classtypebitmask\":0,\"childviews\":[]}", obj1.toString())

    val obj2 = SuggestedEventViewHierarchy.getDictionaryOfView(clickableView, clickableView)
    assertEquals(
        "{\"is_interacted\":true,\"classname\":\"View\",\"classtypebitmask\":32,\"childviews\":[]}",
        obj2.toString())

    // Test with inner children views
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    PowerMockito.`when`(ViewHierarchy.getChildrenOfView(view))
        .thenReturn(listOf<View>(clickableView))
    PowerMockito.`when`(ViewHierarchy.getTextOfView(isA(View::class.java))).thenReturn("")
    PowerMockito.`when`(ViewHierarchy.getHintOfView(isA(View::class.java))).thenReturn("")
    val obj3 = SuggestedEventViewHierarchy.getDictionaryOfView(view, clickableView)
    assertEquals(
        "{\"classname\":\"View\",\"classtypebitmask\":0,\"childviews\":[{\"is_interacted\":true,\"classname\":\"View\",\"classtypebitmask\":0,\"childviews\":[]}]}",
        obj3.toString())
  }

  @Test
  fun `test update basic info`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()

    val view = View(context)
    val json = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(view, json)
    assertEquals("{\"classname\":\"View\",\"classtypebitmask\":0}", json.toString())

    PowerMockito.mockStatic(ViewHierarchy::class.java)
    PowerMockito.`when`(ViewHierarchy.getTextOfView(isA(View::class.java))).thenReturn("Some Text")
    PowerMockito.`when`(ViewHierarchy.getHintOfView(isA(View::class.java))).thenReturn("Some Hint")
    PowerMockito.`when`(ViewHierarchy.getClassTypeBitmask(isA(View::class.java))).thenReturn(100)

    val viewWithText = View(context)
    val jsonWithText = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(viewWithText, jsonWithText)
    assertEquals(
        "{\"classname\":\"View\",\"classtypebitmask\":100,\"text\":\"Some Text\",\"hint\":\"Some Hint\"}",
        jsonWithText.toString())

    val editText = EditText(context)
    val editTextJson = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(editText, editTextJson)
    assertEquals(
        "{\"classname\":\"EditText\",\"classtypebitmask\":100,\"text\":\"Some Text\",\"hint\":\"Some Hint\",\"inputtype\":131073}",
        editTextJson.toString())
  }

  @Test
  fun `test get text Of view recursively`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()
    PowerMockito.mockStatic(ViewHierarchy::class.java)

    val blankView = View(context)
    PowerMockito.`when`(ViewHierarchy.getTextOfView(blankView)).thenReturn("")
    assertEquals("", SuggestedEventViewHierarchy.getTextOfViewRecursively(blankView))

    val viewWithText = View(context)
    PowerMockito.`when`(ViewHierarchy.getTextOfView(viewWithText)).thenReturn("Some Text")
    assertEquals("Some Text", SuggestedEventViewHierarchy.getTextOfViewRecursively(viewWithText))

    PowerMockito.`when`(ViewHierarchy.getChildrenOfView(blankView))
        .thenReturn(listOf<View>(viewWithText))
    assertEquals("Some Text", SuggestedEventViewHierarchy.getTextOfViewRecursively(blankView))
  }
}
