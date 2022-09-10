package com.facebook.internal

import android.content.Context
import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    ImageDownloader::class,
    ImageDownloader.DownloaderContext::class,
    ImageDownloader.RequestKey::class,
    WorkQueue.WorkItem::class)
class ImageDownloaderTest : FacebookPowerMockTestCase() {
  private lateinit var mockRequest: ImageRequest
  private lateinit var mockWorkItem: WorkQueue.WorkItem
  private lateinit var mockDownloaderContext: ImageDownloader.DownloaderContext
  private lateinit var mockRequestKey: ImageDownloader.RequestKey
  private lateinit var mockContext: Context
  private val url = Uri.parse("https://graph.facebook.com/handle")
  private val tag = "tag"

  @Before
  fun init() {
    mockRequest = mock(ImageRequest::class.java)
    mockContext = mock(Context::class.java)

    whenever(mockRequest.imageUri).thenReturn(url)
    whenever(mockRequest.callerTag).thenReturn(tag)
    whenever(mockRequest.isCachedRedirectAllowed).thenReturn(true)
    whenever(mockRequest.context).thenReturn(mockContext)

    mockWorkItem = mock(WorkQueue.WorkItem::class.java)
    whenever(mockWorkItem.cancel()).thenReturn(false)

    mockDownloaderContext = mock(ImageDownloader.DownloaderContext::class.java)
    whenNew(ImageDownloader.DownloaderContext::class.java)
        .withArguments(eq(mockRequest))
        .thenReturn(mockDownloaderContext)
    mockDownloaderContext.workItem = mockWorkItem

    mockRequestKey = mock(ImageDownloader.RequestKey::class.java)
    whenNew(ImageDownloader.RequestKey::class.java)
        .withArguments(eq(url), eq(tag))
        .thenReturn(mockRequestKey)
    mockRequestKey.uri = url
    mockRequestKey.tag = tag
  }

  @Test
  fun `test remove before and after test adding request`() {
    // remove request without adding request
    var isCancelled = ImageDownloader.cancelRequest(mockRequest)
    assertThat(isCancelled).isFalse
    var pendingRequest = ImageDownloader.getPendingRequests()
    assertEquals(0, pendingRequest.size)

    ImageDownloader.downloadAsync(mockRequest)
    pendingRequest = ImageDownloader.getPendingRequests()
    assertEquals(1, pendingRequest.size)

    var (key, downloaderContext) = pendingRequest.entries.first()
    assertEquals(mockRequestKey, key)
    assertEquals(mockDownloaderContext, downloaderContext)

    // cancel request
    isCancelled = ImageDownloader.cancelRequest(mockRequest)
    assertTrue(isCancelled)
    pendingRequest = ImageDownloader.getPendingRequests()
    assertEquals(1, pendingRequest.size)

    val entry = pendingRequest.entries.first()
    key = entry.key
    downloaderContext = entry.value
    assertEquals(mockRequestKey, key)
    assertEquals(mockDownloaderContext, downloaderContext)
  }
}
