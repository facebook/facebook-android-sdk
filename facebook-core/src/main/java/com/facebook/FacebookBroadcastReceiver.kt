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
import android.os.Bundle
import com.facebook.internal.NativeProtocol
import com.facebook.internal.NativeProtocol.isErrorResult

/**
 * This class implements a simple BroadcastReceiver designed to listen for broadcast notifications
 * from the Facebook app. At present, these notifications consistent of success/failure
 * notifications for photo upload operations that happen in the background.
 *
 * Applications may subclass this class and register it in their AndroidManifest.xml. The receiver
 * is listening the com.facebook.platform.AppCallResultBroadcast action.
 */
open class FacebookBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val appCallId = intent.getStringExtra(NativeProtocol.EXTRA_PROTOCOL_CALL_ID)
    val action = intent.getStringExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION)
    val extras = intent.extras
    if (appCallId != null && action != null && extras != null) {
      if (isErrorResult(intent)) {
        onFailedAppCall(appCallId, action, extras)
      } else {
        onSuccessfulAppCall(appCallId, action, extras)
      }
    }
  }

  /**
   * Invoked when the operation was completed successfully.
   *
   * @param appCallId The App Call ID.
   * @param action The action performed.
   * @param extras Any extra information.
   */
  protected open fun onSuccessfulAppCall(appCallId: String, action: String, extras: Bundle) {
    // Default does nothing.
  }

  /**
   * Invoked when the operation failed to complete.
   *
   * @param appCallId The App Call ID.
   * @param action The action performed.
   * @param extras Any extra information.
   */
  protected open fun onFailedAppCall(appCallId: String, action: String, extras: Bundle) {
    // Default does nothing.
  }
}
