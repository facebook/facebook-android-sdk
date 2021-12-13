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
 * Provides the interface for video content to be shared.
 *
 * A general use builder is available in [ShareVideoContent.Builder].
 */
class ShareVideoContent : ShareContent<ShareVideoContent, ShareVideoContent.Builder>, ShareModel {
  /** The description of the video. */
  val contentDescription: String?

  /** The title to display for this video. */
  val contentTitle: String?

  /** Photo to be used as a preview for the video. */
  val previewPhoto: SharePhoto?

  /** Video to be shared. */
  val video: ShareVideo?

  private constructor(builder: Builder) : super(builder) {
    contentDescription = builder.contentDescription
    contentTitle = builder.contentTitle
    previewPhoto = builder.previewPhoto
    video = builder.video
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    contentDescription = parcel.readString()
    contentTitle = parcel.readString()
    val previewPhotoBuilder = SharePhoto.Builder().readFrom(parcel)
    previewPhoto =
        if (previewPhotoBuilder.imageUrl != null || previewPhotoBuilder.bitmap != null) {
          previewPhotoBuilder.build()
        } else {
          null
        }
    video = ShareVideo.Builder().readFrom(parcel).build()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeString(contentDescription)
    out.writeString(contentTitle)
    out.writeParcelable(previewPhoto, 0)
    out.writeParcelable(video, 0)
  }

  /** Builder for the [com.facebook.share.model.ShareVideoContent] interface. */
  class Builder : ShareContent.Builder<ShareVideoContent, Builder>() {
    internal var contentDescription: String? = null
    internal var contentTitle: String? = null
    internal var previewPhoto: SharePhoto? = null
    internal var video: ShareVideo? = null

    /**
     * Sets the description of the video.
     *
     * @param contentDescription The description of the video.
     * @return The builder.
     */
    fun setContentDescription(contentDescription: String?): Builder {
      this.contentDescription = contentDescription
      return this
    }

    /**
     * Sets the title to display for this video.
     *
     * @param contentTitle The video title.
     * @return The builder.
     */
    fun setContentTitle(contentTitle: String?): Builder {
      this.contentTitle = contentTitle
      return this
    }

    /**
     * Sets the photo to be used as a preview for the video.
     *
     * @param previewPhoto Preview [com.facebook.share.model.SharePhoto] for the content.
     * @return The builder.
     */
    fun setPreviewPhoto(previewPhoto: SharePhoto?): Builder {
      this.previewPhoto = previewPhoto?.let { SharePhoto.Builder().readFrom(previewPhoto).build() }
      return this
    }

    /**
     * Sets the video to be shared.
     *
     * @param video [com.facebook.share.model.ShareVideo]
     * @return The builder.
     */
    fun setVideo(video: ShareVideo?): Builder {
      if (video == null) {
        return this
      }
      this.video = ShareVideo.Builder().readFrom(video).build()
      return this
    }

    override fun build(): ShareVideoContent {
      return ShareVideoContent(this)
    }

    override fun readFrom(content: ShareVideoContent?): Builder {
      return if (content == null) {
        this
      } else
          super.readFrom(content)
              .setContentDescription(content.contentDescription)
              .setContentTitle(content.contentTitle)
              .setPreviewPhoto(content.previewPhoto)
              .setVideo(content.video)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareVideoContent> =
        object : Parcelable.Creator<ShareVideoContent> {
          override fun createFromParcel(parcel: Parcel): ShareVideoContent {
            return ShareVideoContent(parcel)
          }

          override fun newArray(size: Int): Array<ShareVideoContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
