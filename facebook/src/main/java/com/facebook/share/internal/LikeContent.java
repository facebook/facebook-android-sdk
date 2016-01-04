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

package com.facebook.share.internal;

import android.os.Parcel;

import com.facebook.share.model.ShareModel;
import com.facebook.share.model.ShareModelBuilder;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 *
 * Represents content that is set on a LikeView to allow users to like and unlike it.
 */
public class LikeContent implements ShareModel {

    private final String objectId;
    private final String objectType;

    private LikeContent(final Builder builder) {
        this.objectId = builder.objectId;
        this.objectType = builder.objectType;
    }

    LikeContent(final Parcel in) {
        this.objectId = in.readString();
        this.objectType = in.readString();
    }

    /**
     * Gets the object Id for the LikeView.
     *
     * @return the object Id
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Gets the type of the object for the LikeView.
     *
     * @return the type of the object
     */
    public String getObjectType() {
        return objectType;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.objectId);
        out.writeString(this.objectType);
    }

    @SuppressWarnings("unused")
    public static final Creator<LikeContent> CREATOR =
            new Creator<LikeContent>() {
                public LikeContent createFromParcel(final Parcel in) {
                    return new LikeContent(in);
                }

                public LikeContent[] newArray(final int size) {
                    return new LikeContent[size];
                }
            };

    /**
     * Builder class for a concrete instance of AppInviteContent
     */
    public static class Builder
            implements ShareModelBuilder<LikeContent, Builder> {
        private String objectId;
        private String objectType;

        /**
         * Sets the object Id for the LikeView
         * @param objectId the object Id
         */
        public Builder setObjectId(final String objectId) {
            this.objectId = objectId;
            return this;
        }

        /**
         * Sets the type of the object for the LikeView
         * @param objectType the type of the object
         */
        public Builder setObjectType(final String objectType) {
            this.objectType = objectType;
            return this;
        }

        @Override
        public LikeContent build() {
            return new LikeContent(this);
        }

        @Override
        public Builder readFrom(final LikeContent content) {
            if (content == null) {
                return this;
            }
            return this
                    .setObjectId(content.getObjectId())
                    .setObjectType(content.getObjectType());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom((LikeContent) parcel
                    .readParcelable(LikeContent.class.getClassLoader()));
        }
    }
}
