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
import java.lang.IllegalArgumentException

class ShareMediaContent : ShareContent<ShareMediaContent, ShareMediaContent.Builder> {
  val media: List<ShareMedia>

  private constructor(builder: Builder) : super(builder) {
    media = builder.media.toList()
  }

  internal constructor(source: Parcel) : super(source) {
    this.media =
        source.readParcelableArray(ShareMedia::class.java.classLoader)?.mapNotNull {
          it as ShareMedia?
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
    internal val media: MutableList<ShareMedia> = ArrayList()

    /**
     * Adds a medium to the content.
     *
     * @param medium [com.facebook.share.model.ShareMedia] to add.
     * @return The builder.
     */
    fun addMedium(medium: ShareMedia?): Builder {
      if (medium != null) {
        val mediumToAdd: ShareMedia =
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
    fun addMedia(media: List<ShareMedia>?): Builder {
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
    fun setMedia(media: List<ShareMedia>?): Builder {
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
