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
import java.util.Collections;
import java.util.List;

public class ContextChooseContent implements ShareModel {

  private final @Nullable List<String> filters;
  private final @Nullable Integer maxSize;
  private final @Nullable Integer minSize;

  private ContextChooseContent(final Builder builder) {
    this.filters = builder.filters;
    this.maxSize = builder.maxSize;
    this.minSize = builder.minSize;
  }

  ContextChooseContent(final Parcel parcel) {
    this.filters = parcel.createStringArrayList();
    this.maxSize = parcel.readInt();
    this.minSize = parcel.readInt();
  }

  public @Nullable List<String> getFilters() {
    return this.filters != null ? Collections.unmodifiableList(this.filters) : null;
  }

  public @Nullable Integer getMaxSize() {
    return this.maxSize;
  }

  public @Nullable Integer getMinSize() {
    return this.minSize;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    out.writeStringList(this.filters);
    out.writeInt(this.maxSize);
    out.writeInt(this.minSize);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  /** Builder class for a concrete instance of ContextChooseContent */
  public static class Builder implements ShareModelBuilder<ContextChooseContent, Builder> {
    private @Nullable List<String> filters;
    private @Nullable Integer maxSize;
    private @Nullable Integer minSize;

    /**
     * Sets the set of filters to apply to the context suggestions.
     *
     * @param filters the set of filter to apply
     * @return the builder
     */
    public Builder setFilters(final @Nullable List<String> filters) {
      this.filters = filters;
      return this;
    }

    /**
     * Sets the maximum number of participants that a suggested context should ideally have.
     *
     * @param maxSize the maximum number of participants
     * @return the builder
     */
    public Builder setMaxSize(final @Nullable Integer maxSize) {
      this.maxSize = maxSize;
      return this;
    }

    /**
     * Sets the minimum number of participants that a suggested context should ideally have.
     *
     * @param minSize the minimum number of participants
     * @return the builder
     */
    public Builder setMinSize(final @Nullable Integer minSize) {
      this.minSize = minSize;
      return this;
    }

    @Override
    public ContextChooseContent build() {
      return new ContextChooseContent(this);
    }

    @Override
    public Builder readFrom(final ContextChooseContent content) {
      if (content == null) {
        return this;
      }
      return this.setFilters(content.getFilters())
          .setMaxSize(content.getMaxSize())
          .setMinSize(content.getMinSize());
    }

    Builder readFrom(final Parcel parcel) {
      return this.readFrom(
          (ContextChooseContent)
              parcel.readParcelable(ContextChooseContent.class.getClassLoader()));
    }
  }
}
