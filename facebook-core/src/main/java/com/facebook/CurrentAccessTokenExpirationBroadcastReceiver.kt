/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** This class is notified when the current [AccessToken] has expired. */
class CurrentAccessTokenExpirationBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED == intent.action &&
        FacebookSdk.isInitialized()) {
      AccessTokenManager.getInstance().currentAccessTokenChanged()
    }
  }
}
