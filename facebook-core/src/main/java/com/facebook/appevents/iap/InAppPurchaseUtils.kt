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
package com.facebook.appevents.iap

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
}
