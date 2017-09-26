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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.facebook.internal.Utility;

import java.util.Set;

/**
 * NOTE: This API is in a closed beta and not available for general consumption. To apply
 * for access to the beta, please visit:
 * https://developers.facebook.com/products/camera-effects/ar-studio/.
 * Usage of this API without admission to the beta is unsupported and WILL result in errors
 * for users of your application.
 *
 * This class represents the textures that are used by an Effect in the Camera.
 */
public class CameraEffectTextures implements ShareModel {

    private final Bundle textures;

    private CameraEffectTextures(final Builder builder) {
        textures = builder.textures;
    }

    CameraEffectTextures(final Parcel in) {
        textures = in.readBundle(getClass().getClassLoader());
    }

    @Nullable
    public Bitmap getTextureBitmap(final String key) {
        Object value = this.textures.get(key);
        if (value instanceof Bitmap) {
            return (Bitmap) value;
        }
        return null;
    }

    @Nullable
    public Uri getTextureUri(final String key) {
        Object value = this.textures.get(key);
        if (value instanceof Uri) {
            return (Uri) value;
        }
        return null;
    }

    @Nullable
    public Object get(final String key) {
        return this.textures.get(key);
    }

    public Set<String> keySet() {
        return this.textures.keySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeBundle(textures);
    }

    public static final Creator<CameraEffectTextures> CREATOR =
            new Creator<CameraEffectTextures>() {
                public CameraEffectTextures createFromParcel(final Parcel in) {
                    return new CameraEffectTextures(in);
                }

                public CameraEffectTextures[] newArray(final int size) {
                    return new CameraEffectTextures[size];
                }
            };

    /**
     * NOTE: This API is in a closed beta and not available for general consumption. To apply
     * for access to the beta, please visit:
     * https://developers.facebook.com/products/camera-effects/ar-studio/.
     * Usage of this API without admission to the beta is unsupported and WILL result in errors
     * for users of your application.
     *
     * Builder for the {@link com.facebook.share.model.CameraEffectTextures} class.
     */
    public static class Builder
            implements ShareModelBuilder<CameraEffectTextures, Builder> {

        private Bundle textures = new Bundle();

        public Builder putTexture(final String key, Bitmap texture) {
            return putParcelableTexture(key, texture);
        }

        public Builder putTexture(final String key, Uri textureUrl) {
            return putParcelableTexture(key, textureUrl);
        }

        private Builder putParcelableTexture(final String key, final Parcelable parcelableTexture) {
            if (!Utility.isNullOrEmpty(key) && parcelableTexture != null) {
                textures.putParcelable(key, parcelableTexture);
            }
            return this;
        }

        @Override
        public Builder readFrom(final CameraEffectTextures model) {
            if (model != null) {
                this.textures.putAll(model.textures);
            }
            return this;
        }

        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (CameraEffectTextures) parcel.readParcelable(
                            CameraEffectTextures.class.getClassLoader()));
        }

        /**
         * NOTE: This API is in a closed beta and not available for general consumption. To apply
         * for access to the beta, please visit:
         * https://developers.facebook.com/products/camera-effects/ar-studio/.
         * Usage of this API without admission to the beta is unsupported and WILL result in errors
         * for users of your application.
         */
        @Override
        public CameraEffectTextures build() {
            return new CameraEffectTextures(this);
        }
    }
}
