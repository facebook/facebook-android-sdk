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
package com.facebook.internal.logging.dumpsys

import android.annotation.SuppressLint
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import android.widget.TextView
import com.facebook.internal.logging.dumpsys.ResourcesUtil.getIdStringQuietly
import java.io.PrintWriter
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Collections
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@SuppressLint("HexColorValueUsage", "CatchGeneralException", "BadMethodUse-java.lang.String.length")
class EndToEndDumpsysHelper {
  private val rootResolver = AndroidRootResolver()
  private val webViewDumpHelper = WebViewDumpHelper()
  private var lithoViewToStringMethod: Method? = null
  private fun dumpViewHierarchy(prefix: String, writer: PrintWriter, args: Array<String>) {
    writer.print(prefix)
    writer.println("Top Level Window View Hierarchy:")
    val dumpAllRoots = hasArgument(args, ALL_ROOTS_ARGUMENT)
    val dumpTopRootOnly = hasArgument(args, TOP_ROOT_ARGUMENT)
    val withWebView = hasArgument(args, WITH_WEBVIEW_ARGUMENT)
    val withProps = hasArgument(args, WITH_PROPS_ARGUMENT)
    try {
      val roots: List<AndroidRootResolver.Root?>? = rootResolver.listActiveRoots()
      if (roots == null || roots.isEmpty()) {
        // no roots view, nothing to output
        return
      }
      Collections.reverse(roots)

      // the last root with a visible view is the root having the top level view
      // it can have multiple views on the same window identified by same token and incremental type
      var prevRootParam: WindowManager.LayoutParams? = null
      for (root in roots) {
        if (root?.view?.visibility != View.VISIBLE) {
          continue
        }
        // handle multiple root views on the same window (dialogs, tooltips)
        if (!dumpAllRoots &&
            prevRootParam != null &&
            abs(root.param.type - prevRootParam.type) != 1) {
          // the view is no longer on the same window, stop dump
          break
        }
        dumpViewHierarchy("$prefix  ", writer, root.view, 0, 0, withWebView, withProps)
        prevRootParam = root.param
        if (dumpTopRootOnly) {
          break
        }
      }

      // additional dump of web view html data
      webViewDumpHelper.dump(writer)
    } catch (e: Exception) {
      writer.println("Failure in view hierarchy dump: " + e.message)
    }
  }

  private fun dumpViewHierarchy(
      prefix: String,
      writer: PrintWriter,
      view: View?,
      leftOffset: Int,
      topOffset: Int,
      withWebView: Boolean,
      withProps: Boolean
  ) {
    var prefix = prefix
    writer.print(prefix)
    if (view == null) {
      writer.println("null")
      return
    }
    writer.print(view.javaClass.name)
    writer.print("{")
    writer.print(Integer.toHexString(view.hashCode()))
    writeViewFlags(writer, view)
    writeViewBounds(writer, view, leftOffset, topOffset)
    writeViewTestId(writer, view)
    writeViewText(writer, view)
    if (withProps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Api21Utils.writeExtraProps(writer, view)
    }
    writer.println("}")
    if (isExtendsLithoView(view)) {
      writeLithoViewSubHierarchy(writer, view, prefix, withProps)
    }
    if (withWebView && view is WebView) {
      webViewDumpHelper.handle(view)
    }
    if (view !is ViewGroup) {
      return
    }
    val n = view.childCount
    if (n <= 0) {
      return
    }
    prefix = "$prefix  "
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    for (i in 0 until n) {
      dumpViewHierarchy(
          prefix, writer, view.getChildAt(i), location[0], location[1], withWebView, withProps)
    }
  }

  private fun writeLithoViewSubHierarchy(
      writer: PrintWriter,
      view: View,
      prefix: String,
      withProps: Boolean
  ) {
    try {
      if (lithoViewToStringMethod == null) {
        val helperClass = Class.forName(LITHO_VIEW_TEST_HELPER_CLASS)
        lithoViewToStringMethod =
            helperClass.getDeclaredMethod(
                LITHO_VIEW_TO_STRING_METHOD,
                View::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType)
      }

      // Litho contains UI inside a single view with special handling
      // We don't want to call explicitly because we don't want to have a litho dependency
      val lithoViewDump =
          lithoViewToStringMethod?.invoke(null, view, prefix.length / 2 + 1, withProps) as String
      writer.append(lithoViewDump)
    } catch (e: Exception) {
      writer
          .append(prefix)
          .append("Failed litho view sub hierarch dump: ")
          .append(fixString(e.message, 100))
          .println()
    }
  }

  /** Optimization for pre api21 devices */
  private object Api21Utils {
    private var keyedTagsField: Field? = null
    fun writeExtraProps(writer: PrintWriter, view: View) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return
      }
      val nodeInfo = createNodeInfoFromView(view) ?: return
      val props = JSONObject()
      try {
        if (view is TextView) {
          props.put("textColor", view.textColors.defaultColor)
          props.put("textSize", view.textSize.toDouble())
          props.put("hint", fixString(view.hint, 100))
        }
        val tags = getTags(view)
        if (tags != null) {
          props.put("keyedTags", tags)
        }
        val actions = JSONArray()
        for (action in nodeInfo.actionList) {
          val actionLabel = action.label as String
          if (actionLabel != null) {
            actions.put(fixString(actionLabel, 50))
          }
        }
        if (actions.length() > 0) {
          props.put("actions", actions)
        }
        val cd = fixString(nodeInfo.contentDescription, 50)
        if (cd != null && cd.isNotEmpty()) {
          props.put("content-description", cd)
        }
        props
            .put("accessibility-focused", nodeInfo.isAccessibilityFocused)
            .put("checkable", nodeInfo.isCheckable)
            .put("checked", nodeInfo.isChecked)
            .put("class-name", fixString(nodeInfo.className, 50))
            .put("clickable", nodeInfo.isClickable)
            .put("content-invalid", nodeInfo.isContentInvalid)
            .put("dismissable", nodeInfo.isDismissable)
            .put("editable", nodeInfo.isEditable)
            .put("enabled", nodeInfo.isEnabled)
            .put("focusable", nodeInfo.isFocusable)
            .put("focused", nodeInfo.isFocused)
            .put("long-clickable", nodeInfo.isLongClickable)
            .put("multiline", nodeInfo.isMultiLine)
            .put("password", nodeInfo.isPassword)
            .put("scrollable", nodeInfo.isScrollable)
            .put("selected", nodeInfo.isSelected)
            .put("visible-to-user", nodeInfo.isVisibleToUser)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          Api24Utils.addExtraProps(props, nodeInfo)
        }
      } catch (e: Exception) {
        try {
          props.put("DUMP-ERROR", fixString(e.message, 50))
        } catch (ex: JSONException) {
          // ignore
        }
      }
      writer.append(" props=\"").append(props.toString()).append("\"")
    }

    private fun getTags(view: View): JSONObject? {
      var tags: JSONObject? = null
      try {
        if (keyedTagsField == null) {
          keyedTagsField = View::class.java.getDeclaredField("mKeyedTags")
          keyedTagsField?.isAccessible = true
        }
        val keyedTags = keyedTagsField?.get(view) as SparseArray<*>
        if (keyedTags != null && keyedTags.size() > 0) {
          tags = JSONObject()
          var i = 0
          val count = keyedTags.size()
          while (i < count) {
            val id = getIdStringQuietly(view.resources, keyedTags.keyAt(i))
            try {
              tags.put(id, keyedTags.valueAt(i))
            } catch (e: JSONException) {
              // ignore
            }
            i++
          }
        }
      } catch (e: Exception) {
        // ignore
      }
      return tags
    }

    init {
      try {
        keyedTagsField = View::class.java.getDeclaredField("mKeyedTags")
        keyedTagsField?.isAccessible = true
      } catch (e: NoSuchFieldException) {
        // ignore
      }
    }
  }

  /** Handle VerifyError bug in Android 5.* */
  private object Api24Utils {
    @Throws(JSONException::class)
    fun addExtraProps(props: JSONObject, nodeInfo: AccessibilityNodeInfo) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        return
      }
      props
          .put("context-clickable", nodeInfo.isContextClickable)
          .put("drawing-order", nodeInfo.drawingOrder)
          .put("important-for-accessibility", nodeInfo.isImportantForAccessibility)
    }
  }

  companion object {
    private const val LITHO_VIEW_CLASS = "com.facebook.litho.LithoView"
    private const val LITHO_VIEW_TEST_HELPER_CLASS = "com.facebook.litho.LithoViewTestHelper"
    private const val LITHO_VIEW_TO_STRING_METHOD = "viewToStringForE2E"
    private const val E2E_ARGUMENT = "e2e"
    private const val TOP_ROOT_ARGUMENT = "top-root"
    private const val ALL_ROOTS_ARGUMENT = "all-roots"
    private const val WITH_WEBVIEW_ARGUMENT = "webview"
    private const val WITH_PROPS_ARGUMENT = "props"
    private const val RC_TEXT_VIEW_SIMPLE_CLASS_NAME = "RCTextView"
    private var instance: EndToEndDumpsysHelper? = null
    private var rcTextViewGetTextMethod: Method? = null
    @JvmStatic
    fun maybeDump(prefix: String, writer: PrintWriter, args: Array<String>?): Boolean {
      if (args != null && args.isNotEmpty() && E2E_ARGUMENT == args[0]) {
        // run special DBF view-hierarchy dump if requested using e2e arg
        if (instance == null) {
          // always runs on the main thread
          instance = EndToEndDumpsysHelper()
        }
        instance?.dumpViewHierarchy(prefix, writer, args)
        return true
      }
      return false
    }

    private fun isExtendsLithoView(view: View): Boolean {
      var aClass: Class<*>? = view.javaClass
      while (aClass != null) {
        if (aClass.name == LITHO_VIEW_CLASS) {
          return true
        }
        aClass = aClass.superclass
      }
      return false
    }

    private fun writeViewFlags(writer: PrintWriter, view: View) {
      writer.print(" ")
      when (view.visibility) {
        View.VISIBLE -> writer.print("V")
        View.INVISIBLE -> writer.print("I")
        View.GONE -> writer.print("G")
        else -> writer.print(".")
      }
      writer.print(if (view.isFocusable) "F" else ".")
      writer.print(if (view.isEnabled) "E" else ".")
      writer.print(".")
      writer.print(if (view.isHorizontalScrollBarEnabled) "H" else ".")
      writer.print(if (view.isVerticalScrollBarEnabled) "V" else ".")
      writer.print(if (view.isClickable) "C" else ".")
      writer.print(if (view.isLongClickable) "L" else ".")
      writer.print(" ")
      writer.print(if (view.isFocused) "F" else ".")
      writer.print(if (view.isSelected) "S" else ".")
      writer.print(if (view.isHovered) "H" else ".")
      writer.print(if (view.isActivated) "A" else ".")
      writer.print(if (view.isDirty) "D" else ".")
    }

    private fun writeViewBounds(writer: PrintWriter, view: View, leftOffset: Int, topOffset: Int) {
      val location = IntArray(2)
      view.getLocationOnScreen(location)
      writer.print(" ")
      writer.print(location[0] - leftOffset)
      writer.print(",")
      writer.print(location[1] - topOffset)
      writer.print("-")
      writer.print(location[0] + view.width - leftOffset)
      writer.print(",")
      writer.print(location[1] + view.height - topOffset)
    }

    private fun writeViewTestId(writer: PrintWriter, view: View) {
      try {
        val id = view.id
        if (id == View.NO_ID) {
          maybeWriteViewTestIdFromTag(writer, view)
          return
        }
        writer.append(" #")
        writer.append(Integer.toHexString(id))
        val resources = view.resources
        if (id <= 0 || resources == null) {
          maybeWriteViewTestIdFromTag(writer, view)
          return
        }
        val packageName: String
        packageName =
            when (id and -0x1000000) {
              0x7f000000 -> "app"
              0x01000000 -> "android"
              else -> resources.getResourcePackageName(id)
            }
        writer.print(" ")
        writer.print(packageName)
        writer.print(":")
        writer.print(resources.getResourceTypeName(id))
        writer.print("/")
        writer.print(resources.getResourceEntryName(id))
      } catch (e: Exception) {
        // ignore
        maybeWriteViewTestIdFromTag(writer, view)
      }
    }

    private fun maybeWriteViewTestIdFromTag(writer: PrintWriter, view: View) {
      val tag = view.tag as? String ?: return
      if (tag.isEmpty()) {
        return
      }
      writer.print(" app:tag/")
      writer.print(fixString(tag, 60))
    }

    @SuppressLint("ReflectionMethodUse")
    private fun writeViewText(writer: PrintWriter, view: View) {
      try {
        var text: String? = null
        if (view is TextView) {
          text = view.text.toString()
        } else if (view.javaClass.simpleName == RC_TEXT_VIEW_SIMPLE_CLASS_NAME) {
          text = getTextFromRcTextView(view)
        } else {
          val content = view.contentDescription
          if (content != null) {
            text = content.toString()
          }
          if (text == null || text.isEmpty()) {
            val tag = view.tag
            if (tag != null) {
              text = tag.toString().trim { it <= ' ' }
            }
          }
        }
        if (text == null || text.isEmpty()) {
          return
        }
        writer.print(" text=\"")
        writer.print(fixString(text, 600))
        writer.print("\"")
      } catch (e: Exception) {
        // ignore
      }
    }

    @SuppressLint("PrivateApi", "ReflectionMethodUse")
    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class)
    private fun getTextFromRcTextView(view: View): String? {
      if (rcTextViewGetTextMethod == null) {
        rcTextViewGetTextMethod = view.javaClass.getDeclaredMethod("getText")
      }
      val textViewValue = rcTextViewGetTextMethod?.invoke(view)
      return textViewValue?.toString()
    }

    private fun fixString(str: CharSequence?, maxLength: Int): String {
      if (str == null || str.isEmpty()) {
        return ""
      }
      var fixed = str.toString().replace(" \n", " ").replace("\n", " ").replace("\"", "")
      if (str.length > maxLength) {
        fixed = fixed.substring(0, maxLength) + "..."
      }
      return fixed
    }

    private fun hasArgument(args: Array<String>?, argument: String): Boolean {
      if (args == null) {
        return false
      }
      for (arg in args) {
        if (argument.equals(arg, ignoreCase = true)) {
          return true
        }
      }
      return false
    }

    private fun createNodeInfoFromView(view: View?): AccessibilityNodeInfo? {
      if (view == null) {
        return null
      }
      val nodeInfo = AccessibilityNodeInfo.obtain()
      // For some unknown reason, Android seems to occasionally throw a NPE from
      // onInitializeAccessibilityNodeInfo.
      try {
        view.onInitializeAccessibilityNodeInfo(nodeInfo)
      } catch (e: NullPointerException) {
        nodeInfo?.recycle()
        return null
      }
      return nodeInfo
    }
  }
}
