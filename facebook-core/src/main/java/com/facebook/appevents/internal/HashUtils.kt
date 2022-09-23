/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.util.concurrent.locks.ReentrantLock

/** Utility class to compute file checksums. */
internal object HashUtils {
  private const val MD5 = "MD5"
  private const val BUFFER_SIZE = 1024
  private val TAG = HashUtils::class.java.simpleName
  private val TRUSTED_CERTS =
      arrayOf(
          "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")
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

  @JvmStatic
  fun computeChecksumWithPackageManager(context: Context, nanosTimeout: Long?): String? {
    val certFactory = CertificateFactory.getInstance("X.509")
    val trustedInstaller =
        TRUSTED_CERTS.map {
              certFactory.generateCertificate(
                  ByteArrayInputStream(Base64.decode(it, Base64.DEFAULT)))
            }
            .toMutableList()
    var resultChecksum: String? = null
    val lock = ReentrantLock()
    val checksumReady = lock.newCondition()
    lock.lock()

    try {
      val checksumClass = Class.forName("android.content.pm.Checksum")
      val typeWholeMd5Field: Field = checksumClass.getField("TYPE_WHOLE_MD5")
      val TYPE_WHOLE_MD5 = typeWholeMd5Field.get(null)
      val checksumReadyListenerClass =
          Class.forName("android.content.pm.PackageManager\$OnChecksumsReadyListener")
      val listener: Any =
          Proxy.newProxyInstance(
              HashUtils::class.java.classLoader,
              arrayOf(checksumReadyListenerClass),
              object : InvocationHandler {
                override operator fun invoke(o: Any?, method: Method, objects: Array<Any>): Any? {
                  try {
                    if (method.name == "onChecksumsReady" &&
                        objects.size == 1 &&
                        objects[0] is List<*>) {
                      val list = objects[0] as List<*>
                      for (c in list) {
                        if (c != null) {
                          val getSplitNameMethod: Method = c.javaClass.getMethod("getSplitName")
                          val getTypeMethod: Method = c.javaClass.getMethod("getType")
                          if (getSplitNameMethod.invoke(c) == null &&
                              getTypeMethod.invoke(c) == TYPE_WHOLE_MD5) {
                            val getValueMethod: Method = c.javaClass.getMethod("getValue")
                            val checksumValue = getValueMethod.invoke(c) as ByteArray
                            resultChecksum = BigInteger(1, checksumValue).toString(16)
                            lock.lock()
                            try {
                              checksumReady.signalAll()
                            } finally {
                              lock.unlock()
                            }
                            return null
                          }
                        }
                      }
                    }
                  } catch (t: Throwable) {
                    Log.d(TAG, "Can't fetch checksum.", t)
                  }
                  return null
                }
              })
      val requestChecksumsMethod: Method =
          PackageManager::class
              .java
              .getMethod(
                  "requestChecksums",
                  String::class.java,
                  Boolean::class.javaPrimitiveType,
                  Int::class.javaPrimitiveType,
                  MutableList::class.java,
                  checksumReadyListenerClass)
      requestChecksumsMethod.invoke(
          context.packageManager,
          context.packageName,
          false,
          TYPE_WHOLE_MD5,
          trustedInstaller.toMutableList(),
          listener)
      if (nanosTimeout == null) {
        checksumReady.await()
      } else {
        checksumReady.awaitNanos(nanosTimeout)
      }
      return resultChecksum
    } catch (t: Throwable) {
      // Checksum API is not available, return null
      return null
    } finally {
      lock.unlock()
    }
  }
}
