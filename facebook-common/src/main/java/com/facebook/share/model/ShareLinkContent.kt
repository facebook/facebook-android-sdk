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
 * Describes link content to be shared.
 *
 * Use [ShareLinkContent.Builder] to build instances.
 *
 * See documentation for
 * [best practices](https://developers.facebook.com/docs/sharing/best-practices).
 */
class ShareLinkContent : ShareContent<ShareLinkContent, ShareLinkContent.Builder> {
  /** The quoted text to display for this link. */
  val quote: String?

  private constructor(builder: Builder) : super(builder) {
    quote = builder.quote
  }

  internal constructor(source: Parcel) : super(source) {
    quote = source.readString()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    super.writeToParcel(out, flags)
    out.writeString(quote)
  }

  /** Builder for the [ShareLinkContent] interface. */
  class Builder : ShareContent.Builder<ShareLinkContent, Builder>() {
    internal var quote: String? = null

    /**
     * Set the quote to display for this link.
     *
     * @param quote The text quoted from the link.
     * @return The builder.
     */
    fun setQuote(quote: String?): Builder {
      this.quote = quote
      return this
    }

    override fun build(): ShareLinkContent {
      return ShareLinkContent(this)
    }

    override fun readFrom(model: ShareLinkContent?): Builder {
      return if (model == null) {
        this
      } else super.readFrom(model).setQuote(model.quote)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareLinkContent> =
        object : Parcelable.Creator<ShareLinkContent> {
          override fun createFromParcel(source: Parcel): ShareLinkContent {
            return ShareLinkContent(source)
          }

          override fun newArray(size: Int): Array<ShareLinkContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
