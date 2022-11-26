/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TimePicker
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.ViewHierarchyConstants.CHILDREN_VIEW_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_TYPE_BITMASK_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.HINT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.INPUT_TYPE_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.IS_INTERACTED_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal object SuggestedEventViewHierarchy {
  private val blacklistedViews: List<Class<out View>> =
      listOf(
          Switch::class.java,
          Spinner::class.java,
          DatePicker::class.java,
          TimePicker::class.java,
          RadioGroup::class.java,
          RatingBar::class.java,
          EditText::class.java,
          AdapterView::class.java)

  @JvmStatic
  fun getDictionaryOfView(view: View, clickedView: View): JSONObject {
    val json = JSONObject()
    try {
      if (view === clickedView) {
        json.put(IS_INTERACTED_KEY, true)
      }
      updateBasicInfo(view, json)
      val childViews = JSONArray()
      val children = ViewHierarchy.getChildrenOfView(view)
      for (child in children) {
        val childInfo = getDictionaryOfView(child, clickedView)
        childViews.put(childInfo)
      }
      json.put(CHILDREN_VIEW_KEY, childViews)
    } catch (e: JSONException) {
      /*no op*/
    }
    return json
  }

  @JvmStatic
  fun updateBasicInfo(view: View, json: JSONObject) {
    try {
      val text = ViewHierarchy.getTextOfView(view)
      val hint = ViewHierarchy.getHintOfView(view)
      json.put(CLASS_NAME_KEY, view.javaClass.simpleName)
      json.put(CLASS_TYPE_BITMASK_KEY, ViewHierarchy.getClassTypeBitmask(view))
      if (text.isNotEmpty()) {
        json.put(TEXT_KEY, text)
      }
      if (hint.isNotEmpty()) {
        json.put(HINT_KEY, hint)
      }
      if (view is EditText) {
        json.put(INPUT_TYPE_KEY, view.inputType)
      }
    } catch (e: JSONException) {
      /*no op*/
    }
  }

  @JvmStatic
  fun getAllClickableViews(view: View): List<View> {
    val clickableViews: MutableList<View> = ArrayList()
    for (viewClass in blacklistedViews) {
      if (viewClass.isInstance(view)) {
        return clickableViews
      }
    }
    if (view.isClickable) {
      clickableViews.add(view)
    }
    val children = ViewHierarchy.getChildrenOfView(view)
    for (child in children) {
      clickableViews.addAll(getAllClickableViews(child))
    }
    return clickableViews
  }

  @JvmStatic
  fun getTextOfViewRecursively(hostView: View): String {
    val text = ViewHierarchy.getTextOfView(hostView)
    if (text.isNotEmpty()) {
      return text
    }
    val childrenText = getTextOfChildren(hostView)
    return TextUtils.join(" ", childrenText)
  }

  private fun getTextOfChildren(view: View): List<String?> {
    val childrenText: MutableList<String?> = ArrayList()
    val childrenView = ViewHierarchy.getChildrenOfView(view)
    for (childView in childrenView) {
      val childText = ViewHierarchy.getTextOfView(childView)
      if (childText.isNotEmpty()) {
        childrenText.add(childText)
      }
      childrenText.addAll(getTextOfChildren(childView))
    }
    return childrenText
  }
}
