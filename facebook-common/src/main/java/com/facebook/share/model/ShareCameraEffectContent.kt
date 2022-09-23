/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Describes the Camera Effect to be shared. Use [ShareCameraEffectContent.Builder] to build
 * instances.
 *
 * See documentation for
 * [best practices](https://developers.facebook.com/docs/sharing/best-practices).
 */
class ShareCameraEffectContent :
    ShareContent<ShareCameraEffectContent, ShareCameraEffectContent.Builder> {
  /**
   * Returns the Effect Id represented in this content instance, as set in the Builder.
   * @return The Effect Id
   */
  var effectId: String?
    private set

  /**
   * Returns the Arguments for the Effect represented in this content instance, as set in the
   * Builder.
   *
   * @return Effect Arguments
   */
  var arguments: CameraEffectArguments?
    private set

  /**
   * Returns the Textures for the Effect represented in this content instance, as set in the
   * Builder.
   *
   * @return Effect Textures
   */
  var textures: CameraEffectTextures?
    private set

  private constructor(builder: Builder) : super(builder) {
    effectId = builder.effectId
    arguments = builder.arguments
    textures = builder.textures
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    effectId = parcel.readString()
    arguments = CameraEffectArguments.Builder().readFrom(parcel).build()
    textures = CameraEffectTextures.Builder().readFrom(parcel).build()
  }

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeString(effectId)
    out.writeParcelable(arguments, 0)
    out.writeParcelable(textures, 0)
  }

  /** Builder for the [ShareCameraEffectContent] interface. */
  class Builder : ShareContent.Builder<ShareCameraEffectContent, Builder>() {
    internal var effectId: String? = null
    internal var arguments: CameraEffectArguments? = null
    internal var textures: CameraEffectTextures? = null

    /**
     * Sets the Effect Id for the Effect represented by this content instance. This must be an Id of
     * an effect that is published and approved.
     *
     * @param effectId Id of the Effect.
     * @return This builder instance
     */
    fun setEffectId(effectId: String?): Builder {
      this.effectId = effectId
      return this
    }

    /**
     * Sets the Arguments for the Effect represented by this content instance.
     *
     * @param arguments Arguments for this Effect
     * @return This builder instance
     */
    fun setArguments(arguments: CameraEffectArguments?): Builder {
      this.arguments = arguments
      return this
    }

    /**
     * Sets the Textures for the Effect represented by this content instance.
     *
     * @param textures Textures for this Effect
     * @return This builder instance
     */
    fun setTextures(textures: CameraEffectTextures?): Builder {
      this.textures = textures
      return this
    }

    /**
     * Creates a new instance of ShareCameraEffectContent with the properties as set on this Builder
     * instance
     *
     * @return A new instance of ShareCameraEffectContent
     */
    override fun build(): ShareCameraEffectContent {
      return ShareCameraEffectContent(this)
    }

    override fun readFrom(model: ShareCameraEffectContent?): Builder {
      return if (model == null) {
        this
      } else
          super.readFrom(model)
              .setEffectId(model.effectId)
              .setArguments(model.arguments)
              .setTextures(model.textures)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareCameraEffectContent> =
        object : Parcelable.Creator<ShareCameraEffectContent> {
          override fun createFromParcel(parcel: Parcel): ShareCameraEffectContent {
            return ShareCameraEffectContent(parcel)
          }

          override fun newArray(size: Int): Array<ShareCameraEffectContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
