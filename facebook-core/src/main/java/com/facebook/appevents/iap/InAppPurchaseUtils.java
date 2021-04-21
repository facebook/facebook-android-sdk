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

package com.facebook.appevents.iap;

import androidx.annotation.Nullable;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@AutoHandleExceptions
public class InAppPurchaseUtils {
  /** Returns the Class object associated with the class or interface with the given string name */
  @Nullable
  public static Class<?> getClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Returns a Method object that reflects the specified public member method of the class or
   * interface represented by this Class object.
   */
  @Nullable
  public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... args) {
    try {
      return clazz.getMethod(methodName, args);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Invokes the underlying method represented by this Method object, on the specified object with
   * the specified parameters.
   */
  @Nullable
  public static Object invokeMethod(
      Class<?> clazz, Method method, @Nullable Object obj, @Nullable Object... args) {
    if (obj != null) {
      obj = clazz.cast(obj);
    }

    try {
      return method.invoke(obj, args);
    } catch (IllegalAccessException e) {
      /* swallow */
    } catch (InvocationTargetException e) {
      /* swallow */
    }
    return null;
  }
}
