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
import androidx.annotation.Nullable;

/**
 * The base class for Messenger share action buttons.
 *
 * @deprecated Sharing to Messenger via the SDK is unsupported. https://developers.facebook.com/docs/messenger-platform/changelog/#20190610.
 * Sharing should be performed by the native share sheet."
 */
@Deprecated
public abstract class ShareMessengerActionButton implements ShareModel {

    private final String title;

    protected ShareMessengerActionButton(final Builder builder) {
        this.title = builder.title;
    }

    ShareMessengerActionButton(final Parcel in) {
        this.title = in.readString();
    }

    /**
     * The title displayed to the user for the Messenger action button.
     */
    public String getTitle() {
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
    }

    /**
     * Abstract builder for {@link com.facebook.share.model.ShareMessengerActionButton}
     */
    public static abstract class Builder<M extends ShareMessengerActionButton, B extends Builder>
            implements ShareModelBuilder<M, B> {
        private String title;

        /**
         * Sets the title for the Messenger action button.
         */
        public B setTitle(@Nullable final String title) {
            this.title = title;
            return (B) this;
        }

        @Override
        public B readFrom(final M model) {
            if (model == null) {
                return (B) this;
            }
            return this.setTitle(model.getTitle());
        }
    }
}
