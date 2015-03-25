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

/**
 * Describes the content that will be displayed by the AppInviteDialog
 */
public final class AppInviteContent implements ShareModel {
    private final String applinkUrl;
    private final String previewImageUrl;

    private AppInviteContent(final Builder builder) {
        this.applinkUrl = builder.applinkUrl;
        this.previewImageUrl = builder.previewImageUrl;
    }

    AppInviteContent(final Parcel in) {
        this.applinkUrl = in.readString();
        this.previewImageUrl = in.readString();
    }

    /**
     * Gets the applink url.
     */
    public String getApplinkUrl() {
        return applinkUrl;
    }

    /**
     * Gets the preview image url.
     */
    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.applinkUrl);
        out.writeString(this.previewImageUrl);
    }

    /**
     * Builder class for a concrete instance of AppInviteContent
     */
    public static class Builder
            implements ShareModelBuilder<AppInviteContent, Builder> {
        private String applinkUrl;
        private String previewImageUrl;

        /**
         * Sets the applink url that will be used for deep-linking
         *
         * @param applinkUrl the applink url
         * @return the builder
         */
        public Builder setApplinkUrl(final String applinkUrl) {
            this.applinkUrl = applinkUrl;
            return this;
        }

        /**
         * Sets the preview image url for this invite. See guidelines for correct dimensions.
         *
         * @param previewImageUrl url of the image that is going to be used as a preview for invite
         * @return the builder
         */
        public Builder setPreviewImageUrl(final String previewImageUrl) {
            this.previewImageUrl = previewImageUrl;
            return this;
        }

        @Override
        public AppInviteContent build() {
            return new AppInviteContent(this);
        }


        @Override
        public Builder readFrom(final AppInviteContent content) {
            if (content == null) {
                return this;
            }
            return this
                    .setApplinkUrl(content.getApplinkUrl())
                    .setPreviewImageUrl(content.getPreviewImageUrl());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom((AppInviteContent) parcel
                    .readParcelable(AppInviteContent.class.getClassLoader()));
        }
    }
}
