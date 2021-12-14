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
