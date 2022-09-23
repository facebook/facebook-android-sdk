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
