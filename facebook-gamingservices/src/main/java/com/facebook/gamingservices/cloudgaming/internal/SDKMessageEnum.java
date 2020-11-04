// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.gamingservices.cloudgaming.internal;

import androidx.annotation.Nullable;

public enum SDKMessageEnum {
  OPEN_PLAY_STORE("openPlayStore"),
  OPEN_APP_STORE("openAppStore"),
  MARK_GAME_LOADED("markGameLoaded"),
  GET_PLAYER_DATA("getPlayerData"),
  SET_PLAYER_DATA("setPlayerData"),
  GET_CATALOG("getCatalog"),
  GET_PURCHASES("getPurchases"),
  PURCHASE("purchase"),
  CONSUME_PURCHASE("consumePurchase"),
  ON_READY("onReady"),
  LOAD_INTERSTITIAL_AD("loadInterstitialAd"),
  LOAD_REWARDED_VIDEO("loadRewardedVideo"),
  SHOW_INTERSTITIAL_AD("showInterstitialAd"),
  SHOW_REWARDED_VIDEO("showRewardedVideo"),
  GET_ACCESS_TOKEN("getAccessToken"),
  GET_CONTEXT_TOKEN("getContextToken"),
  GET_PAYLOAD("getPayload"),
  IS_ENV_READY("isEnvReady"),
  SHARE("share"),
  CAN_CREATE_SHORTCUT("canCreateShortcut"),
  CREATE_SHORTCUT("createShortcut"),
  OPEN_GAMING_SERVICES_DEEP_LINK("openGamingServicesDeepLink"),
  OPEN_GAME_REQUESTS_DIALOG("openGameRequestsDialog"),
  OPEN_CG_E2E_DIALOG("openCGE2EDialog");

  private final String mStringValue;

  SDKMessageEnum(String stringValue) {
    this.mStringValue = stringValue;
  }

  @Override
  public String toString() {
    return mStringValue;
  }

  public static @Nullable SDKMessageEnum fromString(String messageType) {
    for (SDKMessageEnum messageEnum : SDKMessageEnum.values()) {
      if (messageEnum.toString().equals(messageType)) {
        return messageEnum;
      }
    }
    return null;
  }
}
