package com.facebook.appevents.internal

import android.annotation.TargetApi
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FileDownloadTask::class)
class FileDownloadTaskTest : FacebookPowerMockTestCase() {
  private val uriStr = "www.facebook.com"
  private val content = "this is a test"
  private val fileName = "tempFile.txt"

  private lateinit var tempFile: File
  private lateinit var mockUrl: URL
  private lateinit var mockConnection: URLConnection
  private lateinit var mockDataInputStream: DataInputStream

  @Before
  fun `set up`() {
    tempFile = File.createTempFile(fileName, null)
    mockUrl = mock()
    mockConnection = mock()
    mockDataInputStream = DataInputStream(ByteArrayInputStream(content.toByteArray()))

    whenever(mockConnection.contentLength).thenReturn(content.length)
    whenever(mockUrl.openConnection()).thenReturn(mockConnection)
    PowerMockito.whenNew(URL::class.java).withArguments(uriStr).thenReturn(mockUrl)
    PowerMockito.whenNew(DataInputStream::class.java)
        .withAnyArguments()
        .thenReturn(mockDataInputStream)
  }

  @After
  fun `tear down`() {
    tempFile.deleteOnExit()
  }

  @Test
  @TargetApi(26)
  fun `test file download`() {
    val task = mock<FileDownloadTask>()
    whenever(task.doInBackground()).thenCallRealMethod()
    WhiteboxImpl.setInternalState(task, "uriStr", uriStr)
    WhiteboxImpl.setInternalState(task, "destFile", tempFile)
    assertTrue(task.doInBackground())

    val lines = Files.readAllLines(tempFile.toPath())
    assertEquals(1, lines.size)
    assertEquals(content, lines[0])
  }
}
