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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for shared media (photos, videos, etc).
 */
public abstract class ShareMedia implements ShareModel {

    public enum Type {
        PHOTO,
        VIDEO,
        ;
    }

    private final Bundle params;

    protected ShareMedia(final Builder builder) {
        this.params = new Bundle(builder.params);
    }

    ShareMedia(final Parcel in) {
        this.params = in.readBundle();
    }

    /**
     * @deprecated This method is deprecated. Use GraphRequest directly to set parameters.
     */
    @Deprecated
    public Bundle getParameters() {
        return new Bundle(params);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(params);
    }

    public abstract Type getMediaType();

    /**
     * Builder for the {@link com.facebook.share.model.ShareMedia} class.
     */
    public static abstract class Builder<M extends ShareMedia, B extends Builder>
            implements ShareModelBuilder<M, B> {
        private Bundle params = new Bundle();

        /**
         * @deprecated This method is deprecated. Use GraphRequest directly to set parameters.
         */
        @Deprecated
        public B setParameter(final String key, final String value) {
            params.putString(key, value);
            return (B) this;
        }

        /**
         * @deprecated This method is deprecated. Use GraphRequest directly to set parameters.
         */
        @Deprecated
        public B setParameters(final Bundle parameters) {
            params.putAll(parameters);
            return (B) this;
        }

        @Override
        public B readFrom(final M model) {
            if (model == null) {
                return (B) this;
            }
            return this.setParameters(model.getParameters());
        }

        static void writeListTo(
                final Parcel out,
                int parcelFlags,
                final List<ShareMedia> media) {
            out.writeParcelableArray((ShareMedia[]) media.toArray(), parcelFlags);
        }

        static List<ShareMedia> readListFrom(final Parcel in) {
            Parcelable[] parcelables = in.readParcelableArray(
                    ShareMedia.class.getClassLoader());
            List<ShareMedia> shareMedia = new ArrayList<>(parcelables.length);
            for (Parcelable parcelable : parcelables) {
                shareMedia.add((ShareMedia) parcelable);
            }
            return shareMedia;
        }
    }
}
