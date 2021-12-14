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
package com.facebook.share.internal

import android.os.Parcel
import android.os.Parcelable
import com.facebook.share.model.ShareContent

// This class is used specifically for backwards support in unity for various feed parameters
// Currently this content is only supported if you set the mode to Feed when sharing.
class ShareFeedContent : ShareContent<ShareFeedContent, ShareFeedContent.Builder> {
  val toId: String?
  val link: String?
  val linkName: String?
  val linkCaption: String?
  val linkDescription: String?
  val picture: String?
  val mediaSource: String?

  private constructor(builder: Builder) : super(builder) {
    toId = builder.toId
    link = builder.link
    linkName = builder.linkName
    linkCaption = builder.linkCaption
    linkDescription = builder.linkDescription
    picture = builder.picture
    mediaSource = builder.mediaSource
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    toId = parcel.readString()
    link = parcel.readString()
    linkName = parcel.readString()
    linkCaption = parcel.readString()
    linkDescription = parcel.readString()
    picture = parcel.readString()
    mediaSource = parcel.readString()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeString(toId)
    out.writeString(link)
    out.writeString(linkName)
    out.writeString(linkCaption)
    out.writeString(linkDescription)
    out.writeString(picture)
    out.writeString(mediaSource)
  }

  /** Builder for the [ShareFeedContent] interface. */
  class Builder : ShareContent.Builder<ShareFeedContent, Builder>() {
    internal var toId: String? = null
    internal var link: String? = null
    internal var linkName: String? = null
    internal var linkCaption: String? = null
    internal var linkDescription: String? = null
    internal var picture: String? = null
    internal var mediaSource: String? = null

    fun setToId(toId: String?): Builder {
      this.toId = toId
      return this
    }

    fun setLink(link: String?): Builder {
      this.link = link
      return this
    }

    fun setLinkName(linkName: String?): Builder {
      this.linkName = linkName
      return this
    }

    fun setLinkCaption(linkCaption: String?): Builder {
      this.linkCaption = linkCaption
      return this
    }

    fun setLinkDescription(linkDescription: String?): Builder {
      this.linkDescription = linkDescription
      return this
    }

    fun setPicture(picture: String?): Builder {
      this.picture = picture
      return this
    }

    fun setMediaSource(mediaSource: String?): Builder {
      this.mediaSource = mediaSource
      return this
    }

    override fun build(): ShareFeedContent {
      return ShareFeedContent(this)
    }

    override fun readFrom(content: ShareFeedContent?): Builder {
      return if (content == null) {
        this
      } else
          super.readFrom(content)
              .setToId(content.toId)
              .setLink(content.link)
              .setLinkName(content.linkName)
              .setLinkCaption(content.linkCaption)
              .setLinkDescription(content.linkDescription)
              .setPicture(content.picture)
              .setMediaSource(content.mediaSource)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareFeedContent> =
        object : Parcelable.Creator<ShareFeedContent> {
          override fun createFromParcel(parcel: Parcel): ShareFeedContent {
            return ShareFeedContent(parcel)
          }

          override fun newArray(size: Int): Array<ShareFeedContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
