package com.facebook.appevents.suggestedevents

import android.content.Context
import android.view.ViewGroup
import android.widget.EditText
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.internal.Utility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class, PredictionHistoryManager::class, Utility::class, ViewHierarchy::class)
class PredictionHistoryManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val PATH_ID = "path123"
    private const val PATH_ID_NOT_ADDED = "path345"
    private const val EVENT_NAME = "some event"
    private const val TEXT = "view text"
  }
  private lateinit var mockContext: Context
  private lateinit var mockSharedPreferences: MockSharedPreference
  private lateinit var mockView: EditText
  private lateinit var mockParentView: ViewGroup
  @Before
  fun init() {
    mockContext = mock()
    mockView = mock()
    mockParentView = mock()
    mockSharedPreferences = MockSharedPreference()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(Utility::class.java)
    PowerMockito.mockStatic(ViewHierarchy::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    PowerMockito.`when`(
            mockContext.getSharedPreferences(
                "com.facebook.internal.SUGGESTED_EVENTS_HISTORY", Context.MODE_PRIVATE))
        .thenReturn(mockSharedPreferences)
  }

  @Test
  fun testAddPrediction() {
    Whitebox.setInternalState(
        PredictionHistoryManager::class.java, "initialized", AtomicBoolean(false))
    PredictionHistoryManager.addPrediction(PATH_ID, EVENT_NAME)

    val initialized =
        Whitebox.getInternalState<AtomicBoolean>(
            PredictionHistoryManager::class.java, "initialized")
    assertThat(initialized.get()).isTrue

    assertThat(PredictionHistoryManager.queryEvent(PATH_ID)).isEqualTo(EVENT_NAME)
    assertThat(PredictionHistoryManager.queryEvent(PATH_ID_NOT_ADDED)).isNull()
  }

  @Test
  fun testGetPathID() {
    var pathRouteStr: String? = null
    val expectedPathRoute =
        JSONObject(
            mapOf(
                TEXT_KEY to TEXT,
                CLASS_NAME_KEY to
                    JSONArray(
                        listOf(
                            mockView.javaClass.simpleName, mockParentView.javaClass.simpleName))))
    PowerMockito.`when`(Utility.sha256hash(any<String>())).thenAnswer {
      pathRouteStr = it.arguments[0] as String
      ""
    }

    PowerMockito.`when`(ViewHierarchy.getParentOfView(mockView)).thenReturn(mockParentView)

    PredictionHistoryManager.getPathID(mockView, TEXT)
    assertThat(pathRouteStr).isEqualTo(expectedPathRoute.toString())
  }
}
