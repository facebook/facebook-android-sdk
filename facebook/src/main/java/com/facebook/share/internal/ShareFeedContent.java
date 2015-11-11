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
import android.os.Parcelable;

import com.facebook.share.model.ShareContent;

import java.util.HashMap;
import java.util.Map;

// This class is used specifically for backwards support in unity for various feed parameters
// Currently this content is only supported if you set the mode to Feed when sharing.
public class ShareFeedContent
        extends ShareContent<ShareFeedContent, ShareFeedContent.Builder> {
    private final String toId;
    private final String link;
    private final String linkName;
    private final String linkCaption;
    private final String linkDescription;
    private final String picture;
    private final String mediaSource;

    private ShareFeedContent(final Builder builder) {
        super(builder);
        this.toId = builder.toId;
        this.link = builder.link;
        this.linkName = builder.linkName;
        this.linkCaption = builder.linkCaption;
        this.linkDescription = builder.linkDescription;
        this.picture = builder.picture;
        this.mediaSource = builder.mediaSource;
    }

    ShareFeedContent(final Parcel in) {
        super(in);
        this.toId = in.readString();
        this.link = in.readString();
        this.linkName = in.readString();
        this.linkCaption = in.readString();
        this.linkDescription = in.readString();
        this.picture = in.readString();
        this.mediaSource = in.readString();
    }

    public String getToId() {
        return toId;
    }

    public String getLink() {
        return link;
    }

    public String getLinkName() {
        return linkName;
    }

    public String getLinkCaption() {
        return linkCaption;
    }

    public String getLinkDescription() {
        return linkDescription;
    }

    public String getPicture() {
        return picture;
    }

    public String getMediaSource() {
        return mediaSource;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeString(this.toId);
        out.writeString(this.link);
        out.writeString(this.linkName);
        out.writeString(this.linkCaption);
        out.writeString(this.linkDescription);
        out.writeString(this.picture);
        out.writeString(this.mediaSource);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ShareFeedContent> CREATOR =
            new Parcelable.Creator<ShareFeedContent>() {
                public ShareFeedContent createFromParcel(final Parcel in) {
                    return new ShareFeedContent(in);
                }

                public ShareFeedContent[] newArray(final int size) {
                    return new ShareFeedContent[size];
                }
            };

    /**
     * Builder for the {@link ShareFeedContent} interface.
     */
    public static final class Builder
            extends ShareContent.Builder<ShareFeedContent, Builder> {
        private String toId;
        private String link;
        private String linkName;
        private String linkCaption;
        private String linkDescription;
        private String picture;
        private String mediaSource;

        public ShareFeedContent.Builder setToId(String toId) {
            this.toId = toId;
            return this;
        }

        public ShareFeedContent.Builder setLink(String link) {
            this.link = link;
            return this;
        }

        public ShareFeedContent.Builder setLinkName(String linkName) {
            this.linkName = linkName;
            return this;
        }

        public ShareFeedContent.Builder setLinkCaption(String linkCaption) {
            this.linkCaption = linkCaption;
            return this;
        }

        public ShareFeedContent.Builder setLinkDescription(String linkDescription) {
            this.linkDescription = linkDescription;
            return this;
        }

        public ShareFeedContent.Builder setPicture(String picture) {
            this.picture = picture;
            return this;
        }

        public ShareFeedContent.Builder setMediaSource(String mediaSource) {
            this.mediaSource = mediaSource;
            return this;
        }

        @Override
        public ShareFeedContent build() {
            return new ShareFeedContent(this);
        }

        @Override
        public Builder readFrom(final ShareFeedContent model) {
            if (model == null) {
                return this;
            }
            return super
                    .readFrom(model)
                    .setToId(model.getToId())
                    .setLink(model.getLink())
                    .setLinkName(model.getLinkName())
                    .setLinkCaption(model.getLinkCaption())
                    .setLinkDescription(model.getLinkDescription())
                    .setPicture(model.getPicture())
                    .setMediaSource(model.getMediaSource())
                    ;
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareFeedContent) parcel.readParcelable(
                            ShareFeedContent.class.getClassLoader()));
        }

    }
}
