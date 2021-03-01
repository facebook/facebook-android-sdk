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
import android.util.Log
import com.facebook.FacebookContentProvider
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.UUID

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * This class works in conjunction with [com.facebook.FacebookContentProvider] to allow apps to
 * attach binary attachments (e.g., images) to native dialogs launched via the sdk.It stores
 * attachments in temporary files and allows the Facebook application to retrieve them via the
 * content provider.
 */
object NativeAppCallAttachmentStore {
  private val TAG = NativeAppCallAttachmentStore::class.java.name
  const val ATTACHMENTS_DIR_NAME = "com.facebook.NativeAppCallAttachmentStore.files"
  private var attachmentsDirectory: File? = null

  @JvmStatic
  fun createAttachment(callId: UUID, attachmentBitmap: Bitmap): Attachment {
    return Attachment(callId, attachmentBitmap, null)
  }

  @JvmStatic
  fun createAttachment(callId: UUID, attachmentUri: Uri): Attachment {
    return Attachment(callId, null, attachmentUri)
  }

  @Throws(IOException::class)
  private fun processAttachmentBitmap(bitmap: Bitmap, outputFile: File) {
    val outputStream = FileOutputStream(outputFile)
    try {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    } finally {
      Utility.closeQuietly(outputStream)
    }
  }

  @Throws(IOException::class)
  private fun processAttachmentFile(imageUri: Uri, isContentUri: Boolean, outputFile: File) {
    val outputStream = FileOutputStream(outputFile)
    try {
      val inputStream =
          if (!isContentUri) {
            FileInputStream(imageUri.path)
          } else {
            FacebookSdk.getApplicationContext().contentResolver.openInputStream(imageUri)
          }
      Utility.copyAndCloseInputStream(inputStream, outputStream)
    } finally {
      Utility.closeQuietly(outputStream)
    }
  }

  @Throws(FacebookException::class)
  @JvmStatic
  fun addAttachments(attachments: Collection<Attachment>?) {
    if (attachments == null || attachments.isEmpty()) {
      return
    }

    // If this is the first time we've been instantiated, clean up any existing attachments.
    if (attachmentsDirectory == null) {
      cleanupAllAttachments()
    }
    ensureAttachmentsDirectoryExists()
    val filesToCleanup: MutableList<File?> = ArrayList()
    try {
      for (attachment in attachments) {
        if (!attachment.shouldCreateFile) {
          continue
        }
        val file = getAttachmentFile(attachment.callId, attachment.attachmentName, true)
        if (file != null) {
          filesToCleanup.add(file)
          if (attachment.bitmap != null) {
            processAttachmentBitmap(attachment.bitmap, file)
          } else if (attachment.originalUri != null) {
            processAttachmentFile(attachment.originalUri, attachment.isContentUri, file)
          }
        }
      }
    } catch (exception: IOException) {
      Log.e(TAG, "Got unexpected exception:$exception")
      for (file in filesToCleanup) {
        try {
          file?.delete()
        } catch (e: Exception) {
          // Always try to delete other files.
        }
      }
      throw FacebookException(exception)
    }
  }

  /**
   * Removes any temporary files associated with a particular native app call.
   *
   * @param callId the unique ID of the call
   */
  @JvmStatic
  fun cleanupAttachmentsForCall(callId: UUID) {
    val dir = getAttachmentsDirectoryForCall(callId, false)
    if (dir != null) {
      Utility.deleteDirectory(dir)
    }
  }

  @Throws(FileNotFoundException::class)
  @JvmStatic
  fun openAttachment(callId: UUID?, attachmentName: String?): File? {
    if (Utility.isNullOrEmpty(attachmentName) || callId == null) {
      throw FileNotFoundException()
    }
    return try {
      getAttachmentFile(callId, attachmentName, false)
    } catch (e: IOException) {
      // We don't try to create the file, so we shouldn't get any IOExceptions. But if we do,
      // just act like the file wasn't found.
      throw FileNotFoundException()
    }
  }

  @Synchronized
  @JvmStatic
  fun getAttachmentsDirectory(): File? {
    if (attachmentsDirectory == null) {
      attachmentsDirectory =
          File(FacebookSdk.getApplicationContext().cacheDir, ATTACHMENTS_DIR_NAME)
    }
    return attachmentsDirectory
  }

  @JvmStatic
  fun ensureAttachmentsDirectoryExists(): File? {
    val dir = getAttachmentsDirectory()
    dir?.mkdirs()
    return dir
  }

  @JvmStatic
  fun getAttachmentsDirectoryForCall(callId: UUID, create: Boolean): File? {
    if (attachmentsDirectory == null) {
      return null
    }
    val dir = File(attachmentsDirectory, callId.toString())
    if (create && !dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  @Throws(IOException::class)
  @JvmStatic
  fun getAttachmentFile(callId: UUID, attachmentName: String?, createDirs: Boolean): File? {
    val dir = getAttachmentsDirectoryForCall(callId, createDirs) ?: return null
    return try {
      File(dir, URLEncoder.encode(attachmentName, "UTF-8"))
    } catch (e: UnsupportedEncodingException) {
      null
    }
  }

  @JvmStatic
  fun cleanupAllAttachments() {
    // Attachments directory may or may not exist; we won't create it if not, since we are just
    // going to delete it.
    Utility.deleteDirectory(getAttachmentsDirectory())
  }

  class Attachment(val callId: UUID, val bitmap: Bitmap?, val originalUri: Uri?) {
    val attachmentUrl: String
    val attachmentName: String?
    var isContentUri = false
    var shouldCreateFile = false

    init {
      if (originalUri != null) {
        val scheme = originalUri.scheme
        if ("content".equals(scheme, ignoreCase = true)) {
          isContentUri = true
          shouldCreateFile =
              with(originalUri.authority) { this != null && !this.startsWith("media") }
        } else if ("file".equals(originalUri.scheme, ignoreCase = true)) {
          shouldCreateFile = true
        } else if (!Utility.isWebUri(originalUri)) {
          throw FacebookException("Unsupported scheme for media Uri : $scheme")
        }
      } else if (bitmap != null) {
        shouldCreateFile = true
      } else {
        throw FacebookException("Cannot share media without a bitmap or Uri set")
      }
      attachmentName = if (!shouldCreateFile) null else UUID.randomUUID().toString()
      attachmentUrl =
          if (!shouldCreateFile) {
            originalUri.toString()
          } else {
            FacebookContentProvider.getAttachmentUrl(
                FacebookSdk.getApplicationId(), callId, attachmentName)
          }
    }
  }
}
