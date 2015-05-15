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

package com.facebook.share.model;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import junit.framework.Assert;

import java.util.*;

public class ShareModelTestUtility {
    public static final String OPEN_GRAPH_ACTION_TYPE = "myActionType";
    public static final boolean OPEN_GRAPH_BOOLEAN_VALUE = true;
    public static final String OPEN_GRAPH_BOOLEAN_VALUE_KEY = "OPEN_GRAPH_BOOLEAN_VALUE";
    public static final boolean[] OPEN_GRAPH_BOOLEAN_ARRAY = { true, false };
    public static final String OPEN_GRAPH_BOOLEAN_ARRAY_KEY = "OPEN_GRAPH_BOOLEAN_ARRAY";
    public static final ShareOpenGraphAction OPEN_GRAPH_CONTENT_ACTION =
            getOpenGraphActionBuilder().build();
    public static final String OPEN_GRAPH_CONTENT_PREVIEW_PROPERTY_NAME = "myActionProperty";
    public static final String OPEN_GRAPH_CONTENT_PREVIEW_PROPERTY_VALUE = "myActionPropertyValue";
    public static final double OPEN_GRAPH_DOUBLE_VALUE = Double.MAX_VALUE;
    public static final String OPEN_GRAPH_DOUBLE_VALUE_KEY = "OPEN_GRAPH_DOUBLE_VALUE";
    public static final double[] OPEN_GRAPH_DOUBLE_ARRAY =
            { Double.MIN_VALUE, -7, 0, 42, Double.MAX_VALUE };
    public static final String OPEN_GRAPH_DOUBLE_ARRAY_KEY = "OPEN_GRAPH_DOUBLE_ARRAY";
    public static final int OPEN_GRAPH_INT_VALUE = 42;
    public static final String OPEN_GRAPH_INT_VALUE_KEY = "OPEN_GRAPH_INT_VALUE";
    public static final int[] OPEN_GRAPH_INT_ARRAY =
            { Integer.MIN_VALUE, -7, 0, 42, Integer.MAX_VALUE };
    public static final String OPEN_GRAPH_INT_ARRAY_KEY = "OPEN_GRAPH_INT_ARRAY";
    public static final long OPEN_GRAPH_LONG_VALUE = Long.MAX_VALUE;
    public static final String OPEN_GRAPH_LONG_VALUE_KEY = "OPEN_GRAPH_LONG_VALUE";
    public static final long[] OPEN_GRAPH_LONG_ARRAY =
            { Long.MIN_VALUE, -7, 0, 42, Long.MAX_VALUE };
    public static final String OPEN_GRAPH_LONG_ARRAY_KEY = "OPEN_GRAPH_LONG_ARRAY";
    public static final String OPEN_GRAPH_STRING = "this is a string";
    public static final String OPEN_GRAPH_STRING_KEY = "OPEN_GRAPH_STRING";
    public static final ArrayList<String> OPEN_GRAPH_STRING_ARRAY_LIST = new ArrayList<String>() {{
        add("string1");
        add("string2");
        add("string3");
    }};
    public static final String OPEN_GRAPH_STRING_ARRAY_LIST_KEY = "OPEN_GRAPH_STRING_ARRAY_LIST";
    public static final String OPEN_GRAPH_UNUSED_KEY = "unused";
    public static final Bitmap PHOTO_BITMAP = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
    public static final ArrayList<SharePhoto> PHOTO_CONTENT_PHOTOS = new ArrayList<SharePhoto>() {{
        add(getPhotoBuilder(PhotoBuilderType.LOCAL_URL).build());
    }};
    public static final boolean PHOTO_USER_GENERATED = true;
    public static final String STATUS_CONTENT_DESCRIPTION = "this is a description";
    public static final Uri STATUS_CONTENT_IMAGE_URL = Uri.parse(
            "http://www.facebook.com/image/url");
    public static final String STATUS_CONTENT_TITLE = "this is a title";
    public static final SharePhoto VIDEO_CONTENT_PREVIEW_PHOTO = getPhotoBuilder(
            PhotoBuilderType.LOCAL_URL).build();
    public static final ShareVideo VIDEO_CONTENT_VIDEO = getVideoBuilder().build();

    public enum PhotoBuilderType {
        BITMAP,
        LOCAL_URL,
    }

    public static Uri getTestPhotoImageUrl() {
        return Uri.parse("file://" +
                        Environment.getExternalStorageDirectory().getPath() +
                        "crazy-cat-photo.jpg");
    }

    public static Uri getTestVideoLocalUrl() {
        return Uri.parse("file://" +
                Environment.getExternalStorageDirectory().getPath() +
                "crazy-cat-video.mp4");
    }

    public static ShareOpenGraphAction.Builder getOpenGraphActionBuilder() {
        return prepareOpenGraphValueContainerBuilder(
                new ShareOpenGraphAction.Builder())
                .setActionType(OPEN_GRAPH_ACTION_TYPE)
                .putString(
                        OPEN_GRAPH_CONTENT_PREVIEW_PROPERTY_NAME,
                        OPEN_GRAPH_CONTENT_PREVIEW_PROPERTY_VALUE);
    }

    public static ShareOpenGraphContent.Builder getOpenGraphContentBuilder() {
        return new ShareOpenGraphContent.Builder()
                .setAction(OPEN_GRAPH_CONTENT_ACTION)
                .setPreviewPropertyName(OPEN_GRAPH_CONTENT_PREVIEW_PROPERTY_NAME)
                ;
    }

    public static ShareOpenGraphObject.Builder getOpenGraphObjectBuilder() {
        return prepareOpenGraphValueContainerBuilder(new ShareOpenGraphObject.Builder());
    }

    public static SharePhoto.Builder getPhotoBuilder(final PhotoBuilderType type) {
        final SharePhoto.Builder builder = new SharePhoto.Builder().setUserGenerated(
                PHOTO_USER_GENERATED);
        switch (type) {
            case BITMAP:
                builder.setBitmap(PHOTO_BITMAP);
                break;
            case LOCAL_URL:
                builder.setImageUrl(getTestPhotoImageUrl());
                break;
        }
        return builder;
    }

    public static SharePhotoContent.Builder getPhotoContentBuilder() {
        return new SharePhotoContent.Builder().addPhotos(PHOTO_CONTENT_PHOTOS);
    }

    public static ShareLinkContent.Builder getLinkContentBuilder() {
        return new ShareLinkContent.Builder()
                .setContentDescription(STATUS_CONTENT_DESCRIPTION)
                .setImageUrl(STATUS_CONTENT_IMAGE_URL)
                .setContentTitle(STATUS_CONTENT_TITLE)
                ;
    }

    public static ShareVideo.Builder getVideoBuilder() {
        return new ShareVideo.Builder().setLocalUrl(getTestVideoLocalUrl());
    }

    public static ShareVideoContent.Builder getVideoContentBuilder() {
        return new ShareVideoContent.Builder()
                .setPreviewPhoto(VIDEO_CONTENT_PREVIEW_PHOTO)
                .setVideo(VIDEO_CONTENT_VIDEO)
                ;
    }

    private static <E extends ShareOpenGraphValueContainer.Builder> E
    prepareOpenGraphValueContainerBuilder(
            final E builder
    ) {
        return (E)builder
                .putBoolean(OPEN_GRAPH_BOOLEAN_VALUE_KEY, OPEN_GRAPH_BOOLEAN_VALUE)
                .putBooleanArray(OPEN_GRAPH_BOOLEAN_ARRAY_KEY, OPEN_GRAPH_BOOLEAN_ARRAY)
                .putDouble(OPEN_GRAPH_DOUBLE_VALUE_KEY, OPEN_GRAPH_DOUBLE_VALUE)
                .putDoubleArray(OPEN_GRAPH_DOUBLE_ARRAY_KEY, OPEN_GRAPH_DOUBLE_ARRAY)
                .putInt(OPEN_GRAPH_INT_VALUE_KEY, OPEN_GRAPH_INT_VALUE)
                .putIntArray(OPEN_GRAPH_INT_ARRAY_KEY, OPEN_GRAPH_INT_ARRAY)
                .putLong(OPEN_GRAPH_LONG_VALUE_KEY, OPEN_GRAPH_LONG_VALUE)
                .putLongArray(OPEN_GRAPH_LONG_ARRAY_KEY, OPEN_GRAPH_LONG_ARRAY)
                .putString(OPEN_GRAPH_STRING_KEY, OPEN_GRAPH_STRING)
                .putStringArrayList(OPEN_GRAPH_STRING_ARRAY_LIST_KEY, OPEN_GRAPH_STRING_ARRAY_LIST)
                ;
    }

    public static <E> void assertEquals(final E a, final E b) {
        if (a == null) {
            // if a is null, b should be null
            Assert.assertNull(b);
        } else if ((a instanceof boolean[]) && (b instanceof boolean[])) {
            // check for arrays of all of the primitive types, then arrays of Object, and route
            // those through Arrays equals
            Assert.assertTrue(Arrays.equals((boolean[]) a, (boolean[]) b));
        } else if ((a instanceof byte[]) && (b instanceof byte[])) {
            Assert.assertTrue(Arrays.equals((byte[])a, (byte[])b));
        } else if ((a instanceof char[]) && (b instanceof char[])) {
            Assert.assertTrue(Arrays.equals((char[])a, (char[])b));
        } else if ((a instanceof double[]) && (b instanceof double[])) {
            Assert.assertTrue(Arrays.equals((double[])a, (double[])b));
        } else if ((a instanceof float[]) && (b instanceof float[])) {
            Assert.assertTrue(Arrays.equals((float[])a, (float[])b));
        } else if ((a instanceof int[]) && (b instanceof int[])) {
            Assert.assertTrue(Arrays.equals((int[])a, (int[])b));
        } else if ((a instanceof long[]) && (b instanceof long[])) {
            Assert.assertTrue(Arrays.equals((long[])a, (long[])b));
        } else if ((a instanceof short[]) && (b instanceof short[])) {
            Assert.assertTrue(Arrays.equals((short[])a, (short[])b));
        } else if ((a instanceof Object[]) && (b instanceof Object[])) {
            Assert.assertTrue(Arrays.deepEquals((Object[])a, (Object[])b));
        } else if ((a instanceof List) && (b instanceof List)) {
            // check for Lists
            assertEquals((List)a, (List)b);
        } else if ((a instanceof ShareModel) && (b instanceof ShareModel)) {
            // check for ShareModels
            assertEquals((ShareModel)a, (ShareModel)b);
        } else {
            // now use Object.equals
            Assert.assertTrue(a.equals(b));
        }
    }

    public static void assertEquals(final List a, final List b) {
        final int size = a.size();
        Assert.assertEquals(size, b.size());
        for (int i = 0; i < size; ++i) {
            assertEquals(a.get(i), b.get(i));
        }
    }

    public static <E extends ShareModel> void assertEquals(final E a, final E b) {
        if ((a instanceof ShareOpenGraphAction) && (b instanceof ShareOpenGraphAction)) {
            assertEquals((ShareOpenGraphAction)a, (ShareOpenGraphAction)b);
        } else if ((a instanceof ShareOpenGraphObject) && (b instanceof ShareOpenGraphObject)) {
            assertEquals((ShareOpenGraphObject) a, (ShareOpenGraphObject) b);
        } else if ((a instanceof ShareOpenGraphContent) && (b instanceof ShareOpenGraphContent)) {
            assertEquals((ShareOpenGraphContent)a, (ShareOpenGraphContent)b);
        } else if ((a instanceof SharePhoto) && (b instanceof SharePhoto)) {
            assertEquals((SharePhoto)a, (SharePhoto)b);
        } else if ((a instanceof SharePhotoContent) && (b instanceof SharePhotoContent)) {
            assertEquals((SharePhotoContent)a, (SharePhotoContent)b);
        } else if ((a instanceof ShareLinkContent) && (b instanceof ShareLinkContent)) {
            assertEquals((ShareLinkContent)a, (ShareLinkContent)b);
        } else if ((a instanceof ShareVideo) && (b instanceof ShareVideo)) {
            assertEquals((ShareVideo)a, (ShareVideo)b);
        } else if ((a instanceof ShareVideoContent) && (b instanceof ShareVideoContent)) {
            assertEquals((ShareVideoContent)a, (ShareVideoContent)b);
        } else {
            Assert.fail(a.getClass().toString() + " models do not have an equality test");
        }
    }

    public static void assertEquals(final ShareOpenGraphAction a, final ShareOpenGraphAction b) {
        Assert.assertEquals(a.getActionType(), b.getActionType());
        assertOpenGraphValueContainerEquals(a, b);
    }

    public static void assertEquals(final ShareOpenGraphContent a, final ShareOpenGraphContent b) {
        assertEquals(a.getAction(), b.getAction());
        Assert.assertEquals(a.getPreviewPropertyName(), b.getPreviewPropertyName());
        assertContentEquals(a, b);
    }

    public static void assertEquals(final ShareOpenGraphObject a, final ShareOpenGraphObject b) {
        assertOpenGraphValueContainerEquals(a, b);
    }

    public static void assertEquals(final SharePhoto a, final SharePhoto b) {
        Assert.assertEquals(a.getBitmap(), b.getBitmap());
        Assert.assertEquals(a.getImageUrl(), b.getImageUrl());
        Assert.assertEquals(a.getUserGenerated(), b.getUserGenerated());
    }

    public static void assertEquals(final SharePhotoContent a, final SharePhotoContent b) {
        assertEquals(a.getPhotos(), b.getPhotos());
        assertContentEquals(a, b);
    }

    public static void assertEquals(final ShareLinkContent a, final ShareLinkContent b) {
        Assert.assertEquals(a.getContentDescription(), b.getContentDescription());
        Assert.assertEquals(a.getImageUrl(), b.getImageUrl());
        Assert.assertEquals(a.getContentTitle(), b.getContentTitle());
        assertContentEquals(a, b);
    }

    public static void assertEquals(final ShareVideo a, final ShareVideo b) {
        Assert.assertEquals(a.getLocalUrl(), b.getLocalUrl());
    }

    public static void assertEquals(final ShareVideoContent a, final ShareVideoContent b) {
        assertEquals(a.getPreviewPhoto(), b.getPreviewPhoto());
        assertEquals(a.getVideo(), b.getVideo());
        assertContentEquals(a, b);
    }

    private static void assertContentEquals(final ShareContent a, final ShareContent b) {
        Assert.assertEquals(a.getContentUrl(), b.getContentUrl());
        Assert.assertEquals(a.getPeopleIds(), b.getPeopleIds());
        Assert.assertEquals(a.getPlaceId(), b.getPlaceId());
        Assert.assertEquals(a.getRef(), b.getRef());
    }

    private static void assertOpenGraphValueContainerEquals(
            final ShareOpenGraphValueContainer a,
            final ShareOpenGraphValueContainer b
    ) {
        final HashSet<String> keySet = new HashSet<String>();
        keySet.addAll(a.keySet());
        keySet.addAll(b.keySet());
        for (String key : keySet) {
            assertEquals(a.get(key), b.get(key));
        }
    }

    private ShareModelTestUtility() {}
}
