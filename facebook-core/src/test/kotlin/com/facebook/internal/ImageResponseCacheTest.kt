package com.facebook.internal

import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.util.common.anyObject
import com.nhaarman.mockitokotlin2.whenever
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, ImageResponseCache::class, FileLruCache::class)
class ImageResponseCacheTest : FacebookPowerMockTestCase() {
  private lateinit var mockCache: FileLruCache
  private lateinit var mockInputStream: InputStream
  private lateinit var mockHttpURLConnection: HttpURLConnection

  private val url = "https://graph.facebook.com/handle"
  private val cdnUrl = "http://fake-dfs.fbcdn.net/handle"
  private val noCachedCdnUrl = "http://fake-dfs.fbcdn.net/handle-no-cahce"
  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    mockCache = mock(FileLruCache::class.java)
    mockInputStream = mock(InputStream::class.java)
    mockHttpURLConnection = mock(HttpURLConnection::class.java)

    whenever(mockCache.get(cdnUrl)).thenReturn(mockInputStream)
    whenever(mockCache.interceptAndPut(anyString(), anyObject())).thenReturn(mockInputStream)
    whenever(mockHttpURLConnection.url).thenReturn(URL(cdnUrl))

    Whitebox.setInternalState(ImageResponseCache::class.java, "imageCache", mockCache)
  }

  @Test
  fun `test get cache`() {
    val imageCache = ImageResponseCache.getCache()
    assertEquals(mockCache, imageCache)
  }

  @Test
  fun `test get cached image stream from invalid url`() {
    val uri: Uri = Uri.parse(url)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertNull(stream)
  }

  @Test
  fun `test get cached image stream from cdn url`() {
    val uri: Uri = Uri.parse(cdnUrl)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertEquals(mockInputStream, stream)
  }

  @Test
  fun `test get non-cached image stream from cdn url`() {
    val uri: Uri = Uri.parse(noCachedCdnUrl)
    val stream = ImageResponseCache.getCachedImageStream(uri)
    assertNull(stream)
  }

  @Test
  fun `test intercept and cache image`() {
    whenever(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
    val stream = ImageResponseCache.interceptAndCacheImageStream(mockHttpURLConnection)
    assertEquals(mockInputStream, stream)
  }

  @Test
  fun `test intercept and cache image with bad connection`() {
    whenever(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST)
    val stream = ImageResponseCache.interceptAndCacheImageStream(mockHttpURLConnection)
    assertNull(stream)
  }
}
