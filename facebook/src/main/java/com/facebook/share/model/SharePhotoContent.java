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

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.facebook.share.internal.ShareConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes photo content to be shared.
 *
 * Use {@link SharePhotoContent.Builder} to create instances
 */
public final class SharePhotoContent
        extends ShareContent<SharePhotoContent, SharePhotoContent.Builder> {
    private final List<SharePhoto> photos;

    private SharePhotoContent(final Builder builder) {
        super(builder);
        this.photos = Collections.unmodifiableList(builder.photos);
    }

    SharePhotoContent(final Parcel in) {
        super(in);
        this.photos = Collections.unmodifiableList(SharePhoto.Builder.readListFrom(in));
    }

    /**
     * Photos to be shared.
     * @return {@link java.util.List} of {@link SharePhoto}s.
     */
    @Nullable
    public List<SharePhoto> getPhotos() {
        return this.photos;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        SharePhoto.Builder.writeListTo(out, this.photos);
    }

    @SuppressWarnings("unused")
    public static final Creator<SharePhotoContent> CREATOR = new Creator<SharePhotoContent>() {
        public SharePhotoContent createFromParcel(final Parcel in) {
            return new SharePhotoContent(in);
        }

        public SharePhotoContent[] newArray(final int size) {
            return new SharePhotoContent[size];
        }
    };

    /**
     * Builder for the {@link SharePhotoContent} interface.
     */
    public static class Builder extends ShareContent.Builder<SharePhotoContent, Builder> {
        private final List<SharePhoto> photos = new ArrayList<SharePhoto>();

        /**
         * Adds a photo to the content.
         * @param photo {@link com.facebook.share.model.SharePhoto} to add.
         * @return The builder.
         */
        public Builder addPhoto(@Nullable final SharePhoto photo) {
            if (photo != null) {
                this.photos.add(new SharePhoto.Builder().readFrom(photo).build());
            }
            return this;
        }

        /**
         * Adds multiple photos to the content.
         * @param photos {@link java.util.List} of {@link com.facebook.share.model.SharePhoto}s
         *                                      to add.
         * @return The builder.
         */
        public Builder addPhotos(@Nullable final List<SharePhoto> photos) {
            if (photos != null) {
                for (SharePhoto photo : photos) {
                    this.addPhoto(photo);
                }
            }
            return this;
        }

        @Override
        public SharePhotoContent build() {
            return new SharePhotoContent(this);
        }

        @Override
        public Builder readFrom(final SharePhotoContent model) {
            if (model == null) {
                return this;
            }
            return super.
                    readFrom(model)
                    .addPhotos(model.getPhotos());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (SharePhotoContent) parcel.readParcelable(
                            SharePhotoContent.class.getClassLoader()));
        }

        /**
         * Replaces the photos for the builder.
         * @param photos {@link java.util.List} of {@link com.facebook.share.model.SharePhoto}s to add.
         * @return The builder.
         */
        public Builder setPhotos(@Nullable final List<SharePhoto> photos) {
            this.photos.clear();
            this.addPhotos(photos);
            return this;
        }
    }
}
