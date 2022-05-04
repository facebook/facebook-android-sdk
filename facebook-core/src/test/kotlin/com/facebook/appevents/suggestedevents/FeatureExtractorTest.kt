package com.facebook.appevents.suggestedevents

import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.internal.ViewHierarchyConstants.CHILDREN_VIEW_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_TYPE_BITMASK_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.HINT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.IMAGEVIEW_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.IS_INTERACTED_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.SCREEN_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.VIEW_KEY
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.powermock.reflect.Whitebox

class FeatureExtractorTest : FacebookPowerMockTestCase() {
  private lateinit var dir: File
  private lateinit var file: File

  @Before
  fun init() {
    val dirName = UUID.randomUUID().toString()
    dir = File(dirName)
    dir.mkdir()

    file = File(dir, "jsonFile")
    val output = BufferedWriter(FileWriter(file))
    output.write(buildRules().toString())
    output.close()
  }

  @After
  fun cleanTestDirectory() {
    dir.deleteRecursively()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T103598626
  @Test
  fun testInitialize() {
    Whitebox.setInternalState(FeatureExtractor::class.java, "initialized", false)
    FeatureExtractor.initialize(file)
    assertThat(FeatureExtractor.isInitialized()).isTrue
    val rules = Whitebox.getInternalState<JSONObject>(FeatureExtractor::class.java, "rules")
    val languageInfo =
        Whitebox.getInternalState<Map<String, String>>(FeatureExtractor::class.java, "languageInfo")
    val eventInfo =
        Whitebox.getInternalState<Map<String, String>>(FeatureExtractor::class.java, "eventInfo")
    val textTypeInfo =
        Whitebox.getInternalState<Map<String, String>>(FeatureExtractor::class.java, "textTypeInfo")
    assertThat(
            rules
                .optJSONObject("rulesForLanguage")
                .optJSONObject("1")
                .optJSONObject("rulesForEvent")
                .optJSONObject("0")
                .optJSONObject("positiveRules")
                .optString("1"))
        .isEqualTo("text")
    assertThat(languageInfo).isNotEmpty
    assertThat(eventInfo).isNotEmpty
    assertThat(textTypeInfo).isNotEmpty
  }

  @Test
  fun testGetDenseFeatures() {
    Whitebox.setInternalState(FeatureExtractor::class.java, "initialized", false)
    FeatureExtractor.initialize(file)

    val viewHierarchy = JSONObject()
    val viewTree = JSONObject()
    viewTree.put(IS_INTERACTED_KEY, true)
    viewTree.put(CLASS_NAME_KEY, "ImageView")
    viewTree.put(CLASS_TYPE_BITMASK_KEY, 1 shl IMAGEVIEW_BITMASK)
    viewTree.put(TEXT_KEY, "text")
    viewTree.put(HINT_KEY, "hint")
    viewTree.put(CHILDREN_VIEW_KEY, JSONArray())

    viewHierarchy.put(VIEW_KEY, viewTree)
    viewHierarchy.put(SCREEN_NAME_KEY, "activity")
    val ret = FeatureExtractor.getDenseFeatures(viewHierarchy, "HackBook")
    val expectedFeatures = FloatArray(30) { 0F }
    expectedFeatures[13] = -1f
    expectedFeatures[14] = -1f
    expectedFeatures[15] = 1f
    expectedFeatures[22] = 1f
    expectedFeatures[28] = 1f
    assertThat(ret).isEqualTo(expectedFeatures)
  }

  private fun buildRules(): JSONObject {
    val languageMap =
        mutableMapOf<String, Map<String, Map<String, Map<String, Map<String, String>>>>>()
    for (language in 1..4) {
      val eventMap = mutableMapOf<String, Map<String, Map<String, String>>>()
      for (event in 0..8) {
        val textMap = mutableMapOf<String, String>()
        for (text in 1..4) {
          textMap[text.toString()] = "text"
        }
        eventMap[event.toString()] = mapOf("positiveRules" to textMap.toMap())
      }

      languageMap[language.toString()] = mapOf("rulesForEvent" to eventMap.toMap())
    }
    return JSONObject(mapOf("rulesForLanguage" to languageMap))
  }
}
