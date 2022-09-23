/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices

import android.media.Image
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.facebook.gamingservices.internal.DateFormatter
import com.facebook.gamingservices.internal.TournamentScoreType
import com.facebook.gamingservices.internal.TournamentSortOrder
import com.facebook.share.model.ShareModel
import com.facebook.share.model.ShareModelBuilder
import java.time.Instant

class TournamentConfig : ShareModel {

  /** Gets the tournament title. */
  val title: String?
  /** Gets the tournament sort order. */
  val sortOrder: TournamentSortOrder?
  /** Gets the tournament score format. */
  val scoreType: TournamentScoreType?
  /** Gets the tournament end time. */
  val endTime: Instant?
  /** Gets the tournament image. */
  val image: Image?
  /** Gets the tournament payload. */
  val payload: String?

  private constructor(builder: Builder) {
    title = builder.title
    sortOrder = builder.sortOrder
    scoreType = builder.scoreType
    endTime = builder.endTime
    image = builder.image
    payload = builder.payload
  }

  internal constructor(parcel: Parcel) {
    title = parcel.readString()
    sortOrder = TournamentSortOrder.values().find { it.name == parcel.readString() }
    scoreType = TournamentScoreType.values().find { it.name == parcel.readString() }
    endTime =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Instant.from(DateFormatter.format(parcel.readString()))
        } else {
          null
        }
    payload = parcel.readString()
    image = null
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeString(sortOrder.toString())
    out.writeString(scoreType.toString())
    out.writeString(endTime.toString())
    out.writeString(title)
    out.writeString(payload)
  }

  /** Builder class for a concrete instance of TournamentCreationConfig */
  class Builder : ShareModelBuilder<TournamentConfig, Builder> {
    var title: String? = null
    var sortOrder: TournamentSortOrder? = null
    var scoreType: TournamentScoreType? = null
    var endTime: Instant? = null
    var image: Image? = null
    var payload: String? = null

    /**
     * Sets the tournament title
     *
     * @param title the title of the tournament
     * @return the builder
     */
    fun setTournamentTitle(title: String?): Builder {
      this.title = title
      return this
    }

    /**
     * Sets the sort order of the tournament.
     *
     * @param sortOrder The sort order for scores in the tournament
     * @return the builder
     */
    fun setTournamentSortOrder(sortOrder: TournamentSortOrder): Builder {
      this.sortOrder = sortOrder
      return this
    }

    /**
     * Sets the score type of the tournament.
     *
     * @param scoreType The type of score format for the tournament. {@link ScoreType}
     * @return the builder
     */
    fun setTournamentScoreType(scoreType: TournamentScoreType): Builder {
      this.scoreType = scoreType
      return this
    }

    /**
     * Sets the end time of the tournament.
     *
     * @param endTime The timestamp for the expiration of the tournament
     * @return the builder
     */
    fun setTournamentEndTime(endTime: Instant): Builder {
      this.endTime = endTime
      return this
    }

    /**
     * Sets the tournament image.
     *
     * @param image the payload
     * @return the builder
     */
    fun setTournamentImage(image: Image?): Builder {
      this.image = image
      return this
    }

    /**
     * Sets the tournament payload.
     *
     * @param payload the payload
     * @return the builder
     */
    fun setTournamentPayload(payload: String?): Builder {
      this.payload = payload
      return this
    }

    override fun build(): TournamentConfig {
      return TournamentConfig(this)
    }

    internal fun readFrom(parcel: Parcel): Builder {
      (parcel.readParcelable(TournamentConfig::class.java.classLoader) as TournamentConfig?).let {
        return this.readFrom(it)
      }
      return this
    }

    override fun readFrom(model: TournamentConfig?): Builder {
      if (model == null) {
        return this
      }
      model.sortOrder?.let { it -> setTournamentSortOrder(it) }
      model.scoreType?.let { it -> setTournamentScoreType(it) }
      model.endTime?.let { it -> setTournamentEndTime(it) }
      model.title?.let { it -> setTournamentTitle(it) }
      setTournamentPayload(model.payload)
      return this
    }
  }

  companion object CREATOR : Parcelable.Creator<TournamentConfig> {
    override fun createFromParcel(parcel: Parcel): TournamentConfig {
      return TournamentConfig(parcel)
    }

    override fun newArray(size: Int): Array<TournamentConfig?> {
      return arrayOfNulls(size)
    }
  }
}
