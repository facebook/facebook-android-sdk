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

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;

/**
 * Describes a video for sharing.
 *
 * Use {@link ShareVideo.Builder} to create instances
 */
public final class ShareVideo extends ShareMedia {
    private final Uri localUrl;

    private ShareVideo(final Builder builder) {
        super(builder);
        this.localUrl = builder.localUrl;
    }

    ShareVideo(final Parcel in) {
        super(in);
        this.localUrl = in.readParcelable(Uri.class.getClassLoader());
    }

    /**
     * This method supplies the URL to locate the video.
     * @return {@link android.net.Uri} that points to the location of the video on disk.
     */
    @Nullable
    public Uri getLocalUrl() {
        return this.localUrl;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelable(this.localUrl, 0);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareVideo> CREATOR = new Creator<ShareVideo>() {
        public ShareVideo createFromParcel(final Parcel in) {
            return new ShareVideo(in);
        }

        public ShareVideo[] newArray(final int size) {
            return new ShareVideo[size];
        }
    };

    /**
     * Builder for the {@link com.facebook.share.model.ShareVideo} class.
     */
    public static final class Builder extends ShareMedia.Builder<ShareVideo, Builder> {
        private Uri localUrl;

        /**
         * Sets the URL to locate the video.
         * @param localUrl {@link android.net.Uri} that points to the location of the video on disk.
         * @return The builder.
         */
        public Builder setLocalUrl(@Nullable final Uri localUrl) {
            this.localUrl = localUrl;
            return this;
        }

        @Override
        public ShareVideo build() {
            return new ShareVideo(this);
        }

        @Override
        public Builder readFrom(final ShareVideo model) {
            if (model == null) {
                return this;
            }
            return super.readFrom(model)
                    .setLocalUrl(model.getLocalUrl());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareVideo) parcel.readParcelable(ShareVideo.class.getClassLoader()));
        }
    }
}
