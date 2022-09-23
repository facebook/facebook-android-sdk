/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
