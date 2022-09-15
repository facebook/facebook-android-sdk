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
import android.os.Parcelable

/** Provides a data model class for a Messenger share URL action button. */
class ShareMessengerURLActionButton : ShareMessengerActionButton {
  /** The display height ratio of the webview when shown in the Messenger app. */
  enum class WebviewHeightRatio {
    /** The webview will cover 100% screen. */
    WebviewHeightRatioFull,

    /** The webview will cover 75% of the screen. */
    WebviewHeightRatioTall,

    /** The webview will cover 50% of the screen. */
    WebviewHeightRatioCompact
  }

  /** Get the URL that this button should open when tapped. */
  val url: Uri?
  /** Get the fallback URL of the button. */
  val fallbackUrl: Uri?
  /** Get whether the URL is enabled with Messenger Extensions. */
  val isMessengerExtensionURL: Boolean
  /** Get whether the webview shows the share button. */
  val shouldHideWebviewShareButton: Boolean
  /** Get the display height ratio of browser. */
  val webviewHeightRatio: WebviewHeightRatio?

  @Deprecated(
      "getIsMessengerExtensionURL is deprecated. Use isMessengerExtensionURL instead",
      replaceWith = ReplaceWith("isMessengerExtensionURL"))
  fun getIsMessengerExtensionURL(): Boolean = isMessengerExtensionURL

  private constructor(builder: Builder) : super(builder) {
    url = builder.url
    isMessengerExtensionURL = builder.isMessengerExtensionURL
    fallbackUrl = builder.fallbackUrl
    webviewHeightRatio = builder.webviewHeightRatio
    shouldHideWebviewShareButton = builder.shouldHideWebviewShareButton
  }

  internal constructor(parcel: Parcel) : super(parcel) {
    url = parcel.readParcelable(Uri::class.java.classLoader)
    isMessengerExtensionURL = parcel.readByte().toInt() != 0
    fallbackUrl = parcel.readParcelable(Uri::class.java.classLoader)
    webviewHeightRatio = parcel.readSerializable() as WebviewHeightRatio?
    shouldHideWebviewShareButton = parcel.readByte().toInt() != 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    super.writeToParcel(dest, flags)

    dest.writeParcelable(url, 0)
    dest.writeByte((if (isMessengerExtensionURL) 1 else 0).toByte())
    dest.writeParcelable(fallbackUrl, 0)
    dest.writeSerializable(webviewHeightRatio)
    dest.writeByte((if (isMessengerExtensionURL) 1 else 0).toByte())
  }

  /** Builder class for [ShareMessengerURLActionButton] class. */
  class Builder : ShareMessengerActionButton.Builder<ShareMessengerURLActionButton, Builder>() {
    internal var url: Uri? = null
    internal var isMessengerExtensionURL = false
    internal var fallbackUrl: Uri? = null
    internal var webviewHeightRatio: WebviewHeightRatio? = null
    internal var shouldHideWebviewShareButton = false

    /** Set the URL of this action button. */
    fun setUrl(url: Uri?): Builder {
      this.url = url
      return this
    }

    /**
     * Set whether the url is a Messenger Extensions url. This must be true if the URL is a
     * Messenger Extensions url. Defaults to NO.
     */
    fun setIsMessengerExtensionURL(isMessengerExtensionURL: Boolean): Builder {
      this.isMessengerExtensionURL = isMessengerExtensionURL
      return this
    }

    /**
     * Set the fallback URL for a Messenger Extensions enabled button. This is a fallback url for a
     * Messenger Extensions enabled button. It is used on clients that do not support Messenger
     * Extensions. If this is not defined, the url will be used as a fallback. Optional, but ignored
     * unless messengerExtensions == YES.
     */
    fun setFallbackUrl(fallbackUrl: Uri?): Builder {
      this.fallbackUrl = fallbackUrl
      return this
    }

    /**
     * Set the display height ratio of the webview when shown in the Messenger app. This controls
     * the display height of the webview when shown in the Messenger app. Defaults to Full
     */
    fun setWebviewHeightRatio(webviewHeightRatio: WebviewHeightRatio?): Builder {
      this.webviewHeightRatio = webviewHeightRatio
      return this
    }

    /**
     * Set whether we want to hide the share button in the webview or not. This controls whether we
     * want to hide the share button in the webview or not. It is useful to hide the share button
     * when the webview is user-specific and contains sensitive information Defaults to NO.
     */
    fun setShouldHideWebviewShareButton(shouldHideWebviewShareButton: Boolean): Builder {
      this.shouldHideWebviewShareButton = shouldHideWebviewShareButton
      return this
    }

    override fun readFrom(model: ShareMessengerURLActionButton?): Builder {
      return if (model == null) {
        this
      } else
          setUrl(model.url)
              .setIsMessengerExtensionURL(model.isMessengerExtensionURL)
              .setFallbackUrl(model.fallbackUrl)
              .setWebviewHeightRatio(model.webviewHeightRatio)
              .setShouldHideWebviewShareButton(model.shouldHideWebviewShareButton)
    }

    override fun build(): ShareMessengerURLActionButton {
      return ShareMessengerURLActionButton(this)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareMessengerURLActionButton> =
        object : Parcelable.Creator<ShareMessengerURLActionButton> {
          override fun createFromParcel(parcel: Parcel): ShareMessengerURLActionButton {
            return ShareMessengerURLActionButton(parcel)
          }

          override fun newArray(size: Int): Array<ShareMessengerURLActionButton?> {
            return arrayOfNulls(size)
          }
        }
  }
}
