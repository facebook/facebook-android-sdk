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

package com.facebook.appevents.codeless

import android.app.Activity
import android.os.Handler
import android.view.View
import android.view.Window
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.EventBinding
import com.facebook.appevents.codeless.internal.ParameterComponent
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.collections.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(ViewHierarchy::class)
class CodelessMatcherTest : FacebookPowerMockTestCase() {
  private lateinit var mockHostView: View
  private lateinit var mockRootView: View
  private lateinit var mockActivity: Activity
  private lateinit var mockMapping: EventBinding
  private lateinit var parameterComponentList: ArrayList<ParameterComponent>
  private lateinit var matchedViews: ArrayList<CodelessMatcher.MatchedView>
  private lateinit var matchedView: CodelessMatcher.MatchedView
  private lateinit var mockCompanion: CodelessMatcher.ViewMatcher.Companion
  private val testName = "test_name"
  private val testValue = "test_value"
  private val path = "{'class_name': 'android.widget.LinearLayout'}"
  private val testViewText = "test_view_text"

  @Before
  fun init() {
    mockRootView = mock()
    mockHostView = mock()
    mockMapping = mock()
    mockActivity = mock()
    val mockActivityWindow = mock<Window>()
    whenever(mockActivity.window).thenReturn(mockActivityWindow)
    val mockDecorView = mock<View>()
    whenever(mockActivityWindow.decorView).thenReturn(mockDecorView)
    whenever(mockDecorView.rootView).thenReturn(mockRootView)

    parameterComponentList = ArrayList()
    matchedViews = ArrayList()
    matchedView = CodelessMatcher.MatchedView(mockRootView, "map_key")
    matchedViews.add(matchedView)

    mockStatic(ViewHierarchy::class.java)

    whenever(ViewHierarchy.getTextOfView(any())).thenReturn(testViewText)
    whenever(mockMapping.viewParameters).thenReturn(parameterComponentList)

    mockCompanion = mock()
    Whitebox.setInternalState(CodelessMatcher.ViewMatcher::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.findViewByPath(anyOrNull(), anyOrNull(), any(), any(), any(), any()))
        .thenReturn(matchedViews)
    // clear the singleton instance
    Whitebox.setInternalState(
        CodelessMatcher::class.java, "codelessMatcher", null as CodelessMatcher?)
  }

  @Test
  fun `when mapping is null`() {
    val bundle = CodelessMatcher.getParameters(null, mockRootView, mockHostView)
    assertThat(bundle.isEmpty).isTrue
  }

  @Test
  fun `when mapping is not null and ViewParameters is empty`() {
    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.isEmpty).isTrue
  }

  @Test
  fun `when ParameterComponent's value is not null`() {
    val parameterComponentJsonString = "{'name': $testName, 'value': $testValue}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.getString(testName)).isEqualTo(testValue)
  }

  @Test
  fun `when ParameterComponent's value is null and ParameterComponent's path is empty`() {
    val parameterComponentJsonString = "{'name': $testName}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.isEmpty).isTrue
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and MatchedView's view is null`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val mockMatchedView: CodelessMatcher.MatchedView = mock()
    whenever(mockMatchedView.getView()).thenReturn(null)
    val mockMatchedViews = ArrayList<CodelessMatcher.MatchedView>()
    mockMatchedViews.add(mockMatchedView)
    whenever(mockCompanion.findViewByPath(anyOrNull(), anyOrNull(), any(), any(), any(), any()))
        .thenReturn(mockMatchedViews)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.isEmpty).isTrue
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and MatchedView's view is not null`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.getString(testName)).isEqualTo(testViewText)
  }

  @Test
  fun `when ParameterComponent's value is null, ParameterComponent's path is not empty and multiple MatchedViews`() {
    val parameterComponentJsonString = "{'name': $testName, 'path': [ $path ]}"
    updateParameterComponentList(parameterComponentJsonString)

    val testViewText2 = "test_view_text_2"
    whenever(ViewHierarchy.getTextOfView(any())).thenReturn(testViewText, testViewText2)

    val matchedView2 = CodelessMatcher.MatchedView(mockRootView, "map_key")
    matchedViews.add(matchedView2)

    val bundle = CodelessMatcher.getParameters(mockMapping, mockRootView, mockHostView)
    assertThat(bundle.getString(testName)).isEqualTo(testViewText)
  }

  @Test
  fun `add an activity will start tracking the root view`() {
    val matcher = CodelessMatcher.getInstance()
    val mockUIHandler = mock<Handler>()
    Whitebox.setInternalState(matcher, "uiThreadHandler", mockUIHandler)

    matcher.add(mockActivity)

    val uiRunnableCaptor = argumentCaptor<Runnable>()
    val delayCaptor = argumentCaptor<Long>()
    verify(mockUIHandler).postDelayed(uiRunnableCaptor.capture(), delayCaptor.capture())
    assertThat(delayCaptor.firstValue).isGreaterThan(0)
    assertThat(uiRunnableCaptor.firstValue).isInstanceOf(CodelessMatcher.ViewMatcher::class.java)
  }

  @Test
  fun `add or remove an activity will add or remove the activity to activity set`() {
    val matcher = CodelessMatcher.getInstance()
    val mockUIHandler = mock<Handler>()
    Whitebox.setInternalState(matcher, "uiThreadHandler", mockUIHandler)
    val activities = Whitebox.getInternalState<MutableSet<Activity>>(matcher, "activitiesSet")

    matcher.add(mockActivity)
    assertThat(activities).isNotEmpty()

    matcher.remove(mockActivity)
    assertThat(activities).isEmpty()
  }

  @Test
  fun `remove an activity will save the listeners until it is destroyed`() {
    val matcher = CodelessMatcher.getInstance()
    val mockUIHandler = mock<Handler>()
    Whitebox.setInternalState(matcher, "uiThreadHandler", mockUIHandler)
    val listenerMap =
        Whitebox.getInternalState<HashMap<Int, HashSet<String>>>(matcher, "activityToListenerMap")

    matcher.add(mockActivity)
    matcher.remove(mockActivity)
    assertThat(listenerMap[mockActivity.hashCode()]).isNotNull

    matcher.destroy(mockActivity)
    assertThat(listenerMap[mockActivity.hashCode()]).isNull()
  }

  private fun updateParameterComponentList(parameterComponentJsonString: String) {
    val parameterComponent = ParameterComponent(JSONObject(parameterComponentJsonString))
    parameterComponentList.add(parameterComponent)
  }
}
