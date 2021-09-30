/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved. <p> You are hereby granted a
 * non-exclusive, worldwide, royalty-free license to use, copy, modify, and distribute this software
 * in source code or binary form for use in connection with the web services and APIs provided by
 * Facebook. <p> As with any software that integrates with the Facebook platform, your use of this
 * software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
 * OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.codeless

import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.EventBinding
import com.facebook.appevents.codeless.internal.ParameterComponent
import com.facebook.appevents.codeless.internal.ViewHierarchy
import kotlin.collections.ArrayList
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(CodelessMatcher.ViewMatcher::class, ViewHierarchy::class)
class CodelessMatcherTest : FacebookPowerMockTestCase() {

  private lateinit var mockHostView: View
  private lateinit var mockRootView: View
  private lateinit var mockMapping: EventBinding
  private lateinit var parameterComponentList: ArrayList<ParameterComponent>
  private lateinit var matchedViews: ArrayList<CodelessMatcher.MatchedView>
  private lateinit var matchedView: CodelessMatcher.MatchedView

  private val testName = "test_name"
  private val testValue = "test_value"
  private val path = "{'class_name': 'android.widget.LinearLayout'}"
  private val testViewText = "test_view_text"

  @Before
  fun init() {
    mockRootView = mock(View::class.java)
    mockHostView = mock(View::class.java)
    mockMapping = mock(EventBinding::class.java)

    parameterComponentList = ArrayList()
    matchedViews = ArrayList()
    matchedView = CodelessMatcher.MatchedView(mockRootView, "map_key")
    matchedViews.add(matchedView)

    mockStatic(CodelessMatcher.ViewMatcher::class.java)
    mockStatic(ViewHierarchy::class.java)

    whenCalled(ViewHierarchy.getTextOfView(isA(View::class.java))).thenReturn(testViewText)
    whenCalled(mockMapping.viewParameters).thenReturn(parameterComponentList)
    whenCalled(
            CodelessMatcher.ViewMatcher.findViewByPath(
                isA(EventBinding::class.java),
                isA(View::class.java),
                anyList(),
                anyInt(),
                anyInt(),
                anyString()))
        .thenReturn(matchedViews)
  }

  @Test
  fun `when mapping is null`() {
    val bundle = CodelessMatcher.getParameters(null, mockRootView, mockHostView)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun `when mapping is not null and ViewParameters is empty`() {
    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun `when ParameterComponent's value is not null`() {
    val parameterComponentJsonString = "{'name': $testName, 'value': $testValue}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertEquals(testValue, bundle.getString(testName))
  }

  @Test
  fun `when ParameterComponent's value is null and ParameterComponent's path is empty`() {
    val parameterComponentJsonString = "{'name': $testName}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and MatchedView's view is null`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val mockMatchedView = mock(CodelessMatcher.MatchedView::class.java)
    whenCalled(mockMatchedView.view).thenReturn(null)
    val mockMatchedViews = ArrayList<CodelessMatcher.MatchedView>()
    mockMatchedViews.add(mockMatchedView)
    whenCalled(
            CodelessMatcher.ViewMatcher.findViewByPath(
                isA(EventBinding::class.java),
                isA(View::class.java),
                anyList(),
                anyInt(),
                anyInt(),
                anyString()))
        .thenReturn(mockMatchedViews)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertTrue(bundle.isEmpty)
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and MatchedView's view is not null`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertEquals(testViewText, bundle.getString(testName))
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and multiple MatchedViews`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val testViewText2 = "test_view_text_2"
    whenCalled(ViewHierarchy.getTextOfView(isA(View::class.java)))
        .thenReturn(testViewText, testViewText2)

    val matchedView2 = CodelessMatcher.MatchedView(mockRootView, "map_key")
    matchedViews.add(matchedView2)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertEquals(testViewText, bundle.getString(testName))
  }

  private fun updateParameterComponentList(parameterComponentJsonString: String) {
    val parameterComponent = ParameterComponent(JSONObject(parameterComponentJsonString))
    parameterComponentList.add(parameterComponent)
  }
}
