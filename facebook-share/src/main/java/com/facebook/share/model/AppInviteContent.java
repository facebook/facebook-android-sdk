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
import android.text.TextUtils;

/**
 * Describes the content that will be displayed by the AppInviteDialog
 */
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
     * Gets the applink url.
     * @return The applink url for the invite.
     */
    public String getApplinkUrl() {
        return applinkUrl;
    }

    /**
     * Gets the preview image url.
     * @return The preview image url for the invite.
     */
    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    /**
     * Gets the promotion code.
     * @return The promotion code for invite.
     */
    public String getPromotionCode() {
        return promoCode;
    }

    /**
     * Gets the promotion text.
     * @return The promotion text for invite.
     */
    public String getPromotionText() {
        return promoText;
    }

    /**
     * Gets the destination for the invite.
     * @return The destination for the invite.
     */
    public Builder.Destination getDestination() {
        if (destination != null) {
            return destination;
        } else {
            return Builder.Destination.FACEBOOK;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.applinkUrl);
        out.writeString(this.previewImageUrl);
        out.writeString(this.promoText);
        out.writeString(this.promoCode);
        out.writeString(this.destination.toString());
    }

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
     * Builder class for a concrete instance of AppInviteContent
     */
    public static class Builder
            implements ShareModelBuilder<AppInviteContent, Builder> {
        private String applinkUrl;
        private String previewImageUrl;
        private String promoCode;
        private String promoText;
        private Destination destination;


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

        /**
         * Sets promotion code and promotion text to be shown on sender and receiver flows
         * for app invites.
         *
         * @param promotionText Promotion text to be shown on sender and receiver flows.
         *                      Promotion text has to be between 1 and 80 characters long.
         * @param promotionCode Promotion code to be shown on sender and receiver flows.
         *                      Promotion code is optional and has to be less than 10 characters
         *                      long. promotionText needs to be specified if promotionCode
         *                      is provided.
         * @return the builder
         */
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

        public Builder setDestination(Destination destination) {
            this.destination = destination;
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
