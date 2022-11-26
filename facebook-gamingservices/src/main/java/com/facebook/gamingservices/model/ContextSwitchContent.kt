/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.model

import android.os.Parcel
import android.os.Parcelable
import com.facebook.share.model.ShareModel
import com.facebook.share.model.ShareModelBuilder

class ContextSwitchContent : ShareModel {
  val contextID: String?

  private constructor(builder: Builder) {
    contextID = builder.contextID
  }

  internal constructor(parcel: Parcel) {
    contextID = parcel.readString()
  }

  override fun writeToParcel(out: Parcel, flags: Int) = out.writeString(contextID)

  override fun describeContents(): Int = 0

  /** Builder class for a concrete instance of ContextSwitchContent */
  class Builder : ShareModelBuilder<ContextSwitchContent, Builder> {
    internal var contextID: String? = null

    /**
     * Sets the context ID that the player will switch into.
     *
     * @param contextID the context ID
     * @return the builder
     */
    fun setContextID(contextID: String?): Builder {
      this.contextID = contextID
      return this
    }

    override fun build(): ContextSwitchContent = ContextSwitchContent(this)

    override fun readFrom(model: ContextSwitchContent?): Builder =
        model?.let { setContextID(it.contextID) } ?: this

    fun readFrom(parcel: Parcel): Builder =
        this.readFrom(
            parcel.readParcelable<Parcelable>(ContextSwitchContent::class.java.classLoader)
                as ContextSwitchContent?)
  }

  companion object CREATOR : Parcelable.Creator<ContextSwitchContent> {
    override fun createFromParcel(parcel: Parcel): ContextSwitchContent {
      return ContextSwitchContent(parcel)
    }

    override fun newArray(size: Int): Array<ContextSwitchContent?> {
      return arrayOfNulls(size)
    }
  }
}
