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
import android.net.Uri;
import android.util.Log;

import com.facebook.FacebookContentProvider;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * <p>This class works in conjunction with {@link com.facebook.FacebookContentProvider} to allow
 * apps to attach binary attachments (e.g., images) to native dialogs launched via the sdk.It stores
 * attachments in temporary files and allows the Facebook application to retrieve them via the
 * content provider.</p>
 */
public final class NativeAppCallAttachmentStore {
    private static final String TAG = NativeAppCallAttachmentStore.class.getName();
    static final String ATTACHMENTS_DIR_NAME = "com.facebook.NativeAppCallAttachmentStore.files";
    private static File attachmentsDirectory;

    private NativeAppCallAttachmentStore() {}

    public static Attachment createAttachment(UUID callId, Bitmap attachmentBitmap) {
        Validate.notNull(callId, "callId");
        Validate.notNull(attachmentBitmap, "attachmentBitmap");

        return new Attachment(callId, attachmentBitmap, null);
    }

    public static Attachment createAttachment(UUID callId, Uri attachmentUri) {
        Validate.notNull(callId, "callId");
        Validate.notNull(attachmentUri, "attachmentUri");

        return new Attachment(callId, null, attachmentUri);
    }

    private static void processAttachmentBitmap(Bitmap bitmap, File outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } finally {
            Utility.closeQuietly(outputStream);
        }
    }

    private static void processAttachmentFile(
            Uri imageUri,
            boolean isContentUri,
            File outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        try {
            InputStream inputStream = null;
            if (!isContentUri) {
                inputStream = new FileInputStream(imageUri.getPath());
            } else {
                inputStream = FacebookSdk
                        .getApplicationContext()
                        .getContentResolver()
                        .openInputStream(imageUri);
            }

            Utility.copyAndCloseInputStream(inputStream, outputStream);
        } finally {
            Utility.closeQuietly(outputStream);
        }
    }

    public static void addAttachments(Collection<Attachment> attachments) {
        if (attachments == null || attachments.size() == 0) {
            return;
        }

        // If this is the first time we've been instantiated, clean up any existing attachments.
        if (attachmentsDirectory == null) {
            cleanupAllAttachments();
        }

        ensureAttachmentsDirectoryExists();

        List<File> filesToCleanup = new ArrayList<>();

        try {
            for (Attachment attachment : attachments) {
                if (!attachment.isBinaryData) {
                    continue;
                }

                File file = getAttachmentFile(
                        attachment.callId,
                        attachment.attachmentName,
                        true);
                filesToCleanup.add(file);

                if (attachment.bitmap != null) {
                    processAttachmentBitmap(attachment.bitmap, file);
                } else if (attachment.imageUri != null) {
                    processAttachmentFile(
                            attachment.imageUri,
                            attachment.isContentUri,
                            file);
                }
            }
        } catch (IOException exception) {
            Log.e(TAG, "Got unexpected exception:" + exception);
            for (File file : filesToCleanup) {
                try {
                    file.delete();
                } catch (Exception e) {
                    // Always try to delete other files.
                }
            }
            throw new FacebookException(exception);
        }
    }

    /**
     * Removes any temporary files associated with a particular native app call.
     *
     * @param callId the unique ID of the call
     */
    public static void cleanupAttachmentsForCall(UUID callId) {
        File dir = getAttachmentsDirectoryForCall(callId, false);
        if (dir != null) {
            Utility.deleteDirectory(dir);
        }
    }

    public static File openAttachment(UUID callId, String attachmentName)
            throws FileNotFoundException {
        if (Utility.isNullOrEmpty(attachmentName) ||
                callId == null) {
            throw new FileNotFoundException();
        }

        try {
            return getAttachmentFile(callId, attachmentName, false);
        } catch (IOException e) {
            // We don't try to create the file, so we shouldn't get any IOExceptions. But if we do,
            // just act like the file wasn't found.
            throw new FileNotFoundException();
        }
    }

    synchronized static File getAttachmentsDirectory() {
        if (attachmentsDirectory == null) {
            attachmentsDirectory = new File(
                    FacebookSdk.getApplicationContext().getCacheDir(),
                    ATTACHMENTS_DIR_NAME);
        }
        return attachmentsDirectory;
    }

    static File ensureAttachmentsDirectoryExists() {
        File dir = getAttachmentsDirectory();
        dir.mkdirs();
        return dir;
    }

    static File getAttachmentsDirectoryForCall(UUID callId, boolean create) {
        if (attachmentsDirectory == null) {
            return null;
        }

        File dir = new File(attachmentsDirectory, callId.toString());
        if (create && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    static File getAttachmentFile(
            UUID callId,
            String attachmentName,
            boolean createDirs
    ) throws IOException {
        File dir = getAttachmentsDirectoryForCall(callId, createDirs);
        if (dir == null) {
            return null;
        }

        try {
            return new File(dir, URLEncoder.encode(attachmentName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static void cleanupAllAttachments() {
        // Attachments directory may or may not exist; we won't create it if not, since we are just
        // going to delete it.
        File dir = getAttachmentsDirectory();
        Utility.deleteDirectory(dir);
    }

    public static final class Attachment {
        private final UUID callId;
        private final String attachmentUrl;
        private final String attachmentName;

        private Bitmap bitmap;
        private Uri imageUri;

        private boolean isContentUri;
        private boolean isBinaryData;

        private Attachment(UUID callId, Bitmap bitmap, Uri uri) {
            this.callId = callId;
            this.bitmap = bitmap;
            this.imageUri = uri;

            if (uri != null) {
                String scheme = uri.getScheme();
                if ("content".equalsIgnoreCase(scheme)) {
                    isContentUri = true;
                    isBinaryData = true;
                } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                    isBinaryData = true;
                } else if (!Utility.isWebUri(uri)) {
                    throw new FacebookException("Unsupported scheme for image Uri : " + scheme);
                }
            } else if (bitmap != null) {
                isBinaryData = true;
            } else {
                throw new FacebookException("Cannot share a photo without a bitmap or Uri set");
            }

            attachmentName = !isBinaryData ? null : UUID.randomUUID().toString();
            attachmentUrl = !isBinaryData
                    ? this.imageUri.toString() // http(s) images can be used directly
                    : FacebookContentProvider.getAttachmentUrl(
                            FacebookSdk.getApplicationId(),
                            callId,
                            attachmentName);
        }

        public String getAttachmentUrl() {
            return attachmentUrl;
        }
    }
}
