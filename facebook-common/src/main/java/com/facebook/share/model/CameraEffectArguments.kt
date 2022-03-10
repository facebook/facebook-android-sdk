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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

/** This class represents a set of Arguments that are used to configure an Effect in the Camera. */
class CameraEffectArguments : ShareModel {
  private val params: Bundle?

  private constructor(builder: Builder) {
    params = builder.params
  }

  internal constructor(parcel: Parcel) {
    params = parcel.readBundle(javaClass.classLoader)
  }

  /**
   * Returns the value of a String argument associated with the passed in key. If the key does not
   * exist, or if it points to an object that is not a String, null will be returned.
   *
   * @param key Key for the value desired.
   * @return The String associated with the passed in key, or null if the key does not exist or if
   * the value is not a String.
   */
  fun getString(key: String?): String? {
    return params?.getString(key)
  }

  /**
   * Returns the value of a String[] argument associated with the passed in key. If the key does not
   * exist, or if it points to an object that is not a String[], null will be returned.
   *
   * @param key Key for the value desired.
   * @return The String[] associated with the passed in key, or null if the key does not exist or if
   * the value is not a String[].
   */
  fun getStringArray(key: String?): Array<String>? {
    return params?.getStringArray(key)
  }

  /**
   * Returns the value of the argument associated with the passed in key. If the key does not exist,
   * null will be returned
   *
   * @param key Key for the value desired.
   * @return The value associated with the passed in key, or null if the key does not exist.
   */
  operator fun get(key: String?): Any? {
    return params?.get(key)
  }

  /**
   * The set of keys that have been set in this instance of CameraEffectArguments
   *
   * @return The set of keys that have been set in this instance of CameraEffectArguments
   */
  fun keySet(): Set<String> {
    return params?.keySet() ?: setOf()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeBundle(params)
  }

  /** Builder for the [com.facebook.share.model.CameraEffectArguments] class. */
  class Builder : ShareModelBuilder<CameraEffectArguments, Builder> {
    internal val params = Bundle()

    /**
     * Sets the passed in value for the passed in key. This will override any previous calls with
     * the same key.
     *
     * @param key Key for the argument
     * @param value Value of the argument
     * @return This Builder instance
     */
    fun putArgument(key: String, value: String): Builder {
      params.putString(key, value)
      return this
    }

    /**
     * Sets the passed in value for the passed in key. This will override any previous calls with
     * the same key.
     *
     * @param key Key for the argument
     * @param arrayValue Value of the argument
     * @return This Builder instance
     */
    fun putArgument(key: String, arrayValue: Array<String>): Builder {
      params.putStringArray(key, arrayValue)
      return this
    }

    override fun readFrom(model: CameraEffectArguments?): Builder {
      if (model != null) {
        params.putAll(model.params)
      }
      return this
    }

    /** This method is for internal use only. */
    fun readFrom(parcel: Parcel): Builder {
      return this.readFrom(
          parcel.readParcelable(CameraEffectArguments::class.java.classLoader) as
              CameraEffectArguments?)
    }

    /**
     * Creates a new instance of CameraEffectArguments with the arguments that have been set in this
     * Builder instance.
     *
     * @return A new instance of CameraEffectArguments.
     */
    override fun build(): CameraEffectArguments {
      return CameraEffectArguments(this)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<CameraEffectArguments> =
        object : Parcelable.Creator<CameraEffectArguments> {
          override fun createFromParcel(parcel: Parcel): CameraEffectArguments {
            return CameraEffectArguments(parcel)
          }

          override fun newArray(size: Int): Array<CameraEffectArguments?> {
            return arrayOfNulls(size)
          }
        }
  }
}
