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

import android.os.Parcel
import android.os.Parcelable

/**
 * Describes a hashtag for sharing.
 *
 * Use [ShareHashtag.Builder] to build instances
 */
class ShareHashtag : ShareModel {
  /** @return Gets the value of the hashtag for this instance */
  val hashtag: String?

  private constructor(builder: Builder) {
    hashtag = builder.hashtag
  }

  internal constructor(parcel: Parcel) {
    hashtag = parcel.readString()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(hashtag)
  }

  /** Builder for the [com.facebook.share.model.ShareHashtag] class. */
  class Builder : ShareModelBuilder<ShareHashtag, Builder> {
    /** @return Gets the value of the hashtag for this instance */
    var hashtag: String? = null
      private set

    /**
     * Sets the hashtag value for this instance.
     *
     * @param hashtag
     * @return the Builder instance
     */
    fun setHashtag(hashtag: String?): Builder {
      this.hashtag = hashtag
      return this
    }

    override fun readFrom(model: ShareHashtag?): Builder {
      return if (model == null) {
        this
      } else {
        setHashtag(model.hashtag)
      }
    }

    internal fun readFrom(parcel: Parcel): Builder {
      return this.readFrom(
          parcel.readParcelable(ShareHashtag::class.java.classLoader) as ShareHashtag?)
    }

    override fun build(): ShareHashtag {
      return ShareHashtag(this)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareHashtag> =
        object : Parcelable.Creator<ShareHashtag> {
          override fun createFromParcel(source: Parcel): ShareHashtag {
            return ShareHashtag(source)
          }

          override fun newArray(size: Int): Array<ShareHashtag?> {
            return arrayOfNulls(size)
          }
        }
  }
}
