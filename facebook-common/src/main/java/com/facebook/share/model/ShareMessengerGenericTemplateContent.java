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
import android.os.Parcelable;

/**
 * Provide a model for sharing a generic template element to Messenger. This wrapper element allows
 * specifying whether or not the bubble is sharable and what aspect to render the images. See
 * https://developers.facebook.com/docs/messenger-platform/send-messages/template/generic for more
 * details.
 *
 * @deprecated Sharing to Messenger via the SDK is unsupported.
 *     https://developers.facebook.com/docs/messenger-platform/changelog/#20190610. Sharing should
 *     be performed by the native share sheet."
 */
@Deprecated
public final class ShareMessengerGenericTemplateContent
    extends ShareContent<
        ShareMessengerGenericTemplateContent, ShareMessengerGenericTemplateContent.Builder> {
  /**
   * The aspect ratio for when the image is rendered in the generic template bubble after being
   * shared
   */
  public enum ImageAspectRatio {
    /** The aspect ratio for image is 1:1.91. */
    HORIZONTAL,
    /** The aspect ratio for image is 1:1. */
    SQUARE,
  }

  private final boolean isSharable;
  private final ImageAspectRatio imageAspectRatio;
  private final ShareMessengerGenericTemplateElement genericTemplateElement;

  protected ShareMessengerGenericTemplateContent(Builder builder) {
    super(builder);
    this.isSharable = builder.isSharable;
    this.imageAspectRatio = builder.imageAspectRatio;
    this.genericTemplateElement = builder.genericTemplateElement;
  }

  ShareMessengerGenericTemplateContent(Parcel in) {
    super(in);
    this.isSharable = (in.readByte() != 0);
    this.imageAspectRatio = (ImageAspectRatio) in.readSerializable();
    this.genericTemplateElement =
        in.readParcelable(ShareMessengerGenericTemplateElement.class.getClassLoader());
  }

  /**
   * Get whether or not this generic template message can be shared again after the initial share.
   */
  public boolean getIsSharable() {
    return isSharable;
  }

  /**
   * Get the aspect ratio for when the image is rendered in the generic template bubble after being
   * shared.
   */
  public ImageAspectRatio getImageAspectRatio() {
    return imageAspectRatio;
  }

  /** Get a generic template element with a title, optional subtitle, optional image, etc. */
  public ShareMessengerGenericTemplateElement getGenericTemplateElement() {
    return genericTemplateElement;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    super.writeToParcel(out, flags);
    out.writeByte((byte) (this.isSharable ? 1 : 0));
    out.writeSerializable(this.imageAspectRatio);
    out.writeParcelable(this.genericTemplateElement, flags);
  }

  @SuppressWarnings("unused")
  public static final Parcelable.Creator<ShareMessengerGenericTemplateContent> CREATOR =
      new Parcelable.Creator<ShareMessengerGenericTemplateContent>() {

        @Override
        public ShareMessengerGenericTemplateContent createFromParcel(final Parcel source) {
          return new ShareMessengerGenericTemplateContent(source);
        }

        @Override
        public ShareMessengerGenericTemplateContent[] newArray(final int size) {
          return new ShareMessengerGenericTemplateContent[size];
        }
      };

  /** Builder for the {@link ShareMessengerGenericTemplateContent} class. */
  public static class Builder
      extends ShareContent.Builder<ShareMessengerGenericTemplateContent, Builder> {

    private boolean isSharable;
    private ImageAspectRatio imageAspectRatio;
    private ShareMessengerGenericTemplateElement genericTemplateElement;

    /**
     * Set whether or not this generic template message can be shared again after the initial share.
     * Defaults to false.
     */
    public Builder setIsSharable(boolean isSharable) {
      this.isSharable = isSharable;
      return this;
    }

    /**
     * Set the aspect ratio for when the image is rendered in the generic template bubble after
     * being shared. Defaults to horizontal.
     */
    public Builder setImageAspectRatio(ImageAspectRatio imageAspectRatio) {
      this.imageAspectRatio = imageAspectRatio;
      return this;
    }

    /**
     * Set a generic template element with a title, optional subtitle, optional image, etc.
     * Required.
     */
    public Builder setGenericTemplateElement(
        ShareMessengerGenericTemplateElement genericTemplateElement) {
      this.genericTemplateElement = genericTemplateElement;
      return this;
    }

    @Override
    public ShareMessengerGenericTemplateContent build() {
      return new ShareMessengerGenericTemplateContent(this);
    }

    @Override
    public Builder readFrom(final ShareMessengerGenericTemplateContent model) {
      if (model == null) {
        return this;
      }
      return super.readFrom(model)
          .setIsSharable(model.getIsSharable())
          .setImageAspectRatio(model.getImageAspectRatio())
          .setGenericTemplateElement(model.getGenericTemplateElement());
    }
  }
}
