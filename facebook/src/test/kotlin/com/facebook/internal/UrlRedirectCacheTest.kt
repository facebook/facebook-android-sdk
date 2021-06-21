package com.facebook.internal

import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.File
import java.io.IOException
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class UrlRedirectCacheTest : FacebookPowerMockTestCase() {

  companion object {
    private const val TO_URI_STRING = "http://facebook.com"
    private const val TO_URI_STRING_LONG =
        "http://facebook.com/this_is_the_very_very_very_loooooooooooooooooooooooooooooooooooooooooooooo000000000000000000000000000000000000000oooog_address_to_test_the_cache"
    private const val FROM_URI_STRING = "http://fbtest.com"
    private const val FROM_URI_STRING2 = "http://fb.com"
  }

  @Mock private lateinit var fromUri: Uri
  @Mock private lateinit var fromUri2: Uri
  @Mock private lateinit var toUri: Uri
  @Mock private lateinit var toUriLong: Uri
  private lateinit var testCacheFilePath: File

  @Before
  fun init() {
    testCacheFilePath = File(java.util.UUID.randomUUID().toString())
    testCacheFilePath.mkdir()

    mockStatic(FacebookSdk::class.java)
    `when`(FacebookSdk.isInitialized()).thenReturn(true)
    `when`(FacebookSdk.getExecutor()).thenReturn(FacebookSerialExecutor())
    `when`(FacebookSdk.getCacheDir()).thenReturn(testCacheFilePath)

    `when`(fromUri.toString()).thenReturn(FROM_URI_STRING)
    `when`(fromUri2.toString()).thenReturn(FROM_URI_STRING2)
    `when`(toUri.toString()).thenReturn(TO_URI_STRING)
    `when`(toUriLong.toString()).thenReturn(TO_URI_STRING_LONG)

    val nullCache: FileLruCache? = null
    Whitebox.setInternalState(UrlRedirectCache::class.java, "urlRedirectFileLruCache", nullCache)
  }

  @After
  fun clean() {
    UrlRedirectCache.clearCache()
    testCacheFilePath.deleteRecursively()
  }

  @Test
  fun `test read and write cache with normal input`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    UrlRedirectCache.cacheUriRedirect(fromUri2, toUriLong)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertEquals(fetchedResultForURL.toString(), toUri.toString())

    val fetchedResultForURL2 = UrlRedirectCache.getRedirectedUri(fromUri2)
    assertEquals(fetchedResultForURL2.toString(), toUriLong.toString())
  }

  @Test
  fun `test read after clearing the cache`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    UrlRedirectCache.clearCache()
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertNull(fetchedResultForURL)
  }

  @Test
  fun `test read and write cache with null`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, null)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertNull(fetchedResultForURL)

    val fetchedResultForNull = UrlRedirectCache.getRedirectedUri(null)
    assertNull(fetchedResultForNull)
  }

  @Test
  fun `test write the cache twice`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, toUriLong)
    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertEquals(fetchedResultForURL.toString(), toUri.toString())
  }

  @Test
  fun `test write the cache twice with same address`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertEquals(fetchedResultForURL.toString(), toUri.toString())
  }

  @Test
  fun `test redirect to itself`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, fromUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertEquals(fetchedResultForURL.toString(), fetchedResultForURL.toString())
  }

  @Test
  fun `test loop in redirect`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, fromUri2)
    UrlRedirectCache.cacheUriRedirect(fromUri2, toUri)
    UrlRedirectCache.cacheUriRedirect(toUri, fromUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertNull(fetchedResultForURL)
  }

  @Test
  fun `test multiple redirects`() {
    UrlRedirectCache.cacheUriRedirect(fromUri, fromUri2)
    UrlRedirectCache.cacheUriRedirect(fromUri2, toUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertEquals(fetchedResultForURL.toString(), toUri.toString())
  }

  @Test
  fun `test FileLRUCache is broken`() {
    val mockFileLruCache = mock(FileLruCache::class.java)
    `when`(mockFileLruCache.get(anyString(), anyString())).thenThrow(IOException("mock exception"))
    `when`(mockFileLruCache.openPutStream(anyString(), anyString()))
        .thenThrow(IOException("mock exception"))

    Whitebox.setInternalState(
        UrlRedirectCache::class.java, "urlRedirectFileLruCache", mockFileLruCache)

    UrlRedirectCache.cacheUriRedirect(fromUri, toUri)
    val fetchedResultForURL = UrlRedirectCache.getRedirectedUri(fromUri)
    assertNull(fetchedResultForURL)
  }
}
