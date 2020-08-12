// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.gamingservices.cloudgaming.internal;

import androidx.annotation.Nullable;

public enum SDKShareIntentEnum {
  INVITE("INVITE"),
  REQUEST("REQUEST"),
  CHALLENGE("CHALLENGE"),
  SHARE("SHARE");

  private final String mStringValue;

  SDKShareIntentEnum(String stringValue) {
    this.mStringValue = stringValue;
  }

  @Override
  public String toString() {
    return mStringValue;
  }

  public static @Nullable String validate(String intentType) {
    for (SDKShareIntentEnum intentEnum : SDKShareIntentEnum.values()) {
      if (intentEnum.toString().equals(intentType)) {
        return intentType;
      }
    }
    return null;
  }

  public static @Nullable SDKShareIntentEnum fromString(String intentType) {
    for (SDKShareIntentEnum intentEnum : SDKShareIntentEnum.values()) {
      if (intentEnum.toString().equals(intentType)) {
        return intentEnum;
      }
    }
    return null;
  }
}
