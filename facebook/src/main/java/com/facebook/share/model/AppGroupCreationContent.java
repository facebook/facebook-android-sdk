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
 * Describes the content that will be displayed by the AppGroupCreationDialog
 */
public final class AppGroupCreationContent implements ShareModel {
    private final String name;
    private final String description;
    private AppGroupPrivacy privacy;

    private AppGroupCreationContent(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.privacy = builder.privacy;
    }

    AppGroupCreationContent(final Parcel in) {
        this.name = in.readString();
        this.description = in.readString();
        this.privacy = (AppGroupPrivacy) in.readSerializable();
    }

    /**
     * Gets the name of the group that will be created.
     *
     * @return name of the group
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the description of the group that will be created.
     *
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the privacy for the group that will be created
     *
     * @return the privacy of the group
     */
    public AppGroupPrivacy getAppGroupPrivacy() {
        return this.privacy;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.name);
        out.writeString(this.description);
        out.writeSerializable(this.privacy);
    }

    @SuppressWarnings("unused")
    public static final Creator<AppGroupCreationContent> CREATOR =
            new Creator<AppGroupCreationContent>() {
                public AppGroupCreationContent createFromParcel(final Parcel in) {
                    return new AppGroupCreationContent(in);
                }

                public AppGroupCreationContent[] newArray(final int size) {
                    return new AppGroupCreationContent[size];
                }
            };

    /**
     * Specifies the privacy of a group.
     */
    public enum AppGroupPrivacy {
        /**
         * Anyone can see the group, who's in it and what members post.
         */
        Open,

        /**
         * Anyone can see the group and who's in it, but only members can see posts.
         */
        Closed,
    }

    /**
     * Builder class for a concrete instance of AppGroupCreationContent
     */
    public static class Builder
            implements ShareModelBuilder<AppGroupCreationContent, Builder> {
        private String name;
        private String description;
        private AppGroupPrivacy privacy;

        /**
         * Sets the name of the group that will be created.
         *
         * @param name name of the group
         * @return the builder
         */
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description of the group that will be created.
         *
         * @param description the description
         * @return the builder
         */
        public Builder setDescription(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the privacy for the group that will be created
         *
         * @param privacy privacy of the group
         * @return the builder
         */
        public Builder setAppGroupPrivacy(final AppGroupPrivacy privacy) {
            this.privacy = privacy;
            return this;
        }

        @Override
        public AppGroupCreationContent build() {
            return new AppGroupCreationContent(this);
        }


        @Override
        public Builder readFrom(final AppGroupCreationContent content) {
            if (content == null) {
                return this;
            }
            return this
                    .setName(content.getName())
                    .setDescription(content.getDescription())
                    .setAppGroupPrivacy(content.getAppGroupPrivacy());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (AppGroupCreationContent) parcel.readParcelable(
                            AppGroupCreationContent.class.getClassLoader()));
        }
    }
}
