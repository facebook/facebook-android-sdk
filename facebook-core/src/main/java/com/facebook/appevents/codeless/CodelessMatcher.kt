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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ListView
import androidx.annotation.UiThread
import com.facebook.FacebookException
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.appevents.codeless.CodelessLoggingEventListener.AutoLoggingOnClickListener
import com.facebook.appevents.codeless.CodelessLoggingEventListener.AutoLoggingOnItemClickListener
import com.facebook.appevents.codeless.internal.Constants
import com.facebook.appevents.codeless.internal.EventBinding
import com.facebook.appevents.codeless.internal.PathComponent
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.AppEventUtility.getRootView
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsWithoutQuery
import com.facebook.internal.InternalSettings.isUnityApp
import com.facebook.internal.Utility.coerceValueIfNullOrEmpty
import com.facebook.internal.Utility.logd
import com.facebook.internal.Utility.sha256hash
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@AutoHandleExceptions
internal class CodelessMatcher private constructor() {
  private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
  private val activitiesSet: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
  private val viewMatchers: MutableSet<ViewMatcher> = mutableSetOf()
  private var listenerSet: HashSet<String> = hashSetOf()
  private val activityToListenerMap: HashMap<Int, HashSet<String>> = hashMapOf()
  @UiThread
  fun add(activity: Activity) {
    if (isUnityApp) {
      return
    }
    if (Thread.currentThread() !== Looper.getMainLooper().thread) {
      throw FacebookException("Can't add activity to CodelessMatcher on non-UI thread")
    }
    activitiesSet.add(activity)
    listenerSet.clear()
    activityToListenerMap[activity.hashCode()]?.let { listenerSet = it }
    startTracking()
  }

  @UiThread
  fun remove(activity: Activity) {
    if (isUnityApp) {
      return
    }
    if (Thread.currentThread() !== Looper.getMainLooper().thread) {
      throw FacebookException("Can't remove activity from CodelessMatcher on non-UI thread")
    }
    activitiesSet.remove(activity)
    viewMatchers.clear()
    activityToListenerMap[activity.hashCode()] = listenerSet.clone() as HashSet<String>
    listenerSet.clear()
  }

  @UiThread
  fun destroy(activity: Activity) {
    activityToListenerMap.remove(activity.hashCode())
  }

  private fun startTracking() {
    if (Thread.currentThread() === Looper.getMainLooper().thread) {
      matchViews()
    } else {
      uiThreadHandler.post { matchViews() }
    }
  }

  private fun matchViews() {
    for (activity in activitiesSet) {
      if (null != activity) {
        val rootView = getRootView(activity)
        val activityName = activity.javaClass.simpleName
        val matcher = ViewMatcher(rootView, uiThreadHandler, listenerSet, activityName)
        viewMatchers.add(matcher)
      }
    }
  }

  class MatchedView(view: View, viewMapKey: String) {
    private val view: WeakReference<View>?
    val viewMapKey: String
    fun getView(): View? {
      return view?.get()
    }

    init {
      this.view = WeakReference(view)
      this.viewMapKey = viewMapKey
    }
  }

  @UiThread
  class ViewMatcher(
      rootView: View?,
      handler: Handler,
      listenerSet: HashSet<String>,
      activityName: String
  ) : ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnScrollChangedListener, Runnable {
    private val rootView: WeakReference<View?> = WeakReference(rootView)
    private var eventBindings: List<EventBinding>? = null
    private val handler: Handler = handler
    private val listenerSet: HashSet<String> = listenerSet
    private val activityName: String = activityName
    override fun run() {
      val appId = getApplicationId()
      val appSettings = getAppSettingsWithoutQuery(appId)
      if (appSettings == null || !appSettings.codelessEventsEnabled) {
        return
      }
      eventBindings = EventBinding.parseArray(appSettings.eventBindings)
      if (eventBindings != null) {
        val rootView = rootView.get() ?: return
        val observer = rootView.viewTreeObserver
        if (observer.isAlive) {
          observer.addOnGlobalLayoutListener(this)
          observer.addOnScrollChangedListener(this)
        }
        startMatch()
      }
    }

    override fun onGlobalLayout() {
      startMatch()
    }

    override fun onScrollChanged() {
      startMatch()
    }

    private fun startMatch() {
      eventBindings?.let {
        if (rootView.get() != null) {
          for (i in it.indices) {
            val binding = it[i]
            findView(binding, rootView.get())
          }
        }
      }
    }

    private fun findView(mapping: EventBinding?, rootView: View?) {
      if (mapping == null || rootView == null) {
        return
      }
      if (!mapping.activityName.isNullOrEmpty() && mapping.activityName != activityName) {
        return
      }
      val path = mapping.viewPath
      if (path.size > Constants.MAX_TREE_DEPTH) {
        return
      }
      val matchedViews = findViewByPath(mapping, rootView, path, 0, -1, activityName)
      for (view in matchedViews) {
        attachListener(view, rootView, mapping)
      }
    }

    private fun attachListener(matchedView: MatchedView, rootView: View, mapping: EventBinding?) {
      if (mapping == null) {
        return
      }
      try {
        val view = matchedView.getView() ?: return
        // If it's React Native Button, then attach React Native OnTouchListener
        val rctRootView = ViewHierarchy.findRCTRootView(view)
        if (null != rctRootView && ViewHierarchy.isRCTButton(view, rctRootView)) {
          attachRCTListener(matchedView, rootView, mapping)
          return
        }
        // Skip if the view comes from React Native
        if (view.javaClass.name.startsWith("com.facebook.react")) {
          return
        }
        if (view !is AdapterView<*>) {
          // attach onClickListener
          attachOnClickListener(matchedView, rootView, mapping)
        } else if (view is ListView) {
          // attach AdapterView onItemClickListener
          attachOnItemClickListener(matchedView, rootView, mapping)
        }
      } catch (e: Exception) {
        logd(TAG, e)
      }
    }

    private fun attachOnClickListener(
        matchedView: MatchedView,
        rootView: View,
        mapping: EventBinding
    ) {
      val view = matchedView.getView() ?: return
      val mapKey = matchedView.viewMapKey
      val existingListener = ViewHierarchy.getExistingOnClickListener(view)
      val isCodelessListener = existingListener is AutoLoggingOnClickListener
      val listenerSupportCodelessLogging =
          isCodelessListener &&
              (existingListener as AutoLoggingOnClickListener).supportCodelessLogging
      if (!listenerSet.contains(mapKey) && !listenerSupportCodelessLogging) {
        val listener: View.OnClickListener =
            CodelessLoggingEventListener.getOnClickListener(mapping, rootView, view)
        view.setOnClickListener(listener)
        listenerSet.add(mapKey)
      }
    }

    private fun attachOnItemClickListener(
        matchedView: MatchedView,
        rootView: View,
        mapping: EventBinding
    ) {
      val view = matchedView.getView() as AdapterView<*>? ?: return
      val mapKey = matchedView.viewMapKey
      val existingListener = view.onItemClickListener
      val isCodelessListener = existingListener is AutoLoggingOnItemClickListener
      val listenerSupportCodelessLogging =
          isCodelessListener &&
              (existingListener as AutoLoggingOnItemClickListener).supportCodelessLogging
      if (!listenerSet.contains(mapKey) && !listenerSupportCodelessLogging) {
        val listener: AdapterView.OnItemClickListener =
            CodelessLoggingEventListener.getOnItemClickListener(mapping, rootView, view)
        view.onItemClickListener = listener
        listenerSet.add(mapKey)
      }
    }

    private fun attachRCTListener(matchedView: MatchedView, rootView: View, mapping: EventBinding) {
      val view = matchedView.getView() ?: return
      val mapKey = matchedView.viewMapKey
      val existingListener = ViewHierarchy.getExistingOnTouchListener(view)
      val isRCTCodelessListener =
          existingListener is RCTCodelessLoggingEventListener.AutoLoggingOnTouchListener
      val listenerSupportCodelessLogging =
          isRCTCodelessListener &&
              (existingListener as RCTCodelessLoggingEventListener.AutoLoggingOnTouchListener)
                  .supportCodelessLogging
      if (!listenerSet.contains(mapKey) && !listenerSupportCodelessLogging) {
        val listener: View.OnTouchListener =
            RCTCodelessLoggingEventListener.getOnTouchListener(mapping, rootView, view)
        view.setOnTouchListener(listener)
        listenerSet.add(mapKey)
      }
    }

    companion object {
      @JvmStatic
      fun findViewByPath(
          mapping: EventBinding?,
          view: View?,
          path: List<PathComponent>,
          level: Int,
          index: Int,
          mapKey: String
      ): List<MatchedView> {
        val mapKey = "$mapKey.$index"
        val result: MutableList<MatchedView> = ArrayList()
        if (null == view) {
          return result
        }
        if (level >= path.size) {
          // Match all children views if their parent view is matched
          result.add(MatchedView(view, mapKey))
        } else {
          val pathElement = path[level]
          if (pathElement.className == PARENT_CLASS_NAME) {
            val parent = view.parent
            if (parent is ViewGroup) {
              val visibleViews = findVisibleChildren(parent)
              val childCount = visibleViews.size
              for (i in 0 until childCount) {
                val child = visibleViews[i]
                val matchedViews = findViewByPath(mapping, child, path, level + 1, i, mapKey)
                result.addAll(matchedViews)
              }
            }
            return result
          } else if (pathElement.className == CURRENT_CLASS_NAME) {
            // Set self as selected element
            result.add(MatchedView(view, mapKey))
            return result
          }
          if (!isTheSameView(view, pathElement, index)) {
            return result
          }

          // Found it!
          if (level == path.size - 1) {
            result.add(MatchedView(view, mapKey))
          }
        }
        if (view is ViewGroup) {
          val visibleViews = findVisibleChildren(view)
          val childCount = visibleViews.size
          for (i in 0 until childCount) {
            val child = visibleViews[i]
            val matchedViews = findViewByPath(mapping, child, path, level + 1, i, mapKey)
            result.addAll(matchedViews)
          }
        }
        return result
      }

      private fun isTheSameView(targetView: View, pathElement: PathComponent, index: Int): Boolean {
        if (pathElement.index != -1 && index != pathElement.index) {
          return false
        }
        if (targetView.javaClass.canonicalName != pathElement.className) {
          if (pathElement.className.matches(".*android\\..*".toRegex())) {
            val names = pathElement.className.split(".")
            if (names.isNotEmpty()) {
              val simpleName = names[names.size - 1]
              if (targetView.javaClass.simpleName != simpleName) {
                return false
              }
            } else {
              return false
            }
          } else {
            return false
          }
        }
        if (pathElement.matchBitmask and PathComponent.MatchBitmaskType.ID.value > 0) {
          if (pathElement.id != targetView.id) {
            return false
          }
        }
        if (pathElement.matchBitmask and PathComponent.MatchBitmaskType.TEXT.value > 0) {
          val pathText = pathElement.text
          val text = ViewHierarchy.getTextOfView(targetView)
          val hashedText = coerceValueIfNullOrEmpty(sha256hash(text), "")
          if (pathText != text && pathText != hashedText) {
            return false
          }
        }
        if (pathElement.matchBitmask and PathComponent.MatchBitmaskType.DESCRIPTION.value > 0) {
          val pathDesc = pathElement.description
          val targetDesc =
              if (targetView.contentDescription == null) ""
              else targetView.contentDescription.toString()
          val hashedDesc = coerceValueIfNullOrEmpty(sha256hash(targetDesc), "")
          if (pathDesc != targetDesc && pathDesc != hashedDesc) {
            return false
          }
        }
        if (pathElement.matchBitmask and PathComponent.MatchBitmaskType.HINT.value > 0) {
          val pathHint = pathElement.hint
          val targetHint = ViewHierarchy.getHintOfView(targetView)
          val hashedHint = coerceValueIfNullOrEmpty(sha256hash(targetHint), "")
          if (pathHint != targetHint && pathHint != hashedHint) {
            return false
          }
        }
        if (pathElement.matchBitmask and PathComponent.MatchBitmaskType.TAG.value > 0) {
          val tag = pathElement.tag
          val targetTag = if (targetView.tag == null) "" else targetView.tag.toString()
          val hashedTag = coerceValueIfNullOrEmpty(sha256hash(targetTag), "")
          if (tag != targetTag && tag != hashedTag) {
            return false
          }
        }
        return true
      }

      private fun findVisibleChildren(viewGroup: ViewGroup): List<View> {
        val visibleViews: MutableList<View> = ArrayList()
        val childCount = viewGroup.childCount
        for (i in 0 until childCount) {
          val child = viewGroup.getChildAt(i)
          if (child.visibility == View.VISIBLE) {
            visibleViews.add(child)
          }
        }
        return visibleViews
      }
    }

    init {
      this.handler.postDelayed(this, 200)
    }
  }

  companion object {
    private const val PARENT_CLASS_NAME = ".."
    private const val CURRENT_CLASS_NAME = "."
    private val TAG = CodelessMatcher::class.java.canonicalName
    private var codelessMatcher: CodelessMatcher? = null

    @Synchronized
    @JvmStatic
    fun getInstance(): CodelessMatcher {
      if (codelessMatcher == null) {
        codelessMatcher = CodelessMatcher()
      }
      return codelessMatcher as CodelessMatcher
    }

    @UiThread
    @JvmStatic
    fun getParameters(mapping: EventBinding?, rootView: View, hostView: View): Bundle {
      val params = Bundle()
      if (null == mapping) {
        return params
      }
      val parameters = mapping.viewParameters
      if (null != parameters) {
        for (component in parameters) {
          if (component.value != null && component.value.isNotEmpty()) {
            params.putString(component.name, component.value)
          } else if (component.path.size > 0) {
            var matchedViews: List<MatchedView>
            val pathType = component.pathType
            matchedViews =
                if (pathType == Constants.PATH_TYPE_RELATIVE) {
                  ViewMatcher.findViewByPath(
                      mapping, hostView, component.path, 0, -1, hostView.javaClass.simpleName)
                } else {
                  ViewMatcher.findViewByPath(
                      mapping, rootView, component.path, 0, -1, rootView.javaClass.simpleName)
                }
            for (view in matchedViews) {
              if (view.getView() == null) {
                continue
              }
              val text = ViewHierarchy.getTextOfView(view.getView())
              if (text.isNotEmpty()) {
                params.putString(component.name, text)
                break
              }
            }
          }
        }
      }
      return params
    }
  }
}
