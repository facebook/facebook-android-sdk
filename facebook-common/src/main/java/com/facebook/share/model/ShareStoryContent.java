// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.share.model;

import android.os.Parcel;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes content (video, photo, sticker) to be shared into story.
 *
 * */
public final class ShareStoryContent
    extends ShareContent<ShareStoryContent, ShareStoryContent.Builder>{
  private final ShareMedia mBackgroundAsset; //could be photo or video
  private final SharePhoto mStickerAsset;
  @Nullable private final List<String> mBackgroundColorList;
  private final String mAttributionLink;


  private ShareStoryContent(final Builder builder) {
    super(builder);
    this.mBackgroundAsset = builder.mBackgroundAsset;
    this.mStickerAsset = builder.mStickerAsset;
    this.mBackgroundColorList = builder.mBackgroundColorList;
    this.mAttributionLink = builder.mAttributionLink;
  }

  ShareStoryContent(final Parcel in) {
    super(in);
    this.mBackgroundAsset = in.readParcelable(ShareMedia.class.getClassLoader());
    this.mStickerAsset = in.readParcelable(SharePhoto.class.getClassLoader());
    this.mBackgroundColorList = readUnmodifiableStringList(in);
    this.mAttributionLink = in.readString();
  }

  public ShareMedia getBackgroundAsset() {
    return this.mBackgroundAsset;
  }

  public SharePhoto getStickerAsset() {
    return this.mStickerAsset;
  }

  @Nullable public List<String> getBackgroundColorList() {
    return this.mBackgroundColorList == null?
        null : Collections.unmodifiableList(this.mBackgroundColorList);
  }

  public String getAttributionLink() {
    return this.mAttributionLink;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    super.writeToParcel(out, flags);
    out.writeParcelable(this.mBackgroundAsset, 0);
    out.writeParcelable(this.mStickerAsset, 0);
    out.writeStringList(this.mBackgroundColorList);
    out.writeString(this.mAttributionLink);
  }

  @SuppressWarnings("unused")
  public static final Creator<ShareStoryContent> CREATOR =
      new Creator<ShareStoryContent>() {
        public ShareStoryContent createFromParcel(final Parcel in) {
          return new ShareStoryContent(in);
        }

        public ShareStoryContent[] newArray(final int size) {
          return new ShareStoryContent[size];
        }
      };

  @Nullable
  private List<String> readUnmodifiableStringList(final Parcel in) {
    final List<String> list = new ArrayList<>();
    in.readStringList(list);
    return (list.isEmpty()? null : Collections.unmodifiableList(list));
  }

  /**
   * Builder for the {@Story ShareStoryContent} interface.
   */
  public static final class Builder
      extends ShareContent.Builder<ShareStoryContent, Builder> {
    static final String TAG = Builder.class.getSimpleName();
    private ShareMedia mBackgroundAsset;
    private SharePhoto mStickerAsset;
    private List<String> mBackgroundColorList;
    private String mAttributionLink;

    /**
     * Set the Background Asset to display
     * @param backgroundAsset the background asset of the story, could be a photo or video
     * @return The builder.
     */
    public Builder setBackgroundAsset(ShareMedia backgroundAsset) {
      mBackgroundAsset = backgroundAsset;
      return this;
    }

    /**
     * Set the Sticker Asset to display
     * @param stickerAsset the sticker asset of the story, should be a photo
     * @return The builder.
     */
    public Builder setStickerAsset(SharePhoto stickerAsset) {
      mStickerAsset = stickerAsset;
      return this;
    }

    /**
     * Set the background color list to display
     * @param backgroundColorList a list of color which will be draw from top to bottom
     * @return The builder.
     */
    public Builder setBackgroundColorList(List<String> backgroundColorList) {
      mBackgroundColorList = backgroundColorList;
      return this;
    }

    /**
     * Set the attribution link
     * @param attributionLink link that set by 3rd party app
     * @return The builder.
     */
    public Builder setAttributionLink(String attributionLink) {
      mAttributionLink = attributionLink;
      return this;
    }


    @Override
    public ShareStoryContent build() {
      return new ShareStoryContent(this);
    }

    @Override
    public Builder readFrom(final ShareStoryContent model) {
      if (model == null) {
        return this;
      }
      return super
          .readFrom(model)
          .setBackgroundAsset(model.getBackgroundAsset())
          .setStickerAsset(model.getStickerAsset())
          .setBackgroundColorList(model.getBackgroundColorList())
          .setAttributionLink(model.getAttributionLink())
          ;
    }
  }
}
