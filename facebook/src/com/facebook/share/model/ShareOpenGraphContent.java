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
 * Describes Open Graph content that is to be shared
 * <p/>
 * Use {@link ShareOpenGraphContent.Builder} to create instances
 * <p/>
 * See the documentation for <a href="https://developers.facebook.com/docs/opengraph">Open Graph</a>
 * and for <a href="https://developers.facebook.com/docs/sharing/best-practices">best practices</a>.
 */
public final class ShareOpenGraphContent
        extends ShareContent<ShareOpenGraphContent, ShareOpenGraphContent.Builder> {
    private final ShareOpenGraphAction action;
    private final String previewPropertyName;

    private ShareOpenGraphContent(final Builder builder) {
        super(builder);
        this.action = builder.action;
        this.previewPropertyName = builder.previewPropertyName;
    }

    ShareOpenGraphContent(final Parcel in) {
        super(in);
        this.action = new ShareOpenGraphAction.Builder().readFrom(in).build();
        this.previewPropertyName = in.readString();
    }

    /**
     * The Open Graph Action for the content.
     *
     * @return {@link ShareOpenGraphAction}
     */
    @Nullable
    public ShareOpenGraphAction getAction() {
        return this.action;
    }

    /**
     * The property name for the primary {@link com.facebook.share.model.ShareOpenGraphObject}
     * in the action.
     *
     * @return The property name for the preview object.
     */
    @Nullable
    public String getPreviewPropertyName() {
        return this.previewPropertyName;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelable(this.action, 0);
        out.writeString(this.previewPropertyName);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareOpenGraphContent> CREATOR =
            new Creator<ShareOpenGraphContent>() {
                public ShareOpenGraphContent createFromParcel(final Parcel in) {
                    return new ShareOpenGraphContent(in);
                }

                public ShareOpenGraphContent[] newArray(final int size) {
                    return new ShareOpenGraphContent[size];
                }
            };

    /**
     * Builder for the {@link com.facebook.share.model.ShareOpenGraphContent} interface.
     */
    public static final class Builder
            extends ShareContent.Builder<com.facebook.share.model.ShareOpenGraphContent, Builder> {
        private ShareOpenGraphAction action;
        private String previewPropertyName;

        /**
         * Sets the Open Graph Action for the content.
         *
         * @param action {@link com.facebook.share.model.ShareOpenGraphAction}
         * @return The builder.
         */
        public Builder setAction(@Nullable final ShareOpenGraphAction action) {
            this.action =
                    (action == null
                            ? null
                            : new ShareOpenGraphAction.Builder()
                            .readFrom(action).build());
            return this;
        }

        /**
         * Sets the property name for the primary
         * {@link com.facebook.share.model.ShareOpenGraphObject} in the action.
         *
         * @param previewPropertyName The property name for the preview object.
         * @return The builder.
         */
        public Builder setPreviewPropertyName(
                @Nullable final String previewPropertyName) {
            this.previewPropertyName = previewPropertyName;
            return this;
        }

        @Override
        public com.facebook.share.model.ShareOpenGraphContent build() {
            return new ShareOpenGraphContent(this);
        }

        @Override
        public Builder readFrom(final com.facebook.share.model.ShareOpenGraphContent model) {
            if (model == null) {
                return this;
            }
            return super
                    .readFrom(model)
                    .setAction(model.getAction())
                    .setPreviewPropertyName(model.getPreviewPropertyName())
                    ;
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (com.facebook.share.model.ShareOpenGraphContent) parcel.readParcelable(
                            ShareOpenGraphContent.class.getClassLoader()));
        }

    }
}
