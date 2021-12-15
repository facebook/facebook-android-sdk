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

package com.facebook.appevents.codeless.internal

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import androidx.annotation.RestrictTo
import com.facebook.appevents.codeless.internal.SensitiveUserDataUtils.isSensitiveUserData
import com.facebook.appevents.internal.ViewHierarchyConstants.ADAPTER_VIEW_ITEM_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.BUTTON_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.CHECKBOX_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.CHILDREN_VIEW_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_TYPE_BITMASK_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.CLICKABLE_VIEW_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.DESC_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_HEIGHT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_LEFT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_SCROLL_X_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_SCROLL_Y_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_TOP_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_VISIBILITY_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.DIMENSION_WIDTH_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.HINT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.ICON_BITMAP
import com.facebook.appevents.internal.ViewHierarchyConstants.ID_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.IMAGEVIEW_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.INPUT_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.IS_USER_INPUT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.LABEL_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.PICKER_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.RADIO_GROUP_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.RATINGBAR_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.REACT_NATIVE_BUTTON_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.SWITCH_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.TAG_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXTVIEW_BITMASK
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_IS_BOLD
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_IS_ITALIC
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_SIZE
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_STYLE
import com.facebook.internal.Utility.coerceValueIfNullOrEmpty
import com.facebook.internal.Utility.logd
import com.facebook.internal.Utility.sha256hash
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ViewHierarchy {
  private val TAG = ViewHierarchy::class.java.canonicalName

  // React Native class names
  private const val CLASS_RCTROOTVIEW = "com.facebook.react.ReactRootView"
  private const val CLASS_RCTVIEWGROUP = "com.facebook.react.views.view.ReactViewGroup"
  private const val CLASS_TOUCHTARGETHELPER = "com.facebook.react.uimanager.TouchTargetHelper"

  // TouchTargetHelper method names
  private const val METHOD_FIND_TOUCHTARGET_VIEW = "findTouchTargetView"
  private const val ICON_MAX_EDGE_LENGTH = 44
  private var RCTRootViewReference = WeakReference<View?>(null)
  private var methodFindTouchTargetView: Method? = null

  @JvmStatic
  fun getParentOfView(view: View?): ViewGroup? {
    if (null == view) {
      return null
    }
    val parent = view.parent
    return if (parent is ViewGroup) {
      parent
    } else null
  }

  @JvmStatic
  fun getChildrenOfView(view: View?): List<View> {
    val children = ArrayList<View>()
    if (view is ViewGroup) {
      val count = view.childCount
      for (i in 0 until count) {
        children.add(view.getChildAt(i))
      }
    }
    return children
  }

  @JvmStatic
  fun updateBasicInfoOfView(view: View, json: JSONObject) {
    try {
      val text = getTextOfView(view)
      val hint = getHintOfView(view)
      val tag = view.tag
      val description = view.contentDescription
      json.put(CLASS_NAME_KEY, view.javaClass.canonicalName)
      json.put(CLASS_TYPE_BITMASK_KEY, getClassTypeBitmask(view))
      json.put(ID_KEY, view.id)
      if (!isSensitiveUserData(view)) {
        json.put(TEXT_KEY, coerceValueIfNullOrEmpty(sha256hash(text), ""))
      } else {
        json.put(TEXT_KEY, "")
        json.put(IS_USER_INPUT_KEY, true)
      }
      json.put(HINT_KEY, coerceValueIfNullOrEmpty(sha256hash(hint), ""))
      if (tag != null) {
        json.put(TAG_KEY, coerceValueIfNullOrEmpty(sha256hash(tag.toString()), ""))
      }
      if (description != null) {
        json.put(DESC_KEY, coerceValueIfNullOrEmpty(sha256hash(description.toString()), ""))
      }
      val dimension = getDimensionOfView(view)
      json.put(DIMENSION_KEY, dimension)
    } catch (e: JSONException) {
      logd(TAG, e)
    }
  }

  @JvmStatic
  fun updateAppearanceOfView(view: View, json: JSONObject, displayDensity: Float) {
    try {
      val textStyle = JSONObject()
      if (view is TextView) {
        val textView = view
        val typeface = textView.typeface
        if (typeface != null) {
          textStyle.put(TEXT_SIZE, textView.textSize.toDouble())
          textStyle.put(TEXT_IS_BOLD, typeface.isBold)
          textStyle.put(TEXT_IS_ITALIC, typeface.isItalic)
          json.put(TEXT_STYLE, textStyle)
        }
      }
      if (view is ImageView) {
        val drawable = view.drawable
        if (drawable is BitmapDrawable) {
          if (view.getHeight() / displayDensity <= ICON_MAX_EDGE_LENGTH &&
              view.getWidth() / displayDensity <= ICON_MAX_EDGE_LENGTH) {
            val bitmap = drawable.bitmap
            if (bitmap != null) {
              val byteArrayOutputStream = ByteArrayOutputStream()
              bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
              val byteArray = byteArrayOutputStream.toByteArray()
              val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
              json.put(ICON_BITMAP, encoded)
            }
          }
        }
      }
    } catch (e: JSONException) {
      logd(TAG, e)
    }
  }

  @JvmStatic
  fun getDictionaryOfView(view: View): JSONObject {
    if (view.javaClass.name == CLASS_RCTROOTVIEW) {
      RCTRootViewReference = WeakReference(view)
    }
    val json = JSONObject()
    try {
      updateBasicInfoOfView(view, json)
      val childViews = JSONArray()
      val children = getChildrenOfView(view)
      for (i in children.indices) {
        val child = children[i]
        val childInfo = getDictionaryOfView(child)
        childViews.put(childInfo)
      }
      json.put(CHILDREN_VIEW_KEY, childViews)
    } catch (e: JSONException) {
      Log.e(TAG, "Failed to create JSONObject for view.", e)
    }
    return json
  }

  @JvmStatic
  fun getClassTypeBitmask(view: View): Int {
    var bitmask = 0
    if (view is ImageView) {
      bitmask = bitmask or (1 shl IMAGEVIEW_BITMASK)
    }
    if (view.isClickable) {
      bitmask = bitmask or (1 shl CLICKABLE_VIEW_BITMASK)
    }
    if (isAdapterViewItem(view)) {
      bitmask = bitmask or (1 shl ADAPTER_VIEW_ITEM_BITMASK)
    }
    if (view is TextView) {
      bitmask = bitmask or (1 shl LABEL_BITMASK)
      bitmask = bitmask or (1 shl TEXTVIEW_BITMASK)
      if (view is Button) {
        bitmask = bitmask or (1 shl BUTTON_BITMASK)
        if (view is Switch) {
          bitmask = bitmask or (1 shl SWITCH_BITMASK)
        } else if (view is CheckBox) {
          bitmask = bitmask or (1 shl CHECKBOX_BITMASK)
        }
      }
      if (view is EditText) {
        bitmask = bitmask or (1 shl INPUT_BITMASK)
      }
    } else if (view is Spinner || view is DatePicker) {
      bitmask = bitmask or (1 shl PICKER_BITMASK)
    } else if (view is RatingBar) {
      bitmask = bitmask or (1 shl RATINGBAR_BITMASK)
    } else if (view is RadioGroup) {
      bitmask = bitmask or (1 shl RADIO_GROUP_BITMASK)
    } else if (view is ViewGroup && isRCTButton(view, RCTRootViewReference.get())) {
      bitmask = bitmask or (1 shl REACT_NATIVE_BUTTON_BITMASK)
    }
    return bitmask
  }

  @JvmStatic
  private fun isAdapterViewItem(view: View): Boolean {
    val parent = view.parent
    if (parent is AdapterView<*>) {
      return true
    }
    var nestedScrollingChildClass = getExistingClass("android.support.v4.view.NestedScrollingChild")
    if (nestedScrollingChildClass != null && nestedScrollingChildClass.isInstance(parent)) {
      return true
    }
    nestedScrollingChildClass = getExistingClass("androidx.core.view.NestedScrollingChild")
    return nestedScrollingChildClass != null && nestedScrollingChildClass.isInstance(parent)
  }

  @JvmStatic
  fun getTextOfView(view: View?): String {
    var textObj: Any? = null
    if (view is TextView) {
      textObj = view.text
      if (view is Switch) {
        val isOn = view.isChecked
        textObj = if (isOn) "1" else "0"
      }
    } else if (view is Spinner) {
      if (view.count > 0) {
        val selectedItem = view.selectedItem
        if (selectedItem != null) {
          textObj = selectedItem.toString()
        }
      }
    } else if (view is DatePicker) {
      val y = view.year
      val m = view.month
      val d = view.dayOfMonth
      textObj = String.format("%04d-%02d-%02d", y, m, d)
    } else if (view is TimePicker) {
      val h = view.currentHour
      val m = view.currentMinute
      textObj = String.format("%02d:%02d", h, m)
    } else if (view is RadioGroup) {
      val checkedId = view.checkedRadioButtonId
      val childCount = view.childCount
      for (i in 0 until childCount) {
        val child = view.getChildAt(i)
        if (child.id == checkedId && child is RadioButton) {
          textObj = child.text
          break
        }
      }
    } else if (view is RatingBar) {
      val rating = view.rating
      textObj = rating.toString()
    }
    return textObj?.toString() ?: ""
  }

  @JvmStatic
  fun getHintOfView(view: View?): String {
    var hintObj: CharSequence? = null
    if (view is EditText) {
      hintObj = view.hint
    } else if (view is TextView) {
      hintObj = view.hint
    }
    return hintObj?.toString() ?: ""
  }

  private fun getDimensionOfView(view: View): JSONObject {
    val dimension = JSONObject()
    try {
      dimension.put(DIMENSION_TOP_KEY, view.top)
      dimension.put(DIMENSION_LEFT_KEY, view.left)
      dimension.put(DIMENSION_WIDTH_KEY, view.width)
      dimension.put(DIMENSION_HEIGHT_KEY, view.height)
      dimension.put(DIMENSION_SCROLL_X_KEY, view.scrollX)
      dimension.put(DIMENSION_SCROLL_Y_KEY, view.scrollY)
      dimension.put(DIMENSION_VISIBILITY_KEY, view.visibility)
    } catch (e: JSONException) {
      Log.e(TAG, "Failed to create JSONObject for dimension.", e)
    }
    return dimension
  }

  @JvmStatic
  fun getExistingOnClickListener(view: View?): View.OnClickListener? {
    try {
      val listenerInfoField = Class.forName("android.view.View").getDeclaredField("mListenerInfo")
      if (listenerInfoField != null) {
        listenerInfoField.isAccessible = true
      }
      val listenerObj = listenerInfoField[view] ?: return null
      var listener: View.OnClickListener? = null
      val listenerField =
          Class.forName("android.view.View\$ListenerInfo").getDeclaredField("mOnClickListener")
      if (listenerField != null) {
        listenerField.isAccessible = true
        listener = listenerField[listenerObj] as View.OnClickListener
      }
      return listener
    } catch (e: NoSuchFieldException) {
      /* no op */
    } catch (e: ClassNotFoundException) {
      /* no op */
    } catch (e: IllegalAccessException) {
      /* no op */
    }
    return null
  }

  @JvmStatic
  fun setOnClickListener(view: View, newListener: View.OnClickListener?) {
    try {
      var listenerInfoField: Field? = null
      var listenerField: Field? = null
      try {
        listenerInfoField = Class.forName("android.view.View").getDeclaredField("mListenerInfo")
        listenerField =
            Class.forName("android.view.View\$ListenerInfo").getDeclaredField("mOnClickListener")
      } catch (e: ClassNotFoundException) {
        /* no op */
      } catch (e: NoSuchFieldException) {
        /* no op */
      }
      if (listenerInfoField == null || listenerField == null) {
        view.setOnClickListener(newListener)
        return
      }
      listenerInfoField.isAccessible = true
      listenerField.isAccessible = true
      var listenerObj: Any? = null
      try {
        listenerInfoField.isAccessible = true
        listenerObj = listenerInfoField[view]
      } catch (e: IllegalAccessException) {
        /* no op */
      }
      if (listenerObj == null) {
        view.setOnClickListener(newListener)
        return
      }
      listenerField[listenerObj] = newListener
    } catch (e: Exception) {
      /* no op */
    }
  }

  @JvmStatic
  fun getExistingOnTouchListener(view: View?): View.OnTouchListener? {
    try {
      val listenerInfoField = Class.forName("android.view.View").getDeclaredField("mListenerInfo")
      if (listenerInfoField != null) {
        listenerInfoField.isAccessible = true
      }
      val listenerObj = listenerInfoField[view] ?: return null
      var listener: View.OnTouchListener? = null
      val listenerField =
          Class.forName("android.view.View\$ListenerInfo").getDeclaredField("mOnTouchListener")
      if (listenerField != null) {
        listenerField.isAccessible = true
        listener = listenerField[listenerObj] as View.OnTouchListener
      }
      return listener
    } catch (e: NoSuchFieldException) {
      logd(TAG, e)
    } catch (e: ClassNotFoundException) {
      logd(TAG, e)
    } catch (e: IllegalAccessException) {
      logd(TAG, e)
    }
    return null
  }

  private fun getTouchReactView(location: FloatArray?, RCTRootView: View?): View? {
    initTouchTargetHelperMethods()
    if (null == methodFindTouchTargetView || null == RCTRootView) {
      return null
    }
    try {
      val nativeTargetView =
          checkNotNull(methodFindTouchTargetView).invoke(null, location, RCTRootView) as View
      if (nativeTargetView != null && nativeTargetView.id > 0) {
        return nativeTargetView.parent as View
      }
    } catch (e: IllegalAccessException) {
      logd(TAG, e)
    } catch (e: InvocationTargetException) {
      logd(TAG, e)
    }
    return null
  }

  fun isRCTButton(view: View, RCTRootView: View?): Boolean {
    // React Native Button and Touchable components are all ReactViewGroup
    val className = view.javaClass.name
    if (className == CLASS_RCTVIEWGROUP) {
      val location = getViewLocationOnScreen(view)
      val touchTargetView = getTouchReactView(location, RCTRootView)
      return touchTargetView != null && touchTargetView.id == view.id
    }
    return false
  }

  private fun isRCTRootView(view: View): Boolean {
    return view.javaClass.name == CLASS_RCTROOTVIEW
  }

  @JvmStatic
  fun findRCTRootView(view: View?): View? {
    var view = view
    while (null != view) {
      if (isRCTRootView(view)) {
        return view
      }
      val viewParent = view.parent
      view =
          if (viewParent is View) {
            viewParent
          } else {
            break
          }
    }
    return null
  }

  private fun getViewLocationOnScreen(view: View): FloatArray {
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    return floatArrayOf(location[0].toFloat(), location[1].toFloat())
  }

  private fun initTouchTargetHelperMethods() {
    if (null != methodFindTouchTargetView) {
      return
    }
    try {
      val RCTTouchTargetHelper = Class.forName(CLASS_TOUCHTARGETHELPER)
      methodFindTouchTargetView =
          RCTTouchTargetHelper.getDeclaredMethod(
              METHOD_FIND_TOUCHTARGET_VIEW, FloatArray::class.java, ViewGroup::class.java)
      checkNotNull(methodFindTouchTargetView).isAccessible = true
    } catch (e: ClassNotFoundException) {
      logd(TAG, e)
    } catch (e: NoSuchMethodException) {
      logd(TAG, e)
    }
  }

  private fun getExistingClass(className: String): Class<*>? {
    return try {
      Class.forName(className)
    } catch (e: ClassNotFoundException) {
      null
    }
  }
}
