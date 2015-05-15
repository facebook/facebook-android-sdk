/**
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

package com.facebook.internal;


import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;
import com.facebook.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import static org.junit.Assert.*;

public final class FileLruCacheTest extends FacebookTestCase {
    private static final Random random = new Random();

    @Before
    public void before() {
        FacebookSdk.sdkInitialize(Robolectric.application);
    }

    @Test
    public void testCacheOutputStream() throws Exception {
        int dataSize = 1024;
        byte[] data = generateBytes(dataSize);
        String key = "a";

        // Limit to 2x to allow for extra header data
        FileLruCache cache = new FileLruCache("testCacheOutputStream", limitCacheSize(2*dataSize));

        try {
            put(cache, key, data);
            checkValue(cache, key, data);
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheInputStream() throws Exception {
        int dataSize = 1024;
        byte[] data = generateBytes(dataSize);
        String key = "a";
        InputStream stream = new ByteArrayInputStream(data);

        // Limit to 2x to allow for extra header data
        FileLruCache cache = new FileLruCache("testCacheInputStream", limitCacheSize(2*dataSize));
        try {
            TestUtils.clearFileLruCache(cache);

            InputStream wrapped = cache.interceptAndPut(key, stream);
            consumeAndClose(wrapped);
            checkValue(cache, key, data);
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheClear() throws Exception {
        int dataSize = 1024;
        byte[] data = generateBytes(dataSize);
        String key = "a";

        // Limit to 2x to allow for extra header data
        FileLruCache cache = new FileLruCache("testCacheClear", limitCacheSize(2*dataSize));
        try {
            TestUtils.clearFileLruCache(cache);

            put(cache, key, data);
            checkValue(cache, key, data);

            TestUtils.clearFileLruCache(cache);
            assertEquals(false, hasValue(cache, key));
            assertEquals(0, cache.sizeInBytesForTest());
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheClearMidBuffer() throws Exception {
        int dataSize = 1024;
        byte[] data = generateBytes(dataSize);
        String key = "a";
        String key2 = "b";

        // Limit to 2x to allow for extra header data
        FileLruCache cache = new FileLruCache("testCacheClear", limitCacheSize(2*dataSize));
        try {
            TestUtils.clearFileLruCache(cache);

            put(cache, key, data);
            checkValue(cache, key, data);
            OutputStream stream = cache.openPutStream(key2);
            Thread.sleep(200);

            TestUtils.clearFileLruCache(cache);

            stream.write(data);
            stream.close();

            assertEquals(false, hasValue(cache, key));
            assertEquals(false, hasValue(cache, key2));
            assertEquals(0, cache.sizeInBytesForTest());
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testSizeInBytes() throws Exception {
        int count = 17;
        int dataSize = 53;
        int cacheSize = count * dataSize;
        byte[] data = generateBytes(dataSize);

        // Limit to 2x to allow for extra header data
        FileLruCache cache = new FileLruCache("testSizeInBytes", limitCacheSize(2*cacheSize));
        try {
            TestUtils.clearFileLruCache(cache);

            for (int i = 0; i < count; i++) {
                put(cache, i, data);

                // The size reported by sizeInBytes includes a version/size token as well
                // as a JSON blob that records the name.  Verify that the cache size is larger
                // than the data content but not more than twice as large.  This guarantees
                // that sizeInBytes is doing at least approximately the right thing.
                int totalDataSize = (i + 1) * dataSize;
                assertTrue(cache.sizeInBytesForTest() > totalDataSize);
                assertTrue(cache.sizeInBytesForTest() < 2 * totalDataSize);
            }
            for (int i = 0; i < count; i++) {
                String key = Integer.valueOf(i).toString();
                checkValue(cache, key, data);
            }
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheSizeLimit() throws Exception {
        int count = 64;
        int dataSize = 32;
        int cacheSize = count * dataSize / 2;
        byte[] data = generateBytes(dataSize);

        // Here we do not set the limit to 2x to make sure we hit the limit well before we have
        // added all the data.
        FileLruCache cache = new FileLruCache("testCacheSizeLimit", limitCacheSize(cacheSize));
        try {
            TestUtils.clearFileLruCache(cache);

            for (int i = 0; i < count; i++) {
                put(cache, i, data);

                // See comment in testSizeInBytes for why this is not an exact calculation.
                //
                // This changes verification such that the final cache size lands somewhere
                // between half and full quota.
                int totalDataSize = (i + 1) * dataSize;
                assertTrue(cache.sizeInBytesForTest() > Math.min(totalDataSize, cacheSize / 2));
                assertTrue(cache.sizeInBytesForTest() < Math.min(2 * totalDataSize, cacheSize));
            }

            // sleep for a bit to make sure the trim finishes
            Thread.sleep(200);

            // Verify that some keys exist and others do not
            boolean hasValueExists = false;
            boolean hasNoValueExists = false;

            for (int i = 0; i < count; i++) {
                String key = Integer.valueOf(i).toString();
                if (hasValue(cache, key)) {
                    hasValueExists = true;
                    checkValue(cache, key, data);
                } else {
                    hasNoValueExists = true;
                }
            }

            assertEquals(true, hasValueExists);
            assertEquals(true, hasNoValueExists);
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheCountLimit() throws Exception {
        int count = 64;
        int dataSize = 32;
        int cacheCount = count / 2;
        byte[] data = generateBytes(dataSize);

        // Here we only limit by count, and we allow half of the entries.
        FileLruCache cache = new FileLruCache("testCacheCountLimit", limitCacheCount(cacheCount));
        try {
            TestUtils.clearFileLruCache(cache);

            for (int i = 0; i < count; i++) {
                put(cache, i, data);
            }

            // sleep for a bit to make sure the trim finishes
            Thread.sleep(200);

            // Verify that some keys exist and others do not
            boolean hasValueExists = false;
            boolean hasNoValueExists = false;

            for (int i = 0; i < count; i++) {
                if (hasValue(cache, i)) {
                    hasValueExists = true;
                    checkValue(cache, i, data);
                } else {
                    hasNoValueExists = true;
                }
            }

            assertEquals(true, hasValueExists);
            assertEquals(true, hasNoValueExists);
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testCacheLru() throws IOException, InterruptedException {
        int keepCount = 10;
        int otherCount = 5;
        int dataSize = 64;
        byte[] data = generateBytes(dataSize);

        // Limit by count, and allow all the keep keys plus one other.
        FileLruCache cache = new FileLruCache("testCacheLru", limitCacheCount(keepCount + 1));
        try {
            TestUtils.clearFileLruCache(cache);

            for (int i = 0; i < keepCount; i++) {
                put(cache, i, data);
            }

            // Make sure operations are separated by enough time that the file timestamps are all different.
            // On the test device, it looks like lastModified has 1-second resolution, so we have to wait at
            // least a second to guarantee that updated timestamps will come later.
            Thread.sleep(1000);
            for (int i = 0; i < otherCount; i++) {
                put(cache, keepCount + i, data);
                Thread.sleep(1000);

                // By verifying all the keep keys, they should be LRU and survive while the others do not.
                for (int keepIndex = 0; keepIndex < keepCount; keepIndex++) {
                    checkValue(cache, keepIndex, data);
                }
                Thread.sleep(200);
            }

            // All but the last other key should have been pushed out
            for (int i = 0; i < (otherCount - 1); i++) {
                String key = Integer.valueOf(keepCount + i).toString();
                assertEquals(false, hasValue(cache, key));
            }
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    @Test
    public void testConcurrentWritesToSameKey() throws IOException, InterruptedException {
        final int count = 5;
        final int dataSize = 81;
        final int threadCount = 31;
        final int iterationCount = 10;
        final byte[] data = generateBytes(dataSize);

        final FileLruCache cache = new FileLruCache(
                "testConcurrentWritesToSameKey", limitCacheCount(count+1));
        try {
            TestUtils.clearFileLruCache(cache);

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    for (int iterations = 0; iterations < iterationCount; iterations++) {
                        for (int i = 0; i < count; i++) {
                            put(cache, i, data);
                        }
                    }
                }
            };

            // Create a bunch of threads to write a set of keys repeatedly
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(run);
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join(10 * 1000, 0);
            }

            // Verify that the file state ended up consistent in the end
            for (int i = 0; i < count; i++) {
                checkValue(cache, i, data);
            }
        } finally {
            TestUtils.clearAndDeleteLruCacheDirectory(cache);
        }
    }

    byte[] generateBytes(int n) {
        byte[] bytes = new byte[n];
        random.nextBytes(bytes);
        return bytes;
    }

    FileLruCache.Limits limitCacheSize(int n) {
        FileLruCache.Limits limits = new FileLruCache.Limits();
        limits.setByteCount(n);
        return limits;
    }

    FileLruCache.Limits limitCacheCount(int n) {
        FileLruCache.Limits limits = new FileLruCache.Limits();
        limits.setFileCount(n);
        return limits;
    }

    void put(FileLruCache cache, int i, byte[] data) {
        put(cache, Integer.valueOf(i).toString(), data);
    }

    void put(FileLruCache cache, String key, byte[] data) {
        try {
            OutputStream stream = cache.openPutStream(key);
            assertNotNull(stream);

            stream.write(data);
            stream.close();
        } catch (IOException e) {
            // Fail test and print Exception
            assertNull(e);
        }
    }

    void checkValue(FileLruCache cache, int i, byte[] expected) {
        checkValue(cache, Integer.valueOf(i).toString(), expected);
    }

    void checkValue(FileLruCache cache, String key, byte[] expected) {
        try {
            InputStream stream = cache.get(key);
            assertNotNull(stream);

            checkInputStream(expected, stream);
            stream.close();
        } catch (IOException e) {
            // Fail test and print Exception
            assertNull(e);
        }
    }

    boolean hasValue(FileLruCache cache, int i) {
        return hasValue(cache, Integer.valueOf(i).toString());
    }

    boolean hasValue(FileLruCache cache, String key) {
        InputStream stream = null;

        try {
            stream = cache.get(key);
        } catch (IOException e) {
            // Fail test and print Exception
            assertNull(e);
        }

        return stream != null;
    }

    void checkInputStream(byte[] expected, InputStream actual) {
        try {
            for (int i = 0; i < expected.length; i++) {
                int b = actual.read();
                assertEquals(((int)expected[i]) & 0xff, b);
            }

            int eof = actual.read();
            assertEquals(-1, eof);
        } catch (IOException e) {
            // Fail test and print Exception
            assertNull(e);
        }
    }

    void consumeAndClose(InputStream stream) {
        try {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) > -1) {
                // these bytes intentionally ignored
            }
            stream.close();
        } catch (IOException e) {
            // Fail test and print Exception
            assertNull(e);
        }
    }
}
