/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Describes a video for sharing.
 *
 * Use [ShareVideo.Builder] to create instances
 */
class ShareVideo : ShareMedia<ShareVideo, ShareVideo.Builder> {
  /**
   * This method supplies the URL to locate the video.
   *
   * @return [android.net.Uri] that points to the location of the video on disk.
   */
  val localUrl: Uri?

  private constructor(builder: Builder) : super(builder) {
    localUrl = builder.localUrl
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    localUrl = parcel.readParcelable(Uri::class.java.classLoader)
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeParcelable(localUrl, 0)
  }

  override val mediaType: Type = Type.VIDEO

  /** Builder for the [com.facebook.share.model.ShareVideo] class. */
  class Builder : ShareMedia.Builder<ShareVideo, Builder>() {
    internal var localUrl: Uri? = null

    /**
     * Sets the URL to locate the video.
     *
     * @param localUrl [android.net.Uri] that points to the location of the video on disk.
     * @return The builder.
     */
    fun setLocalUrl(localUrl: Uri?): Builder {
      this.localUrl = localUrl
      return this
    }

    override fun build(): ShareVideo = ShareVideo(this)

    override fun readFrom(model: ShareVideo?): Builder {
      return if (model == null) {
        this
      } else setLocalUrl(model.localUrl)
    }

    internal fun readFrom(parcel: Parcel): Builder {
      return this.readFrom(parcel.readParcelable(ShareVideo::class.java.classLoader))
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareVideo> =
        object : Parcelable.Creator<ShareVideo> {
          override fun createFromParcel(source: Parcel): ShareVideo = ShareVideo(source)

          override fun newArray(size: Int): Array<ShareVideo?> = arrayOfNulls(size)
        }
  }
}
