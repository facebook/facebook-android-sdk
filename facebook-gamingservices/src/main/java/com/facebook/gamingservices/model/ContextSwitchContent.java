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

public final class ContextSwitchContent implements ShareModel {

  private final @Nullable String contextID;

  private ContextSwitchContent(final Builder builder) {
    this.contextID = builder.contextID;
  }

  ContextSwitchContent(final Parcel parcel) {
    this.contextID = parcel.readString();
  }

  public @Nullable String getContextID() {
    return this.contextID;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    out.writeString(this.contextID);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  /** Builder class for a concrete instance of ContextSwitchContent */
  public static class Builder implements ShareModelBuilder<ContextSwitchContent, Builder> {
    private @Nullable String contextID;

    /**
     * Sets the context ID that the player will switch into.
     *
     * @param contextID the context ID
     * @return the builder
     */
    public Builder setContextID(final @Nullable String contextID) {
      this.contextID = contextID;
      return this;
    }

    @Override
    public ContextSwitchContent build() {
      return new ContextSwitchContent(this);
    }

    @Override
    public Builder readFrom(final ContextSwitchContent content) {
      if (content == null) {
        return this;
      }
      return this.setContextID(content.getContextID());
    }

    Builder readFrom(final Parcel parcel) {
      return this.readFrom(
          (ContextSwitchContent)
              parcel.readParcelable(ContextSwitchContent.class.getClassLoader()));
    }
  }
}
