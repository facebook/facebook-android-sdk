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

import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import java.lang.RuntimeException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

class AndroidRootResolver {
  private var initialized = false
  private var windowManagerObj: Any? = null
  private var viewsField: Field? = null
  private var paramsField: Field? = null

  class Root(val view: View, val param: WindowManager.LayoutParams)
  interface Listener {
    fun onRootAdded(root: View?)
    fun onRootRemoved(root: View?)
    fun onRootsChanged(roots: List<View?>?)
  }

  class ListenableArrayList : ArrayList<View?>() {
    private var listener: Listener? = null
    fun setListener(listener: Listener?) {
      this.listener = listener
    }

    override fun add(value: View?): Boolean {
      val ret = super.add(value)
      if (ret && listener != null) {
        listener?.onRootAdded(value)
        listener?.onRootsChanged(this)
      }
      return ret
    }

    override fun remove(value: View?): Boolean {
      val ret = super.remove(value)
      if (ret && listener != null && value is View) {
        listener?.onRootRemoved(value as View?)
        listener?.onRootsChanged(this)
      }
      return ret
    }

    override fun removeAt(index: Int): View? {
      val view = super.removeAt(index)
      if (listener != null) {
        listener?.onRootRemoved(view)
        listener?.onRootsChanged(this)
      }
      return view
    }
  }

  fun attachActiveRootListener(listener: Listener?) {
    if (Build.VERSION.SDK_INT < 19 || listener == null) {
      // We dont have a use for this on older APIs. If you do then modify accordingly :)
      return
    }
    if (!initialized) {
      initialize()
    }
    try {
      val modifiers = Field::class.java.getDeclaredField("accessFlags")
      modifiers.isAccessible = true
      modifiers.setInt(viewsField, viewsField?.modifiers ?: 0 and Modifier.FINAL.inv())
      val views = viewsField?.get(windowManagerObj) as ArrayList<View>
      val listenableViews = ListenableArrayList()
      listenableViews.setListener(listener)
      listenableViews.addAll(views)
      viewsField?.set(windowManagerObj, listenableViews)
    } catch (e: Throwable) {
      Log.d(TAG, "Couldn't attach root listener.", e)
    }
  }

  fun listActiveRoots(): List<Root>? {
    if (!initialized) {
      initialize()
    }
    if (null == windowManagerObj) {
      Log.d(TAG, "No reflective access to windowmanager object.")
      return null
    }
    if (null == viewsField) {
      Log.d(TAG, "No reflective access to mViews")
      return null
    }
    if (null == paramsField) {
      Log.d(TAG, "No reflective access to mPArams")
      return null
    }
    var views: List<View>? = null
    var params: List<WindowManager.LayoutParams>? = null
    try {
      if (Build.VERSION.SDK_INT < 19) {
        views = (viewsField?.get(windowManagerObj) as Array<View>?)?.toList()
        params =
            (paramsField?.get(windowManagerObj) as Array<WindowManager.LayoutParams>?)?.toList()
      } else {
        views = viewsField?.get(windowManagerObj) as List<View>?
        params = paramsField?.get(windowManagerObj) as List<WindowManager.LayoutParams>?
      }
    } catch (re: RuntimeException) {
      Log.d(
          TAG,
          String.format(
              "Reflective access to %s or %s on %s failed.",
              viewsField,
              paramsField,
              windowManagerObj),
          re)
      return null
    } catch (iae: IllegalAccessException) {
      Log.d(
          TAG,
          String.format(
              "Reflective access to %s or %s on %s failed.",
              viewsField,
              paramsField,
              windowManagerObj),
          iae)
      return null
    }
    val roots: MutableList<Root> = ArrayList()
    (views ?: emptyList()).zip(params ?: emptyList()).forEach { (view, param) ->
      roots.add(Root(view, param))
    }
    return roots
  }

  private fun initialize() {
    initialized = true
    val accessClass =
        if (Build.VERSION.SDK_INT > 16) WINDOW_MANAGER_GLOBAL_CLAZZ else WINDOW_MANAGER_IMPL_CLAZZ
    val instanceMethod = if (Build.VERSION.SDK_INT > 16) GET_GLOBAL_INSTANCE else GET_DEFAULT_IMPL
    try {
      val clazz = Class.forName(accessClass)
      val getMethod = clazz.getMethod(instanceMethod)
      windowManagerObj = getMethod.invoke(null)
      viewsField = clazz.getDeclaredField(VIEWS_FIELD)
      viewsField?.isAccessible = true
      paramsField = clazz.getDeclaredField(WINDOW_PARAMS_FIELD)
      paramsField?.isAccessible = true
    } catch (ite: InvocationTargetException) {
      Log.d(
          TAG, String.format("could not invoke: %s on %s", instanceMethod, accessClass), ite.cause)
    } catch (cnfe: ClassNotFoundException) {
      Log.d(TAG, String.format("could not find class: %s", accessClass), cnfe)
    } catch (nsfe: NoSuchFieldException) {
      Log.d(
          TAG,
          String.format(
              "could not find field: %s or %s on %s",
              WINDOW_PARAMS_FIELD,
              VIEWS_FIELD,
              accessClass),
          nsfe)
    } catch (nsme: NoSuchMethodException) {
      Log.d(
          TAG, String.format("could not find method: %s on %s", instanceMethod, accessClass), nsme)
    } catch (re: RuntimeException) {
      Log.d(
          TAG,
          String.format(
              "reflective setup failed using obj: %s method: %s field: %s",
              accessClass,
              instanceMethod,
              VIEWS_FIELD),
          re)
    } catch (iae: IllegalAccessException) {
      Log.d(
          TAG,
          String.format(
              "reflective setup failed using obj: %s method: %s field: %s",
              accessClass,
              instanceMethod,
              VIEWS_FIELD),
          iae)
    }
  }

  companion object {
    private val TAG = AndroidRootResolver::class.java.simpleName
    private const val WINDOW_MANAGER_IMPL_CLAZZ = "android.view.WindowManagerImpl"
    private const val WINDOW_MANAGER_GLOBAL_CLAZZ = "android.view.WindowManagerGlobal"
    private const val VIEWS_FIELD = "mViews"
    private const val WINDOW_PARAMS_FIELD = "mParams"
    private const val GET_DEFAULT_IMPL = "getDefault"
    private const val GET_GLOBAL_INSTANCE = "getInstance"
  }
}
