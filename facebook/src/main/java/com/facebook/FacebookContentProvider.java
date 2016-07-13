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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import com.facebook.internal.NativeAppCallAttachmentStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * <p>Implements a
 * <a href="http://developer.android.com/reference/android/content/ContentProvider.html">
 * ContentProvider</a> that can be used to provide binary attachments (e.g., images) to calls made
 * via {@link com.facebook.FacebookDialog}.
 * <p/>
 * <p>Note that this ContentProvider is only necessary if an application wishes to attach images,
 * etc., that are stored in memory and do not have another way to be referenced by a content URI.
 * For images obtained from, e.g., the Camera or Gallery, that already have a content URI associated
 * with them, use of this class is not necessary.
 * </p>
 * <p>If an application wishes to attach images that are stored in-memory within the application,
 * this content provider must be listed in the application's AndroidManifest.xml, and it should be
 * named according to the pattern
 * <code>"com.facebook.app.FacebookContentProvider{FACEBOOK_APP_ID}"</code>.
 * See the {@link FacebookContentProvider#getAttachmentUrl getContentProviderName} method.
 * </p>
 */
public class FacebookContentProvider extends ContentProvider {
    private static final String TAG = FacebookContentProvider.class.getName();
    private static final String ATTACHMENT_URL_BASE =
            "content://com.facebook.app.FacebookContentProvider";

    public FacebookContentProvider() {
    }

    /**
     * Returns the name of the content provider formatted correctly for constructing URLs.
     * @param applicationId the Facebook application ID of the application
     * @return the String to use as the authority portion of a content URI.
     */
    public static String getAttachmentUrl(
            String applicationId,
            UUID callId,
            String attachmentName) {
        return String.format(
                "%s%s/%s/%s",
                ATTACHMENT_URL_BASE,
                applicationId,
                callId.toString(),
                attachmentName);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    @Override
    public android.os.ParcelFileDescriptor openFile(android.net.Uri uri, java.lang.String mode)
            throws java.io.FileNotFoundException {

        Pair<UUID, String> callIdAndAttachmentName = parseCallIdAndAttachmentName(uri);
        if (callIdAndAttachmentName == null) {
            throw new FileNotFoundException();
        }

        try {
            File file = NativeAppCallAttachmentStore.openAttachment(
                    callIdAndAttachmentName.first,
                    callIdAndAttachmentName.second);

            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException exception) {
            Log.e(TAG, "Got unexpected exception:" + exception);
            throw exception;
        }
    }

    Pair<UUID, String> parseCallIdAndAttachmentName(Uri uri) {
        try {
            // We don't do explicit format checking here. Malformed URIs may generate
            // NullPointerExceptions or array bounds exceptions, which we'll catch and return null.
            // All of these will result in a FileNotFoundException being thrown in openFile.
            String callIdAndAttachmentName = uri.getPath().substring(1);
            String [] parts = callIdAndAttachmentName.split("/");

            String callIdString = parts[0];
            String attachmentName = parts[1];
            UUID callId = UUID.fromString(callIdString);

            return new Pair<UUID, String>(callId, attachmentName);
        } catch (Exception exception) {
            return null;
        }
    }
}
