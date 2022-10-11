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
import java.util.Collections

class ContextChooseContent : ShareModel {
  private val filters: List<String>?
  val maxSize: Int?
  val minSize: Int?

  private constructor(builder: Builder) {
    filters = builder.filters
    maxSize = builder.maxSize
    minSize = builder.minSize
  }

  internal constructor(parcel: Parcel) {
    filters = parcel.createStringArrayList()
    maxSize = parcel.readInt()
    minSize = parcel.readInt()
  }

  fun getFilters(): List<String>? = filters?.let { Collections.unmodifiableList(it) }

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeStringList(filters)
    out.writeInt(maxSize ?: 0)
    out.writeInt(minSize ?: 0)
  }

  override fun describeContents(): Int = 0

  /** Builder class for a concrete instance of ContextChooseContent */
  class Builder : ShareModelBuilder<ContextChooseContent, Builder> {
    internal var filters: List<String>? = null
    internal var maxSize: Int? = null
    internal var minSize: Int? = null

    /**
     * Sets the set of filters to apply to the context suggestions.
     *
     * @param filters the set of filter to apply
     * @return the builder
     */
    fun setFilters(filters: List<String>?): Builder {
      this.filters = filters
      return this
    }

    /**
     * Sets the maximum number of participants that a suggested context should ideally have.
     *
     * @param maxSize the maximum number of participants
     * @return the builder
     */
    fun setMaxSize(maxSize: Int?): Builder {
      this.maxSize = maxSize
      return this
    }

    /**
     * Sets the minimum number of participants that a suggested context should ideally have.
     *
     * @param minSize the minimum number of participants
     * @return the builder
     */
    fun setMinSize(minSize: Int?): Builder {
      this.minSize = minSize
      return this
    }

    override fun build(): ContextChooseContent = ContextChooseContent(this)

    override fun readFrom(model: ContextChooseContent?): Builder =
        model?.let { setFilters(it.getFilters()).setMaxSize(it.maxSize).setMinSize(it.minSize) }
            ?: this

    fun readFrom(parcel: Parcel): Builder =
        readFrom(
            parcel.readParcelable<Parcelable>(ContextChooseContent::class.java.classLoader)
                as ContextChooseContent?)
  }

  companion object CREATOR : Parcelable.Creator<ContextChooseContent> {
    override fun createFromParcel(parcel: Parcel): ContextChooseContent {
      return ContextChooseContent(parcel)
    }

    override fun newArray(size: Int): Array<ContextChooseContent?> {
      return arrayOfNulls(size)
    }
  }
}
