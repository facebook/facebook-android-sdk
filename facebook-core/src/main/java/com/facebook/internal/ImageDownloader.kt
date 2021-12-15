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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookException
import com.facebook.internal.ImageResponseCache.getCachedImageStream
import com.facebook.internal.ImageResponseCache.interceptAndCacheImageStream
import com.facebook.internal.UrlRedirectCache.cacheUriRedirect
import com.facebook.internal.UrlRedirectCache.getRedirectedUri
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ImageDownloader {
  private const val DOWNLOAD_QUEUE_MAX_CONCURRENT = WorkQueue.DEFAULT_MAX_CONCURRENT
  private const val CACHE_READ_QUEUE_MAX_CONCURRENT = 2

  @get:Synchronized
  private var handler: Handler? = null
    private get() {
      if (field == null) {
        field = Handler(Looper.getMainLooper())
      }
      return field
    }
  private val downloadQueue = WorkQueue(DOWNLOAD_QUEUE_MAX_CONCURRENT)
  private val cacheReadQueue = WorkQueue(CACHE_READ_QUEUE_MAX_CONCURRENT)
  private val pendingRequests: MutableMap<RequestKey, DownloaderContext> = HashMap()

  /**
   * Downloads the image specified in the passed in request. If a callback is specified, it is
   * guaranteed to be invoked on the calling thread.
   *
   * @param request Request to process
   */
  @JvmStatic
  fun downloadAsync(request: ImageRequest?) {
    if (request == null) {
      return
    }

    // NOTE: This is the ONLY place where the original request's Url is read. From here on,
    // we will keep track of the Url separately. This is because we might be dealing with a
    // redirect response and the Url might change. We can't create our own new ImageRequests
    // for these changed Urls since the caller might be doing some book-keeping with the
    // requests object reference. So we keep the old references and just map them to new urls in
    // the downloader.
    val key = RequestKey(request.imageUri, request.callerTag)
    synchronized(pendingRequests) {
      val downloaderContext = pendingRequests[key]
      if (downloaderContext != null) {
        downloaderContext.request = request
        downloaderContext.isCancelled = false
        downloaderContext.workItem?.moveToFront()
      } else {
        enqueueCacheRead(request, key, request.isCachedRedirectAllowed)
      }
    }
  }

  @JvmStatic
  fun cancelRequest(request: ImageRequest): Boolean {
    var cancelled = false
    val key = RequestKey(request.imageUri, request.callerTag)
    synchronized(pendingRequests) {
      val downloaderContext = pendingRequests[key]
      if (downloaderContext != null) {
        // If we were able to find the request in our list of pending requests, then we will
        // definitely be able to prevent an ImageResponse from being issued. This is
        // regardless of whether a cache-read or network-download is underway for this
        // request.
        cancelled = true
        val workItem = downloaderContext.workItem
        if (workItem !== null && workItem.cancel()) {
          pendingRequests.remove(key)
        } else {
          // May be attempting a cache-read right now. So keep track of the cancellation
          // to prevent network calls etc
          downloaderContext.isCancelled = true
        }
      }
    }
    return cancelled
  }

  @JvmStatic
  fun prioritizeRequest(request: ImageRequest) {
    val key = RequestKey(request.imageUri, request.callerTag)
    synchronized(pendingRequests) {
      val downloaderContext = pendingRequests[key]
      if (downloaderContext != null) {
        downloaderContext.workItem?.moveToFront()
      }
    }
  }

  @JvmStatic
  fun clearCache() {
    ImageResponseCache.clearCache()
    UrlRedirectCache.clearCache()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun getPendingRequests(): Map<RequestKey, DownloaderContext> = pendingRequests

  private fun enqueueCacheRead(
      request: ImageRequest,
      key: RequestKey,
      allowCachedRedirects: Boolean
  ) {
    enqueueRequest(request, key, cacheReadQueue, CacheReadWorkItem(key, allowCachedRedirects))
  }

  private fun enqueueDownload(request: ImageRequest, key: RequestKey) {
    enqueueRequest(request, key, downloadQueue, DownloadImageWorkItem(key))
  }

  private fun enqueueRequest(
      request: ImageRequest,
      key: RequestKey,
      workQueue: WorkQueue,
      workItem: Runnable
  ) {
    synchronized(pendingRequests) {
      val downloaderContext = DownloaderContext(request)
      pendingRequests[key] = downloaderContext

      // The creation of the WorkItem should be done after the pending request has been
      // registered. This is necessary since the WorkItem might kick off right away and
      // attempt to retrieve the request's DownloaderContext prior to it being ready for
      // access.
      //
      // It is also necessary to hold on to the lock until after the workItem is created,
      // since calls to cancelRequest or prioritizeRequest might come in and expect a
      // registered request to have a workItem available as well.
      downloaderContext.workItem = workQueue.addActiveWorkItem(workItem)
    }
  }

  private fun issueResponse(
      key: RequestKey,
      error: Exception?,
      bitmap: Bitmap?,
      isCachedRedirect: Boolean
  ) {
    // Once the old downloader context is removed, we are thread-safe since this is the
    // only reference to it
    val completedRequestContext = removePendingRequest(key)
    if (completedRequestContext != null && !completedRequestContext.isCancelled) {
      val request = completedRequestContext.request
      val callback = request?.callback
      if (callback != null) {
        handler?.post(
            Runnable {
              val response = ImageResponse(request, error, isCachedRedirect, bitmap)
              callback.onCompleted(response)
            })
      }
    }
  }

  private fun readFromCache(key: RequestKey, allowCachedRedirects: Boolean) {
    var cachedStream: InputStream? = null
    var isCachedRedirect = false
    if (allowCachedRedirects) {
      val redirectUri = getRedirectedUri(key.uri)
      if (redirectUri != null) {
        cachedStream = getCachedImageStream(redirectUri)
        isCachedRedirect = cachedStream != null
      }
    }
    if (!isCachedRedirect) {
      cachedStream = getCachedImageStream(key.uri)
    }
    if (cachedStream != null) {
      // We were able to find a cached image.
      val bitmap = BitmapFactory.decodeStream(cachedStream)
      Utility.closeQuietly(cachedStream)
      issueResponse(key, null, bitmap, isCachedRedirect)
    } else {
      // Once the old downloader context is removed, we are thread-safe since this is the
      // only reference to it
      val downloaderContext = removePendingRequest(key)
      val request = downloaderContext?.request
      if (downloaderContext != null && !downloaderContext.isCancelled && request !== null) {
        enqueueDownload(request, key)
      }
    }
  }

  private fun download(key: RequestKey) {
    var connection: HttpURLConnection? = null
    var stream: InputStream? = null
    var error: Exception? = null
    var bitmap: Bitmap? = null
    var issueResponse = true
    try {
      val url = URL(key.uri.toString())
      connection = url.openConnection() as HttpURLConnection
      connection.instanceFollowRedirects = false
      when (connection.responseCode) {
        HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
          // redirect. So we need to perform further requests
          issueResponse = false
          val redirectLocation = connection.getHeaderField("location")
          if (!Utility.isNullOrEmpty(redirectLocation)) {
            val redirectUri = Uri.parse(redirectLocation)
            cacheUriRedirect(key.uri, redirectUri)

            // Once the old downloader context is removed, we are thread-safe since this
            // is the only reference to it
            val downloaderContext = removePendingRequest(key)
            if (downloaderContext != null && !downloaderContext.isCancelled) {
              enqueueCacheRead(downloaderContext.request, RequestKey(redirectUri, key.tag), false)
            }
          }
        }
        HttpURLConnection.HTTP_OK -> {
          // image should be available
          stream = interceptAndCacheImageStream(connection)
          bitmap = BitmapFactory.decodeStream(stream)
        }
        else -> {
          stream = connection.errorStream
          val errorMessageBuilder = StringBuilder()
          if (stream != null) {
            val reader = InputStreamReader(stream)
            val buffer = CharArray(128)
            var bufferLength: Int
            while (reader.read(buffer, 0, buffer.size).also { bufferLength = it } > 0) {
              errorMessageBuilder.append(buffer, 0, bufferLength)
            }
            Utility.closeQuietly(reader)
          } else {
            errorMessageBuilder.append("Unexpected error while downloading an image.")
          }
          error = FacebookException(errorMessageBuilder.toString())
        }
      }
    } catch (e: IOException) {
      error = e
    } finally {
      Utility.closeQuietly(stream)
      Utility.disconnectQuietly(connection)
    }
    if (issueResponse) {
      issueResponse(key, error, bitmap, false)
    }
  }

  private fun removePendingRequest(key: RequestKey): DownloaderContext? {
    synchronized(pendingRequests) {
      return pendingRequests.remove(key)
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  class RequestKey(var uri: Uri, var tag: Any) {
    override fun hashCode(): Int {
      var result = HASH_SEED
      result = result * HASH_MULTIPLIER + uri.hashCode()
      result = result * HASH_MULTIPLIER + tag.hashCode()
      return result
    }

    override fun equals(o: Any?): Boolean {
      var isEqual = false
      if (o != null && o is RequestKey) {
        val compareTo = o
        isEqual = compareTo.uri === uri && compareTo.tag === tag
      }
      return isEqual
    }

    companion object {
      private const val HASH_SEED = 29 // Some random prime number
      private const val HASH_MULTIPLIER = 37 // Some random prime number
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  class DownloaderContext internal constructor(var request: ImageRequest) {
    var workItem: WorkQueue.WorkItem? = null
    var isCancelled = false
  }

  private class CacheReadWorkItem
  internal constructor(private val key: RequestKey, private val allowCachedRedirects: Boolean) :
      Runnable {
    override fun run() {
      readFromCache(key, allowCachedRedirects)
    }
  }

  private class DownloadImageWorkItem internal constructor(private val key: RequestKey) : Runnable {
    override fun run() {
      download(key)
    }
  }
}
