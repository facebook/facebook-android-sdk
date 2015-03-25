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

package com.facebook.widget;

import com.facebook.FacebookTestCase;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.widget.ShareDialog;

public class FacebookDialogTests extends FacebookTestCase {

    private String getAttachmentNameFromContentUri(String contentUri) {
        int lastSlash = contentUri.lastIndexOf("/");
        return contentUri.substring(lastSlash + 1);
    }

    //
    // TODO(v4) - Fix and uncomment these
    //
//    public void testCantSetAttachmentsWithNullBitmaps() throws JSONException {
//        try {
//            ArrayList<SharePhoto> photos = new ArrayList<>();
//            photos.add(new SharePhotoBuilder().setBitmap(null).build());
//
//            ShareOpenGraphContent ogContent = new ShareOpenGraphContentBuilder()
//                    .setAction(
//                            new ShareOpenGraphActionBuilder()
//                                    .setActionType("foo")
//                                    .putString("foo", "bar")
//                                    .putPhotoArrayList("image", photos)
//                                    .build())
//                    .setPreviewPropertyName("foo")
//                    .build();
//
//            ShareDialog.share(getActivity(), ogContent);
//
//            fail("expected exception");
//        } catch (NullPointerException exception) {
//        }
//    }
//
//    public void testCantSetObjectAttachmentsWithNullBitmaps() throws JSONException {
//        try {
//            ArrayList<SharePhoto> photos = new ArrayList<>();
//            photos.add(new SharePhotoBuilder().setBitmap(null).build());
//
//            ShareOpenGraphObject ogObject = new ShareOpenGraphObjectBuilder()
//                    .putString("type", "bar")
//                    .putPhotoArrayList("image", photos)
//                    .build();
//
//            ShareOpenGraphContent ogContent = new ShareOpenGraphContentBuilder()
//                    .setAction(
//                            new ShareOpenGraphActionBuilder()
//                                    .setActionType("foo")
//                                    .putObject("foo", ogObject)
//                                    .build())
//                    .setPreviewPropertyName("foo")
//                    .build();
//
//            ShareDialog.share(getActivity(), ogContent);
//
//            fail("expected exception");
//        } catch (NullPointerException exception) {
//        }
//    }

    // TODO(v4) - Add tests for ShareContentValidation.
}
