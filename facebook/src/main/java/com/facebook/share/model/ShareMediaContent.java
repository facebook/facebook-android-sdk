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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ShareMediaContent
        extends ShareContent<ShareMediaContent, ShareMediaContent.Builder> {
    private final List<ShareMedia> media;

    private ShareMediaContent(final Builder builder) {
        super(builder);
        this.media = Collections.unmodifiableList(builder.media);
    }

    ShareMediaContent(final Parcel in) {
        super(in);
        ShareMedia[] shareMedia = (ShareMedia[])in.readParcelableArray(
                ShareMedia.class.getClassLoader());
        this.media = Arrays.asList(shareMedia);
    }

    /**
     * Media to be shared.
     *
     * @return {@link java.util.List} of {@link ShareMedia}s.
     */
    @Nullable
    public List<ShareMedia> getMedia() {
        return this.media;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelableArray((ShareMedia[])this.media.toArray(), flags);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareMediaContent> CREATOR = new Creator<ShareMediaContent>() {
        public ShareMediaContent createFromParcel(final Parcel in) {
            return new ShareMediaContent(in);
        }

        public ShareMediaContent[] newArray(final int size) {
            return new ShareMediaContent[size];
        }
    };

    /**
     * Builder for the {@link SharePhotoContent} interface.
     */
    public static class Builder extends ShareContent.Builder<ShareMediaContent, Builder> {
        private final List<ShareMedia> media = new ArrayList<>();

        /**
         * Adds a medium to the content.
         *
         * @param medium {@link com.facebook.share.model.ShareMedia} to add.
         * @return The builder.
         */
        public Builder addMedium(@Nullable final ShareMedia medium) {
            if (medium != null) {
                ShareMedia mediumToAdd;
                if (medium instanceof SharePhoto) {
                    mediumToAdd = new SharePhoto.Builder().readFrom((SharePhoto) medium).build();
                } else if (medium instanceof ShareVideo) {
                    mediumToAdd = new ShareVideo.Builder().readFrom((ShareVideo) medium).build();
                } else {
                    throw new IllegalArgumentException(
                            "medium must be either a SharePhoto or ShareVideo");
                }
                this.media.add(mediumToAdd);
            }
            return this;
        }

        /**
         * Adds multiple media to the content.
         *
         * @param media {@link java.util.List} of {@link com.facebook.share.model.ShareMedia}
         *               to add.
         * @return The builder.
         */
        public Builder addMedia(@Nullable final List<ShareMedia> media) {
            if (media != null) {
                for (ShareMedia medium : media) {
                    this.addMedium(medium);
                }
            }
            return this;
        }

        @Override
        public ShareMediaContent build() {
            return new ShareMediaContent(this);
        }

        @Override
        public Builder readFrom(final ShareMediaContent model) {
            if (model == null) {
                return this;
            }
            return super.
                    readFrom(model)
                    .addMedia(model.getMedia());
        }

        /**
         * Replaces the media for the builder.
         *
         * @param media {@link java.util.List} of {@link com.facebook.share.model.ShareMedia}
         *   to add.
         * @return The builder.
         */
        public Builder setMedia(@Nullable final List<ShareMedia> media) {
            this.media.clear();
            this.addMedia(media);
            return this;
        }
    }
}
