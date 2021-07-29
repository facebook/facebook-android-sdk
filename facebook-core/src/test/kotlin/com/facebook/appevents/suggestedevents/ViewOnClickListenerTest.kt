package com.facebook.appevents.suggestedevents

import android.os.Bundle
import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
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
  private var param: Bundle? = null
  private lateinit var mockHostView: View
  private lateinit var mockRootView: View
  private lateinit var mockViewListener: View.OnClickListener
  private var setListenerTimes = 0
  private var getListenerTimes = 0
  private var viewOnClickListener: ViewOnClickListener? = null

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

    PowerMockito.`when`(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    PowerMockito.`when`(PredictionHistoryManager.queryEvent(any())).thenReturn(EVENT_NAME)
    PowerMockito.`when`(PredictionHistoryManager.getPathID(any(), any())).thenReturn(PATH_ID)
    PowerMockito.`when`(SuggestedEventViewHierarchy.getTextOfViewRecursively(any()))
        .thenReturn(VIEW_TEXT)
    PowerMockito.`when`(SuggestedEventsManager.isProductionEvents(any())).thenReturn(false)
    PowerMockito.`when`(SuggestedEventsManager.isEligibleEvents(any())).thenReturn(true)

    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    PowerMockito.`when`(
            mockGraphRequestCompanion.newPostRequest(isNull(), any(), isNull(), isNull()))
        .thenReturn(mockGraphRequest)
    PowerMockito.`when`(mockGraphRequest::parameters.set(any())).thenAnswer {
      param = it.arguments[0] as Bundle
      Unit
    }

    PowerMockito.`when`(ViewHierarchy.setOnClickListener(eq(mockHostView), any())).thenAnswer {
      viewOnClickListener = it.arguments[1] as ViewOnClickListener
      setListenerTimes++
      Unit
    }

    PowerMockito.`when`(ViewHierarchy.getExistingOnClickListener(eq(mockHostView))).thenAnswer {
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
