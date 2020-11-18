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
package com.facebook.core.internal.logging.dumpsys;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint({
  "HexColorValueUsage",
  "CatchGeneralException",
  "BadMethodUse-java.lang.String.length"
})
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Internal copy"))
public final class EndToEndDumpsysHelper {

  private static final String LITHO_VIEW_CLASS = "com.facebook.litho.LithoView";
  private static final String LITHO_VIEW_TEST_HELPER_CLASS =
      "com.facebook.litho.LithoViewTestHelper";
  private static final String LITHO_VIEW_TO_STRING_METHOD = "viewToStringForE2E";

  private static final String E2E_ARGUMENT = "e2e";
  private static final String TOP_ROOT_ARGUMENT = "top-root";
  private static final String ALL_ROOTS_ARGUMENT = "all-roots";
  private static final String WITH_WEBVIEW_ARGUMENT = "webview";
  private static final String WITH_PROPS_ARGUMENT = "props";
  private static final String RC_TEXT_VIEW_SIMPLE_CLASS_NAME = "RCTextView";

  private static EndToEndDumpsysHelper mInstance;
  @Nullable private static Method sRcTextViewGetTextMethod;

  private final AndroidRootResolver mRootResolver = new AndroidRootResolver();
  private final WebViewDumpHelper mWebViewDumpHelper = new WebViewDumpHelper();
  @Nullable private Method mLithoViewToStringMethod;

  public EndToEndDumpsysHelper() {}

  public static boolean maybeDump(String prefix, PrintWriter writer, String[] args) {
    if (args != null && args.length > 0 && EndToEndDumpsysHelper.E2E_ARGUMENT.equals(args[0])) {
      // run special DBF view-hierarchy dump if requested using e2e arg
      if (mInstance == null) {
        // always runs on the main thread
        mInstance = new EndToEndDumpsysHelper();
      }
      mInstance.dumpViewHierarchy(prefix, writer, args);
      return true;
    }
    return false;
  }

  private void dumpViewHierarchy(String prefix, PrintWriter writer, String[] args) {
    writer.print(prefix);
    writer.println("Top Level Window View Hierarchy:");
    boolean dumpAllRoots = hasArgument(args, ALL_ROOTS_ARGUMENT);
    boolean dumpTopRootOnly = hasArgument(args, TOP_ROOT_ARGUMENT);
    boolean withWebView = hasArgument(args, WITH_WEBVIEW_ARGUMENT);
    boolean withProps = hasArgument(args, WITH_PROPS_ARGUMENT);

    try {
      List<AndroidRootResolver.Root> roots = mRootResolver.listActiveRoots();
      if (roots == null || roots.isEmpty()) {
        // no roots view, nothing to output
        return;
      }
      Collections.reverse(roots);

      // the last root with a visible view is the root having the top level view
      // it can have multiple views on the same window identified by same token and incremental type
      WindowManager.LayoutParams prevRootParam = null;
      for (AndroidRootResolver.Root root : roots) {
        if (root.view.getVisibility() != View.VISIBLE) {
          continue;
        }
        // handle multiple root views on the same window (dialogs, tooltips)
        if (!dumpAllRoots
            && prevRootParam != null
            && Math.abs(root.param.type - prevRootParam.type) != 1) {
          // the view is no longer on the same window, stop dump
          break;
        }
        dumpViewHierarchy(prefix + "  ", writer, root.view, 0, 0, withWebView, withProps);
        prevRootParam = root.param;
        if (dumpTopRootOnly) {
          break;
        }
      }

      // additional dump of web view html data
      mWebViewDumpHelper.dump(writer);
    } catch (Exception e) {
      writer.println("Failure in view hierarchy dump: " + e.getMessage());
    }
  }

  private void dumpViewHierarchy(
      String prefix,
      PrintWriter writer,
      @Nullable View view,
      int leftOffset,
      int topOffset,
      boolean withWebView,
      boolean withProps) {
    writer.print(prefix);

    if (view == null) {
      writer.println("null");
      return;
    }

    writer.print(view.getClass().getName());
    writer.print("{");
    writer.print(Integer.toHexString(view.hashCode()));
    writeViewFlags(writer, view);
    writeViewBounds(writer, view, leftOffset, topOffset);
    writeViewTestId(writer, view);
    writeViewText(writer, view);
    if (withProps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Api21Utils.writeExtraProps(writer, view);
    }
    writer.println("}");

    if (isExtendsLithoView(view)) {
      writeLithoViewSubHierarchy(writer, view, prefix, withProps);
    }

    if (withWebView && view instanceof WebView) {
      mWebViewDumpHelper.handle((WebView) view);
    }

    if (!(view instanceof ViewGroup)) {
      return;
    }

    ViewGroup grp = (ViewGroup) view;
    final int n = grp.getChildCount();
    if (n <= 0) {
      return;
    }
    prefix = prefix + "  ";

    int[] location = new int[2];
    view.getLocationOnScreen(location);
    for (int i = 0; i < n; i++) {
      dumpViewHierarchy(
          prefix, writer, grp.getChildAt(i), location[0], location[1], withWebView, withProps);
    }
  }

  private static boolean isExtendsLithoView(View view) {
    Class<?> aClass = view.getClass();
    while (aClass != null) {
      if (aClass.getName().equals(LITHO_VIEW_CLASS)) {
        return true;
      }
      aClass = aClass.getSuperclass();
    }
    return false;
  }

  private void writeLithoViewSubHierarchy(
      PrintWriter writer, View view, String prefix, boolean withProps) {
    try {
      if (mLithoViewToStringMethod == null) {
        Class<?> helperClass = Class.forName(LITHO_VIEW_TEST_HELPER_CLASS);
        mLithoViewToStringMethod =
            helperClass.getDeclaredMethod(
                LITHO_VIEW_TO_STRING_METHOD, View.class, int.class, boolean.class);
      }

      // Litho contains UI inside a single view with special handling
      // We don't want to call explicitly because we don't want to have a litho dependency
      String lithoViewDump =
          (String) mLithoViewToStringMethod.invoke(null, view, prefix.length() / 2 + 1, withProps);
      writer.append(lithoViewDump);
    } catch (Exception e) {
      writer
          .append(prefix)
          .append("Failed litho view sub hierarch dump: ")
          .append(fixString(e.getMessage(), 100))
          .println();
    }
  }

  private static void writeViewFlags(PrintWriter writer, View view) {
    writer.print(" ");
    switch (view.getVisibility()) {
      case View.VISIBLE:
        writer.print("V");
        break;
      case View.INVISIBLE:
        writer.print("I");
        break;
      case View.GONE:
        writer.print("G");
        break;
      default:
        writer.print(".");
    }
    writer.print(view.isFocusable() ? "F" : ".");
    writer.print(view.isEnabled() ? "E" : ".");
    writer.print(".");
    writer.print(view.isHorizontalScrollBarEnabled() ? "H" : ".");
    writer.print(view.isVerticalScrollBarEnabled() ? "V" : ".");
    writer.print(view.isClickable() ? "C" : ".");
    writer.print(view.isLongClickable() ? "L" : ".");
    writer.print(" ");
    writer.print(view.isFocused() ? "F" : ".");
    writer.print(view.isSelected() ? "S" : ".");
    writer.print(view.isHovered() ? "H" : ".");
    writer.print(view.isActivated() ? "A" : ".");
    writer.print(view.isDirty() ? "D" : ".");
  }

  private static void writeViewBounds(
      PrintWriter writer, View view, int leftOffset, int topOffset) {
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    writer.print(" ");
    writer.print(location[0] - leftOffset);
    writer.print(",");
    writer.print(location[1] - topOffset);
    writer.print("-");
    writer.print(location[0] + view.getWidth() - leftOffset);
    writer.print(",");
    writer.print(location[1] + view.getHeight() - topOffset);
  }

  private static void writeViewTestId(PrintWriter writer, View view) {
    try {
      int id = view.getId();
      if (id == View.NO_ID) {
        maybeWriteViewTestIdFromTag(writer, view);
        return;
      }

      writer.append(" #");
      writer.append(Integer.toHexString(id));

      Resources resources = view.getResources();
      if (id <= 0 || resources == null) {
        maybeWriteViewTestIdFromTag(writer, view);
        return;
      }

      String packageName;
      switch (id & 0xff000000) {
        case 0x7f000000:
          packageName = "app";
          break;
        case 0x01000000:
          packageName = "android";
          break;
        default:
          packageName = resources.getResourcePackageName(id);
          break;
      }

      writer.print(" ");
      writer.print(packageName);
      writer.print(":");
      writer.print(resources.getResourceTypeName(id));
      writer.print("/");
      writer.print(resources.getResourceEntryName(id));
    } catch (Exception e) {
      // ignore
      maybeWriteViewTestIdFromTag(writer, view);
    }
  }

  private static void maybeWriteViewTestIdFromTag(PrintWriter writer, View view) {
    Object tag = view.getTag();
    if (!(tag instanceof String)) {
      return;
    }
    String tagStr = (String) tag;
    if (tagStr.length() <= 0) {
      return;
    }
    writer.print(" app:tag/");
    writer.print(fixString(tagStr, 60));
  }

  @SuppressLint("ReflectionMethodUse")
  private static void writeViewText(PrintWriter writer, View view) {
    try {
      String text = null;
      if (view instanceof TextView) {
        text = ((TextView) view).getText().toString();
      } else if (
      // this class in general isn't invoked from product code, so this reflection should be safe.
      view.getClass().getSimpleName().equals(RC_TEXT_VIEW_SIMPLE_CLASS_NAME)) {
        text = getTextFromRcTextView(view);
      } else {
        CharSequence content = view.getContentDescription();
        if (content != null) {
          text = content.toString();
        }
        if (text == null || text.isEmpty()) {
          Object tag = view.getTag();
          if (tag != null) {
            text = tag.toString().trim();
          }
        }
      }
      if (text == null || text.isEmpty()) {
        return;
      }
      writer.print(" text=\"");
      writer.print(fixString(text, 600));
      writer.print("\"");
    } catch (Exception e) {
      // ignore
    }
  }

  @SuppressLint({"PrivateApi", "ReflectionMethodUse"})
  @Nullable
  private static String getTextFromRcTextView(View view)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    if (sRcTextViewGetTextMethod == null) {
      sRcTextViewGetTextMethod = view.getClass().getDeclaredMethod("getText");
    }
    Object textViewValue = sRcTextViewGetTextMethod.invoke(view);
    return textViewValue != null ? textViewValue.toString() : null;
  }

  private static String fixString(@Nullable CharSequence str, int maxLength) {
    if (str == null || str.length() < 1) {
      return "";
    }
    String fixed = str.toString().replace(" \n", " ").replace("\n", " ").replace("\"", "");
    if (str.length() > maxLength) {
      fixed = fixed.substring(0, maxLength) + "...";
    }
    return fixed;
  }

  private static boolean hasArgument(String[] args, String argument) {
    if (args == null) {
      return false;
    }
    for (String arg : args) {
      if (argument.equalsIgnoreCase(arg)) {
        return true;
      }
    }
    return false;
  }

  private static @Nullable AccessibilityNodeInfo createNodeInfoFromView(View view) {
    if (view == null) {
      return null;
    }
    final AccessibilityNodeInfo nodeInfo = AccessibilityNodeInfo.obtain();
    // For some unknown reason, Android seems to occasionally throw a NPE from
    // onInitializeAccessibilityNodeInfo.
    try {
      view.onInitializeAccessibilityNodeInfo(nodeInfo);
    } catch (NullPointerException e) {
      if (nodeInfo != null) {
        nodeInfo.recycle();
      }
      return null;
    }
    return nodeInfo;
  }

  /** Optimization for pre api21 devices */
  private static class Api21Utils {

    @Nullable private static Field mKeyedTagsField;

    static {
      try {
        mKeyedTagsField = View.class.getDeclaredField("mKeyedTags");
        mKeyedTagsField.setAccessible(true);
      } catch (NoSuchFieldException e) {
        // ignore
      }
    }

    static void writeExtraProps(PrintWriter writer, View view) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return;
      }
      final AccessibilityNodeInfo nodeInfo = createNodeInfoFromView(view);
      if (nodeInfo == null) {
        return;
      }

      JSONObject props = new JSONObject();
      try {
        if (view instanceof TextView) {
          TextView textView = (TextView) view;
          props.put("textColor", textView.getTextColors().getDefaultColor());
          props.put("textSize", textView.getTextSize());
          props.put("hint", fixString(textView.getHint(), 100));
        }

        JSONObject tags = getTags(view);
        if (tags != null) {
          props.put("keyedTags", tags);
        }

        JSONArray actions = new JSONArray();
        for (AccessibilityNodeInfo.AccessibilityAction action : nodeInfo.getActionList()) {
          final String actionLabel = (String) action.getLabel();
          if (actionLabel != null) {
            actions.put(fixString(actionLabel, 50));
          }
        }
        if (actions.length() > 0) {
          props.put("actions", actions);
        }

        String cd = fixString(nodeInfo.getContentDescription(), 50);
        if (cd != null && cd.length() > 0) {
          props.put("content-description", cd);
        }

        props
            .put("accessibility-focused", nodeInfo.isAccessibilityFocused())
            .put("checkable", nodeInfo.isCheckable())
            .put("checked", nodeInfo.isChecked())
            .put("class-name", fixString(nodeInfo.getClassName(), 50))
            .put("clickable", nodeInfo.isClickable())
            .put("content-invalid", nodeInfo.isContentInvalid())
            .put("dismissable", nodeInfo.isDismissable())
            .put("editable", nodeInfo.isEditable())
            .put("enabled", nodeInfo.isEnabled())
            .put("focusable", nodeInfo.isFocusable())
            .put("focused", nodeInfo.isFocused())
            .put("long-clickable", nodeInfo.isLongClickable())
            .put("multiline", nodeInfo.isMultiLine())
            .put("password", nodeInfo.isPassword())
            .put("scrollable", nodeInfo.isScrollable())
            .put("selected", nodeInfo.isSelected())
            .put("visible-to-user", nodeInfo.isVisibleToUser());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          Api24Utils.addExtraProps(props, nodeInfo);
        }
      } catch (Exception e) {
        try {
          props.put("DUMP-ERROR", fixString(e.getMessage(), 50));
        } catch (JSONException ex) {
          // ignore
        }
      }
      writer.append(" props=\"").append(props.toString()).append("\"");
    }

    private static @Nullable JSONObject getTags(View view) {
      JSONObject tags = null;
      try {
        if (mKeyedTagsField == null) {
          mKeyedTagsField = View.class.getDeclaredField("mKeyedTags");
          mKeyedTagsField.setAccessible(true);
        }
        SparseArray keyedTags = (SparseArray) mKeyedTagsField.get(view);
        if (keyedTags != null && keyedTags.size() > 0) {
          tags = new JSONObject();
          for (int i = 0, count = keyedTags.size(); i < count; i++) {
            final String id =
                ResourcesUtil.getIdStringQuietly(
                    view.getContext(), view.getResources(), keyedTags.keyAt(i));
            try {
              tags.put(id, keyedTags.valueAt(i));
            } catch (JSONException e) {
              // ignore
            }
          }
        }
      } catch (Exception e) {
        // ignore
      }
      return tags;
    }
  }

  /** Handle VerifyError bug in Android 5.* */
  private static class Api24Utils {
    static void addExtraProps(JSONObject props, AccessibilityNodeInfo nodeInfo)
        throws JSONException {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        return;
      }
      props
          .put("context-clickable", nodeInfo.isContextClickable())
          .put("drawing-order", nodeInfo.getDrawingOrder())
          .put("important-for-accessibility", nodeInfo.isImportantForAccessibility());
    }
  }
}
