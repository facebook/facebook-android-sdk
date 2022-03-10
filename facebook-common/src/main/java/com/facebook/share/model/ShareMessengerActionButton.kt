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

/** The base class for Messenger share action buttons. */
abstract class ShareMessengerActionButton : ShareModel {
  /** The title displayed to the user for the Messenger action button. */
  val title: String?

  protected constructor(builder: Builder<*, *>) {
    title = builder.title
  }

  internal constructor(parcel: Parcel) {
    title = parcel.readString()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(title)
  }

  /** Abstract builder for [com.facebook.share.model.ShareMessengerActionButton] */
  abstract class Builder<M : ShareMessengerActionButton, B : Builder<M, B>> :
      ShareModelBuilder<M, B> {
    internal var title: String? = null

    /** Sets the title for the Messenger action button. */
    fun setTitle(title: String?): B {
      this.title = title
      return this as B
    }

    override fun readFrom(model: M?): B {
      return if (model == null) {
        this as B
      } else setTitle(model.title)
    }
  }
}
