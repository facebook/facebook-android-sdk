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
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;

/**
 * Provide a model for sharing a generic template element to Messenger. This allows specifying
 * title, subtitle, image, default action, and any other button. Title is required. See
 * https://developers.facebook.com/docs/messenger-platform/send-messages/template/generic for more
 * details.
 *
 * @deprecated Sharing to Messenger via the SDK is unsupported.
 *     https://developers.facebook.com/docs/messenger-platform/changelog/#20190610. Sharing should
 *     be performed by the native share sheet."
 */
@Deprecated
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public final class ShareMessengerGenericTemplateElement implements ShareModel {

  private final String title;
  private final String subtitle;
  private final Uri imageUrl;
  private final ShareMessengerActionButton defaultAction;
  private final ShareMessengerActionButton button;

  private ShareMessengerGenericTemplateElement(final Builder builder) {
    this.title = builder.title;
    this.subtitle = builder.subtitle;
    this.imageUrl = builder.imageUrl;
    this.defaultAction = builder.defaultAction;
    this.button = builder.button;
  }

  ShareMessengerGenericTemplateElement(final Parcel in) {
    this.title = in.readString();
    this.subtitle = in.readString();
    this.imageUrl = in.readParcelable(Uri.class.getClassLoader());
    this.defaultAction = in.readParcelable(ShareMessengerActionButton.class.getClassLoader());
    this.button = in.readParcelable(ShareMessengerActionButton.class.getClassLoader());
  }

  /** Get the rendered title for the shared generic template element. */
  public String getTitle() {
    return title;
  }

  /** Get the rendered subtitle for the shared generic template element. */
  public String getSubtitle() {
    return subtitle;
  }

  /** Get the image url that will be downloaded and rendered at the top of the generic template. */
  public Uri getImageUrl() {
    return imageUrl;
  }

  /** Get the default action executed when this shared generic template is tapped. */
  public ShareMessengerActionButton getDefaultAction() {
    return defaultAction;
  }

  /** Get the button shown on the shared generic template. */
  public ShareMessengerActionButton getButton() {
    return button;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.title);
    dest.writeString(this.subtitle);
    dest.writeParcelable(this.imageUrl, flags);
    dest.writeParcelable(this.defaultAction, flags);
    dest.writeParcelable(this.button, flags);
  }

  @SuppressWarnings("unused")
  public static final Creator<ShareMessengerGenericTemplateElement> CREATOR =
      new Creator<ShareMessengerGenericTemplateElement>() {
        public ShareMessengerGenericTemplateElement createFromParcel(final Parcel in) {
          return new ShareMessengerGenericTemplateElement(in);
        }

        public ShareMessengerGenericTemplateElement[] newArray(final int size) {
          return new ShareMessengerGenericTemplateElement[size];
        }
      };

  /** Builder for the {@link ShareMessengerGenericTemplateElement} class. */
  public static class Builder
      implements ShareModelBuilder<ShareMessengerGenericTemplateElement, Builder> {

    private String title;
    private String subtitle;
    private Uri imageUrl;
    private ShareMessengerActionButton defaultAction;
    private ShareMessengerActionButton button;

    /** Set the rendered title for the shared generic template element. Required. */
    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    /** Set the rendered subtitle for the shared generic template element. Optional. */
    public Builder setSubtitle(String subtitle) {
      this.subtitle = subtitle;
      return this;
    }

    /**
     * Set the image url that will be downloaded and rendered at the top of the generic template.
     * Optional.
     */
    public Builder setImageUrl(Uri imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    /** Set the default action executed when this shared generic template is tapped. Optional. */
    public Builder setDefaultAction(ShareMessengerActionButton defaultAction) {
      this.defaultAction = defaultAction;
      return this;
    }

    /** Set the button to append to the bottom of the generic template. */
    public Builder setButton(ShareMessengerActionButton button) {
      this.button = button;
      return this;
    }

    @Override
    public ShareMessengerGenericTemplateElement build() {
      return new ShareMessengerGenericTemplateElement(this);
    }

    @Override
    public Builder readFrom(ShareMessengerGenericTemplateElement model) {
      if (model == null) {
        return this;
      }

      return this.setTitle(model.title)
          .setSubtitle(model.subtitle)
          .setImageUrl(model.imageUrl)
          .setDefaultAction(model.defaultAction)
          .setButton(model.button);
    }

    Builder readFrom(final Parcel parcel) {
      return this.readFrom(
          (ShareMessengerGenericTemplateElement)
              parcel.readParcelable(ShareMessengerGenericTemplateElement.class.getClassLoader()));
    }
  }
}
