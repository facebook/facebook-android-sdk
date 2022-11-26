/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Pair
import com.facebook.internal.NativeAppCallAttachmentStore.openAttachment
import java.io.FileNotFoundException
import java.util.UUID

/**
 * Implements a
 * [
 * ContentProvider](http://developer.android.com/reference/android/content/ContentProvider.html)
 * that can be used to provide binary attachments (e.g., images) to calls made via
 * [com.facebook.FacebookDialog].
 *
 * Note that this ContentProvider is only necessary if an application wishes to attach images, etc.,
 * that are stored in memory and do not have another way to be referenced by a content URI. For
 * images obtained from, e.g., the Camera or Gallery, that already have a content URI associated
 * with them, use of this class is not necessary.
 *
 * If an application wishes to attach images that are stored in-memory within the application, this
 * content provider must be listed in the application's AndroidManifest.xml, and it should be named
 * according to the pattern `"com.facebook.app.FacebookContentProvider{FACEBOOK_APP_ID}" ` * . See
 * the [getContentProviderName][FacebookContentProvider.getAttachmentUrl] method.
 */
class FacebookContentProvider : ContentProvider() {
  override fun onCreate(): Boolean = true

  override fun query(
      uri: Uri,
      strings: Array<String>?,
      s: String?,
      strings2: Array<String>?,
      s2: String?
  ): Cursor? {
    return null
  }

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, contentValues: ContentValues?): Uri? = null

  override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int = 0

  override fun update(
      uri: Uri,
      contentValues: ContentValues?,
      s: String?,
      strings: Array<String>?
  ): Int {
    return 0
  }

  @Throws(FileNotFoundException::class)
  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    val callIdAndAttachmentName = parseCallIdAndAttachmentName(uri) ?: throw FileNotFoundException()
    return try {
      val file =
          openAttachment(callIdAndAttachmentName.first, callIdAndAttachmentName.second)
              ?: throw FileNotFoundException()
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    } catch (exception: FileNotFoundException) {
      Log.e(TAG, "Got unexpected exception:$exception")
      throw exception
    }
  }

  private fun parseCallIdAndAttachmentName(uri: Uri): Pair<UUID, String>? {
    return try {
      // We don't do explicit format checking here. Malformed URIs may generate
      // NullPointerExceptions or array bounds exceptions, which we'll catch and return null.
      // All of these will result in a FileNotFoundException being thrown in openFile.
      val callIdAndAttachmentName = checkNotNull(uri.path).substring(1)
      val parts = callIdAndAttachmentName.split("/").toTypedArray()
      val callIdString = parts[0]
      val attachmentName = parts[1]

      // Protects against malicious actors (https://support.google.com/faqs/answer/7496913)"
      if (INVALID_FILE_NAME.contentEquals(callIdString) ||
          INVALID_FILE_NAME.contentEquals(attachmentName)) {
        throw Exception()
      }
      val callId = UUID.fromString(callIdString)
      Pair(callId, attachmentName)
    } catch (exception: Exception) {
      null
    }
  }

  companion object {
    private val TAG = FacebookContentProvider::class.java.name
    private const val ATTACHMENT_URL_BASE = "content://com.facebook.app.FacebookContentProvider"
    private const val INVALID_FILE_NAME = ".."

    /**
     * Returns the name of the content provider formatted correctly for constructing URLs.
     *
     * @param applicationId the Facebook application ID of the application
     * @return the String to use as the authority portion of a content URI.
     */
    @JvmStatic
    fun getAttachmentUrl(applicationId: String?, callId: UUID, attachmentName: String?): String {
      return String.format(
          "%s%s/%s/%s", ATTACHMENT_URL_BASE, applicationId, callId.toString(), attachmentName)
    }
  }
}
