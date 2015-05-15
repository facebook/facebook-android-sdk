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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import com.facebook.TestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ImageResponseCacheTest extends AndroidTestCase {

    @LargeTest
    public void testImageCaching() throws Exception {
        // In unit test, since we need verify first access the image is not in cache
        // we need clear the cache first
        TestUtils.clearFileLruCache(ImageResponseCache.getCache(safeGetContext()));
        String imgUrl = "http://profile.ak.fbcdn.net/hprofile-ak-frc1/369438_100003049100322_615834658_n.jpg";

        Bitmap bmp1 = readImage(imgUrl, false);
        Bitmap bmp2 = readImage(imgUrl, true);
        compareImages(bmp1, bmp2);
    }

    @LargeTest
    public void testImageNotCaching() throws IOException {

        String imgUrl = "https://graph.facebook.com/ryanseacrest/picture?type=large";

        Bitmap bmp1 = readImage(imgUrl, false);
        Bitmap bmp2 = readImage(imgUrl, false);
        compareImages(bmp1, bmp2);
    }

    private Bitmap readImage(String uri, boolean expectedFromCache) {
        Bitmap bmp = null;
        InputStream istream = null;
        try
        {
            Uri url = Uri.parse(uri);
            // Check if the cache contains value for this url
            boolean isInCache =
                    (ImageResponseCache.getCache(safeGetContext()).get(url.toString()) != null);
            assertTrue(isInCache == expectedFromCache);
            // Read the image
            istream = ImageResponseCache.getCachedImageStream(url, safeGetContext());
            if (istream == null) {
                HttpURLConnection connection =
                        (HttpURLConnection) (new URL(uri)).openConnection();
                istream = ImageResponseCache.interceptAndCacheImageStream(
                        safeGetContext(),
                        connection);
            }

            assertTrue(istream != null);
            bmp = BitmapFactory.decodeStream(istream);
            assertTrue(bmp != null);
        } catch (Exception e) {
            assertNull(e);
        } finally {
            Utility.closeQuietly(istream);
        }
        return bmp;
    }

    private static void compareImages(Bitmap bmp1, Bitmap bmp2) {
        assertTrue(bmp1.getHeight() == bmp2.getHeight());
        assertTrue(bmp1.getWidth() == bmp1.getWidth());
        ByteBuffer buffer1 = ByteBuffer.allocate(bmp1.getHeight() * bmp1.getRowBytes());
        bmp1.copyPixelsToBuffer(buffer1);

        ByteBuffer buffer2 = ByteBuffer.allocate(bmp2.getHeight() * bmp2.getRowBytes());
        bmp2.copyPixelsToBuffer(buffer2);

        assertTrue(Arrays.equals(buffer1.array(), buffer2.array()));
    }

    private Context safeGetContext() {
        for (;;) {
            if ((getContext() != null) && (getContext().getApplicationContext() != null)) {
                return getContext();
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
        }
    }
}
