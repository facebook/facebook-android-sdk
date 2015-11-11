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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the base class for content to be shared. Contains all common methods for
 * the different types of content.
 */
public abstract class ShareContent<P extends ShareContent, E extends ShareContent.Builder>
        implements ShareModel {
    private final Uri contentUrl;
    private final List<String> peopleIds;
    private final String placeId;
    private final String ref;

    protected ShareContent(final Builder builder) {
        super();
        this.contentUrl = builder.contentUrl;
        this.peopleIds = builder.peopleIds;
        this.placeId = builder.placeId;
        this.ref = builder.ref;
    }

    protected ShareContent(final Parcel in) {
        this.contentUrl = in.readParcelable(Uri.class.getClassLoader());
        this.peopleIds = readUnmodifiableStringList(in);
        this.placeId = in.readString();
        this.ref = in.readString();
    }

    /**
     * URL for the content being shared.  This URL will be checked for app link meta tags for
     * linking in platform specific ways.
     * <p/>
     * See documentation for <a href="https://developers.facebook.com/docs/applinks/">App Links</a>.
     *
     * @return {@link android.net.Uri} representation of the content link.
     */
    @Nullable
    public Uri getContentUrl() {
        return this.contentUrl;
    }

    /**
     * List of Ids for taggable people to tag with this content.
     * <p/>
     * See documentation for
     * <a href="https://developers.facebook.com/docs/graph-api/reference/user/taggable_friends">
     * Taggable Friends</a>.
     *
     * @return {@link java.util.List} of Ids for people to tag.
     */
    @Nullable
    public List<String> getPeopleIds() {
        return this.peopleIds;
    }

    /**
     * The Id for a place to tag with this content.
     *
     * @return The Id for the place to tag.
     */
    @Nullable
    public String getPlaceId() {
        return this.placeId;
    }

    /**
     * A value to be added to the referrer URL when a person follows a link from this shared
     * content on feed.
     *
     * @return The ref for the content.
     */
    @Nullable
    public String getRef() {
        return this.ref;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeParcelable(this.contentUrl, 0);
        out.writeStringList(this.peopleIds);
        out.writeString(this.placeId);
        out.writeString(this.ref);
    }

    private List<String> readUnmodifiableStringList(final Parcel in) {
        final List<String> list = new ArrayList<String>();
        in.readStringList(list);
        return (list.size() == 0 ? null : Collections.unmodifiableList(list));
    }

    /**
     * Abstract builder for {@link com.facebook.share.model.ShareContent}
     */
    public abstract static class Builder<P extends ShareContent, E extends Builder>
            implements ShareModelBuilder<P, E> {
        private Uri contentUrl;
        private List<String> peopleIds;
        private String placeId;
        private String ref;

        /**
         * Set the URL for the content being shared.
         *
         * @param contentUrl {@link android.net.Uri} representation of the content link.
         * @return The builder.
         */
        public E setContentUrl(@Nullable final Uri contentUrl) {
            this.contentUrl = contentUrl;
            return (E) this;
        }

        /**
         * Set the list of Ids for taggable people to tag with this content.
         *
         * @param peopleIds {@link java.util.List} of Ids for people to tag.
         * @return The builder.
         */
        public E setPeopleIds(@Nullable final List<String> peopleIds) {
            this.peopleIds = (peopleIds == null ? null : Collections.unmodifiableList(peopleIds));
            return (E) this;
        }

        /**
         * Set the Id for a place to tag with this content.
         *
         * @param placeId The Id for the place to tag.
         * @return The builder.
         */
        public E setPlaceId(@Nullable final String placeId) {
            this.placeId = placeId;
            return (E) this;
        }

        /**
         * Set the value to be added to the referrer URL when a person follows a link from this
         * shared content on feed.
         *
         * @param ref The ref for the content.
         * @return The builder.
         */
        public E setRef(@Nullable final String ref) {
            this.ref = ref;
            return (E) this;
        }

        @Override
        public E readFrom(final P content) {
            if (content == null) {
                return (E) this;
            }
            return (E) this
                    .setContentUrl(content.getContentUrl())
                    .setPeopleIds(content.getPeopleIds())
                    .setPlaceId(content.getPlaceId())
                    .setRef(content.getRef());
        }
    }
}
