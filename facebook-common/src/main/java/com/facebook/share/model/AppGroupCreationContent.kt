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
