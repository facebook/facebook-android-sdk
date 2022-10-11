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

class ContextCreateContent : ShareModel {
  val suggestedPlayerID: String?

  private constructor(builder: Builder) {
    suggestedPlayerID = builder.suggestedPlayerID
  }

  internal constructor(parcel: Parcel) {
    suggestedPlayerID = parcel.readString()
  }

  override fun writeToParcel(out: Parcel, flags: Int) = out.writeString(suggestedPlayerID)

  override fun describeContents(): Int = 0

  /** Builder class for a concrete instance of ContextCreateContent */
  class Builder : ShareModelBuilder<ContextCreateContent, Builder> {
    internal var suggestedPlayerID: String? = null

    /**
     * Sets the string of the id of the suggested player
     *
     * @param suggestedPlayerID string of the id of the suggested player
     * @return the builder
     */
    fun setSuggestedPlayerID(suggestedPlayerID: String?): Builder {
      this.suggestedPlayerID = suggestedPlayerID
      return this
    }

    override fun build(): ContextCreateContent = ContextCreateContent(this)

    override fun readFrom(model: ContextCreateContent?): Builder =
        model?.let { setSuggestedPlayerID(it.suggestedPlayerID) } ?: this

    fun readFrom(parcel: Parcel): Builder =
        this.readFrom(
            parcel.readParcelable<Parcelable>(ContextCreateContent::class.java.classLoader)
                as ContextCreateContent?)
  }

  companion object CREATOR : Parcelable.Creator<ContextCreateContent> {
    override fun createFromParcel(parcel: Parcel): ContextCreateContent {
      return ContextCreateContent(parcel)
    }

    override fun newArray(size: Int): Array<ContextCreateContent?> {
      return arrayOfNulls(size)
    }
  }
}
