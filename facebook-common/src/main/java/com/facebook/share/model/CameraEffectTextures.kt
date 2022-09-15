/*
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

package com.facebook.share.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

/** This class represents the textures that are used by an Effect in the Camera. */
class CameraEffectTextures : ShareModel {
  private val textures: Bundle?

  private constructor(builder: Builder) {
    textures = builder.textures
  }

  internal constructor(parcel: Parcel) {
    textures = parcel.readBundle(javaClass.classLoader)
  }

  fun getTextureBitmap(key: String?): Bitmap? {
    return textures?.get(key) as? Bitmap
  }

  fun getTextureUri(key: String?): Uri? {
    return textures?.get(key) as? Uri
  }

  operator fun get(key: String?): Any? {
    return textures?.get(key)
  }

  /**
   * The set of keys that have been set in this instance of CameraEffectTextures
   * @return The set of keys that have been set in this instance of CameraEffectTextures
   */
  fun keySet(): Set<String> {
    return textures?.keySet() ?: setOf()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeBundle(textures)
  }

  /** Builder for the [com.facebook.share.model.CameraEffectTextures] class. */
  class Builder : ShareModelBuilder<CameraEffectTextures, Builder> {
    internal val textures = Bundle()

    /**
     * Sets the passed in bitmap for the passed in key.
     *
     * @param key The key
     * @param texture The bitmap
     * @return The builder instance
     */
    fun putTexture(key: String, texture: Bitmap?): Builder {
      return putParcelableTexture(key, texture)
    }

    /**
     * Sets the passed in textureUrl for the passed in key.
     *
     * @param key The key
     * @param textureUrl The texture url
     * @return The builder instance
     */
    fun putTexture(key: String, textureUrl: Uri?): Builder {
      return putParcelableTexture(key, textureUrl)
    }

    private fun putParcelableTexture(key: String, parcelableTexture: Parcelable?): Builder {
      if (key.isNotEmpty() && parcelableTexture != null) {
        textures.putParcelable(key, parcelableTexture)
      }
      return this
    }

    override fun readFrom(model: CameraEffectTextures?): Builder {
      if (model != null) {
        textures.putAll(model.textures)
      }
      return this
    }

    /** This method is for internal use only. */
    fun readFrom(parcel: Parcel): Builder {
      return this.readFrom(parcel.readParcelable(CameraEffectTextures::class.java.classLoader))
    }

    override fun build(): CameraEffectTextures {
      return CameraEffectTextures(this)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<CameraEffectTextures> =
        object : Parcelable.Creator<CameraEffectTextures> {
          override fun createFromParcel(parcel: Parcel): CameraEffectTextures {
            return CameraEffectTextures(parcel)
          }

          override fun newArray(size: Int): Array<CameraEffectTextures?> {
            return arrayOfNulls(size)
          }
        }
  }
}
