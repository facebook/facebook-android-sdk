/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.model;

import android.os.Parcel;
import androidx.annotation.Nullable;
import com.facebook.share.model.ShareModel;
import com.facebook.share.model.ShareModelBuilder;

public class ContextCreateContent implements ShareModel {

  private final @Nullable String suggestedPlayerID;

  private ContextCreateContent(final Builder builder) {
    this.suggestedPlayerID = builder.suggestedPlayerID;
  }

  ContextCreateContent(final Parcel parcel) {
    this.suggestedPlayerID = parcel.readString();
  }

  public @Nullable String getSuggestedPlayerID() {
    return this.suggestedPlayerID;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    out.writeString(this.suggestedPlayerID);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  /** Builder class for a concrete instance of ContextCreateContent */
  public static class Builder implements ShareModelBuilder<ContextCreateContent, Builder> {
    private @Nullable String suggestedPlayerID;

    /**
     * Sets the string of the id of the suggested player
     *
     * @param suggestedPlayerID string of the id of the suggested player
     * @return the builder
     */
    public Builder setSuggestedPlayerID(final @Nullable String suggestedPlayerID) {
      this.suggestedPlayerID = suggestedPlayerID;
      return this;
    }

    @Override
    public ContextCreateContent build() {
      return new ContextCreateContent(this);
    }

    @Override
    public Builder readFrom(final ContextCreateContent content) {
      if (content == null) {
        return this;
      }
      return this.setSuggestedPlayerID(content.getSuggestedPlayerID());
    }

    Builder readFrom(final Parcel parcel) {
      return this.readFrom(
          (ContextCreateContent)
              parcel.readParcelable(ContextCreateContent.class.getClassLoader()));
    }
  }
}
