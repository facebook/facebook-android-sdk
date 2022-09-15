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

package com.facebook.internal

import android.net.Uri
import android.util.Log
import com.facebook.LoggingBehavior
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ImageResponseCache {
  val TAG = ImageResponseCache::class.java.simpleName
  private lateinit var imageCache: FileLruCache

  @Synchronized
  @JvmStatic
  @Throws(IOException::class)
  fun getCache(): FileLruCache {
    if (!this::imageCache.isInitialized) {
      imageCache = FileLruCache(TAG, FileLruCache.Limits())
    }
    return imageCache
  }

  // Get stream from cache, or return null if the image is not cached.
  // Does not throw if there was an error.
  @JvmStatic
  fun getCachedImageStream(uri: Uri?): InputStream? {
    var imageStream: InputStream? = null
    if (uri != null) {
      if (isCDNURL(uri)) {
        try {
          val cache = getCache()
          imageStream = cache[uri.toString()]
        } catch (e: IOException) {
          Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, e.toString())
        }
      }
    }
    return imageStream
  }

  @JvmStatic
  @Throws(IOException::class)
  fun interceptAndCacheImageStream(connection: HttpURLConnection): InputStream? {
    var stream: InputStream? = null
    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
      val uri = Uri.parse(connection.url.toString())
      stream = connection.inputStream // Default stream in case caching fails
      try {
        if (isCDNURL(uri)) {
          val cache = getCache()

          // Wrap stream with a caching stream
          stream =
              cache.interceptAndPut(uri.toString(), BufferedHttpInputStream(stream, connection))
        }
      } catch (e: IOException) {
        // Caching is best effort
      }
    }
    return stream
  }

  private fun isCDNURL(uri: Uri?): Boolean {
    if (uri != null) {
      val uriHost = uri.host
      if (uriHost != null) {
        if (uriHost == "fbcdn.net" || uriHost.endsWith(".fbcdn.net")) {
          return true
        }
        if (uriHost.startsWith("fbcdn") && uriHost.endsWith(".akamaihd.net")) {
          return true
        }
      }
    }
    return false
  }
  @JvmStatic
  fun clearCache() {
    try {
      getCache().clearCache()
    } catch (e: IOException) {
      Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "clearCache failed " + e.message)
    }
  }

  private class BufferedHttpInputStream
  internal constructor(stream: InputStream?, var connection: HttpURLConnection) :
      BufferedInputStream(stream, Utility.DEFAULT_STREAM_BUFFER_SIZE) {
    @Throws(IOException::class)
    override fun close() {
      super.close()
      Utility.disconnectQuietly(connection)
    }
  }
}
