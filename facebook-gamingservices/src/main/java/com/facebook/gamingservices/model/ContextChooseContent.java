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

  ContextChooseContent(final Parcel in) {
    this.filters = in.createStringArrayList();
    this.maxSize = in.readInt();
    this.minSize = in.readInt();
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
