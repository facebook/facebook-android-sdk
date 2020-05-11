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

/**
 * Provides a data model class for Messenger Media Template content to be shared.
 *
 * @deprecated Sharing to Messenger via the SDK is unsupported.
 *     https://developers.facebook.com/docs/messenger-platform/changelog/#20190610. Sharing should
 *     be performed by the native share sheet."
 */
@Deprecated
public final class ShareMessengerMediaTemplateContent
    extends ShareContent<
        ShareMessengerMediaTemplateContent, ShareMessengerMediaTemplateContent.Builder> {

  /** The media type (image or video) for this media template content. */
  public enum MediaType {
    IMAGE,
    VIDEO,
  }

  private final MediaType mediaType;
  private final String attachmentId;
  private final Uri mediaUrl;
  private final ShareMessengerActionButton button;

  private ShareMessengerMediaTemplateContent(final Builder builder) {
    super(builder);
    this.mediaType = builder.mediaType;
    this.attachmentId = builder.attachmentId;
    this.mediaUrl = builder.mediaUrl;
    this.button = builder.button;
  }

  ShareMessengerMediaTemplateContent(final Parcel in) {
    super(in);
    this.mediaType = (MediaType) in.readSerializable();
    this.attachmentId = in.readString();
    this.mediaUrl = in.readParcelable(Uri.class.getClassLoader());
    this.button = in.readParcelable(ShareMessengerActionButton.class.getClassLoader());
  }

  /** Get the media type (image or video) for this content. */
  public MediaType getMediaType() {
    return mediaType;
  }

  /** Get the attachmentID of the item to share. */
  public String getAttachmentId() {
    return attachmentId;
  }

  /** Get the Facebook URL for this piece of media. */
  public Uri getMediaUrl() {
    return mediaUrl;
  }

  /** Get the action button to show below the media. */
  public ShareMessengerActionButton getButton() {
    return button;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(this.mediaType);
    dest.writeString(this.attachmentId);
    dest.writeParcelable(this.mediaUrl, flags);
    dest.writeParcelable(this.button, flags);
  }

  @SuppressWarnings("unused")
  public static final Creator<ShareMessengerMediaTemplateContent> CREATOR =
      new Creator<ShareMessengerMediaTemplateContent>() {
        public ShareMessengerMediaTemplateContent createFromParcel(final Parcel in) {
          return new ShareMessengerMediaTemplateContent(in);
        }

        public ShareMessengerMediaTemplateContent[] newArray(final int size) {
          return new ShareMessengerMediaTemplateContent[size];
        }
      };

  /** Builder for the {@link ShareMessengerMediaTemplateContent} interface. */
  public static class Builder
      extends ShareContent.Builder<ShareMessengerMediaTemplateContent, Builder> {

    private MediaType mediaType;
    private String attachmentId;
    private Uri mediaUrl;
    private ShareMessengerActionButton button;

    /**
     * Set the media type (image or video) for this content. This must match the media type
     * specified in the attachmentID/mediaURL to avoid an error when sharing. Defaults to image.
     */
    public Builder setMediaType(MediaType mediaType) {
      this.mediaType = mediaType;
      return this;
    }

    /**
     * Set the attachmentID of the item to share. Optional, but either attachmentID or mediaURL must
     * be specified.
     */
    public Builder setAttachmentId(String attachmentId) {
      this.attachmentId = attachmentId;
      return this;
    }

    /**
     * Set the Facebook url for this piece of media. External urls will not work; this must be a
     * Facebook url.Optional, but either attachmentID or mediaURL must be specified. See
     * https://developers.facebook.com/docs/messenger-platform/send-messages/template/media for
     * details.
     */
    public Builder setMediaUrl(Uri mediaUrl) {
      this.mediaUrl = mediaUrl;
      return this;
    }

    /** Set the action button to show below the media. */
    public Builder setButton(ShareMessengerActionButton button) {
      this.button = button;
      return this;
    }

    @Override
    public Builder readFrom(final ShareMessengerMediaTemplateContent content) {
      if (content == null) {
        return this;
      }
      return super.readFrom(content)
          .setMediaType(content.getMediaType())
          .setAttachmentId(content.getAttachmentId())
          .setMediaUrl(content.getMediaUrl())
          .setButton(content.getButton());
    }

    @Override
    public ShareMessengerMediaTemplateContent build() {
      return new ShareMessengerMediaTemplateContent(this);
    }
  }
}
