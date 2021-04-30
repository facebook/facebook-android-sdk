// Copyright 2004-present Facebook. All Rights Reserved.
package com.facebook.appevents.internal

import androidx.annotation.VisibleForTesting
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest

/** Utility class to compute file checksums. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object HashUtils {
  private const val MD5 = "MD5"
  private const val BUFFER_SIZE = 1024

  @Throws(Exception::class)
  @JvmStatic
  fun computeChecksum(path: String?): String {
    return computeFileMd5(File(path))
  }

  @Throws(Exception::class)
  private fun computeFileMd5(file: File): String {
    BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { fis ->
      val md = MessageDigest.getInstance(MD5)
      val buffer = ByteArray(BUFFER_SIZE)
      var numRead: Int
      do {
        numRead = fis.read(buffer)
        if (numRead > 0) {
          md.update(buffer, 0, numRead)
        }
      } while (numRead != -1)

      // Convert byte array to hex string and return result.
      return BigInteger(1, md.digest()).toString(16)
    }
  }
}
