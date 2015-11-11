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
 * Describes link content to be shared.
 *
 * Use {@link ShareLinkContent.Builder} to build instances.
 *
 * See documentation for <a href="https://developers.facebook.com/docs/sharing/best-practices">best practices</a>.
 */
public final class ShareLinkContent
        extends ShareContent<ShareLinkContent, ShareLinkContent.Builder> {
    private final String contentDescription;
    private final String contentTitle;
    private final Uri imageUrl;

    private ShareLinkContent(final Builder builder) {
        super(builder);
        this.contentDescription = builder.contentDescription;
        this.contentTitle = builder.contentTitle;
        this.imageUrl = builder.imageUrl;
    }

    ShareLinkContent(final Parcel in) {
        super(in);
        this.contentDescription = in.readString();
        this.contentTitle = in.readString();
        this.imageUrl = in.readParcelable(Uri.class.getClassLoader());
    }

    /**
     * The description of the link.  If not specified, this field is automatically populated by
     * information scraped from the link, typically the title of the page.
     * @return The description of the link.
     */
    public String getContentDescription() {
        return this.contentDescription;
    }

    /**
     * The title to display for this link.
     * @return The link title.
     */
    @Nullable
    public String getContentTitle() {
        return this.contentTitle;
    }

    /**
     * The URL of a picture to attach to this content.
     * @return The network URL of an image.
     */
    @Nullable
    public Uri getImageUrl() {
        return this.imageUrl;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeString(this.contentDescription);
        out.writeString(this.contentTitle);
        out.writeParcelable(this.imageUrl, 0);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareLinkContent> CREATOR =
            new Creator<ShareLinkContent>() {
        public ShareLinkContent createFromParcel(final Parcel in) {
            return new ShareLinkContent(in);
        }

        public ShareLinkContent[] newArray(final int size) {
            return new ShareLinkContent[size];
        }
    };

    /**
     * Builder for the {@link ShareLinkContent} interface.
     */
    public static final class Builder
            extends ShareContent.Builder<ShareLinkContent, Builder> {
        private String contentDescription;
        private String contentTitle;
        private Uri imageUrl;

        /**
         * Set the contentDescription of the link.
         * @param contentDescription The contentDescription of the link.
         * @return The builder.
         */
        public Builder setContentDescription(
                @Nullable final String contentDescription) {
            this.contentDescription = contentDescription;
            return this;
        }

        /**
         * Set the contentTitle to display for this link.
         * @param contentTitle The link contentTitle.
         * @return The builder.
         */
        public Builder setContentTitle(@Nullable final String contentTitle) {
            this.contentTitle = contentTitle;
            return this;
        }

        /**
         * Set the URL of a picture to attach to this content.
         * @param imageUrl The network URL of an image.
         * @return The builder.
         */
        public Builder setImageUrl(@Nullable final Uri imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        @Override
        public ShareLinkContent build() {
            return new ShareLinkContent(this);
        }

        @Override
        public Builder readFrom(final ShareLinkContent model) {
            if (model == null) {
                return this;
            }
            return super
                    .readFrom(model)
                    .setContentDescription(model.getContentDescription())
                    .setImageUrl(model.getImageUrl())
                    .setContentTitle(model.getContentTitle())
                    ;
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareLinkContent) parcel.readParcelable(
                            ShareLinkContent.class.getClassLoader()));
        }

    }
}
