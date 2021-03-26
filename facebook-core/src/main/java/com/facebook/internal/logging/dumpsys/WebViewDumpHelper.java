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

import android.annotation.SuppressLint;
import android.util.DisplayMetrics;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressLint({
  "NewApi",
  "StringFormatUse",
  "DefaultLocale",
  "BadMethodUse-java.lang.String.length"
})
final class WebViewDumpHelper {

  /**
   * A JS function that will be executed in the WebView to get the html inside it with absolute
   * position rectangle. We inject the absolute position of the WebView itself in Android to offset
   * the position of the html elements.
   */
  public static final String GET_WEBVIEW_HTML_JS_SCRIPT =
      "(function() {"
          + "  try {"
          + "    const leftOf = %d;"
          + "    const topOf = %d;"
          + "    const density = %f;"
          + ""
          + "    const elements = Array.from(document.querySelectorAll('body, body *'));"
          + "    for (const el of elements) {"
          + "      const rect = el.getBoundingClientRect();"
          + "      const left = Math.round(leftOf + rect.left * density);"
          + "      const top = Math.round(topOf + rect.top * density);"
          + "      const width = Math.round(rect.width * density);"
          + "      const height = Math.round(rect.height * density);"
          + "      el.setAttribute('data-rect', `${left},${top},${width},${height}`);"
          + ""
          + "      const style = window.getComputedStyle(el);"
          + "      const hidden = style.display === 'none' || style.visibility !== 'visible' || el.getAttribute('hidden') === 'true';"
          + "      const disabled = el.disabled || el.getAttribute('aria-disabled') === 'true';"
          + "      const focused = el === document.activeElement;"
          + "      if (hidden || disabled || focused) {"
          + "        el.setAttribute('data-flag', `${hidden ? 'H' : ''}${disabled ? 'D' : ''}${focused ? 'F' : ''}`);"
          + "      } else {"
          + "        el.removeAttribute('data-flag');"
          + "      }"
          + "    }"
          + "    document.activeElement.setAttribute('focused', 'true');"
          + ""
          + "    const doc = document.cloneNode(true);"
          + "    for (const el of Array.from(doc.querySelectorAll('script, link'))) {"
          + "      el.remove();"
          + "    }"
          + "    for (const el of Array.from(doc.querySelectorAll('*'))) {"
          + "      el.removeAttribute('class');"
          + "    }"
          + "    return doc.getElementsByTagName('body')[0].outerHTML.trim();"
          + "  } catch (e) {"
          + "    return 'Failed: ' + e;"
          + "  }"
          + "})();";

  private final Set<WebViewData> mWebViews = new HashSet<>();
  private final Map<String, String> mWebViewHTMLs = new HashMap<>();

  public void handle(WebView view) {
    final WebViewData data = new WebViewData(view);
    mWebViews.add(data);

    DisplayMetrics dm = view.getResources().getDisplayMetrics();
    String scriptWithOffset =
        String.format(GET_WEBVIEW_HTML_JS_SCRIPT, data.left, data.top, dm.scaledDensity);
    view.evaluateJavascript(
        scriptWithOffset,
        new ValueCallback<String>() {
          @Override
          public void onReceiveValue(String html) {
            mWebViewHTMLs.put(data.key, html);
          }
        });
  }

  public void dump(PrintWriter writer) {
    try {
      for (WebViewData data : mWebViews) {
        String html = mWebViewHTMLs.get(data.key);
        if (html != null) {
          writer.print("WebView HTML for ");
          writer.print(data);
          writer.println(":");
          writer.println(fixHtmlString(data, html));
        }
      }
    } catch (Exception ignore) {
    }
    mWebViews.clear();
    mWebViewHTMLs.clear();
  }

  private static String fixHtmlString(WebViewData data, String html) {
    html = html.replace("\\u003C", "<");
    html = html.replace("\\n", "");
    html = html.replace("\\\"", "\"");
    return String.format(
        "<html id=\"%s\" data-rect=\"%d,%d,%d,%d\">%s</html>",
        data.key,
        data.left,
        data.top,
        data.width,
        data.height,
        html.substring(1, html.length() - 1));
  }

  private static final class WebViewData {
    private static final int[] mLocation = new int[2];
    public final String key;
    public final int left;
    public final int top;
    public final int width;
    public final int height;

    public WebViewData(WebView webView) {
      key =
          String.format(
              "%s{%s}", webView.getClass().getName(), Integer.toHexString(webView.hashCode()));
      webView.getLocationOnScreen(mLocation);
      left = mLocation[0];
      top = mLocation[1];
      width = webView.getWidth();
      height = webView.getHeight();
    }
  }
}
