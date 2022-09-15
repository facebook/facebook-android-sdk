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

import android.net.Uri
import android.os.Parcel
import java.util.Collections
import kotlin.collections.ArrayList

/**
 * Provides the base class for content to be shared. Contains all common methods for the different
 * types of content.
 */
abstract class ShareContent<M : ShareContent<M, B>, B : ShareContent.Builder<M, B>> : ShareModel {
  /**
   * URL for the content being shared. This URL will be checked for app link meta tags for linking
   * in platform specific ways.
   *
   * See documentation for [AppLinks](https://developers.facebook.com/docs/applinks/).
   *
   * @return [android.net.Uri] representation of the content link.
   */
  val contentUrl: Uri?

  /**
   * List of Ids for taggable people to tag with this content.
   *
   * See documentation for
   * [Taggable Friends](https://developers.facebook.com/docs/graph-api/reference/user/taggable_friends)
   * .
   *
   * @return [java.util.List] of Ids for people to tag.
   */
  val peopleIds: List<String>?

  /**
   * The Id for a place to tag with this content.
   *
   * @return The Id for the place to tag.
   */
  val placeId: String?

  /**
   * For shares into Messenger, this pageID will be used to map the app to page and attach
   * attribution to the share.
   *
   * @return The ID of the Facebook page this share is associated with.
   */
  val pageId: String?

  /**
   * A value to be added to the referrer URL when a person follows a link from this shared content
   * on feed.
   *
   * @return The ref for the content.
   */
  val ref: String?

  /**
   * Gets the ShareHashtag, if one has been set, for this content.
   *
   * @return The hashtag
   */
  val shareHashtag: ShareHashtag?

  protected constructor(builder: Builder<M, B>) : super() {
    contentUrl = builder.contentUrl
    peopleIds = builder.peopleIds
    placeId = builder.placeId
    pageId = builder.pageId
    ref = builder.ref
    shareHashtag = builder.hashtag
  }

  protected constructor(parcel: Parcel) {
    contentUrl = parcel.readParcelable(Uri::class.java.classLoader)
    peopleIds = readUnmodifiableStringList(parcel)
    placeId = parcel.readString()
    pageId = parcel.readString()
    ref = parcel.readString()
    shareHashtag = ShareHashtag.Builder().readFrom(parcel).build()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeParcelable(contentUrl, 0)
    out.writeStringList(peopleIds)
    out.writeString(placeId)
    out.writeString(pageId)
    out.writeString(ref)
    out.writeParcelable(shareHashtag, 0)
  }

  private fun readUnmodifiableStringList(parcel: Parcel): List<String>? {
    val list: List<String> = ArrayList()
    parcel.readStringList(list)
    return if (list.isEmpty()) null else Collections.unmodifiableList(list)
  }

  /** Abstract builder for [com.facebook.share.model.ShareContent] */
  abstract class Builder<M : ShareContent<M, B>, B : Builder<M, B>> : ShareModelBuilder<M, B> {
    internal var contentUrl: Uri? = null
    internal var peopleIds: List<String>? = null
    internal var placeId: String? = null
    internal var pageId: String? = null
    internal var ref: String? = null
    internal var hashtag: ShareHashtag? = null

    /**
     * Set the URL for the content being shared.
     *
     * @param contentUrl [android.net.Uri] representation of the content link.
     * @return The builder.
     */
    fun setContentUrl(contentUrl: Uri?): B {
      this.contentUrl = contentUrl
      return this as B
    }

    /**
     * Set the list of Ids for taggable people to tag with this content.
     *
     * @param peopleIds [java.util.List] of Ids for people to tag.
     * @return The builder.
     */
    fun setPeopleIds(peopleIds: List<String>?): B {
      this.peopleIds = if (peopleIds == null) null else Collections.unmodifiableList(peopleIds)
      return this as B
    }

    /**
     * Set the Id for a place to tag with this content.
     *
     * @param placeId The Id for the place to tag.
     * @return The builder.
     */
    fun setPlaceId(placeId: String?): B {
      this.placeId = placeId
      return this as B
    }

    /**
     * Set the Id of the Facebook page this share is associated with.
     *
     * @param pageId The Id for the Page
     * @return The builder
     */
    fun setPageId(pageId: String?): B {
      this.pageId = pageId
      return this as B
    }

    /**
     * Set the value to be added to the referrer URL when a person follows a link from this shared
     * content on feed.
     *
     * @param ref The ref for the content.
     * @return The builder.
     */
    fun setRef(ref: String?): B {
      this.ref = ref
      return this as B
    }

    /**
     * Set the ShareHashtag for this content
     *
     * @param shareHashtag The hashtag for this content
     * @return The builder
     */
    fun setShareHashtag(shareHashtag: ShareHashtag?): B {
      hashtag = shareHashtag
      return this as B
    }

    override fun readFrom(content: M?): B {
      return if (content == null) {
        this as B
      } else
          setContentUrl(content.contentUrl)
              .setPeopleIds(content.peopleIds)
              .setPlaceId(content.placeId)
              .setPageId(content.pageId)
              .setRef(content.ref)
              .setShareHashtag(content.shareHashtag)
    }
  }
}
