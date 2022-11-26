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
 * Describes photo content to be shared.
 *
 * Use [SharePhotoContent.Builder] to create instances
 */
class SharePhotoContent : ShareContent<SharePhotoContent, SharePhotoContent.Builder> {
  /** Photos to be shared. */
  val photos: List<SharePhoto>

  private constructor(builder: Builder) : super(builder) {
    photos = builder.photos.toList()
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    photos = SharePhoto.Builder.readPhotoListFrom(parcel).toList()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    SharePhoto.Builder.writePhotoListTo(out, flags, photos)
  }

  /** Builder for the [SharePhotoContent] interface. */
  class Builder : ShareContent.Builder<SharePhotoContent, Builder>() {
    internal val photos: MutableList<SharePhoto> = ArrayList()

    /**
     * Adds a photo to the content.
     *
     * @param photo [com.facebook.share.model.SharePhoto] to add.
     * @return The builder.
     */
    fun addPhoto(photo: SharePhoto?): Builder {
      if (photo != null) {
        photos.add(SharePhoto.Builder().readFrom(photo).build())
      }
      return this
    }

    /**
     * Adds multiple photos to the content.
     *
     * @param photos [java.util.List] of [com.facebook.share.model.SharePhoto]s to add.
     * @return The builder.
     */
    fun addPhotos(photos: List<SharePhoto>?): Builder {
      if (photos != null) {
        for (photo in photos) {
          addPhoto(photo)
        }
      }
      return this
    }

    override fun build(): SharePhotoContent = SharePhotoContent(this)

    override fun readFrom(content: SharePhotoContent?): Builder {
      return if (content == null) {
        this
      } else super.readFrom(content).addPhotos(content.photos)
    }

    /**
     * Replaces the photos for the builder.
     *
     * @param photos [java.util.List] of [com.facebook.share.model.SharePhoto]s to add.
     * @return The builder.
     */
    fun setPhotos(photos: List<SharePhoto>?): Builder {
      this.photos.clear()
      addPhotos(photos)
      return this
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<SharePhotoContent> =
        object : Parcelable.Creator<SharePhotoContent> {
          override fun createFromParcel(parcel: Parcel): SharePhotoContent {
            return SharePhotoContent(parcel)
          }

          override fun newArray(size: Int): Array<SharePhotoContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
