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
package com.facebook.internal.logging.dumpsys;

import static android.view.WindowManager.LayoutParams;

import android.os.Build;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Internal copy"))
public final class AndroidRootResolver {

  private static final String TAG = AndroidRootResolver.class.getSimpleName();
  private static final String WINDOW_MANAGER_IMPL_CLAZZ = "android.view.WindowManagerImpl";
  private static final String WINDOW_MANAGER_GLOBAL_CLAZZ = "android.view.WindowManagerGlobal";
  private static final String VIEWS_FIELD = "mViews";
  private static final String WINDOW_PARAMS_FIELD = "mParams";
  private static final String GET_DEFAULT_IMPL = "getDefault";
  private static final String GET_GLOBAL_INSTANCE = "getInstance";

  private boolean initialized;
  private Object windowManagerObj;
  private Field viewsField;
  private Field paramsField;

  public static class Root {
    public final View view;
    public final LayoutParams param;

    private Root(View view, LayoutParams param) {
      this.view = view;
      this.param = param;
    }
  }

  public interface Listener {
    void onRootAdded(View root);

    void onRootRemoved(View root);

    void onRootsChanged(List<View> roots);
  }

  public static class ListenableArrayList extends ArrayList<View> {
    @Nullable private Listener listener;

    public void setListener(Listener listener) {
      this.listener = listener;
    }

    @Override
    public boolean add(View value) {
      boolean ret = super.add(value);
      if (ret && listener != null) {
        listener.onRootAdded(value);
        listener.onRootsChanged(this);
      }
      return ret;
    }

    @Override
    public boolean remove(@Nullable Object value) {
      boolean ret = super.remove(value);
      if (ret && listener != null && value instanceof View) {
        listener.onRootRemoved((View) value);
        listener.onRootsChanged(this);
      }
      return ret;
    }

    @Override
    public View remove(int index) {
      View view = super.remove(index);
      if (listener != null) {
        listener.onRootRemoved(view);
        listener.onRootsChanged(this);
      }
      return view;
    }
  }

  public void attachActiveRootListener(Listener listener) {
    if (Build.VERSION.SDK_INT < 19 || listener == null) {
      // We dont have a use for this on older APIs. If you do then modify accordingly :)
      return;
    }

    if (!initialized) {
      initialize();
    }

    try {
      Field modifiers = Field.class.getDeclaredField("accessFlags");
      modifiers.setAccessible(true);
      modifiers.setInt(viewsField, viewsField.getModifiers() & ~Modifier.FINAL);
      ArrayList<View> views = (ArrayList<View>) viewsField.get(windowManagerObj);
      ListenableArrayList listenableViews = new ListenableArrayList();
      listenableViews.setListener(listener);
      listenableViews.addAll(views);
      viewsField.set(windowManagerObj, listenableViews);
    } catch (Throwable e) {
      Log.d(TAG, "Couldn't attach root listener.", e);
    }
  }

  public @Nullable List<Root> listActiveRoots() {
    if (!initialized) {
      initialize();
    }

    if (null == windowManagerObj) {
      Log.d(TAG, "No reflective access to windowmanager object.");
      return null;
    }

    if (null == viewsField) {
      Log.d(TAG, "No reflective access to mViews");
      return null;
    }
    if (null == paramsField) {
      Log.d(TAG, "No reflective access to mPArams");
      return null;
    }

    List<View> views = null;
    List<LayoutParams> params = null;

    try {
      if (Build.VERSION.SDK_INT < 19) {
        views = Arrays.asList((View[]) viewsField.get(windowManagerObj));
        params = Arrays.asList((LayoutParams[]) paramsField.get(windowManagerObj));
      } else {
        views = (List<View>) viewsField.get(windowManagerObj);
        params = (List<LayoutParams>) paramsField.get(windowManagerObj);
      }
    } catch (RuntimeException re) {
      Log.d(
          TAG,
          String.format(
              "Reflective access to %s or %s on %s failed.",
              viewsField, paramsField, windowManagerObj),
          re);
      return null;
    } catch (IllegalAccessException iae) {
      Log.d(
          TAG,
          String.format(
              "Reflective access to %s or %s on %s failed.",
              viewsField, paramsField, windowManagerObj),
          iae);
      return null;
    }

    List<Root> roots = new ArrayList<>();
    for (int i = 0, stop = views.size(); i < stop; i++) {
      roots.add(new Root(views.get(i), params.get(i)));
    }
    return roots;
  }

  private void initialize() {
    initialized = true;
    String accessClass =
        Build.VERSION.SDK_INT > 16 ? WINDOW_MANAGER_GLOBAL_CLAZZ : WINDOW_MANAGER_IMPL_CLAZZ;
    String instanceMethod = Build.VERSION.SDK_INT > 16 ? GET_GLOBAL_INSTANCE : GET_DEFAULT_IMPL;

    try {
      Class<?> clazz = Class.forName(accessClass);
      Method getMethod = clazz.getMethod(instanceMethod);
      windowManagerObj = getMethod.invoke(null);
      viewsField = clazz.getDeclaredField(VIEWS_FIELD);
      viewsField.setAccessible(true);
      paramsField = clazz.getDeclaredField(WINDOW_PARAMS_FIELD);
      paramsField.setAccessible(true);
    } catch (InvocationTargetException ite) {
      Log.d(
          TAG,
          String.format("could not invoke: %s on %s", instanceMethod, accessClass),
          ite.getCause());
    } catch (ClassNotFoundException cnfe) {
      Log.d(TAG, String.format("could not find class: %s", accessClass), cnfe);
    } catch (NoSuchFieldException nsfe) {
      Log.d(
          TAG,
          String.format(
              "could not find field: %s or %s on %s",
              WINDOW_PARAMS_FIELD, VIEWS_FIELD, accessClass),
          nsfe);
    } catch (NoSuchMethodException nsme) {
      Log.d(
          TAG, String.format("could not find method: %s on %s", instanceMethod, accessClass), nsme);
    } catch (RuntimeException re) {
      Log.d(
          TAG,
          String.format(
              "reflective setup failed using obj: %s method: %s field: %s",
              accessClass, instanceMethod, VIEWS_FIELD),
          re);
    } catch (IllegalAccessException iae) {
      Log.d(
          TAG,
          String.format(
              "reflective setup failed using obj: %s method: %s field: %s",
              accessClass, instanceMethod, VIEWS_FIELD),
          iae);
    }
  }
}
