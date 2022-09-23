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

  override fun describeContents(): Int = 0

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
      return this.readFrom(parcel.readParcelable(ShareHashtag::class.java.classLoader))
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
