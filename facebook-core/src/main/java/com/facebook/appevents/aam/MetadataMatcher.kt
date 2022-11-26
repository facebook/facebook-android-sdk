/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.aam

import android.content.res.Resources
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
internal object MetadataMatcher {
  private const val MAX_INDICATOR_LENGTH = 100

  @JvmStatic
  fun getCurrentViewIndicators(view: View): List<String> {
    val indicators: MutableList<String> = mutableListOf()
    // Hint
    indicators.add(ViewHierarchy.getHintOfView(view))
    // tag
    val tag = view.tag
    if (tag != null) {
      indicators.add(tag.toString())
    }
    // description
    val description = view.contentDescription
    if (description != null) {
      indicators.add(description.toString())
    }
    // resource id name
    try {
      if (view.id != -1) {
        // resource name format: {package_name}:id/{id_name}
        val resourceName = view.resources.getResourceName(view.id)
        val splitted = resourceName.split("/".toRegex()).toTypedArray()
        if (splitted.size == 2) {
          indicators.add(splitted[1])
        }
      }
    } catch (_e: Resources.NotFoundException) {
      /*no op*/
    }
    val validIndicators: MutableList<String> = mutableListOf()
    for (indicator in indicators) {
      if (indicator.isNotEmpty() && indicator.length <= MAX_INDICATOR_LENGTH) {
        validIndicators.add(indicator.toLowerCase())
      }
    }
    return validIndicators
  }

  @JvmStatic
  fun getAroundViewIndicators(view: View): List<String> {
    val aroundTextIndicators: MutableList<String> = mutableListOf()
    val parentView: View? = ViewHierarchy.getParentOfView(view)
    if (parentView != null) {
      for (child in ViewHierarchy.getChildrenOfView(parentView)) {
        if (view !== child) {
          aroundTextIndicators.addAll(getTextIndicators(child))
        }
      }
    }
    return aroundTextIndicators
  }

  @JvmStatic
  fun matchIndicator(indicators: List<String>, keys: List<String>): Boolean {
    for (indicator in indicators) {
      if (matchIndicator(indicator, keys)) {
        return true
      }
    }
    return false
  }

  private fun matchIndicator(indicator: String, keys: List<String>): Boolean {
    for (key in keys) {
      if (indicator.contains(key)) {
        return true
      }
    }
    return false
  }

  @JvmStatic
  fun matchValue(text: String, rule: String): Boolean {
    return text.matches(Regex(rule))
  }

  private fun getTextIndicators(view: View): List<String> {
    val indicators: MutableList<String> = mutableListOf()
    if (view is EditText) {
      return indicators
    }
    if (view is TextView) {
      val text = view.text.toString()
      if (text.isNotEmpty() && text.length < MAX_INDICATOR_LENGTH) {
        indicators.add(text.toLowerCase())
      }
      return indicators
    }
    val children = ViewHierarchy.getChildrenOfView(view)
    for (child in children) {
      indicators.addAll(getTextIndicators(child))
    }
    return indicators
  }
}
