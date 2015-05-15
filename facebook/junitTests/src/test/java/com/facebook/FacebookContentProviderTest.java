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

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Pair;

import com.facebook.internal.NativeAppCallAttachmentStore;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({ NativeAppCallAttachmentStore.class })
public class FacebookContentProviderTest extends FacebookPowerMockTestCase {
    private static final String APP_ID = "12345";
    private static final UUID CALL_ID = UUID.randomUUID();
    private static final String ATTACHMENT_NAME = "attachMe";

    private FacebookContentProvider providerUnderTest;

    @Before
    public void before() throws Exception {
        mockStatic(NativeAppCallAttachmentStore.class);
        providerUnderTest = new FacebookContentProvider();
    }

    @Test
    public void testGetAttachmentUrl() {
        String url = FacebookContentProvider.getAttachmentUrl(APP_ID, CALL_ID, ATTACHMENT_NAME);
        assertEquals("content://com.facebook.app.FacebookContentProvider" +
                APP_ID + "/" + CALL_ID + "/" + ATTACHMENT_NAME, url);
    }

    @Test
    public void testOnCreate() throws Exception {
        assertTrue(providerUnderTest.onCreate());
    }

    @Test
    public void testQuery() throws Exception {
        assertNull(providerUnderTest.query(null, null, null, null, null));
    }

    @Test
    public void testGetType() throws Exception {
        assertNull(providerUnderTest.getType(null));
    }

    @Test
    public void testInsert() throws Exception {
        assertNull(providerUnderTest.insert(null, null));
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(0, providerUnderTest.delete(null, null, null));
    }

    @Test
    public void testUpdate() throws Exception {
        assertEquals(0, providerUnderTest.update(null, null, null, null));
    }

    @SuppressWarnings("unused")
    @Test
    public void testOpenFileWithNullUri() throws Exception {
        try {
            ParcelFileDescriptor pfd = providerUnderTest.openFile(null, "r");
            fail("expected FileNotFoundException");
        } catch (FileNotFoundException e) {
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void testOpenFileWithBadPath() throws Exception {
        try {
            ParcelFileDescriptor pfd = providerUnderTest.openFile(Uri.parse("/"), "r");
            fail("expected FileNotFoundException");
        } catch (FileNotFoundException e) {
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void testOpenFileWithoutCallIdAndAttachment() throws Exception {
        try {
            ParcelFileDescriptor pfd = providerUnderTest.openFile(Uri.parse("/foo"), "r");
            fail("expected FileNotFoundException");
        } catch (FileNotFoundException e) {
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void testOpenFileWithBadCallID() throws Exception {
        try {
            ParcelFileDescriptor pfd = providerUnderTest.openFile(Uri.parse("/foo/bar"), "r");
            fail("expected FileNotFoundException");
        } catch (FileNotFoundException e) {
        }
    }

    @Test
    public void testOpenFileWithUnknownUri() throws Exception {
        try {
            ParcelFileDescriptor pfd = getTestAttachmentParcelFileDescriptor(UUID.randomUUID());
            assertNotNull(pfd);
            pfd.close();

            fail("expected FileNotFoundException");
        } catch (FileNotFoundException e) {
        }
    }

    @Test
    public void testOpenFileWithKnownUri() throws Exception {
        MockAttachmentStore.addAttachment(CALL_ID, ATTACHMENT_NAME);

        ParcelFileDescriptor pfd = getTestAttachmentParcelFileDescriptor(CALL_ID);
        assertNotNull(pfd);
        pfd.close();
    }

    private ParcelFileDescriptor getTestAttachmentParcelFileDescriptor(UUID callId)
            throws Exception {
        when(NativeAppCallAttachmentStore.openAttachment(callId, ATTACHMENT_NAME))
                .thenReturn(MockAttachmentStore.openAttachment(callId, ATTACHMENT_NAME));

        Uri uri = Uri.parse(
                FacebookContentProvider.getAttachmentUrl(APP_ID, callId, ATTACHMENT_NAME));

        return providerUnderTest.openFile(uri, "r");
    }

    static class MockAttachmentStore {
        private static List<Pair<UUID, String>> attachments = new ArrayList<>();
        private static final String DUMMY_FILE_NAME = "dummyfile";

        public static void addAttachment(UUID callId, String attachmentName) {
            attachments.add(new Pair<>(callId, attachmentName));
        }

        public static File openAttachment(UUID callId, String attachmentName)
                throws FileNotFoundException {
            if (attachments.contains(new Pair<>(callId, attachmentName))) {
                File cacheDir = Robolectric.application.getCacheDir();
                File dummyFile = new File(cacheDir, DUMMY_FILE_NAME);
                if (!dummyFile.exists()) {
                    try {
                        dummyFile.createNewFile();
                    } catch (IOException e) {
                    }
                }

                return dummyFile;
            }

            throw new FileNotFoundException();
        }
    }
}
