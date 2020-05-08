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

import android.os.Parcel;
import android.text.TextUtils;

/**
 * @deprecated
 * AppInvites is deprecated
 */
@Deprecated
public final class AppInviteContent implements ShareModel {
    private final String applinkUrl;
    private final String previewImageUrl;
    private final String promoCode;
    private final String promoText;
    private final Builder.Destination destination;

    private AppInviteContent(final Builder builder) {
        this.applinkUrl = builder.applinkUrl;
        this.previewImageUrl = builder.previewImageUrl;
        this.promoCode = builder.promoCode;
        this.promoText = builder.promoText;
        this.destination = builder.destination;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    AppInviteContent(final Parcel in) {
        this.applinkUrl = in.readString();
        this.previewImageUrl = in.readString();
        this.promoText = in.readString();
        this.promoCode = in.readString();

        String destinationString = in.readString();
        if (destinationString.length() > 0) {
            this.destination = Builder.Destination.valueOf(destinationString);
        }
        else {
            this.destination = Builder.Destination.FACEBOOK;
        }
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public String getApplinkUrl() {
        return applinkUrl;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public String getPromotionCode() {
        return promoCode;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public String getPromotionText() {
        return promoText;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public Builder.Destination getDestination() {
        if (destination != null) {
            return destination;
        } else {
            return Builder.Destination.FACEBOOK;
        }
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public int describeContents() {
        return 0;
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.applinkUrl);
        out.writeString(this.previewImageUrl);
        out.writeString(this.promoText);
        out.writeString(this.promoCode);
        out.writeString(this.destination.toString());
    }

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static final Creator<AppInviteContent> CREATOR =
        new Creator<AppInviteContent>() {
            public AppInviteContent createFromParcel(final Parcel in) {
                return new AppInviteContent(in);
            }

            public AppInviteContent[] newArray(final int size) {
                return new AppInviteContent[size];
            }
        };

    /**
     * @deprecated
     * AppInvites is deprecated
     */
    @Deprecated
    public static class Builder
            implements ShareModelBuilder<AppInviteContent, Builder> {
        private String applinkUrl;
        private String previewImageUrl;
        private String promoCode;
        private String promoText;
        private Destination destination;


        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        public enum Destination {
            FACEBOOK ("facebook"),
            MESSENGER ("messenger");

            private final String name;

            private Destination(String s) {
                name = s;
            }

            public boolean equalsName(String otherName) {
                return (otherName == null) ? false : name.equals(otherName);
            }

            public String toString() {
                return this.name;
            }
        }

        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        public Builder setApplinkUrl(final String applinkUrl) {
            this.applinkUrl = applinkUrl;
            return this;
        }

        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        public Builder setPreviewImageUrl(final String previewImageUrl) {
            this.previewImageUrl = previewImageUrl;
            return this;
        }

        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        public Builder setPromotionDetails(final String promotionText, final String promotionCode) {
            if (!TextUtils.isEmpty(promotionText)) {
                if (promotionText.length() > 80) {
                    throw new IllegalArgumentException("" +
                            "Invalid promotion text, promotionText needs to be between" +
                            "1 and 80 characters long");
                }

                if (!isAlphanumericWithSpaces(promotionText)) {
                    throw new IllegalArgumentException("" +
                            "Invalid promotion text, promotionText can only contain alphanumeric" +
                            "characters and spaces.");
                }

                if (!TextUtils.isEmpty(promotionCode)) {

                    if (promotionCode.length() > 10) {
                        throw new IllegalArgumentException("" +
                                "Invalid promotion code, promotionCode can be between" +
                                "1 and 10 characters long");
                    }

                    if (!isAlphanumericWithSpaces(promotionCode)) {
                        throw new IllegalArgumentException("" +
                                "Invalid promotion code, promotionCode can only contain " +
                                "alphanumeric characters and spaces.");
                    }
                }
            } else if (!TextUtils.isEmpty(promotionCode)) {
                throw new IllegalArgumentException("promotionCode cannot be specified " +
                        "without a valid promotionText");
            }

            this.promoCode = promotionCode;
            this.promoText = promotionText;
            return this;
        }

        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        public Builder setDestination(Destination destination) {
            this.destination = destination;
            return this;
        }


        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        @Override
        public AppInviteContent build() {
            return new AppInviteContent(this);
        }


        /**
         * @deprecated
         * AppInvites is deprecated
         */
        @Deprecated
        @Override
        public Builder readFrom(final AppInviteContent content) {
            if (content == null) {
                return this;
            }
            return this
                    .setApplinkUrl(content.getApplinkUrl())
                    .setPreviewImageUrl(content.getPreviewImageUrl())
                    .setPromotionDetails(content.getPromotionText(), content.getPromotionCode())
                    .setDestination(content.getDestination());
        }

        private boolean isAlphanumericWithSpaces(String str) {
            for (int i=0; i<str.length(); i++) {
                char c = str.charAt(i);
                if (!Character.isDigit(c) && !Character.isLetter(c) && !Character.isSpaceChar(c))
                    return false;
            }

            return true;
        }
    }
}
