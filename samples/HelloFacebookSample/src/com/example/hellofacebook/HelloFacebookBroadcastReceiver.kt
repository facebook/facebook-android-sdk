/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hellofacebook;

import android.os.Bundle
import android.util.Log
import com.facebook.FacebookBroadcastReceiver

/**
 * This is a simple example to demonstrate how an app could extend FacebookBroadcastReceiver to
 * handle notifications that long-running operations such as photo uploads have finished.
 */
class HelloFacebookBroadcastReceiver: FacebookBroadcastReceiver() {
    override fun onSuccessfulAppCall(appCallId: String, action: String, extras: Bundle) {
        // A real app could update UI or notify the user that their photo was uploaded.
        Log.d("HelloFacebook", "Photo uploaded by call $appCallId succeeded.")
    }

    override fun onFailedAppCall(appCallId: String, action: String, extras: Bundle) {
        // A real app could update UI or notify the user that their photo was not uploaded.
        Log.d("HelloFacebook", "Photo uploaded by call $appCallId failed.")
    }
}
