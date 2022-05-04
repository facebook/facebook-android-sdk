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

package com.facebook

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Pair
import com.facebook.internal.NativeAppCallAttachmentStore
import com.facebook.internal.NativeAppCallAttachmentStore.openAttachment
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import kotlin.collections.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RuntimeEnvironment

@PrepareForTest(NativeAppCallAttachmentStore::class)
class FacebookContentProviderTest : FacebookPowerMockTestCase() {
  private lateinit var providerUnderTest: FacebookContentProvider

  @Before
  fun before() {
    PowerMockito.mockStatic(NativeAppCallAttachmentStore::class.java)
    providerUnderTest = FacebookContentProvider()
  }

  @Test
  fun testGetAttachmentUrl() {
    val url = FacebookContentProvider.getAttachmentUrl(APP_ID, CALL_ID, ATTACHMENT_NAME)
    Assert.assertEquals(
        "content://com.facebook.app.FacebookContentProvider" +
            APP_ID +
            "/" +
            CALL_ID +
            "/" +
            ATTACHMENT_NAME,
        url)
  }

  @Test
  fun testOnCreate() {
    assertThat(providerUnderTest.onCreate()).isTrue
  }

  @Test
  fun testQuery() {
    Assert.assertNull(providerUnderTest.query(Uri.parse("/"), null, null, null, null))
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T103601736
  @Test
  fun testGetType() {
    Assert.assertNull(providerUnderTest.getType(Uri.parse("/")))
  }

  @Test
  fun testInsert() {
    Assert.assertNull(providerUnderTest.insert(Uri.parse("/"), null))
  }

  @Test
  fun testDelete() {
    Assert.assertEquals(0, providerUnderTest.delete(Uri.parse("/"), null, null).toLong())
  }

  @Test
  fun testUpdate() {
    Assert.assertEquals(0, providerUnderTest.update(Uri.parse("/"), null, null, null).toLong())
  }

  @Test
  fun testOpenFileWithNullUri() {
    try {
      val pfd = providerUnderTest.openFile(Uri.parse("/"), "r")
      Assert.fail("expected FileNotFoundException")
    } catch (e: FileNotFoundException) {}
  }

  @Test
  fun testOpenFileWithBadPath() {
    try {
      val pfd = providerUnderTest.openFile(Uri.parse("/"), "r")
      Assert.fail("expected FileNotFoundException")
    } catch (e: FileNotFoundException) {}
  }

  @Test
  fun testOpenFileWithoutCallIdAndAttachment() {
    try {
      val pfd = providerUnderTest.openFile(Uri.parse("/foo"), "r")
      Assert.fail("expected FileNotFoundException")
    } catch (e: FileNotFoundException) {}
  }

  @Test
  fun testOpenFileWithBadCallID() {
    try {
      val pfd = providerUnderTest.openFile(Uri.parse("/foo/bar"), "r")
      Assert.fail("expected FileNotFoundException")
    } catch (e: FileNotFoundException) {}
  }

  @Test
  fun testOpenFileWithUnknownUri() {
    try {
      val pfd = getTestAttachmentParcelFileDescriptor(UUID.randomUUID())
      Assert.assertNotNull(pfd)
      pfd?.close()
      Assert.fail("expected FileNotFoundException")
    } catch (e: FileNotFoundException) {}
  }

  @Test
  fun testOpenFileWithKnownUri() {
    try {
      MockAttachmentStore.addAttachment(CALL_ID, ATTACHMENT_NAME)
      val pfd = getTestAttachmentParcelFileDescriptor(CALL_ID)
      Assert.assertNotNull(pfd)
      pfd?.close()
    } catch (e: FileNotFoundException) {
      Assert.fail("do not expected FileNotFoundException")
    }
  }

  private fun getTestAttachmentParcelFileDescriptor(callId: UUID): ParcelFileDescriptor? {
    val attachment = MockAttachmentStore.openAttachment(callId, ATTACHMENT_NAME)
    whenever(openAttachment(callId, ATTACHMENT_NAME)).thenReturn(attachment)
    val uri = Uri.parse(FacebookContentProvider.getAttachmentUrl(APP_ID, callId, ATTACHMENT_NAME))
    return providerUnderTest.openFile(uri, "r")
  }

  object MockAttachmentStore {
    private val attachments: MutableList<Pair<UUID, String>> = ArrayList()
    private const val DUMMY_FILE_NAME = "dummyfile"
    fun addAttachment(callId: UUID, attachmentName: String) {
      attachments.add(Pair(callId, attachmentName))
    }

    fun openAttachment(callId: UUID, attachmentName: String): File {
      if (attachments.contains(Pair(callId, attachmentName))) {
        val cacheDir = RuntimeEnvironment.application.cacheDir
        val dummyFile = File(cacheDir, DUMMY_FILE_NAME)
        if (!dummyFile.exists()) {
          try {
            dummyFile.createNewFile()
          } catch (e: IOException) {}
        }
        return dummyFile
      }
      throw FileNotFoundException()
    }
  }

  companion object {
    private const val APP_ID = "12345"
    private val CALL_ID = UUID.randomUUID()
    private const val ATTACHMENT_NAME = "attachMe"
  }
}
