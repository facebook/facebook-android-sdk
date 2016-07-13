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

import android.graphics.Bitmap;

import com.facebook.FacebookTestCase;

import org.junit.Test;
import org.robolectric.Robolectric;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;


public class NativeAppCallAttachmentStoreTest extends FacebookTestCase {
    private static final UUID CALL_ID = UUID.randomUUID();
    private static final String ATTACHMENT_NAME = "hello";

    @Override
    public void setUp() {
        super.setUp();
    }

    private Bitmap createBitmap() {
        return Bitmap.createBitmap(20, 20, Bitmap.Config.ALPHA_8);
    }

    private List<NativeAppCallAttachmentStore.Attachment> createAttachments(
            UUID callId, Bitmap bitmap) {
        List<NativeAppCallAttachmentStore.Attachment> attachments = new ArrayList<>();
        attachments.add(NativeAppCallAttachmentStore.createAttachment(callId, bitmap));

        return attachments;
    }

    @Test
    public void testAddAttachmentsForCallWithNullCallId() throws Exception {
        try {
            List<NativeAppCallAttachmentStore.Attachment> attachments =
                    createAttachments(null, createBitmap());
            NativeAppCallAttachmentStore.addAttachments(attachments);
            fail("expected exception");
        } catch (NullPointerException ex) {
            assertTrue(ex.getMessage().contains("callId"));
        }
    }

    @Test
    public void testAddAttachmentsForCallWithNullBitmap() throws Exception {
        try {
            List<NativeAppCallAttachmentStore.Attachment> attachments =
                    createAttachments(CALL_ID, null);
            NativeAppCallAttachmentStore.addAttachments(attachments);
            fail("expected exception");
        } catch (NullPointerException ex) {
            assertTrue(ex.getMessage().contains("attachmentBitmap"));
        }
    }

    @Test
    public void testGetAttachmentsDirectory() throws Exception {
        File dir = NativeAppCallAttachmentStore.getAttachmentsDirectory();
        assertNotNull(dir);
        assertTrue(
                dir.getAbsolutePath().contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME));
    }

    @Test
    public void testGetAttachmentsDirectoryForCall() throws Exception {
        NativeAppCallAttachmentStore.ensureAttachmentsDirectoryExists();
        File dir = NativeAppCallAttachmentStore.getAttachmentsDirectoryForCall(CALL_ID, false);
        assertNotNull(dir);
        assertTrue(
                dir.getAbsolutePath().contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME));
        assertTrue(dir.getAbsolutePath().contains(CALL_ID.toString()));
    }

    @Test
    public void testGetAttachmentFile() throws Exception {
        NativeAppCallAttachmentStore.ensureAttachmentsDirectoryExists();
        File dir = NativeAppCallAttachmentStore.getAttachmentFile(CALL_ID, ATTACHMENT_NAME, false);
        assertNotNull(dir);
        assertTrue(
                dir.getAbsolutePath().contains(NativeAppCallAttachmentStore.ATTACHMENTS_DIR_NAME));
        assertTrue(dir.getAbsolutePath().contains(CALL_ID.toString()));
        assertTrue(dir.getAbsolutePath().contains(ATTACHMENT_NAME.toString()));
    }
}
