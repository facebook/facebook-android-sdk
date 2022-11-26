/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class NativeAppCallAttachmentStoreTest : FacebookPowerMockTestCase() {
  private lateinit var callId: UUID
  private lateinit var testDirectory: File

  companion object {
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

    callId = UUID.randomUUID()
    testDirectory = File(UUID.randomUUID().toString())
    testDirectory.mkdir()
  }

  @After
  fun clean() {
    NativeAppCallAttachmentStore.cleanupAllAttachments()
    testDirectory.deleteRecursively()
  }

  @Test
  fun `test add attachments of bitmap`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, createBitmap())
    val attachmentUrl = attachment.attachmentUrl
    assertThat(attachmentUrl).isNotNull
    NativeAppCallAttachmentStore.addAttachments(listOf(attachment))

    // Url format: base/call_id/filename
    val attachmentFilename = attachmentUrl.split("/").last()
    val retrievedAttachment =
        NativeAppCallAttachmentStore.getAttachmentFile(callId, attachmentFilename, false)
    checkNotNull(retrievedAttachment)
    assertThat(retrievedAttachment.exists()).isTrue
  }

  @Test(expected = FacebookException::class)
  fun `test create attachments with unexpected uri`() {
    NativeAppCallAttachmentStore.createAttachment(callId, Uri.parse(UNEXPECTED_ATTACHMENT_URI))
  }

  @Test
  fun `test create attachments with content uri`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, Uri.parse(CONTENT_URI))
    assertThat(attachment.originalUri.toString()).isEqualTo(CONTENT_URI)
  }

  @Test
  fun `test create attachments with media uri`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, Uri.parse(MEDIA_URI))
    assertThat(attachment.originalUri.toString()).isEqualTo(MEDIA_URI)
    assertThat(attachment.attachmentUrl).isEqualTo(MEDIA_URI)
  }

  @Test
  fun `test get attachments directory`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentsDirectory()
    checkNotNull(dir)
    assertThat(dir.canonicalPath).contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME)
  }

  @Test
  fun `test get attachments directory for call`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentsDirectoryForCall(callId, false)
    checkNotNull(dir)
    assertThat(dir.canonicalPath).contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME)
    assertThat(dir.canonicalPath).contains(callId.toString())
  }

  @Test
  fun `test get attachment file`() {
    val dir = NativeAppCallAttachmentStore.getAttachmentFile(callId, ATTACHMENT_NAME, false)
    checkNotNull(dir)
    assertThat(dir.canonicalPath).contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME)
    assertThat(dir.canonicalPath).contains(callId.toString())
    assertThat(dir.canonicalPath).contains(ATTACHMENT_NAME)
  }

  @Test
  fun `test adding empty and null attachment list won't crash`() {
    NativeAppCallAttachmentStore.addAttachments(null)
    NativeAppCallAttachmentStore.addAttachments(listOf())
  }

  @Test
  fun `test adding an attachment from url file`() {
    // create attachment input file
    val inputFile = File(testDirectory.path + "/input")
    with(inputFile.outputStream()) {
      this.write(7)
      this.close()
    }
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, Uri.fromFile(inputFile))

    // add attachment to the store
    NativeAppCallAttachmentStore.addAttachments(listOf(attachment))
    val retrievedAttachmentFile =
        NativeAppCallAttachmentStore.openAttachment(callId, attachment.attachmentName)
    checkNotNull(retrievedAttachmentFile)
    with(retrievedAttachmentFile.inputStream()) {
      val data = this.read()
      assertThat(data).isEqualTo(7)
    }
  }

  @Test(expected = FacebookException::class)
  fun `test adding an attachment from a non-exist file will throw an exception`() {
    val inputFile = File(testDirectory.path, "input")
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, Uri.fromFile(inputFile))

    NativeAppCallAttachmentStore.addAttachments(listOf(attachment))
  }

  @Test(expected = FileNotFoundException::class)
  fun `test open an attachment of the empty name will throw an exception`() {
    NativeAppCallAttachmentStore.openAttachment(callId, "")
  }

  @Test
  fun `test cleaning up attachment will delete added attachments`() {
    val attachment = NativeAppCallAttachmentStore.createAttachment(callId, createBitmap())
    NativeAppCallAttachmentStore.addAttachments(listOf(attachment))
    checkNotNull(
        NativeAppCallAttachmentStore.getAttachmentFile(callId, attachment.attachmentName, false))

    NativeAppCallAttachmentStore.cleanupAttachmentsForCall(callId)

    val retrievedAttachmentFile =
        NativeAppCallAttachmentStore.getAttachmentFile(callId, attachment.attachmentName, false)
    assertThat(retrievedAttachmentFile?.exists()).isFalse
  }

  private fun createBitmap(): Bitmap {
    return Bitmap.createBitmap(20, 20, Bitmap.Config.ALPHA_8)
  }
}
