/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@AutoHandleExceptions
object InAppPurchaseUtils {
  /** Returns the Class object associated with the class or interface with the given string name */
  @JvmStatic
  fun getClass(className: String): Class<*>? {
    return try {
      Class.forName(className)
    } catch (e: ClassNotFoundException) {
      null
    }
  }

  /**
   * Returns a Method object that reflects the specified public member method of the class or
   * interface represented by this Class object.
   */
  @JvmStatic
  fun getMethod(clazz: Class<*>, methodName: String, vararg args: Class<*>?): Method? {
    return try {
      clazz.getMethod(methodName, *args)
    } catch (e: NoSuchMethodException) {
      null
    }
  }

  /**
   * Gets the declared method from class provided and returns the method to be use for invocation
   */
  @JvmStatic
  internal fun getDeclaredMethod(
      clazz: Class<*>,
      methodName: String,
      vararg args: Class<*>?
  ): Method? {
    return try {
      clazz.getDeclaredMethod(methodName, *args)
    } catch (e: NoSuchMethodException) {
      null
    }
  }

  /**
   * Invokes the underlying method represented by this Method object, on the specified object with
   * the specified parameters.
   */
  @JvmStatic
  fun invokeMethod(clazz: Class<*>, method: Method, obj: Any?, vararg args: Any?): Any? {
    var obj = obj
    if (obj != null) {
      obj = clazz.cast(obj)
    }
    try {
      return method.invoke(obj, *args)
    } catch (e: IllegalAccessException) {
      /* swallow */
    } catch (e: InvocationTargetException) {
      /* swallow */
    }
    return null
  }

  /** Gets class from the context class loader and returns null if class is not found. */
  @JvmStatic
  internal fun getClassFromContext(context: Context, className: String): Class<*>? {
    try {
      return context.classLoader.loadClass(className)
    } catch (e: ClassNotFoundException) {
      return null
    }
  }
}
