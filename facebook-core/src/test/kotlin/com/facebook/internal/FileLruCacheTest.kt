/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Random
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class FileLruCacheTest : FacebookPowerMockTestCase() {
  companion object {
    private val random = Random()
    private val mockExecutor = FacebookSerialExecutor()
    private const val CACHE_CLEAR_TIMEOUT: Long = 100

    private fun clearFileLruCache(cache: FileLruCache) {
      // since the cache clearing happens in a separate thread, we need to wait until
      // the clear is complete before we can check for the existence of the old files
      val lock = ReentrantLock()
      val condition = lock.newCondition()
      lock.withLock {
        cache.clearCache()
        FacebookSdk.getExecutor().execute {
          lock.withLock { lock.withLock { condition.signalAll() } }
        }
        condition.await(CACHE_CLEAR_TIMEOUT, TimeUnit.MILLISECONDS)
      }
      Thread.sleep(CACHE_CLEAR_TIMEOUT)
    }

    private fun deleteLruCacheDirectory(cache: FileLruCache) {
      val directory = File(cache.location)
      directory.delete()
    }

    private fun clearAndDeleteLruCacheDirectory(cache: FileLruCache) {
      clearFileLruCache(cache)
      deleteLruCacheDirectory(cache)
    }
  }

  @Before
  fun before() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isFullyInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    val tmp = File(UUID.randomUUID().toString())
    tmp.mkdir()
    tmp.deleteOnExit()
    whenever(FacebookSdk.getCacheDir()).thenReturn(tmp)
  }

  @Test
  fun testCacheOutputStream() {
    val dataSize = 1_024
    val data = generateBytes(dataSize)
    val key = "a"

    // Limit to 2x to allow for extra header data
    val cache = FileLruCache("testCacheOutputStream", limitCacheSize(2 * dataSize))
    try {
      put(cache, key, data)
      checkValue(cache, key, data)
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheInputStream() {
    val dataSize = 1_024
    val data = generateBytes(dataSize)
    val key = "a"
    val stream: InputStream = ByteArrayInputStream(data)

    // Limit to 2x to allow for extra header data
    val cache = FileLruCache("testCacheInputStream", limitCacheSize(2 * dataSize))
    try {
      clearFileLruCache(cache)
      val wrapped = cache.interceptAndPut(key, stream)
      consumeAndClose(wrapped)
      checkValue(cache, key, data)
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheClear() {
    val dataSize = 1_024
    val data = generateBytes(dataSize)
    val key = "a"

    // Limit to 2x to allow for extra header data
    val cache = FileLruCache("testCacheClear", limitCacheSize(2 * dataSize))
    try {
      clearFileLruCache(cache)
      put(cache, key, data)
      checkValue(cache, key, data)
      clearFileLruCache(cache)
      Assert.assertEquals(false, hasValue(cache, key))
      Assert.assertEquals(0, cache.sizeInBytesForTest())
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheClearMidBuffer() {
    val dataSize = 1_024
    val data = generateBytes(dataSize)
    val key = "a"
    val key2 = "b"

    // Limit to 2x to allow for extra header data
    val cache = FileLruCache("testCacheClear", limitCacheSize(2 * dataSize))
    try {
      clearFileLruCache(cache)
      put(cache, key, data)
      checkValue(cache, key, data)
      val stream = cache.openPutStream(key2)
      Thread.sleep(200)
      clearFileLruCache(cache)
      stream.write(data)
      stream.close()
      Assert.assertEquals(false, hasValue(cache, key))
      Assert.assertEquals(false, hasValue(cache, key2))
      Assert.assertEquals(0, cache.sizeInBytesForTest())
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testSizeInBytes() {
    val count = 17
    val dataSize = 53
    val cacheSize = count * dataSize
    val data = generateBytes(dataSize)

    // Limit to 2x to allow for extra header data
    val cache = FileLruCache("testSizeInBytes", limitCacheSize(2 * cacheSize))
    try {
      clearFileLruCache(cache)
      for (i in 0 until count) {
        put(cache, i, data)

        // The size reported by sizeInBytes includes a version/size token as well
        // as a JSON blob that records the name.  Verify that the cache size is larger
        // than the data content but not more than twice as large.  This guarantees
        // that sizeInBytes is doing at least approximately the right thing.
        val totalDataSize = (i + 1) * dataSize
        assertThat(cache.sizeInBytesForTest() > totalDataSize).isTrue
        assertThat(cache.sizeInBytesForTest() < 2 * totalDataSize).isTrue
      }
      for (i in 0 until count) {
        checkValue(cache, i.toString(), data)
      }
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheSizeLimit() {
    val count = 64
    val dataSize = 32
    val cacheSize = count * dataSize / 2
    val data = generateBytes(dataSize)

    // Here we do not set the limit to 2x to make sure we hit the limit well before we have
    // added all the data.
    val cache = FileLruCache("testCacheSizeLimit", limitCacheSize(cacheSize))
    try {
      clearFileLruCache(cache)
      for (i in 0 until count) {
        put(cache, i, data)

        // See comment in testSizeInBytes for why this is not an exact calculation.
        //
        // This changes verification such that the final cache size lands somewhere
        // between half and full quota.
        val totalDataSize = (i + 1) * dataSize
        assertThat(cache.sizeInBytesForTest() > min(totalDataSize, cacheSize / 2)).isTrue
        assertThat(cache.sizeInBytesForTest() < min(2 * totalDataSize, cacheSize)).isTrue
      }

      // sleep for a bit to make sure the trim finishes
      Thread.sleep(200)

      // Verify that some keys exist and others do not
      var hasValueExists = false
      var hasNoValueExists = false
      for (i in 0 until count) {
        val key = Integer.valueOf(i).toString()
        if (hasValue(cache, key)) {
          hasValueExists = true
          checkValue(cache, key, data)
        } else {
          hasNoValueExists = true
        }
      }
      Assert.assertEquals(true, hasValueExists)
      Assert.assertEquals(true, hasNoValueExists)
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheCountLimit() {
    val count = 64
    val dataSize = 32
    val cacheCount = count / 2
    val data = generateBytes(dataSize)

    // Here we only limit by count, and we allow half of the entries.
    val cache = FileLruCache("testCacheCountLimit", limitCacheCount(cacheCount))
    try {
      clearFileLruCache(cache)
      for (i in 0 until count) {
        put(cache, i, data)
      }

      // sleep for a bit to make sure the trim finishes
      Thread.sleep(200)

      // Verify that some keys exist and others do not
      var hasValueExists = false
      var hasNoValueExists = false
      for (i in 0 until count) {
        if (hasValue(cache, i)) {
          hasValueExists = true
          checkValue(cache, i, data)
        } else {
          hasNoValueExists = true
        }
      }
      Assert.assertEquals(true, hasValueExists)
      Assert.assertEquals(true, hasNoValueExists)
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testCacheLru() {
    val keepCount = 10
    val otherCount = 5
    val dataSize = 64
    val data = generateBytes(dataSize)

    // Limit by count, and allow all the keep keys plus one other.
    val cache = FileLruCache("testCacheLru", limitCacheCount(keepCount + 1))
    try {
      clearFileLruCache(cache)
      for (i in 0 until keepCount) {
        put(cache, i, data)
      }
      for (i in 0 until otherCount) {
        put(cache, keepCount + i, data)
        Thread.sleep(1_000)

        // By verifying all the keep keys, they should be LRU and survive while the others do not.
        for (keepIndex in 0 until keepCount) {
          checkValue(cache, keepIndex, data)
        }
      }

      // All but the last other key should have been pushed out
      for (i in 0 until otherCount - 1) {
        val key = (keepCount + i).toString()
        Assert.assertEquals(false, hasValue(cache, key))
      }
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  @Test
  fun testConcurrentWritesToSameKey() {
    val count = 5
    val dataSize = 81
    val threadCount = 31
    val iterationCount = 10
    val data = generateBytes(dataSize)
    val cache = FileLruCache("testConcurrentWritesToSameKey", limitCacheCount(count + 1))
    try {
      clearFileLruCache(cache)
      val run = Runnable {
        repeat(iterationCount) {
          for (i in 0 until count) {
            put(cache, i, data)
          }
        }
      }

      val threads = (0 until threadCount).map { Thread(run) }
      for (thread in threads) {
        thread.start()
      }
      for (thread in threads) {
        thread.join(10 * 1_000L, 0)
      }

      // Verify that the file state ended up consistent in the end
      for (i in 0 until count) {
        checkValue(cache, i, data)
      }
    } finally {
      clearAndDeleteLruCacheDirectory(cache)
    }
  }

  private fun generateBytes(n: Int): ByteArray {
    val bytes = ByteArray(n)
    random.nextBytes(bytes)
    return bytes
  }

  private fun limitCacheSize(n: Int): FileLruCache.Limits {
    val limits = FileLruCache.Limits()
    limits.byteCount = n
    return limits
  }

  private fun limitCacheCount(n: Int): FileLruCache.Limits {
    val limits = FileLruCache.Limits()
    limits.fileCount = n
    return limits
  }

  private fun put(cache: FileLruCache, i: Int, data: ByteArray) {
    put(cache, i.toString(), data)
  }

  private fun put(cache: FileLruCache, key: String, data: ByteArray) {
    try {
      val stream = cache.openPutStream(key)
      Assert.assertNotNull(stream)
      stream.write(data)
      stream.close()
    } catch (e: IOException) {
      // Fail test and print Exception
      Assert.assertNull(e)
    }
  }

  private fun checkValue(cache: FileLruCache, i: Int, expected: ByteArray) {
    checkValue(cache, i.toString(), expected)
  }

  private fun checkValue(cache: FileLruCache, key: String, expected: ByteArray) {
    try {
      val stream = cache[key]
      Assert.assertNotNull(stream)
      checkNotNull(stream)
      checkInputStream(expected, stream)
      stream.close()
    } catch (e: IOException) {
      // Fail test and print Exception
      Assert.assertNull(e)
    }
  }

  private fun hasValue(cache: FileLruCache, i: Int): Boolean {
    return hasValue(cache, i.toString())
  }

  private fun hasValue(cache: FileLruCache, key: String): Boolean {
    var stream: InputStream? = null
    try {
      stream = cache[key]
    } catch (e: IOException) {
      // Fail test and print Exception
      Assert.assertNull(e)
    }
    return stream != null
  }

  private fun checkInputStream(expected: ByteArray, actual: InputStream) {
    try {
      for (i in expected.indices) {
        val b = actual.read()
        Assert.assertEquals(expected[i].toInt() and 0xff, b)
      }
      val eof = actual.read()
      Assert.assertEquals(-1, eof)
    } catch (e: IOException) {
      // Fail test and print Exception
      Assert.assertNull(e)
    }
  }

  private fun consumeAndClose(stream: InputStream) {
    try {
      val buffer = ByteArray(1_024)
      while (stream.read(buffer) > -1) {
        // these bytes intentionally ignored
      }
      stream.close()
    } catch (e: IOException) {
      // Fail test and print Exception
      Assert.assertNull(e)
    }
  }
}
