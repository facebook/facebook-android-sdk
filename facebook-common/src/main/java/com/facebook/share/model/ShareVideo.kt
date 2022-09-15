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
