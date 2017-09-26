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
import android.support.annotation.Nullable;

import java.util.Set;

/**
 * NOTE: This API is in a closed beta and not available for general consumption. To apply for access
 * to the beta, please visit https://developers.facebook.com/products/camera-effects/ar-studio/.
 * Usage of this API without admission to the beta is unsupported and WILL result in errors for
 * users of your application.
 *
 * This class represents a set of Arguments that are used to configure an Effect in the Camera.
 */
public class CameraEffectArguments implements ShareModel {

    private final Bundle params;

    private CameraEffectArguments(final Builder builder) {
        params = builder.params;
    }

    CameraEffectArguments(final Parcel in) {
        params = in.readBundle(getClass().getClassLoader());
    }

    /**
     * Returns the value of a String argument associated with the passed in key. If the key does
     * not exist, or if it points to an object that is not a String, null will be returned.
     *
     * @param key Key for the value desired.
     * @return The String associated with the passed in key, or null if the key does not exist or if
     * the value is not a String.
     */
    @Nullable
    public String getString(final String key) {
        return params.getString(key);
    }

    @Nullable
    /**
     * Returns the value of a String[] argument associated with the passed in key. If the key does
     * not exist, or if it points to an object that is not a String[], null will be returned.
     *
     * @param key Key for the value desired.
     * @return The String[] associated with the passed in key, or null if the key does not exist or
     * if the value is not a String[].
     */
    public String[] getStringArray(final String key) {
        return params.getStringArray(key);
    }

    @Nullable
    /**
     * Returns the value of the argument associated with the passed in key. If the key does not
     * exist, null will be returned
     *
     * @param key Key for the value desired.
     * @return The value associated with the passed in key, or null if the key does not exist.
     */
    public Object get(final String key) {
        return this.params.get(key);
    }

    /**
     * The set of keys that have been set in this instance of CameraEffectArguments
     * @return The set of keys that have been set in this instance of CameraEffectArguments
     */
    public Set<String> keySet() {
        return this.params.keySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeBundle(params);
    }

    public static final Creator<CameraEffectArguments> CREATOR =
            new Creator<CameraEffectArguments>() {
                public CameraEffectArguments createFromParcel(final Parcel in) {
                    return new CameraEffectArguments(in);
                }

                public CameraEffectArguments[] newArray(final int size) {
                    return new CameraEffectArguments[size];
                }
            };

    /**
     * NOTE: This API is in a closed beta and not available for general consumption. To apply
     * for access to the beta, please visit:
     * https://developers.facebook.com/products/camera-effects/ar-studio/.
     * Usage of this API without admission to the beta is unsupported and WILL result in errors
     * for users of your application.
     *
     * Builder for the {@link com.facebook.share.model.CameraEffectArguments} class.
     */
    public static class Builder
            implements ShareModelBuilder<CameraEffectArguments, Builder> {

        private Bundle params = new Bundle();

        /**
         * Sets the passed in value for the passed in key. This will override any previous calls
         * with the same key.
         *
         * @param key Key for the argument
         * @param value Value of the argument
         * @return This Builder instance
         */
        public Builder putArgument(final String key, final String value) {
            params.putString(key, value);
            return this;
        }

        /**
         * Sets the passed in value for the passed in key. This will override any previous calls
         * with the same key.
         *
         * @param key Key for the argument
         * @param arrayValue Value of the argument
         * @return This Builder instance
         */
        public Builder putArgument(final String key, final String[] arrayValue) {
            params.putStringArray(key, arrayValue);
            return this;
        }

        @Override
        public Builder readFrom(final CameraEffectArguments model) {
            if (model != null) {
                this.params.putAll(model.params);
            }
            return this;
        }

        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (CameraEffectArguments) parcel.readParcelable(
                            CameraEffectArguments.class.getClassLoader()));
        }

        /**
         * NOTE: This API is in a closed beta and not available for general consumption. To apply
         * for access to the beta, please visit:
         * https://developers.facebook.com/products/camera-effects/ar-studio/.
         * Usage of this API without admission to the beta is unsupported and WILL result in errors
         * for users of your application.
         *
         * Creates a new instance of CameraEffectArguments with the arguments that have been set
         * in this Builder instance.
         *
         * @return A new instance of CameraEffectArguments.
         */
        @Override
        public CameraEffectArguments build() {
            return new CameraEffectArguments(this);
        }
    }
}
