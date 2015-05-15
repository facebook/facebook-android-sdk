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

/**
 * Provides the interface for video content to be shared.
 *
 * A general use builder is available in
 * {@link ShareVideoContent.Builder}.
 */
public final class ShareVideoContent
        extends ShareContent<ShareVideoContent, ShareVideoContent.Builder>
        implements ShareModel {
    private final String contentDescription;
    private final String contentTitle;
    private final SharePhoto previewPhoto;
    private final ShareVideo video;

    private ShareVideoContent(final Builder builder) {
        super(builder);

        this.contentDescription = builder.contentDescription;
        this.contentTitle = builder.contentTitle;
        this.previewPhoto = builder.previewPhoto;
        this.video = builder.video;
    }

    ShareVideoContent(final Parcel in) {
        super(in);
        this.contentDescription = in.readString();
        this.contentTitle = in.readString();
        SharePhoto.Builder previewPhotoBuilder = new SharePhoto.Builder().readFrom(in);
        if (previewPhotoBuilder.getImageUrl() != null || previewPhotoBuilder.getBitmap() != null) {
            this.previewPhoto = previewPhotoBuilder.build();
        } else {
            this.previewPhoto = null;
        }
        this.video = new ShareVideo.Builder().readFrom(in).build();
    }

    /**
     * The description of the video.
     * @return The description of the video.
     */
    @Nullable
    public String getContentDescription() {
        return this.contentDescription;
    }

    /**
     * The title to display for this video.
     * @return The video title.
     */
    @Nullable
    public String getContentTitle() {
        return this.contentTitle;
    }

    /**
     * Photo to be used as a preview for the video.
     * @return Preview {@link SharePhoto} for the content.
     */
    @Nullable
    public SharePhoto getPreviewPhoto() {
        return this.previewPhoto;
    }

    /**
     * Video to be shared.
     * @return {@link ShareVideo}
     */
    @Nullable
    public ShareVideo getVideo() {
        return this.video;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeString(this.contentDescription);
        out.writeString(this.contentTitle);
        out.writeParcelable(this.previewPhoto, 0);
        out.writeParcelable(this.video, 0);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareVideoContent> CREATOR = new Creator<ShareVideoContent>() {
        public ShareVideoContent createFromParcel(final Parcel in) {
            return new ShareVideoContent(in);
        }

        public ShareVideoContent[] newArray(final int size) {
            return new ShareVideoContent[size];
        }
    };

    /**
     * Builder for the {@link com.facebook.share.model.ShareVideoContent} interface.
     */
    public static final class Builder extends ShareContent.Builder<ShareVideoContent, Builder> {
        private String contentDescription;
        private String contentTitle;
        private SharePhoto previewPhoto;
        private ShareVideo video;

        /**
         * Sets the description of the video.
         * @param contentDescription The description of the video.
         * @return The builder.
         */
        public Builder setContentDescription(
                @Nullable final String contentDescription) {
            this.contentDescription = contentDescription;
            return this;
        }

        /**
         * Sets the title to display for this video.
         * @param contentTitle The video title.
         * @return The builder.
         */
        public Builder setContentTitle(@Nullable final String contentTitle) {
            this.contentTitle = contentTitle;
            return this;
        }

        /**
         * Sets the photo to be used as a preview for the video.
         * @param previewPhoto Preview {@link com.facebook.share.model.SharePhoto} for the content.
         * @return The builder.
         */
        public Builder setPreviewPhoto(@Nullable final SharePhoto previewPhoto) {
            this.previewPhoto = (
                    previewPhoto == null ?
                    null :
                    new SharePhoto.Builder().readFrom(previewPhoto).build());
            return this;
        }

        /**
         * Sets the video to be shared.
         * @param video {@link com.facebook.share.model.ShareVideo}
         * @return The builder.
         */
        public Builder setVideo(@Nullable final ShareVideo video) {
            if (video == null) {
                return this;
            }

            this.video = new ShareVideo.Builder().readFrom(video).build();
            return this;
        }

        @Override
        public ShareVideoContent build() {
            return new ShareVideoContent(this);
        }

        @Override
        public Builder readFrom(final ShareVideoContent model) {
            if (model == null) {
                return this;
            }
            return super
                    .readFrom(model)
                    .setContentDescription(model.getContentDescription())
                    .setContentTitle(model.getContentTitle())
                    .setPreviewPhoto(model.getPreviewPhoto())
                    .setVideo(model.getVideo())
                    ;
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom((ShareVideoContent)parcel.readParcelable(
                    ShareVideoContent.class.getClassLoader()));
        }

    }
}
