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
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.HashSet

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object UrlRedirectCache {
  private val tag = UrlRedirectCache::class.simpleName ?: "UrlRedirectCache"
  private val redirectContentTag = tag + "_Redirect"
  private var urlRedirectFileLruCache: FileLruCache? = null

  @Synchronized
  @JvmStatic
  @Throws(IOException::class)
  fun getCache(): FileLruCache {
    val nonNullCache = urlRedirectFileLruCache ?: FileLruCache(tag, FileLruCache.Limits())
    urlRedirectFileLruCache = nonNullCache
    return nonNullCache
  }

  @JvmStatic
  fun getRedirectedUri(uri: Uri?): Uri? {
    if (uri == null) {
      return null
    }

    var uriString = uri.toString()
    var reader: InputStreamReader? = null
    val redirectChain = HashSet<String>()
    redirectChain.add(uriString)
    try {
      val cache = getCache()
      var redirectExists = false
      var stream = cache.get(uriString, this.redirectContentTag)

      while (stream != null) {
        redirectExists = true
        // Get the redirected url
        reader = InputStreamReader(stream)
        val buffer = CharArray(128)
        val urlBuilder = StringBuilder()
        var bufferLength = reader.read(buffer, 0, buffer.size)
        while (bufferLength > 0) {
          urlBuilder.append(buffer, 0, bufferLength)
          bufferLength = reader.read(buffer, 0, buffer.size)
        }
        Utility.closeQuietly(reader)

        // Iterate to the next url in the redirection
        val redirectToUriString = urlBuilder.toString()
        if (redirectChain.contains(redirectToUriString)) {
          if (redirectToUriString == uriString) {
            // uriString redirect to itself. Stop the loop
            break
          } else {
            // A loop with more than 1 address is detected. It's unexpected.
            // In this case, return null so that the caller can directly use the original address.
            Logger.log(
                LoggingBehavior.CACHE, Log.ERROR, this.tag, "A loop detected in UrlRedirectCache")
            return null
          }
        }
        uriString = redirectToUriString
        redirectChain.add(uriString)
        stream = cache.get(uriString, this.redirectContentTag)
      }

      if (redirectExists) {
        return Uri.parse(uriString)
      }
    } catch (e: IOException) {
      Logger.log(
          LoggingBehavior.CACHE,
          Log.INFO,
          this.tag,
          "IOException when accessing cache: " + e.message)
    } finally {
      Utility.closeQuietly(reader)
    }

    return null
  }

  @JvmStatic
  fun cacheUriRedirect(fromUri: Uri?, toUri: Uri?) {
    if (fromUri == null || toUri == null) {
      return
    }

    var redirectStream: OutputStream? = null
    try {
      val cache = getCache()
      redirectStream = cache.openPutStream(fromUri.toString(), this.redirectContentTag)
      redirectStream.write(toUri.toString().toByteArray())
    } catch (e: IOException) {
      // Caching is best effort
      Logger.log(
          LoggingBehavior.CACHE,
          Log.INFO,
          this.tag,
          "IOException when accessing cache: " + e.message)
    } finally {
      Utility.closeQuietly(redirectStream)
    }
  }

  @JvmStatic
  fun clearCache() {
    try {
      getCache().clearCache()
    } catch (e: IOException) {
      Logger.log(LoggingBehavior.CACHE, Log.WARN, tag, "clearCache failed " + e.message)
    }
  }
}
