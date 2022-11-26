/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
