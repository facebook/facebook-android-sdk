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

public class ContextCreateContent implements ShareModel {

  private final @Nullable String suggestedPlayerIDs;

  private ContextCreateContent(final Builder builder) {
    this.suggestedPlayerIDs = builder.suggestedPlayerIDs;
  }

  ContextCreateContent(final Parcel in) {
    this.suggestedPlayerIDs = in.readString();
  }

  public @Nullable String getSuggestedPlayerIDs() {
    return this.suggestedPlayerIDs;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags) {
    out.writeString(this.suggestedPlayerIDs);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  /** Builder class for a concrete instance of ContextCreateContent */
  public static class Builder implements ShareModelBuilder<ContextCreateContent, Builder> {
    private @Nullable String suggestedPlayerIDs;

    /**
     * Sets the string of one or more suggested playerIDs.
     *
     * @param suggestedPlayerIDs string of suggested playerIDs
     * @return the builder
     */
    public Builder setSuggestedPlayerIDs(final @Nullable String suggestedPlayerIDs) {
      this.suggestedPlayerIDs = suggestedPlayerIDs;
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
      return this.setSuggestedPlayerIDs(content.getSuggestedPlayerIDs());
    }

    Builder readFrom(final Parcel parcel) {
      return this.readFrom(
          (ContextCreateContent)
              parcel.readParcelable(ContextCreateContent.class.getClassLoader()));
    }
  }
}
