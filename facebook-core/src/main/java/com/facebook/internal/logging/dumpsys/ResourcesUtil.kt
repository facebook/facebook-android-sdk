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

import android.content.res.Resources

object ResourcesUtil {

  @JvmStatic
  fun getIdStringQuietly(r: Resources?, resourceId: Int): String {
    return try {
      getIdString(r, resourceId)
    } catch (e: Resources.NotFoundException) {
      getFallbackIdString(resourceId)
    }
  }

  @Throws(Resources.NotFoundException::class)
  @JvmStatic
  fun getIdString(r: Resources?, resourceId: Int): String {
    if (r == null) {
      return getFallbackIdString(resourceId)
    }
    val prefix: String
    val prefixSeparator: String
    when (getResourcePackageId(resourceId)) {
      0x7f -> {
        prefix = ""
        prefixSeparator = ""
      }
      else -> {
        prefix = r.getResourcePackageName(resourceId)
        prefixSeparator = ":"
      }
    }
    val typeName = r.getResourceTypeName(resourceId)
    val entryName = r.getResourceEntryName(resourceId)
    val sb =
        StringBuilder(
            1 + prefix.length + prefixSeparator.length + typeName.length + 1 + entryName.length)
    sb.append("@")
    sb.append(prefix)
    sb.append(prefixSeparator)
    sb.append(typeName)
    sb.append("/")
    sb.append(entryName)
    return sb.toString()
  }

  private fun getFallbackIdString(resourceId: Int): String {
    return "#" + Integer.toHexString(resourceId)
  }

  private fun getResourcePackageId(id: Int): Int {
    return id ushr 24 and 0xff
  }
}
