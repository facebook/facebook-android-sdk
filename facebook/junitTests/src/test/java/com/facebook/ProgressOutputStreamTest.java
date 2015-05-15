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

package com.facebook;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProgressOutputStreamTest extends FacebookTestCase {
    private static final int MAX_PROGRESS = 10;

    private GraphRequest r1, r2;
    private Map<GraphRequest, RequestProgress> progressMap;
    private GraphRequestBatch requests;
    private ProgressOutputStream stream;

    @Before
    public void before() throws Exception {
        FacebookSdk.sdkInitialize(Robolectric.application);
        r1 = new GraphRequest(null, "4");
        r2 = new GraphRequest(null, "4");

        progressMap = new HashMap<GraphRequest, RequestProgress>();
        progressMap.put(r1, new RequestProgress(null, r1));
        progressMap.get(r1).addToMax(5);
        progressMap.put(r2, new RequestProgress(null, r2));
        progressMap.get(r2).addToMax(5);

        requests = new GraphRequestBatch(r1, r2);

        ByteArrayOutputStream backing = new ByteArrayOutputStream();
        stream = new ProgressOutputStream(backing, requests, progressMap, MAX_PROGRESS);
    }

    @After
    public void after() throws Exception {
        stream.close();
    }

    @Test
    public void testSetup() {
        assertEquals(0, stream.getBatchProgress());
        assertEquals(MAX_PROGRESS, stream.getMaxProgress());

        for (RequestProgress p : progressMap.values()) {
            assertEquals(0, p.getProgress());
            assertEquals(5, p.getMaxProgress());
        }
    }

    @Test
    public void testWriting() {
        try {
            assertEquals(0, stream.getBatchProgress());

            stream.setCurrentRequest(r1);
            stream.write(0);
            assertEquals(1, stream.getBatchProgress());

            final byte[] buf = new byte[4];
            stream.write(buf);
            assertEquals(5, stream.getBatchProgress());

            stream.setCurrentRequest(r2);
            stream.write(buf, 2, 2);
            stream.write(buf, 1, 3);
            assertEquals(MAX_PROGRESS, stream.getBatchProgress());

            assertEquals(stream.getMaxProgress(), stream.getBatchProgress());
            assertEquals(progressMap.get(r1).getMaxProgress(), progressMap.get(r1).getProgress());
            assertEquals(progressMap.get(r2).getMaxProgress(), progressMap.get(r2).getProgress());
        }
        catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
