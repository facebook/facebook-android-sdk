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

import com.facebook.internal.NativeProtocol;

/**
 * Describes an Open Graph Object to be created.
 * <p/>
 * Use {@link ShareOpenGraphObject.Builder} to create instances
 * <p/>
 * See the documentation for
 * <a href="https://developers.facebook.com/docs/opengraph/objects/">Open Graph Objects</a>.
 */
public final class ShareOpenGraphObject extends
        ShareOpenGraphValueContainer<ShareOpenGraphObject, ShareOpenGraphObject.Builder> {

    private ShareOpenGraphObject(final Builder builder) {
        super(builder);
    }

    ShareOpenGraphObject(final Parcel in) {
        super(in);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareOpenGraphObject> CREATOR =
            new Creator<ShareOpenGraphObject>() {
        public ShareOpenGraphObject createFromParcel(final Parcel in) {
            return new ShareOpenGraphObject(in);
        }

        public ShareOpenGraphObject[] newArray(final int size) {
            return new ShareOpenGraphObject[size];
        }
    };

    /**
     * Builder for the {@link com.facebook.share.model.ShareOpenGraphObject} interface.
     */
    public static final class Builder
            extends ShareOpenGraphValueContainer.Builder<ShareOpenGraphObject, Builder> {
        public Builder() {
            super();
            this.putBoolean(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY, true);
        }

        @Override
        public ShareOpenGraphObject build() {
            return new ShareOpenGraphObject(this);
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareOpenGraphObject)parcel.readParcelable(
                            ShareOpenGraphObject.class.getClassLoader()));
        }
    }
}
