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

/** Describes the content that will be displayed by the AppGroupCreationDialog */
class AppGroupCreationContent : ShareModel {
  /**
   * Gets the name of the group that will be created.
   *
   * @return name of the group
   */
  val name: String?

  /**
   * Gets the description of the group that will be created.
   *
   * @return the description
   */
  val description: String?

  /**
   * Gets the privacy for the group that will be created
   *
   * @return the privacy of the group
   */
  val appGroupPrivacy: AppGroupPrivacy?

  private constructor(builder: Builder) {
    name = builder.name
    description = builder.description
    appGroupPrivacy = builder.appGroupPrivacy
  }

  internal constructor(parcel: Parcel) {
    name = parcel.readString()
    description = parcel.readString()
    appGroupPrivacy = parcel.readSerializable() as AppGroupPrivacy?
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeString(name)
    out.writeString(description)
    out.writeSerializable(appGroupPrivacy)
  }

  /** Specifies the privacy of a group. */
  enum class AppGroupPrivacy {
    /** Anyone can see the group, who's in it and what members post. */
    Open,

    /** Anyone can see the group and who's in it, but only members can see posts. */
    Closed
  }

  /** Builder class for a concrete instance of AppGroupCreationContent */
  class Builder : ShareModelBuilder<AppGroupCreationContent, Builder> {
    internal var name: String? = null
    internal var description: String? = null
    internal var appGroupPrivacy: AppGroupPrivacy? = null

    /**
     * Sets the name of the group that will be created.
     *
     * @param name name of the group
     * @return the builder
     */
    fun setName(name: String?): Builder {
      this.name = name
      return this
    }

    /**
     * Sets the description of the group that will be created.
     *
     * @param description the description
     * @return the builder
     */
    fun setDescription(description: String?): Builder {
      this.description = description
      return this
    }

    /**
     * Sets the privacy for the group that will be created
     *
     * @param appGroupPrivacy privacy of the group
     * @return the builder
     */
    fun setAppGroupPrivacy(appGroupPrivacy: AppGroupPrivacy?): Builder {
      this.appGroupPrivacy = appGroupPrivacy
      return this
    }

    override fun build(): AppGroupCreationContent {
      return AppGroupCreationContent(this)
    }

    override fun readFrom(content: AppGroupCreationContent?): Builder {
      return if (content == null) {
        this
      } else
          setName(content.name)
              .setDescription(content.description)
              .setAppGroupPrivacy(content.appGroupPrivacy)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<AppGroupCreationContent> =
        object : Parcelable.Creator<AppGroupCreationContent> {
          override fun createFromParcel(parcel: Parcel): AppGroupCreationContent? {
            return AppGroupCreationContent(parcel)
          }

          override fun newArray(size: Int): Array<AppGroupCreationContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
