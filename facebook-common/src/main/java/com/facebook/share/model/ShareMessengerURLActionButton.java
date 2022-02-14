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
package com.facebook.share.model;

import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.Nullable;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;

/** Provides a data model class for a Messenger share URL action button. */
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public final class ShareMessengerURLActionButton extends ShareMessengerActionButton {

  /** The display height ratio of the webview when shown in the Messenger app. */
  public enum WebviewHeightRatio {
    /** The webview will cover 100% screen. */
    WebviewHeightRatioFull,
    /** The webview will cover 75% of the screen. */
    WebviewHeightRatioTall,
    /** The webview will cover 50% of the screen. */
    WebviewHeightRatioCompact,
  }

  private final Uri url;
  private final Uri fallbackUrl;
  private final boolean isMessengerExtensionURL;
  private final boolean shouldHideWebviewShareButton;
  private final WebviewHeightRatio webviewHeightRatio;

  private ShareMessengerURLActionButton(Builder builder) {
    super(builder);
    this.url = builder.url;
    this.isMessengerExtensionURL = builder.isMessengerExtensionURL;
    this.fallbackUrl = builder.fallbackUrl;
    this.webviewHeightRatio = builder.webviewHeightRatio;
    this.shouldHideWebviewShareButton = builder.shouldHideWebviewShareButton;
  }

  ShareMessengerURLActionButton(final Parcel in) {
    super(in);
    this.url = in.readParcelable(Uri.class.getClassLoader());
    this.isMessengerExtensionURL = (in.readByte() != 0);
    this.fallbackUrl = in.readParcelable(Uri.class.getClassLoader());
    this.webviewHeightRatio = (WebviewHeightRatio) in.readSerializable();
    this.shouldHideWebviewShareButton = (in.readByte() != 0);
  }

  /** Get the URL that this button should open when tapped. */
  public Uri getUrl() {
    return this.url;
  }

  /** Get whether the URL is enabled with Messenger Extensions. */
  public boolean getIsMessengerExtensionURL() {
    return this.isMessengerExtensionURL;
  }

  /** Get the fallback URL of the button. */
  @Nullable
  public Uri getFallbackUrl() {
    return this.fallbackUrl;
  }

  /** Get the display height ratio of browser. */
  @Nullable
  public WebviewHeightRatio getWebviewHeightRatio() {
    return this.webviewHeightRatio;
  }

  /** Get whether the webview shows the share button. */
  public boolean getShouldHideWebviewShareButton() {
    return this.shouldHideWebviewShareButton;
  }

  @SuppressWarnings("unused")
  public static final Creator<ShareMessengerURLActionButton> CREATOR =
      new Creator<ShareMessengerURLActionButton>() {
        public ShareMessengerURLActionButton createFromParcel(final Parcel in) {
          return new ShareMessengerURLActionButton(in);
        }

        public ShareMessengerURLActionButton[] newArray(final int size) {
          return new ShareMessengerURLActionButton[size];
        }
      };

  /** Builder class for {@link ShareMessengerURLActionButton} class. */
  public static final class Builder
      extends ShareMessengerActionButton.Builder<ShareMessengerURLActionButton, Builder> {

    private Uri url;
    private boolean isMessengerExtensionURL;
    private Uri fallbackUrl;
    private WebviewHeightRatio webviewHeightRatio;
    private boolean shouldHideWebviewShareButton;

    /** Set the URL of this action button. */
    public Builder setUrl(@Nullable final Uri url) {
      this.url = url;
      return this;
    }

    /**
     * Set whether the url is a Messenger Extensions url. This must be true if the URL is a
     * Messenger Extensions url. Defaults to NO.
     */
    public Builder setIsMessengerExtensionURL(final boolean isMessengerExtensionURL) {
      this.isMessengerExtensionURL = isMessengerExtensionURL;
      return this;
    }

    /**
     * Set the fallback URL for a Messenger Extensions enabled button. This is a fallback url for a
     * Messenger Extensions enabled button. It is used on clients that do not support Messenger
     * Extensions. If this is not defined, the url will be used as a fallback. Optional, but ignored
     * unless messengerExtensions == YES.
     */
    public Builder setFallbackUrl(@Nullable final Uri fallbackUrl) {
      this.fallbackUrl = fallbackUrl;
      return this;
    }

    /**
     * Set the display height ratio of the webview when shown in the Messenger app. This controls
     * the display height of the webview when shown in the Messenger app. Defaults to Full
     */
    public Builder setWebviewHeightRatio(WebviewHeightRatio webviewHeightRatio) {
      this.webviewHeightRatio = webviewHeightRatio;
      return this;
    }

    /**
     * Set whether we want to hide the share button in the webview or not. This controls whether we
     * want to hide the share button in the webview or not. It is useful to hide the share button
     * when the webview is user-specific and contains sensitive information Defaults to NO.
     */
    public Builder setShouldHideWebviewShareButton(boolean shouldHideWebviewShareButton) {
      this.shouldHideWebviewShareButton = shouldHideWebviewShareButton;
      return this;
    }

    @Override
    public Builder readFrom(final ShareMessengerURLActionButton content) {
      if (content == null) {
        return this;
      }
      return this.setUrl(content.getUrl())
          .setIsMessengerExtensionURL(content.getIsMessengerExtensionURL())
          .setFallbackUrl(content.getFallbackUrl())
          .setWebviewHeightRatio(content.getWebviewHeightRatio())
          .setShouldHideWebviewShareButton(content.getShouldHideWebviewShareButton());
    }

    @Override
    public ShareMessengerURLActionButton build() {
      return new ShareMessengerURLActionButton(this);
    }
  }
}
