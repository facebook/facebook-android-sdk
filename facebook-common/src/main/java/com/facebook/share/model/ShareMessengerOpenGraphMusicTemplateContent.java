/*
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

/**
 * This share content allows sharing a bubble that plays songs with Open Graph music.
 * See https://developers.facebook.com/docs/messenger-platform/send-messages/template/open-graph
 * for details.
 *
 * @deprecated Sharing to Messenger via the SDK is unsupported. https://developers.facebook.com/docs/messenger-platform/changelog/#20190610.
 * Sharing should be performed by the native share sheet."
 */
@Deprecated
public final class ShareMessengerOpenGraphMusicTemplateContent
        extends ShareContent<
                    ShareMessengerOpenGraphMusicTemplateContent,
                    ShareMessengerOpenGraphMusicTemplateContent.Builder> {

    private final Uri url;
    private final ShareMessengerActionButton button;

    private ShareMessengerOpenGraphMusicTemplateContent(Builder builder) {
        super(builder);
        this.url = builder.url;
        this.button = builder.button;
    }

    ShareMessengerOpenGraphMusicTemplateContent(Parcel in) {
        super(in);
        this.url = in.readParcelable(Uri.class.getClassLoader());
        this.button = in.readParcelable(ShareMessengerActionButton.class.getClassLoader());
    }

    /**
     * Get the Open Graph music URL.
     */
    public Uri getUrl() {
        return url;
    }

    /**
     * Get the action button show in the share attachment.
     */
    public ShareMessengerActionButton getButton() {
        return button;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.url, flags);
        dest.writeParcelable(this.button, flags);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareMessengerOpenGraphMusicTemplateContent> CREATOR =
            new Creator<ShareMessengerOpenGraphMusicTemplateContent>() {
                public ShareMessengerOpenGraphMusicTemplateContent createFromParcel(
                        final Parcel in) {
                    return new ShareMessengerOpenGraphMusicTemplateContent(in);
                }

                public ShareMessengerOpenGraphMusicTemplateContent[] newArray(final int size) {
                    return new ShareMessengerOpenGraphMusicTemplateContent[size];
                }
            };

    /**
     * Builder for the {@link ShareMessengerOpenGraphMusicTemplateContent} interface.
     */
    public static class Builder
            extends ShareContent.Builder<ShareMessengerOpenGraphMusicTemplateContent, Builder> {

        private Uri url;
        private ShareMessengerActionButton button;

        /**
         * Set the Open Graph music URL. Required.
         */
        public Builder setUrl(Uri url) {
            this.url = url;
            return this;
        }

        /**
         * Set the action button shown in the share attachment.
         */
        public Builder setButton(ShareMessengerActionButton button) {
            this.button = button;
            return this;
        }

        @Override
        public Builder readFrom(final ShareMessengerOpenGraphMusicTemplateContent content) {
            if (content == null) {
                return this;
            }
            return super
                    .readFrom(content)
                    .setUrl(content.getUrl())
                    .setButton(content.getButton());
        }

        @Override
        public ShareMessengerOpenGraphMusicTemplateContent build() {
            return new ShareMessengerOpenGraphMusicTemplateContent(this);
        }
    }

}
