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
import android.support.annotation.Nullable;

/**
 * Describes an Open Graph action
 * <p/>
 * Use {@link ShareOpenGraphAction.Builder} to create instances
 * <p/>
 * See the documentation for
 * <a href="https://developers.facebook.com/docs/opengraph/actions/">Open Graph Actions</a>.
 */
public final class ShareOpenGraphAction
        extends ShareOpenGraphValueContainer<ShareOpenGraphAction, ShareOpenGraphAction.Builder> {

    private ShareOpenGraphAction(final Builder builder) {
        super(builder);
    }

    ShareOpenGraphAction(final Parcel in) {
        super(in);
    }

    /**
     * The type for the action.
     *
     * @return The type for the action.
     */
    @Nullable
    public String getActionType() {
        return this.getString(Builder.ACTION_TYPE_KEY);
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareOpenGraphAction> CREATOR =
            new Creator<ShareOpenGraphAction>() {
                public ShareOpenGraphAction createFromParcel(final Parcel in) {
                    return new ShareOpenGraphAction(in);
                }

                public ShareOpenGraphAction[] newArray(final int size) {
                    return new ShareOpenGraphAction[size];
                }
            };

    /**
     * Builder for the {@link com.facebook.share.model.ShareOpenGraphAction} interface.
     */
    public static final class Builder
            extends ShareOpenGraphValueContainer.Builder<ShareOpenGraphAction, Builder> {
        private static final String ACTION_TYPE_KEY = "og:type";

        /**
         * Sets the type for the action.
         *
         * @param actionType The type for the action.
         * @return The builder.
         */
        public Builder setActionType(final String actionType) {
            this.putString(ACTION_TYPE_KEY, actionType);
            return this;
        }

        @Override
        public ShareOpenGraphAction build() {
            return new ShareOpenGraphAction(this);
        }

        @Override
        public Builder readFrom(final ShareOpenGraphAction model) {
            if (model == null) {
                return this;
            }
            return super
                    .readFrom(model)
                    .setActionType(model.getActionType())
                    ;
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (ShareOpenGraphAction) parcel.readParcelable(
                            ShareOpenGraphAction.class.getClassLoader()));
        }
    }
}
