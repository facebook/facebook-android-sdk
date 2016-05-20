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
 * Describes a hashtag for sharing.
 *
 * Use {@link ShareHashtag.Builder} to build instances
 */
public class ShareHashtag implements ShareModel {

    private final String hashtag;

    private ShareHashtag(final Builder builder) {
        this.hashtag = builder.hashtag;
    }

    ShareHashtag(final Parcel in) {
        this.hashtag = in.readString();
    }

    /**
     * @return Gets the value of the hashtag for this instance
     */
    public String getHashtag() {
        return hashtag;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hashtag);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareHashtag> CREATOR = new Creator<ShareHashtag>() {
        public ShareHashtag createFromParcel(final Parcel in) {
            return new ShareHashtag(in);
        }

        public ShareHashtag[] newArray(final int size) {
            return new ShareHashtag[size];
        }
    };

    /**
     * Builder for the {@link com.facebook.share.model.ShareHashtag} class.
     */
    public static class Builder implements ShareModelBuilder<ShareHashtag, Builder> {

        private String hashtag;

        /**
         * Sets the hashtag value for this instance.
         * @param hashtag
         * @return the Builder instance
         */
        public Builder setHashtag(final String hashtag) {
            this.hashtag = hashtag;
            return this;
        }

        /**
         * @return Gets the value of the hashtag for this instance
         */
        public String getHashtag() {
            return hashtag;
        }

        @Override
        public Builder readFrom(final ShareHashtag model) {
            if (model == null) {
                return this;
            }

            return this.setHashtag(model.getHashtag());
        }

        Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareHashtag)parcel.readParcelable(ShareHashtag.class.getClassLoader()));
        }

        @Override
        public ShareHashtag build() {
            return new ShareHashtag(this);
        }
    }
}
