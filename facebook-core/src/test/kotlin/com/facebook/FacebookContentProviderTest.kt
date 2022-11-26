/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Pair
import com.facebook.internal.NativeAppCallAttachmentStore
import com.facebook.internal.NativeAppCallAttachmentStore.openAttachment
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import kotlin.collections.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
