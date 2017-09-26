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
 * NOTE: This API is in a closed beta and not available for general consumption. To apply
 * for access to the beta, please visit:
 * https://developers.facebook.com/products/camera-effects/ar-studio/.
 * Usage of this API without admission to the beta is unsupported and WILL result in errors
 * for users of your application.
 *
 * Describes the Camera Effect to be shared.
 *
 * Use {@link ShareCameraEffectContent.Builder} to build instances.
 *
 * See documentation for
 * <a href="https://developers.facebook.com/docs/sharing/best-practices">best practices</a>.
 */
public class ShareCameraEffectContent
        extends ShareContent<ShareCameraEffectContent, ShareCameraEffectContent.Builder> {

    private String effectId;
    private CameraEffectArguments arguments;
    private CameraEffectTextures textures;

    private ShareCameraEffectContent(final Builder builder) {
        super(builder);

        this.effectId = builder.effectId;
        this.arguments = builder.arguments;
        this.textures = builder.textures;
    }

    ShareCameraEffectContent(final Parcel in) {
        super(in);

        this.effectId = in.readString();
        this.arguments = new CameraEffectArguments.Builder().readFrom(in).build();
        this.textures = new CameraEffectTextures.Builder().readFrom(in).build();
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);

        out.writeString(effectId);
        out.writeParcelable(arguments, 0);
        out.writeParcelable(textures, 0);
    }

    /**
     * Returns the Effect Id represented in this content instance, as set in the Builder.
     *
     * @return The Effect Id
     */
    public String getEffectId() {
        return this.effectId;
    }

    /**
     * Returns the Arguments for the Effect represented in this content instance, as set in the
     * Builder.
     *
     * @return Effect Arguments
     */
    public CameraEffectArguments getArguments() {
        return this.arguments;
    }

    /**
     * Returns the Textures for the Effect represented in this content instance, as set in the
     * Builder.
     *
     * @return Effect Textures
     */
    public CameraEffectTextures getTextures() {
        return this.textures;
    }

    @SuppressWarnings("unused")
    public static final Creator<ShareCameraEffectContent> CREATOR =
            new Creator<ShareCameraEffectContent>() {
                public ShareCameraEffectContent createFromParcel(final Parcel in) {
                    return new ShareCameraEffectContent(in);
                }

                public ShareCameraEffectContent[] newArray(final int size) {
                    return new ShareCameraEffectContent[size];
                }
            };

    /**
     * NOTE: This API is in a closed beta and not available for general consumption. To apply
     * for access to the beta, please visit:
     * https://developers.facebook.com/products/camera-effects/ar-studio/.
     * Usage of this API without admission to the beta is unsupported and WILL result in errors
     * for users of your application.
     *
     * Builder for the {@link ShareCameraEffectContent} interface.
     */
    public static final class Builder
            extends ShareContent.Builder<ShareCameraEffectContent, Builder> {

        private String effectId;
        private CameraEffectArguments arguments;
        private CameraEffectTextures textures;

        /**
         * Sets the Effect Id for the Effect represented by this content instance. This must be an
         * Id of an effect that is published and approved.
         *
         * @param effectId Id of the Effect.
         * @return This builder instance
         */
        public Builder setEffectId(String effectId) {
            this.effectId = effectId;
            return this;
        }

        /**
         * Sets the Arguments for the Effect represented by this content instance.
         *
         * @param arguments Arguments for this Effect
         * @return This builder instance
         */
        public Builder setArguments(CameraEffectArguments arguments) {
            this.arguments = arguments;
            return this;
        }

        /**
         * Sets the Textures for the Effect represented by this content instance.
         *
         * @param textures Textures for this Effect
         * @return This builder instance
         */
        public Builder setTextures(CameraEffectTextures textures) {
            this.textures = textures;
            return this;
        }

        /**
         * NOTE: This API is in a closed beta and not available for general consumption. To apply
         * for access to the beta, please visit:
         * https://developers.facebook.com/products/camera-effects/ar-studio/.
         * Usage of this API without admission to the beta is unsupported and WILL result in errors
         * for users of your application.
         *
         * Creates a new instance of ShareCameraEffectContent with the properties as set on this
         * Builder instance
         *
         * @return A new instance of ShareCameraEffectContent
         */
        @Override
        public ShareCameraEffectContent build() {
            return new ShareCameraEffectContent(this);
        }

        @Override
        public ShareCameraEffectContent.Builder readFrom(final ShareCameraEffectContent model) {
            if (model == null) {
                return this;
            }

            return super
                    .readFrom(model)
                    .setEffectId(effectId)
                    .setArguments(arguments);
        }
    }
}
