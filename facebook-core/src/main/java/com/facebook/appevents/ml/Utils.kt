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
package com.facebook.appevents.ml

import android.text.TextUtils
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File
import java.nio.charset.Charset

@AutoHandleExceptions
object Utils {
  private const val DIR_NAME = "facebook_ml/"
  fun vectorize(texts: String, maxLen: Int): IntArray {
    val ret = IntArray(maxLen)
    val normalizedStr = normalizeString(texts)
    val strBytes = normalizedStr.toByteArray(Charset.forName("UTF-8"))
    for (i in 0 until maxLen) {
      if (i < strBytes.size) {
        ret[i] = strBytes[i].toInt() and 0xFF
      } else {
        ret[i] = 0
      }
    }
    return ret
  }

  fun normalizeString(str: String): String {
    val trim = str.trim { it <= ' ' }
    val strArray = trim.split("\\s+".toRegex()).toTypedArray()
    return TextUtils.join(" ", strArray)
  }

  @JvmStatic
  fun getMlDir(): File? {
    val dir = File(FacebookSdk.getApplicationContext().filesDir, DIR_NAME)
    return if (dir.exists() || dir.mkdirs()) {
      dir
    } else null
  }
}
