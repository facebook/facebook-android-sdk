package com.facebook.appevents.suggestedevents

import android.app.Activity
import android.view.View
import android.widget.EditText
import android.widget.Switch
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.ViewHierarchyConstants.CHILDREN_VIEW_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_TYPE_BITMASK_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.HINT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.INPUT_TYPE_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.IS_INTERACTED_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import java.util.Collections.emptyList
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
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
    val mockListener: View.OnClickListener = mock<View.OnClickListener>()
    clickableView.setOnClickListener(mockListener)
    assertEquals(
        listOf<View>(clickableView),
        SuggestedEventViewHierarchy.getAllClickableViews(clickableView))

    // Test with inner children views
    val view1 = View(context)
    val view2 = EditText(context)
    val childrenViews = listOf<View>(view1, view2)
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    whenever(ViewHierarchy.getChildrenOfView(clickableView)).thenReturn(childrenViews)
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
    val mockListener: View.OnClickListener = mock<View.OnClickListener>()
    clickableView.setOnClickListener(mockListener)

    val obj1 = SuggestedEventViewHierarchy.getDictionaryOfView(view, clickableView)
    assertEquals(
        "{\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":0,\"${CHILDREN_VIEW_KEY}\":[]}",
        obj1.toString())

    val obj2 = SuggestedEventViewHierarchy.getDictionaryOfView(clickableView, clickableView)
    assertEquals(
        "{\"${IS_INTERACTED_KEY}\":true,\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":32,\"${CHILDREN_VIEW_KEY}\":[]}",
        obj2.toString())

    // Test with inner children views
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    whenever(ViewHierarchy.getChildrenOfView(view)).thenReturn(listOf<View>(clickableView))
    whenever(ViewHierarchy.getTextOfView(any())).thenReturn("")
    whenever(ViewHierarchy.getHintOfView(any())).thenReturn("")
    val obj3 = SuggestedEventViewHierarchy.getDictionaryOfView(view, clickableView)
    assertEquals(
        "{\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":0,\"${CHILDREN_VIEW_KEY}\":[{\"${IS_INTERACTED_KEY}\":true,\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":0,\"${CHILDREN_VIEW_KEY}\":[]}]}",
        obj3.toString())
  }

  @Test
  fun `test update basic info`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()

    val view = View(context)
    val json = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(view, json)
    assertEquals(
        "{\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":0}", json.toString())

    PowerMockito.mockStatic(ViewHierarchy::class.java)
    whenever(ViewHierarchy.getTextOfView(any())).thenReturn("Some Text")
    whenever(ViewHierarchy.getHintOfView(any())).thenReturn("Some Hint")
    whenever(ViewHierarchy.getClassTypeBitmask(any())).thenReturn(100)

    val viewWithText = View(context)
    val jsonWithText = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(viewWithText, jsonWithText)
    assertEquals(
        "{\"${CLASS_NAME_KEY}\":\"View\",\"${CLASS_TYPE_BITMASK_KEY}\":100,\"${TEXT_KEY}\":\"Some Text\",\"${HINT_KEY}\":\"Some Hint\"}",
        jsonWithText.toString())

    val editText = EditText(context)
    val editTextJson = JSONObject()
    SuggestedEventViewHierarchy.updateBasicInfo(editText, editTextJson)
    assertEquals(
        "{\"${CLASS_NAME_KEY}\":\"EditText\",\"${CLASS_TYPE_BITMASK_KEY}\":100,\"${TEXT_KEY}\":\"Some Text\",\"${HINT_KEY}\":\"Some Hint\",\"${INPUT_TYPE_KEY}\":131073}",
        editTextJson.toString())
  }

  @Test
  fun `test get text Of view recursively`() {
    val context = Robolectric.buildActivity(Activity::class.java).get()
    PowerMockito.mockStatic(ViewHierarchy::class.java)

    val blankView = View(context)
    whenever(ViewHierarchy.getTextOfView(blankView)).thenReturn("")
    assertEquals("", SuggestedEventViewHierarchy.getTextOfViewRecursively(blankView))

    val viewWithText = View(context)
    whenever(ViewHierarchy.getTextOfView(viewWithText)).thenReturn("Some Text")
    assertEquals("Some Text", SuggestedEventViewHierarchy.getTextOfViewRecursively(viewWithText))

    whenever(ViewHierarchy.getChildrenOfView(blankView)).thenReturn(listOf<View>(viewWithText))
    assertEquals("Some Text", SuggestedEventViewHierarchy.getTextOfViewRecursively(blankView))
  }
}
