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
import android.webkit.WebView
import java.io.PrintWriter
import java.lang.Exception

@SuppressLint("NewApi", "StringFormatUse", "DefaultLocale", "BadMethodUse-java.lang.String.length")
class WebViewDumpHelper {
  private val webViews: MutableSet<WebViewData> = mutableSetOf()
  private val webViewHTMLs: MutableMap<String, String> = mutableMapOf()
  fun handle(view: WebView) {
    val data = WebViewData(view)
    webViews.add(data)
    val dm = view.resources.displayMetrics
    val scriptWithOffset =
        String.format(GET_WEBVIEW_HTML_JS_SCRIPT, data.left, data.top, dm.scaledDensity)
    view.evaluateJavascript(scriptWithOffset) { html -> webViewHTMLs[data.key] = html }
  }

  fun dump(writer: PrintWriter) {
    try {
      for (data in webViews) {
        val html = webViewHTMLs[data.key]
        if (html != null) {
          writer.print("WebView HTML for ")
          writer.print(data)
          writer.println(":")
          writer.println(fixHtmlString(data, html))
        }
      }
    } catch (ignore: Exception) {}
    webViews.clear()
    webViewHTMLs.clear()
  }

  private class WebViewData(webView: WebView) {
    val key: String
    val left: Int
    val top: Int
    val width: Int
    val height: Int

    companion object {
      private val location = IntArray(2)
    }

    init {
      key = String.format("%s{%s}", webView.javaClass.name, Integer.toHexString(webView.hashCode()))
      webView.getLocationOnScreen(location)
      left = location[0]
      top = location[1]
      width = webView.width
      height = webView.height
    }
  }

  companion object {
    /**
     * A JS function that will be executed in the WebView to get the html inside it with absolute
     * position rectangle. We inject the absolute position of the WebView itself in Android to
     * offset the position of the html elements.
     */
    const val GET_WEBVIEW_HTML_JS_SCRIPT =
        "(function() {" +
            "  try {" +
            "    const leftOf = %d;" +
            "    const topOf = %d;" +
            "    const density = %f;" +
            "" +
            "    const elements = Array.from(document.querySelectorAll('body, body *'));" +
            "    for (const el of elements) {" +
            "      const rect = el.getBoundingClientRect();" +
            "      const left = Math.round(leftOf + rect.left * density);" +
            "      const top = Math.round(topOf + rect.top * density);" +
            "      const width = Math.round(rect.width * density);" +
            "      const height = Math.round(rect.height * density);" +
            "      el.setAttribute('data-rect', `\${left},\${top},\${width},\${height}`);" +
            "" +
            "      const style = window.getComputedStyle(el);" +
            "      const hidden = style.display === 'none' || style.visibility !== 'visible' || el.getAttribute('hidden') === 'true';" +
            "      const disabled = el.disabled || el.getAttribute('aria-disabled') === 'true';" +
            "      const focused = el === document.activeElement;" +
            "      if (hidden || disabled || focused) {" +
            "        el.setAttribute('data-flag', `\${hidden ? 'H' : ''}\${disabled ? 'D' : ''}\${focused ? 'F' : ''}`);" +
            "      } else {" +
            "        el.removeAttribute('data-flag');" +
            "      }" +
            "    }" +
            "    document.activeElement.setAttribute('focused', 'true');" +
            "" +
            "    const doc = document.cloneNode(true);" +
            "    for (const el of Array.from(doc.querySelectorAll('script, link'))) {" +
            "      el.remove();" +
            "    }" +
            "    for (const el of Array.from(doc.querySelectorAll('*'))) {" +
            "      el.removeAttribute('class');" +
            "    }" +
            "    return doc.getElementsByTagName('body')[0].outerHTML.trim();" +
            "  } catch (e) {" +
            "    return 'Failed: ' + e;" +
            "  }" +
            "})();"

    private fun fixHtmlString(data: WebViewData, html: String): String {
      var html = html
      html = html.replace("\\u003C", "<").replace("\\n", "").replace("\\\"", "\"")
      return String.format(
          "<html id=\"%s\" data-rect=\"%d,%d,%d,%d\">%s</html>",
          data.key,
          data.left,
          data.top,
          data.width,
          data.height,
          html.substring(1, html.length - 1))
    }
  }
}
