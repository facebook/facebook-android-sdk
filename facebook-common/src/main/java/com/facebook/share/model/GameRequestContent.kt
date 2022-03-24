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
import android.text.TextUtils

/** Describes the content that will be displayed by the GameRequestDialog */
class GameRequestContent : ShareModel {
  enum class ActionType {
    SEND,
    ASKFOR,
    TURN,
    INVITE
  }

  enum class Filters {
    APP_USERS,
    APP_NON_USERS,
    EVERYBODY
  }

  /** Gets the message that users receiving the request will see. */
  val message: String?
  /** Gets the cta that users receiving the request will see. */
  val cta: String?
  /** Gets the user IDs or user names the request will be sent to. */
  val recipients: List<String>?
  /** Gets the optional title for the dialog */
  val title: String?
  /** Gets optional data which can be used for tracking */
  val data: String?
  /** Gets the action type */
  val actionType: ActionType?
  /** Gets the open graph id of the object that action type will be performed on */
  val objectId: String?
  /** Get the filters */
  val filters: Filters?
  /** Gets a list of suggested user ids */
  val suggestions: List<String>?

  private constructor(builder: Builder) {
    message = builder.message
    cta = builder.cta
    recipients = builder.recipients
    title = builder.title
    data = builder.data
    actionType = builder.actionType
    objectId = builder.objectId
    filters = builder.filters
    suggestions = builder.suggestions
  }

  internal constructor(parcel: Parcel) {
    message = parcel.readString()
    cta = parcel.readString()
    recipients = parcel.createStringArrayList()
    title = parcel.readString()
    data = parcel.readString()
    actionType = parcel.readSerializable() as ActionType?
    objectId = parcel.readString()
    filters = parcel.readSerializable() as Filters?
    suggestions = parcel.createStringArrayList()
  }

  /** Gets the user IDs or user names the request will be sent to. */
  @get:Deprecated("Replaced by [getRecipients()]", replaceWith = ReplaceWith("getRecipients"))
  val to: String?
    get() = if (recipients != null) TextUtils.join(",", recipients) else null

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeString(message)
    out.writeString(cta)
    out.writeStringList(recipients)
    out.writeString(title)
    out.writeString(data)
    out.writeSerializable(actionType)
    out.writeString(objectId)
    out.writeSerializable(filters)
    out.writeStringList(suggestions)
  }

  /** Builder class for a concrete instance of GameRequestContent */
  class Builder : ShareModelBuilder<GameRequestContent, Builder> {
    internal var message: String? = null
    internal var cta: String? = null
    internal var recipients: List<String>? = null
    internal var data: String? = null
    internal var title: String? = null
    internal var actionType: ActionType? = null
    internal var objectId: String? = null
    internal var filters: Filters? = null
    internal var suggestions: List<String>? = null

    /**
     * Sets the message users receiving the request will see. The maximum length is 60 characters.
     *
     * @param message the message
     * @return the builder
     */
    fun setMessage(message: String?): Builder {
      this.message = message
      return this
    }

    /**
     * Sets the cta users receiving the request will see. The maximum length is 10 characters.
     *
     * @param cta the cta for the message
     * @return the builder
     */
    fun setCta(cta: String?): Builder {
      this.cta = cta
      return this
    }

    /**
     * Sets the user ID or user name the request will be sent to. If this is not specified, a friend
     * selector will be displayed and the user can select up to 50 friends.
     *
     * @param to the id or user name to send the request to
     * @return the builder
     */
    @Deprecated("Replaced by {@link #setRecipients(List)}")
    fun setTo(to: String?): Builder {
      if (to != null) {
        recipients = to.split(',').toList()
      }
      return this
    }

    /**
     * An array of user IDs, usernames or invite tokens of people to send request. If this is not
     * specified, a friend selector will be displayed and the user can select up to 50 friends.
     *
     * This is equivalent to the "to" parameter when using the web game request dialog.
     *
     * @param recipients the list of user ids to send the request to
     * @return the builder
     */
    fun setRecipients(recipients: List<String>?): Builder {
      this.recipients = recipients
      return this
    }

    /**
     * Sets optional data which can be used for tracking; maximum length is 255 characters.
     *
     * @param data the data
     * @return the builder
     */
    fun setData(data: String?): Builder {
      this.data = data
      return this
    }

    /**
     * Sets an optional title for the dialog; maximum length is 50 characters.
     *
     * @param title the title
     * @return the builder
     */
    fun setTitle(title: String?): Builder {
      this.title = title
      return this
    }

    /** Sets the action type for this request */
    fun setActionType(actionType: ActionType?): Builder {
      this.actionType = actionType
      return this
    }

    /**
     * Sets the open graph id of the object that action type will be performed on Only valid (and
     * required) for ActionTypes SEND, ASKFOR
     */
    fun setObjectId(objectId: String?): Builder {
      this.objectId = objectId
      return this
    }

    /** Sets the filters for everybody/app users/non app users */
    fun setFilters(filters: Filters?): Builder {
      this.filters = filters
      return this
    }

    /** Sets a list of user ids suggested as request receivers */
    fun setSuggestions(suggestions: List<String>?): Builder {
      this.suggestions = suggestions
      return this
    }

    override fun build(): GameRequestContent {
      return GameRequestContent(this)
    }

    override fun readFrom(content: GameRequestContent?): Builder {
      return if (content == null) {
        this
      } else
          setMessage(content.message)
              .setCta(content.cta)
              .setRecipients(content.recipients)
              .setTitle(content.title)
              .setData(content.data)
              .setActionType(content.actionType)
              .setObjectId(content.objectId)
              .setFilters(content.filters)
              .setSuggestions(content.suggestions)
    }

    internal fun readFrom(parcel: Parcel): Builder {
      return this.readFrom(parcel.readParcelable(GameRequestContent::class.java.classLoader))
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<GameRequestContent> =
        object : Parcelable.Creator<GameRequestContent> {
          override fun createFromParcel(parcel: Parcel): GameRequestContent {
            return GameRequestContent(parcel)
          }

          override fun newArray(size: Int): Array<GameRequestContent?> {
            return arrayOfNulls(size)
          }
        }
  }
}
