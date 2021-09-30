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
