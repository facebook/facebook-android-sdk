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

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Describes link content to be shared.
 *
 * <p>Use {@link ShareLinkContent.Builder} to build instances.
 *
 * <p>See documentation for <a
 * href="https://developers.facebook.com/docs/sharing/best-practices">best practices</a>.
 */
public final class ShareLinkContent
    extends ShareContent<ShareLinkContent, ShareLinkContent.Builder> {
  private final String quote;

  private ShareLinkContent(final Builder builder) {
    super(builder);
    this.quote = builder.quote;
  }

  ShareLinkContent(final Parcel in) {
    super(in);
    this.quote = in.readString();
  }

  /**
   * The quoted text to display for this link.
   *
   * @return The text quoted from the link.
   */
  @Nullable
  public String getQuote() {
    return this.quote;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(final Parcel out, final int flags) {
    super.writeToParcel(out, flags);
    out.writeString(this.quote);
  }

  @SuppressWarnings("unused")
  public static final Creator<ShareLinkContent> CREATOR =
      new Creator<ShareLinkContent>() {
        public ShareLinkContent createFromParcel(final Parcel in) {
          return new ShareLinkContent(in);
        }

        public ShareLinkContent[] newArray(final int size) {
          return new ShareLinkContent[size];
        }
      };

  /** Builder for the {@link ShareLinkContent} interface. */
  public static final class Builder extends ShareContent.Builder<ShareLinkContent, Builder> {
    static final String TAG = Builder.class.getSimpleName();

    private String quote;

    /**
     * Set the quote to display for this link.
     *
     * @param quote The text quoted from the link.
     * @return The builder.
     */
    public Builder setQuote(@Nullable final String quote) {
      this.quote = quote;
      return this;
    }

    @Override
    public ShareLinkContent build() {
      return new ShareLinkContent(this);
    }

    @Override
    public Builder readFrom(final ShareLinkContent model) {
      if (model == null) {
        return this;
      }
      return super.readFrom(model).setQuote(model.getQuote());
    }
  }
}
