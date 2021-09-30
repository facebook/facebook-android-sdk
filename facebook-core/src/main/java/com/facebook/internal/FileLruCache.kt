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

import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger.Companion.log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidParameterException
import java.util.Date
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

// This class is intended to be thread-safe.
//
// There are two classes of files:  buffer files and cache files:
// - A buffer file is in the process of being written, and there is an open stream on the file.
//   These files are named as "bufferN" where N is an incrementing integer.  On startup, we delete
//   all existing files of this form. Once the stream is closed, we rename the buffer file to a
//   cache file or attempt to delete if this fails.  We do not otherwise ever attempt to delete
//   these files.
// - A cache file is a non-changing file that is named by the md5 hash of the cache key.  We monitor
//   the size of these files in aggregate and remove the oldest one(s) to stay under quota.  This
//   process does not block threads calling into this class, so theoretically we could go
//   arbitrarily over quota but in practice this should not happen because deleting files should be
//   much cheaper than downloading new file content.
//
// Since there can only ever be one thread accessing a particular buffer file, we do not synchronize
// access to these. We do assume that file rename is atomic when converting a buffer file to a cache
// file, and that if multiple files are renamed to a single target that exactly one of them
// continues to exist.
//
// Standard POSIX file semantics guarantee being able to continue to use a file handle even after
// the corresponding file has been deleted.  Given this and that cache files never change other than
// deleting in trim() or clear(),  we only have to ensure that there is at most one trim() or
// clear() process deleting files at any given time.
/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class FileLruCache(private val tag: String, private val limits: Limits) {
  private val directory: File = File(FacebookSdk.getCacheDir(), tag)
  private var isTrimPending = false
  private var isTrimInProgress = false
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()
  private val lastClearCacheTime = AtomicLong(0)

  // This is not robust to files changing dynamically underneath it and should therefore only be
  // used for test code.  If we ever need this for product code we need to think through
  // synchronization.
  // See the threading notes at the top of this class.
  //
  // Also, since trim() runs asynchronously now, this blocks until any pending trim has completed.
  fun sizeInBytesForTest(): Long {
    lock.withLock {
      while (isTrimPending || isTrimInProgress) {
        try {
          condition.await()
        } catch (e: InterruptedException) {
          // intentional no-op
        }
      }
    }
    val files = directory.listFiles()
    var total: Long = 0
    if (files != null) {
      for (file in files) {
        total += file.length()
      }
    }
    return total
  }

  @JvmOverloads
  @Throws(IOException::class)
  operator fun get(key: String, contentTag: String? = null): InputStream? {
    val file = File(directory, Utility.md5hash(key))
    val input: FileInputStream
    input =
        try {
          FileInputStream(file)
        } catch (e: IOException) {
          return null
        }
    val buffered = BufferedInputStream(input, Utility.DEFAULT_STREAM_BUFFER_SIZE)
    var success = false
    return try {
      val header = StreamHeader.readHeader(buffered) ?: return null
      val foundKey = header.optString(HEADER_CACHEKEY_KEY)
      if (foundKey != key) {
        return null
      }
      val headerContentTag = header.optString(HEADER_CACHE_CONTENT_TAG_KEY, null)
      if (contentTag == null && contentTag != headerContentTag) {
        return null
      }
      val accessTime = Date().time
      log(
          LoggingBehavior.CACHE,
          TAG,
          "Setting lastModified to " + java.lang.Long.valueOf(accessTime) + " for " + file.name)
      file.setLastModified(accessTime)
      success = true
      buffered
    } finally {
      if (!success) {
        buffered.close()
      }
    }
  }

  @JvmOverloads
  @Throws(IOException::class)
  fun openPutStream(key: String, contentTag: String? = null): OutputStream {
    val buffer = BufferFile.newFile(directory)
    buffer.delete()
    if (!buffer.createNewFile()) {
      throw IOException("Could not create file at " + buffer.absolutePath)
    }
    val file: FileOutputStream
    file =
        try {
          FileOutputStream(buffer)
        } catch (e: FileNotFoundException) {
          log(LoggingBehavior.CACHE, Log.WARN, TAG, "Error creating buffer output stream: $e")
          throw IOException(e.message)
        }
    val bufferFileCreateTime = System.currentTimeMillis()
    val renameToTargetCallback: StreamCloseCallback =
        object : StreamCloseCallback {
          override fun onClose() {
            // if the buffer file was created before the cache was cleared, then the buffer file
            // should be deleted rather than renamed and saved.
            if (bufferFileCreateTime < lastClearCacheTime.get()) {
              buffer.delete()
            } else {
              renameToTargetAndTrim(key, buffer)
            }
          }
        }
    val cleanup = CloseCallbackOutputStream(file, renameToTargetCallback)
    val buffered = BufferedOutputStream(cleanup, Utility.DEFAULT_STREAM_BUFFER_SIZE)
    var success = false
    return try {
      // Prefix the stream with the actual key, since there could be collisions
      val header = JSONObject()
      header.put(HEADER_CACHEKEY_KEY, key)
      if (!Utility.isNullOrEmpty(contentTag)) {
        header.put(HEADER_CACHE_CONTENT_TAG_KEY, contentTag)
      }
      StreamHeader.writeHeader(buffered, header)
      success = true
      buffered
    } catch (e: JSONException) {
      // JSON is an implementation detail of the cache, so don't let JSON exceptions out.
      log(LoggingBehavior.CACHE, Log.WARN, TAG, "Error creating JSON header for cache file: $e")
      throw IOException(e.message)
    } finally {
      if (!success) {
        buffered.close()
      }
    }
  }

  fun clearCache() {
    // get the current directory listing of files to delete
    val filesToDelete = directory.listFiles(BufferFile.excludeBufferFiles())
    lastClearCacheTime.set(System.currentTimeMillis())
    if (filesToDelete != null) {
      FacebookSdk.getExecutor().execute {
        for (file in filesToDelete) {
          file.delete()
        }
      }
    }
  }

  /**
   * The location of the cache.
   *
   * @return The location of the cache.
   */
  val location: String
    get() = directory.path

  private fun renameToTargetAndTrim(key: String, buffer: File) {
    val target = File(directory, Utility.md5hash(key))

    // This is triggered by close().  By the time close() returns, the file should be cached, so
    // this needs to happen synchronously on this thread.
    //
    // However, it does not need to be synchronized, since in the race we will just start an
    // unnecessary trim operation.  Avoiding the cost of holding the lock across the file
    // operation seems worth this cost.
    if (!buffer.renameTo(target)) {
      buffer.delete()
    }
    postTrim()
  }

  // Opens an output stream for the key, and creates an input stream wrapper to copy
  // the contents of input into the new output stream.  The effect is to store a
  // copy of input, and associate that data with key.
  @Throws(IOException::class)
  fun interceptAndPut(key: String, input: InputStream): InputStream {
    val output = openPutStream(key)
    return CopyingInputStream(input, output)
  }

  override fun toString(): String {
    return "{FileLruCache:" + " tag:" + tag + " file:" + directory.name + "}"
  }

  private fun postTrim() {
    lock.withLock {
      if (!isTrimPending) {
        isTrimPending = true
        FacebookSdk.getExecutor().execute { trim() }
      }
    }
  }

  private fun trim() {
    lock.withLock {
      isTrimPending = false
      isTrimInProgress = true
    }
    try {
      log(LoggingBehavior.CACHE, TAG, "trim started")
      val heap = PriorityQueue<ModifiedFile>()
      var size: Long = 0
      var count: Long = 0
      val filesToTrim = directory.listFiles(BufferFile.excludeBufferFiles())
      if (filesToTrim != null) {
        for (file in filesToTrim) {
          val modified = ModifiedFile(file)
          heap.add(modified)
          log(
              LoggingBehavior.CACHE,
              TAG,
              "  trim considering time=" +
                  java.lang.Long.valueOf(modified.modified) +
                  " name=" +
                  modified.file.name)
          size += file.length()
          count++
        }
      }
      while (size > limits.byteCount || count > limits.fileCount) {
        val file = heap.remove().file
        log(LoggingBehavior.CACHE, TAG, "  trim removing " + file.name)
        size -= file.length()
        count--
        file.delete()
      }
    } finally {
      lock.withLock {
        isTrimInProgress = false
        condition.signalAll()
      }
    }
  }

  private object BufferFile {
    private const val FILE_NAME_PREFIX = "buffer"
    private val filterExcludeBufferFiles = FilenameFilter { dir, filename ->
      !filename.startsWith(FILE_NAME_PREFIX)
    }
    private val filterExcludeNonBufferFiles = FilenameFilter { dir, filename ->
      filename.startsWith(FILE_NAME_PREFIX)
    }
    fun deleteAll(root: File) {
      val filesToDelete = root.listFiles(excludeNonBufferFiles())
      if (filesToDelete != null) {
        for (file in filesToDelete) {
          file.delete()
        }
      }
    }

    fun excludeBufferFiles(): FilenameFilter {
      return filterExcludeBufferFiles
    }

    fun excludeNonBufferFiles(): FilenameFilter {
      return filterExcludeNonBufferFiles
    }

    fun newFile(root: File?): File {
      val name = FILE_NAME_PREFIX + java.lang.Long.valueOf(bufferIndex.incrementAndGet()).toString()
      return File(root, name)
    }
  }

  // Treats the first part of a stream as a header, reads/writes it as a JSON blob, and
  // leaves the stream positioned exactly after the header.
  //
  // The format is as follows:
  //     byte: meaning
  // ---------------------------------
  //        0: version number
  //      1-3: big-endian JSON header blob size
  // 4-size+4: UTF-8 JSON header blob
  //      ...: stream data
  private object StreamHeader {
    private const val HEADER_VERSION = 0
    @Throws(IOException::class)
    fun writeHeader(stream: OutputStream, header: JSONObject) {
      val headerString = header.toString()
      val headerBytes = headerString.toByteArray()

      // Write version number and big-endian header size
      stream.write(HEADER_VERSION)
      stream.write(headerBytes.size shr 16 and 0xff)
      stream.write(headerBytes.size shr 8 and 0xff)
      stream.write(headerBytes.size shr 0 and 0xff)
      stream.write(headerBytes)
    }

    @Throws(IOException::class)
    fun readHeader(stream: InputStream): JSONObject? {
      val version = stream.read()
      if (version != HEADER_VERSION) {
        return null
      }
      var headerSize = 0
      repeat(3) {
        val b = stream.read()
        if (b == -1) {
          log(
              LoggingBehavior.CACHE,
              TAG,
              "readHeader: stream.read returned -1 while reading header size")
          return null
        }
        headerSize = headerSize shl 8
        headerSize += b and 0xff
      }
      val headerBytes = ByteArray(headerSize)
      var count = 0
      while (count < headerBytes.size) {
        val readCount = stream.read(headerBytes, count, headerBytes.size - count)
        if (readCount < 1) {
          log(
              LoggingBehavior.CACHE,
              TAG,
              "readHeader: stream.read stopped at " +
                  Integer.valueOf(count) +
                  " when expected " +
                  headerBytes.size)
          return null
        }
        count += readCount
      }
      val headerString = String(headerBytes)
      val header: JSONObject
      val tokener = JSONTokener(headerString)
      header =
          try {
            val parsed = tokener.nextValue()
            if (parsed !is JSONObject) {
              log(
                  LoggingBehavior.CACHE,
                  TAG,
                  "readHeader: expected JSONObject, got " + parsed.javaClass.canonicalName)
              return null
            }
            parsed
          } catch (e: JSONException) {
            throw IOException(e.message)
          }
      return header
    }
  }

  private class CloseCallbackOutputStream
  constructor(val innerStream: OutputStream, val callback: StreamCloseCallback) : OutputStream() {
    @Throws(IOException::class)
    override fun close() {
      try {
        innerStream.close()
      } finally {
        callback.onClose()
      }
    }

    @Throws(IOException::class)
    override fun flush() {
      innerStream.flush()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
      innerStream.write(buffer, offset, count)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
      innerStream.write(buffer)
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
      innerStream.write(oneByte)
    }
  }

  private class CopyingInputStream constructor(val input: InputStream, val output: OutputStream) :
      InputStream() {
    @Throws(IOException::class)
    override fun available(): Int {
      return input.available()
    }

    @Throws(IOException::class)
    override fun close() {
      // According to http://www.cs.cornell.edu/andru/javaspec/11.doc.html:
      //  "If a finally clause is executed because of abrupt completion of a try block and the
      //   finally clause itself completes abruptly, then the reason for the abrupt completion
      //   of the try block is discarded and the new reason for abrupt completion is
      //   propagated from there."
      //
      // Android does appear to behave like this.
      try {
        input.close()
      } finally {
        output.close()
      }
    }

    override fun mark(readlimit: Int) {
      throw UnsupportedOperationException()
    }

    override fun markSupported(): Boolean {
      return false
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray): Int {
      val count = input.read(buffer)
      if (count > 0) {
        output.write(buffer, 0, count)
      }
      return count
    }

    @Throws(IOException::class)
    override fun read(): Int {
      val b = input.read()
      if (b >= 0) {
        output.write(b)
      }
      return b
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
      val count = input.read(buffer, offset, length)
      if (count > 0) {
        output.write(buffer, offset, count)
      }
      return count
    }

    @Synchronized
    override fun reset() {
      throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
      val buffer = ByteArray(1024)
      var total: Long = 0
      while (total < byteCount) {
        val count = read(buffer, 0, Math.min(byteCount - total, buffer.size.toLong()).toInt())
        if (count < 0) {
          return total
        }
        total += count.toLong()
      }
      return total
    }
  }

  class Limits {
    // A Samsung Galaxy Nexus can create 1k files in half a second.  By the time
    // it gets to 5k files it takes 5 seconds.  10k files took 15 seconds.  This
    // continues to slow down as files are added.  This assumes all files are in
    // a single directory.
    //
    // Following a git-like strategy where we partition MD5-named files based on
    // the first 2 characters is slower across the board.
    var byteCount: Int = 1024 * 1024
      set(value) {
        if (value < 0) {
          throw InvalidParameterException("Cache byte-count limit must be >= 0")
        }
        field = value
      }
    var fileCount = 1024
      set(value) {
        if (value < 0) {
          throw InvalidParameterException("Cache file count limit must be >= 0")
        }
        field = value
      }
  }

  // Caches the result of lastModified during sort/heap operations
  private class ModifiedFile constructor(val file: File) : Comparable<ModifiedFile> {
    val modified = file.lastModified()
    override fun compareTo(another: ModifiedFile): Int {
      return when {
        modified < another.modified -> -1
        modified > another.modified -> 1
        else -> file.compareTo(another.file)
      }
    }

    override fun equals(another: Any?): Boolean {
      return another is ModifiedFile && compareTo(another) == 0
    }

    override fun hashCode(): Int {
      var result = HASH_SEED
      result = result * HASH_MULTIPLIER + file.hashCode()
      result = result * HASH_MULTIPLIER + (modified % Int.MAX_VALUE).toInt()
      return result
    }

    companion object {
      private const val HASH_SEED = 29 // Some random prime number
      private const val HASH_MULTIPLIER = 37 // Some random prime number
    }
  }

  private fun interface StreamCloseCallback {
    fun onClose()
  }

  companion object {
    val TAG = FileLruCache::class.java.simpleName
    private const val HEADER_CACHEKEY_KEY = "key"
    private const val HEADER_CACHE_CONTENT_TAG_KEY = "tag"
    private val bufferIndex = AtomicLong()
  }

  // The value of tag should be a final String that works as a directory name.
  init {
    // Ensure the cache dir exists
    if (directory.mkdirs() || directory.isDirectory) {
      // Remove any stale partially-written files from a previous run
      BufferFile.deleteAll(directory)
    }
  }
}
