/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.facebook.gamingservices.internal.DateFormatter
import com.facebook.share.model.ShareModel
import com.facebook.share.model.ShareModelBuilder
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Tournament : ShareModel {
  @JvmField @SerializedName("id") val identifier: String
  @JvmField @SerializedName("tournament_title") val title: String?
  @JvmField @SerializedName("tournament_payload") val payload: String?
  @JvmField @SerializedName("tournament_end_time") internal var endTime: String? = null

  var expiration: ZonedDateTime?
    get() {
      return this.endTime?.let {
        DateFormatter.format(it)
      }
    }
    private set(newValue) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (newValue != null) {
          this.endTime = newValue.format(DateTimeFormatter.ISO_DATE_TIME)
          this.expiration = newValue
        }
      }
    }

  constructor(
      identifier: String,
      endTime: String?,
      title: String?,
      payload: String?,
  ) {
    this.identifier = identifier
    this.endTime = endTime
    this.title = title
    this.payload = payload
    this.expiration = this.endTime?.let { DateFormatter.format(it) }
  }

  constructor(
      parcel: Parcel
  ) : this(parcel.toString(), parcel.toString(), parcel.toString(), parcel.toString())

  internal data class Builder(
      var identifier: String,
      var expiration: ZonedDateTime? = null,
      var title: String? = null,
      var payload: String? = null,
  ) : ShareModelBuilder<Tournament, Builder> {
    var endTime: String? = null
    fun identifier(identifier: String): Builder = apply { this.identifier = identifier }
    fun expiration(expiration: ZonedDateTime?): Builder = apply {
      this.expiration = expiration
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (expiration != null) {
          this.endTime = expiration.format(DateTimeFormatter.ISO_DATE_TIME)
        }
      }
    }
    fun title(title: String?): Builder = apply { this.title = title }
    fun payload(payload: String?): Builder = apply { this.payload = payload }

    override fun build(): Tournament {
      return Tournament(identifier, endTime, title, payload)
    }

    override fun readFrom(tournament: Tournament?): Builder =
        tournament?.let {
          this.identifier(it.identifier)
              .expiration(it.expiration)
              .title(it.title)
              .payload(it.payload)
        }
            ?: run { this }
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeString(this.identifier)
    out.writeString(this.endTime)
    out.writeString(this.title)
    out.writeString(this.payload)
  }

  companion object CREATOR : Parcelable.Creator<Tournament> {
    override fun createFromParcel(parcel: Parcel): Tournament {
      return Tournament(parcel)
    }

    override fun newArray(size: Int): Array<Tournament?> {
      return arrayOfNulls(size)
    }
  }
}

class InvalidScoreTypeException : IllegalArgumentException()

class InvalidExpirationDateException : IllegalArgumentException()
