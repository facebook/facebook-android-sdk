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
import java.lang.IllegalArgumentException

class ShareMediaContent : ShareContent<ShareMediaContent, ShareMediaContent.Builder> {
  val media: List<ShareMedia<*, *>>

  private constructor(builder: Builder) : super(builder) {
    media = builder.media.toList()
  }

  internal constructor(source: Parcel) : super(source) {
    this.media =
        source.readParcelableArray(ShareMedia::class.java.classLoader)?.mapNotNull {
          it as ShareMedia<*, *>?
        }
            ?: listOf()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeParcelableArray(media.toTypedArray(), flags)
  }

  /** Builder for the [SharePhotoContent] interface. */
  class Builder : ShareContent.Builder<ShareMediaContent, Builder>() {
    internal val media: MutableList<ShareMedia<*, *>> = ArrayList()

    /**
     * Adds a medium to the content.
     *
     * @param medium [com.facebook.share.model.ShareMedia] to add.
     * @return The builder.
     */
    fun addMedium(medium: ShareMedia<*, *>?): Builder {
      if (medium != null) {
        val mediumToAdd: ShareMedia<*, *> =
            when (medium) {
              is SharePhoto -> {
                SharePhoto.Builder().readFrom(medium).build()
              }
              is ShareVideo -> {
                ShareVideo.Builder().readFrom(medium).build()
              }
              else -> {
                throw IllegalArgumentException("medium must be either a SharePhoto or ShareVideo")
              }
            }
        media.add(mediumToAdd)
      }
      return this
    }

    /**
     * Adds multiple media to the content.
     *
     * @param media [java.util.List] of [com.facebook.share.model.ShareMedia] to add.
     * @return The builder.
     */
    fun addMedia(media: List<ShareMedia<*, *>>?): Builder {
      if (media != null) {
        for (medium in media) {
          addMedium(medium)
        }
      }
      return this
    }

    override fun build(): ShareMediaContent {
      return ShareMediaContent(this)
    }

    override fun readFrom(content: ShareMediaContent?): Builder {
      return if (content == null) {
        this
      } else super.readFrom(content).addMedia(content.media)
    }

    /**
     * Replaces the media for the builder.
     *
     * @param media [java.util.List] of [com.facebook.share.model.ShareMedia] to add.
     * @return The builder.
     */
    fun setMedia(media: List<ShareMedia<*, *>>?): Builder {
      this.media.clear()
      addMedia(media)
      return this
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareMediaContent> =
        object : Parcelable.Creator<ShareMediaContent> {
          override fun createFromParcel(source: Parcel): ShareMediaContent {
            return ShareMediaContent(source)
          }

          override fun newArray(size: Int): Array<ShareMediaContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
