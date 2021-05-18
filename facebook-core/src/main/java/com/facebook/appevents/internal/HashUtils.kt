// Copyright 2004-present Facebook. All Rights Reserved.
package com.facebook.appevents.internal

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock

/** Utility class to compute file checksums. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object HashUtils {
  private const val MD5 = "MD5"
  private const val BUFFER_SIZE = 1024
  private val TAG = HashUtils::class.java.simpleName

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
    var resultChecksum: String? = null
    val lock = ReentrantLock()
    val checksumReady = lock.newCondition()
    lock.lock()

    try {
      val checksumClass = Class.forName("android.content.pm.Checksum")
      val typeWholeMd5Field: Field = checksumClass.getField("TYPE_WHOLE_MD5")
      val TYPE_WHOLE_MD5 = typeWholeMd5Field.get(null)
      val trustAllField: Field = PackageManager::class.java.getField("TRUST_ALL")
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
                            resultChecksum = checksumArrayToString(checksumValue)
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
          PackageManager::class.java.getMethod(
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
          trustAllField.get(null),
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

  private fun checksumArrayToString(checksum: ByteArray): String {
    val builder = StringBuilder()

    checksum.forEach {
      val unsignedByte =
          if (it >= 0) {
            it.toInt()
          } else {
            256 + it.toInt()
          }
      if (unsignedByte < 16) builder.append('0')
      builder.append(unsignedByte.toString(16))
    }
    return builder.toString()
  }
}
