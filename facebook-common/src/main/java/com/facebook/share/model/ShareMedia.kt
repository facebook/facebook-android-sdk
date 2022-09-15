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
import androidx.annotation.RestrictTo

/** Base class for shared media (photos, videos, etc). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ShareMedia<M : ShareMedia<M, B>, B : ShareMedia.Builder<M, B>> : ShareModel {
  enum class Type {
    PHOTO,
    VIDEO,
  }

  private val params: Bundle

  @Deprecated("This method is deprecated. Use GraphRequest directly to set parameters.")
  fun getParameters(): Bundle = Bundle(params)

  protected constructor(builder: Builder<M, B>) {
    params = Bundle(builder.params)
  }

  internal constructor(parcel: Parcel) {
    params = parcel.readBundle(javaClass.classLoader) ?: Bundle()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeBundle(params)
  }

  abstract val mediaType: Type

  /** Builder for the {@link com.facebook.share.model.ShareMedia} class. */
  abstract class Builder<M : ShareMedia<M, B>, B : Builder<M, B>> : ShareModelBuilder<M, B> {
    internal var params: Bundle = Bundle()

    @Deprecated("This method is deprecated. Use GraphRequest directly to set parameters.")
    fun setParameter(key: String, value: String): B {
      params.putString(key, value)
      return this as B
    }

    @Deprecated("This method is deprecated. Use GraphRequest directly to set parameters.")
    fun setParameters(parameters: Bundle): B {
      params.putAll(parameters)
      return this as B
    }

    override fun readFrom(model: M?): B {
      if (model == null) {
        return this as B
      }
      return this.setParameters(model.params)
    }

    companion object {
      @JvmStatic
      internal fun writeListTo(out: Parcel, parcelFlags: Int, media: List<ShareMedia<*, *>>) {
        out.writeParcelableArray(media.toTypedArray(), parcelFlags)
      }

      @JvmStatic
      internal fun readListFrom(parcel: Parcel): List<ShareMedia<*, *>> {
        val parcelables =
            parcel.readParcelableArray(ShareMedia::class.java.classLoader) ?: return listOf()
        return parcelables.filterIsInstance<ShareMedia<*, *>>()
      }
    }
  }
}
