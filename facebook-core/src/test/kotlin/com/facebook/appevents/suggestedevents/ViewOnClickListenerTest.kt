/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.os.Bundle
import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.appevents.codeless.internal.ViewHierarchy
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    ViewHierarchy::class,
    PredictionHistoryManager::class,
    SuggestedEventViewHierarchy::class,
    SuggestedEventsManager::class)
class ViewOnClickListenerTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  private lateinit var mockGraphRequest: GraphRequest
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  private lateinit var param: Bundle
  private lateinit var mockHostView: View
  private lateinit var mockRootView: View
  private lateinit var mockViewListener: View.OnClickListener
  private var setListenerTimes = 0
  private var getListenerTimes = 0
  private lateinit var viewOnClickListener: ViewOnClickListener

  companion object {
    private const val APP_ID = "123"
    private const val EVENT_NAME = "some event"
    private const val PATH_ID = "path_id_123"
    private const val VIEW_TEXT = "view text"
  }
  @Before
  fun init() {
    mockGraphRequest = mock()
    mockGraphRequestCompanion = mock()
    mockHostView = mock()
    mockRootView = mock()
    mockViewListener = mock()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    PowerMockito.mockStatic(PredictionHistoryManager::class.java)
    PowerMockito.mockStatic(SuggestedEventViewHierarchy::class.java)
    PowerMockito.mockStatic(SuggestedEventsManager::class.java)

    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    whenever(PredictionHistoryManager.queryEvent(any())).thenReturn(EVENT_NAME)
    whenever(PredictionHistoryManager.getPathID(any(), any())).thenReturn(PATH_ID)
    whenever(SuggestedEventViewHierarchy.getTextOfViewRecursively(any())).thenReturn(VIEW_TEXT)
    whenever(SuggestedEventsManager.isProductionEvents(any())).thenReturn(false)
    whenever(SuggestedEventsManager.isEligibleEvents(any())).thenReturn(true)

    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    PowerMockito.`when`(
            mockGraphRequestCompanion.newPostRequest(isNull(), any(), isNull(), isNull()))
        .thenReturn(mockGraphRequest)
    whenever(mockGraphRequest::parameters.set(any())).thenAnswer {
      param = it.arguments[0] as Bundle
      Unit
    }

    whenever(ViewHierarchy.setOnClickListener(eq(mockHostView), any())).thenAnswer {
      viewOnClickListener = it.arguments[1] as ViewOnClickListener
      setListenerTimes++
      Unit
    }

    whenever(ViewHierarchy.getExistingOnClickListener(eq(mockHostView))).thenAnswer {
      getListenerTimes++
      mockViewListener
    }
  }
  @Test
  fun testAttachAndClick() {
    ViewOnClickListener.attachListener(mockHostView, mockRootView, "activity_name")

    assertThat(setListenerTimes).isEqualTo(1)
    assertThat(getListenerTimes).isEqualTo(1)
    assertThat(viewOnClickListener).isNotNull

    val mockView: View = mock()
    viewOnClickListener?.onClick(mockView)
    verify(mockViewListener).onClick(mockView)
    verify(mockGraphRequest).executeAndWait()
    assertThat(param).isNotNull
    assertThat(param?.get("event_name")).isEqualTo(EVENT_NAME)
    assertThat(param?.get("metadata"))
        .isEqualTo(JSONObject(mapOf("dense" to "", "button_text" to VIEW_TEXT)).toString())
  }
}
