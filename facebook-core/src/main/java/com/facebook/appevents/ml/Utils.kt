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
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

  /**
   * Parse AppEvents model files.
   *
   * The model file format is described by following parts:
   * 1. First 4 bytes: an 32-bit integer encoded in little-endian order for the length of metadata
   * string, denoted by *L*.
   * 2. Next *L* bytes: a JSONObject string for the metadata. Its keys are the name of the weights
   * and its values are the shape of the weight tensors.
   * 3. Then there are multiple float arrays encoded with the little-endian order for all weight
   * tensors. The arrays are ordered by the lexicographic order of keys in the metadata.
   *
   * @param file the model file object
   * @return a map of weight key string to the weight tensor
   */
  @JvmStatic
  fun parseModelWeights(file: File): Map<String, MTensor>? {
    try {
      val inputStream: InputStream = FileInputStream(file)
      val length = inputStream.available()
      val dataIs = DataInputStream(inputStream)
      val allData = ByteArray(length)
      dataIs.readFully(allData)
      dataIs.close()
      if (length < 4) {
        return null
      }
      var bb = ByteBuffer.wrap(allData, 0, 4)
      bb.order(ByteOrder.LITTLE_ENDIAN)
      val jsonLen = bb.int
      if (length < jsonLen + 4) {
        return null
      }
      val jsonStr = String(allData, 4, jsonLen)
      val info = JSONObject(jsonStr)
      val names = info.names()
      val keys = arrayOfNulls<String>(names.length())
      for (i in keys.indices) {
        keys[i] = names.getString(i)
      }
      keys.sort()
      var offset = 4 + jsonLen
      val weights = hashMapOf<String, MTensor>()
      for (key in keys) {
        if (key == null) {
          continue
        }
        var count = 1
        val shapes = info.getJSONArray(key)
        val shape = IntArray(shapes.length())
        for (i in shape.indices) {
          shape[i] = shapes.getInt(i)
          count *= shape[i]
        }
        if (offset + count * 4 > length) {
          return null
        }
        bb = ByteBuffer.wrap(allData, offset, count * 4)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        val tensor = MTensor(shape)
        bb.asFloatBuffer()[tensor.data, 0, count]
        weights[key] = tensor
        offset += count * 4
      }
      return weights
    } catch (e: Exception) {
      /* no op */
    }
    return null
  }
}
