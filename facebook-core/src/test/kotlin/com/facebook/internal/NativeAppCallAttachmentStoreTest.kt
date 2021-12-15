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
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class NativeAppCallAttachmentStoreTest : FacebookPowerMockTestCase() {
  companion object {
    private val CALL_ID = UUID.randomUUID()
    private const val ATTACHMENT_NAME = "hello"
    private const val UNEXPECTED_ATTACHMENT_URI = "test://nothissiteintheworld.test/abcd"
    private const val CONTENT_URI = "content://testcontent.test/1234"
    private const val MEDIA_URI = "content://media/testcontent.test/1234"
  }

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    NativeAppCallAttachmentStore.ensureAttachmentsDirectoryExists()
  }

  @After
  fun clean() {
    NativeAppCallAttachmentStore.cleanupAllAttachments()
  }

  @Test
  fun `test add attachments of bitmap`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(CALL_ID, createBitmap())
    val attachmentUrl = attachment.attachmentUrl
    Assert.assertNotNull(attachmentUrl)
    NativeAppCallAttachmentStore.addAttachments(listOf(attachment))

    // Url format: base/call_id/filename
    val attachmentFilename = attachmentUrl.split("/").last()
    val retrievedAttachment =
        NativeAppCallAttachmentStore.getAttachmentFile(CALL_ID, attachmentFilename, false)
    Assert.assertNotNull(retrievedAttachment)
    checkNotNull(retrievedAttachment)
    Assert.assertTrue(retrievedAttachment.exists())
  }

  @Test
  fun `test create attachments with unexpected uri`() {
    try {
      NativeAppCallAttachmentStore.createAttachment(CALL_ID, Uri.parse(UNEXPECTED_ATTACHMENT_URI))
      Assert.fail("expected exception")
    } catch (e: FacebookException) {
      Assert.assertTrue(e.message?.contains("Unsupported scheme") ?: false)
    }
  }

  @Test
  fun `test create attachments with content uri`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(CALL_ID, Uri.parse(CONTENT_URI))
    Assert.assertNotNull(attachment)
    checkNotNull(attachment)
    Assert.assertEquals(attachment.originalUri.toString(), CONTENT_URI)
  }

  @Test
  fun `test create attachments with media uri`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(CALL_ID, Uri.parse(MEDIA_URI))
    Assert.assertNotNull(attachment)
    checkNotNull(attachment)
    Assert.assertEquals(attachment.originalUri.toString(), MEDIA_URI)
    Assert.assertEquals(attachment.attachmentUrl, MEDIA_URI)
  }

  @Test
  fun `test get attachments directory`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentsDirectory()
    Assert.assertNotNull(dir)
    checkNotNull(dir)
    Assert.assertTrue(dir.absolutePath.contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME))
  }

  @Test
  fun `test get attachments directory for call`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentsDirectoryForCall(CALL_ID, false)
    Assert.assertNotNull(dir)
    checkNotNull(dir)
    Assert.assertTrue(dir.absolutePath.contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME))
    Assert.assertTrue(dir.absolutePath.contains(CALL_ID.toString()))
  }

  @Test
  fun `test get attachment file`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentFile(CALL_ID, ATTACHMENT_NAME, false)
    Assert.assertNotNull(dir)
    checkNotNull(dir)
    Assert.assertTrue(dir.absolutePath.contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME))
    Assert.assertTrue(dir.absolutePath.contains(CALL_ID.toString()))
    Assert.assertTrue(dir.absolutePath.contains(ATTACHMENT_NAME))
  }

  private fun createBitmap(): Bitmap {
    return Bitmap.createBitmap(20, 20, Bitmap.Config.ALPHA_8)
  }
}
