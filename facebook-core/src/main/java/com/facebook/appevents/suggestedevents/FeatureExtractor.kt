// Copyright 2004-present Facebook. All Rights Reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.facebook.appevents.suggestedevents

import android.text.InputType
import android.util.Patterns
import com.facebook.appevents.internal.ViewHierarchyConstants.ADD_PAYMENT_INFO
import com.facebook.appevents.internal.ViewHierarchyConstants.ADD_TO_CART
import com.facebook.appevents.internal.ViewHierarchyConstants.ADD_TO_WISHLIST
import com.facebook.appevents.internal.ViewHierarchyConstants.BUTTON_ID
import com.facebook.appevents.internal.ViewHierarchyConstants.BUTTON_TEXT
import com.facebook.appevents.internal.ViewHierarchyConstants.CHILDREN_VIEW_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_TYPE_BITMASK_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLICKABLE_VIEW_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.COMPLETE_REGISTRATION
import com.facebook.appevents.internal.ViewHierarchyConstants.ENGLISH
import com.facebook.appevents.internal.ViewHierarchyConstants.GERMAN
import com.facebook.appevents.internal.ViewHierarchyConstants.HINT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.INITIATE_CHECKOUT
import com.facebook.appevents.internal.ViewHierarchyConstants.INPUT_TYPE_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.IS_INTERACTED_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.JAPANESE
import com.facebook.appevents.internal.ViewHierarchyConstants.LEAD
import com.facebook.appevents.internal.ViewHierarchyConstants.PAGE_TITLE
import com.facebook.appevents.internal.ViewHierarchyConstants.PURCHASE
import com.facebook.appevents.internal.ViewHierarchyConstants.RESOLVED_DOCUMENT_LINK
import com.facebook.appevents.internal.ViewHierarchyConstants.SCREEN_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.SEARCH
import com.facebook.appevents.internal.ViewHierarchyConstants.SPANISH
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.VIEW_CONTENT
import com.facebook.appevents.internal.ViewHierarchyConstants.VIEW_KEY
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal object FeatureExtractor {
  private const val NUM_OF_FEATURES = 30
  private const val REGEX_CR_PASSWORD_FIELD = "password"
  private const val REGEX_CR_HAS_CONFIRM_PASSWORD_FIELD =
      "(?i)(confirm.*password)|(password.*(confirmation|confirm)|confirmation)"
  private const val REGEX_CR_HAS_LOG_IN_KEYWORDS = "(?i)(sign in)|login|signIn"
  private const val REGEX_CR_HAS_SIGN_ON_KEYWORDS =
      "(?i)(sign.*(up|now)|registration|" +
          "register|(create|apply).*(profile|account)|open.*account|" +
          "account.*(open|creation|application)|enroll|join.*now)"
  private const val REGEX_ADD_TO_CART_BUTTON_TEXT = "(?i)add to(\\s|\\Z)|update(\\s|\\Z)|cart"
  private const val REGEX_ADD_TO_CART_PAGE_TITLE =
      "(?i)add to(\\s|\\Z)|update(\\s|\\Z)|cart|shop|buy"
  private lateinit var languageInfo: Map<String, String>
  private lateinit var eventInfo: Map<String, String>
  private lateinit var textTypeInfo: Map<String, String>
  private lateinit var rules: JSONObject
  private var initialized = false

  @JvmStatic
  fun isInitialized(): Boolean {
    return initialized
  }

  @JvmStatic
  fun initialize(file: File?) {
    try {
      rules = JSONObject()
      val inputStream: InputStream = FileInputStream(file)
      val size = inputStream.available()
      val buffer = ByteArray(size)
      inputStream.read(buffer)
      inputStream.close()
      rules = JSONObject(String(buffer, Charsets.UTF_8))
    } catch (ex: Exception) {
      return
    }

    // initialize some static fields
    languageInfo = mapOf(ENGLISH to "1", GERMAN to "2", SPANISH to "3", JAPANESE to "4")
    eventInfo =
        mapOf(
            VIEW_CONTENT to "0",
            SEARCH to "1",
            ADD_TO_CART to "2",
            ADD_TO_WISHLIST to "3",
            INITIATE_CHECKOUT to "4",
            ADD_PAYMENT_INFO to "5",
            PURCHASE to "6",
            LEAD to "7",
            COMPLETE_REGISTRATION to "8")
    textTypeInfo =
        mapOf(
            BUTTON_TEXT to "1", PAGE_TITLE to "2", RESOLVED_DOCUMENT_LINK to "3", BUTTON_ID to "4")

    initialized = true
  }

  @JvmStatic
  fun getTextFeature(buttonText: String, activityName: String, appName: String): String {
    // intentionally use "|" and "," to separate to respect how text processed during training
    return "$appName | $activityName, $buttonText".toLowerCase()
  }

  @JvmStatic
  fun getDenseFeatures(viewHierarchy: JSONObject, appName: String): FloatArray? {
    // sanity check
    var appName = appName
    if (!initialized) {
      return null
    }
    val ret = FloatArray(NUM_OF_FEATURES) { 0f }
    try {
      appName = appName.toLowerCase()
      val viewTree = JSONObject(viewHierarchy.optJSONObject(VIEW_KEY).toString()) // copy
      val screenName = viewHierarchy.optString(SCREEN_NAME_KEY)
      val siblings = JSONArray()
      pruneTree(viewTree, siblings) // viewTree is updated here
      val parseResult = parseFeatures(viewTree)
      sum(ret, parseResult)
      val interactedNode = getInteractedNode(viewTree) ?: return null
      val nonparseFeatures =
          nonparseFeatures(interactedNode, siblings, screenName, viewTree.toString(), appName)
      sum(ret, nonparseFeatures)
      return ret
    } catch (je: JSONException) {
      /*no op*/
    }
    return ret
  }

  private fun parseFeatures(node: JSONObject): FloatArray {
    val densefeat = FloatArray(NUM_OF_FEATURES) { 0f }
    val text = node.optString(TEXT_KEY).toLowerCase()
    val hint = node.optString(HINT_KEY).toLowerCase()
    val className = node.optString(CLASS_NAME_KEY).toLowerCase()
    val inputType = node.optInt(INPUT_TYPE_KEY, -1)
    val textValues = arrayOf(text, hint)
    if (matchIndicators(arrayOf("$", "amount", "price", "total"), textValues)) {
      densefeat[0] += 1.0f
    }
    if (matchIndicators(arrayOf("password", "pwd"), textValues)) {
      densefeat[1] += 1.0f
    }
    if (matchIndicators(arrayOf("tel", "phone"), textValues)) {
      densefeat[2] += 1.0f
    }
    if (matchIndicators(arrayOf("search"), textValues)) {
      densefeat[4] += 1.0f
    }

    // input form -- EditText
    if (inputType >= 0) {
      densefeat[5] += 1.0f
    }

    // phone
    if (inputType == InputType.TYPE_CLASS_PHONE || inputType == InputType.TYPE_CLASS_NUMBER) {
      densefeat[6] += 1.0f
    }

    // email
    if (inputType == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
        Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
      densefeat[7] += 1.0f
    }

    // Check Box
    if (className.contains("checkbox")) {
      densefeat[8] += 1.0f
    }
    if (matchIndicators(arrayOf("complete", "confirm", "done", "submit"), arrayOf(text))) {
      densefeat[10] += 1.0f
    }

    // Radio Button
    if (className.contains("radio") && className.contains("button")) {
      densefeat[12] += 1.0f
    }
    try {
      val childViews = node.optJSONArray(CHILDREN_VIEW_KEY)
      val len = childViews.length()
      for (i in 0 until len) {
        sum(densefeat, parseFeatures(childViews.getJSONObject(i)))
      }
    } catch (je: JSONException) {
      /*no op*/
    }
    return densefeat
  }

  private fun nonparseFeatures(
      node: JSONObject,
      siblings: JSONArray,
      screenName: String,
      formFieldsJSON: String,
      appName: String
  ): FloatArray {
    val densefeat = FloatArray(NUM_OF_FEATURES) { 0f }
    val siblingLen = siblings.length()
    densefeat[3] = (if (siblingLen > 1) siblingLen - 1f else 0f).toFloat()
    try {
      for (i in 0 until siblings.length()) {
        if (isButton(siblings.getJSONObject(i))) {
          densefeat[9] += 1f
        }
      }
    } catch (je: JSONException) {
      /*no op*/
    }
    densefeat[13] = -1f
    densefeat[14] = -1f
    val pageTitle = "$screenName|$appName"
    val hintSB = StringBuilder()
    val textSB = StringBuilder()
    updateHintAndTextRecursively(node, textSB, hintSB)
    val buttonID = hintSB.toString()
    val buttonText = textSB.toString()

    // [1] CompleteRegistration specific features
    densefeat[15] =
        if (regexMatched(ENGLISH, COMPLETE_REGISTRATION, BUTTON_TEXT, buttonText)) 1f else 0f
    densefeat[16] =
        if (regexMatched(ENGLISH, COMPLETE_REGISTRATION, PAGE_TITLE, pageTitle)) 1f else 0f
    densefeat[17] =
        if (regexMatched(ENGLISH, COMPLETE_REGISTRATION, BUTTON_ID, buttonID)) 1f else 0f

    // TODO: (T54293420) update the logic to include inputtype
    densefeat[18] = if (formFieldsJSON.contains(REGEX_CR_PASSWORD_FIELD)) 1f else 0f
    densefeat[19] =
        if (regexMatched(REGEX_CR_HAS_CONFIRM_PASSWORD_FIELD, formFieldsJSON)) 1f else 0f
    densefeat[20] = if (regexMatched(REGEX_CR_HAS_LOG_IN_KEYWORDS, formFieldsJSON)) 1f else 0f
    densefeat[21] = if (regexMatched(REGEX_CR_HAS_SIGN_ON_KEYWORDS, formFieldsJSON)) 1f else 0f

    // [2] Purchase specific features
    densefeat[22] = if (regexMatched(ENGLISH, PURCHASE, BUTTON_TEXT, buttonText)) 1f else 0f
    densefeat[24] = if (regexMatched(ENGLISH, PURCHASE, PAGE_TITLE, pageTitle)) 1f else 0f

    // [3] AddToCart specific features
    densefeat[25] = if (regexMatched(REGEX_ADD_TO_CART_BUTTON_TEXT, buttonText)) 1f else 0f
    densefeat[27] = if (regexMatched(REGEX_ADD_TO_CART_PAGE_TITLE, pageTitle)) 1f else 0f

    // [4] Lead specific features
    // TODO: (T54293420) do we need to remove this part?
    densefeat[28] = if (regexMatched(ENGLISH, LEAD, BUTTON_TEXT, buttonText)) 1f else 0f
    densefeat[29] = if (regexMatched(ENGLISH, LEAD, PAGE_TITLE, pageTitle)) 1f else 0f
    return densefeat
  }

  private fun regexMatched(
      language: String,
      event: String,
      textType: String,
      matchText: String
  ): Boolean {
    val regex =
        rules
            .optJSONObject("rulesForLanguage")
            ?.optJSONObject(languageInfo[language])
            ?.optJSONObject("rulesForEvent")
            ?.optJSONObject(eventInfo[event])
            ?.optJSONObject("positiveRules")
            ?.optString(textTypeInfo[textType])
    return if (regex === null) false else regexMatched(regex, matchText)
  }

  private fun regexMatched(pattern: String, matchText: String): Boolean {
    return Pattern.compile(pattern).matcher(matchText).find()
  }

  private fun matchIndicators(indicators: Array<String>, values: Array<String>): Boolean {
    for (indicator in indicators) {
      for (value in values) {
        if (value.contains(indicator)) {
          return true
        }
      }
    }
    return false
  }

  private fun pruneTree(node: JSONObject, siblings: JSONArray): Boolean {
    try {
      val isInteracted = node.optBoolean(IS_INTERACTED_KEY)
      if (isInteracted) {
        return true
      }
      var isChildInteracted = false
      var isDescendantInteracted = false
      val childViews = node.optJSONArray(CHILDREN_VIEW_KEY)
      for (i in 0 until childViews.length()) {
        val child = childViews.getJSONObject(i)
        if (child.optBoolean(IS_INTERACTED_KEY)) {
          isChildInteracted = true
          isDescendantInteracted = true
          break
        }
      }
      val newChildren = JSONArray()
      if (isChildInteracted) {
        for (i in 0 until childViews.length()) {
          val child = childViews.getJSONObject(i)
          siblings.put(child)
        }
      } else {
        for (i in 0 until childViews.length()) {
          val child = childViews.getJSONObject(i)
          if (pruneTree(child, siblings)) {
            isDescendantInteracted = true
            newChildren.put(child)
          }
        }
        node.put(CHILDREN_VIEW_KEY, newChildren)
      }
      return isDescendantInteracted
    } catch (je: JSONException) {
      /*no op*/
    }
    return false
  }

  private fun sum(a: FloatArray, b: FloatArray) {
    for (i in a.indices) {
      a[i] = a[i] + b[i]
    }
  }

  private fun isButton(node: JSONObject): Boolean {
    val classTypeBitmask = node.optInt(CLASS_TYPE_BITMASK_KEY)
    return classTypeBitmask and 1 shl CLICKABLE_VIEW_BITMASK > 0
  }

  private fun updateHintAndTextRecursively(
      view: JSONObject,
      textSB: StringBuilder,
      hintSB: StringBuilder
  ) {
    val text = view.optString(TEXT_KEY, "").toLowerCase()
    val hint = view.optString(HINT_KEY, "").toLowerCase()
    if (text.isNotEmpty()) {
      textSB.append(text).append(" ")
    }
    if (hint.isNotEmpty()) {
      hintSB.append(hint).append(" ")
    }
    val children = view.optJSONArray(CHILDREN_VIEW_KEY) ?: return
    for (i in 0 until children.length()) {
      try {
        val currentChildView = children.getJSONObject(i)
        updateHintAndTextRecursively(currentChildView, textSB, hintSB)
      } catch (je: JSONException) {
        /*no opt*/
      }
    }
  }

  private fun getInteractedNode(view: JSONObject): JSONObject? {
    try {
      if (view.optBoolean(IS_INTERACTED_KEY)) {
        return view
      }
      val children = view.optJSONArray(CHILDREN_VIEW_KEY) ?: return null
      for (i in 0 until children.length()) {
        val sub = getInteractedNode(children.getJSONObject(i))
        if (sub != null) {
          return sub
        }
      }
    } catch (je: JSONException) {
      /*no op*/
    }
    return null
  }
}
