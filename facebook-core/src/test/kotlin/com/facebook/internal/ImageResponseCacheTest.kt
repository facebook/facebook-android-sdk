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
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class ImageResponseCacheTest : FacebookPowerMockTestCase() {
  private lateinit var mockCache: FileLruCache
  private lateinit var mockInputStream: InputStream
  private lateinit var mockHttpURLConnection: HttpURLConnection
  private lateinit var mockCacheDir: File

  private val url = "https://graph.facebook.com/handle"
  private val cdnUrl = "http://fake-dfs.fbcdn.net/handle"
  private val noCachedCdnUrl = "http://fake-dfs.fbcdn.net/handle-no-cahce"

  @Before
  fun init() {
    mockCacheDir = File(UUID.randomUUID().toString())
    mockCacheDir.deleteOnExit()
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getCacheDir()).thenReturn(mockCacheDir)

    mockCache = mock()
    mockInputStream = mock()
    mockHttpURLConnection = mock()

    whenever(mockCache.get(cdnUrl)).thenReturn(mockInputStream)
    whenever(mockCache.interceptAndPut(any(), any())).thenReturn(mockInputStream)
    whenever(mockHttpURLConnection.url).thenReturn(URL(cdnUrl))

    Whitebox.setInternalState(ImageResponseCache::class.java, "imageCache", mockCache)
  }

  @Test
  fun `test get cache`() {
    val imageCache = ImageResponseCache.getCache()
    assertThat(imageCache).isEqualTo(mockCache)
  }

  @Test
  fun `test get cached image stream from invalid url`() {
    val uri: Uri = Uri.parse(url)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertThat(stream).isNull()
  }

  @Test
  fun `test get cached image stream from cdn url`() {
    val uri: Uri = Uri.parse(cdnUrl)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertThat(stream).isEqualTo(mockInputStream)
  }

  @Test
  fun `test get non-cached image stream from cdn url`() {
    val uri: Uri = Uri.parse(noCachedCdnUrl)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertThat(stream).isNull()
  }

  @Test
  fun `test intercept and cache image`() {
    whenever(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
    val stream = ImageResponseCache.interceptAndCacheImageStream(mockHttpURLConnection)
    assertThat(stream).isEqualTo(mockInputStream)
  }

  @Test
  fun `test intercept and cache image with bad connection`() {
    whenever(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST)
    val stream = ImageResponseCache.interceptAndCacheImageStream(mockHttpURLConnection)
    assertThat(stream).isNull()
  }

  @Test
  fun `test clearing cache will clear the base file cache`() {
    ImageResponseCache.clearCache()
    verify(mockCache).clearCache()
  }

  @Test
  fun `test get cache will create a FileLruCache`() {
    Whitebox.setInternalState(ImageResponseCache::class.java, "imageCache", null as FileLruCache?)
    val cache = ImageResponseCache.getCache()
    assertThat(cache).isNotNull
    assertThat(mockCacheDir.isDirectory).isTrue
    mockCacheDir.deleteRecursively()
  }
}
