/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Intent
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.UUID

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@AutoHandleExceptions
class AppCall
/**
 * Constructor.
 *
 * @param requestCode the request code for this app call
 */
@JvmOverloads
constructor(
    /**
     * Gets the request code for this call.
     *
     * @return the request code that will be passed to handleActivityResult upon completion.
     */
    var requestCode: Int,
    /**
     * Returns the unique ID of this call to the Facebook application.
     *
     * @return the unique ID
     */
    val callId: UUID = UUID.randomUUID()
) {

  /**
   * Returns the Intent that was used to initiate this call to the Facebook application.
   *
   * @return the Intent
   */
  var requestIntent: Intent? = null

  /**
   * @return Returns true if there was another AppCall that was already pending and is now canceled
   */
  fun setPending(): Boolean {
    return setCurrentPendingCall(this)
  }

  companion object {
    var currentPendingCall: AppCall? = null
      private set

    @Synchronized
    @JvmStatic
    fun finishPendingCall(callId: UUID, requestCode: Int): AppCall? {
      val pendingCall = currentPendingCall
      if (pendingCall == null ||
          pendingCall.callId != callId ||
          pendingCall.requestCode != requestCode) {
        return null
      }
      setCurrentPendingCall(null)
      return pendingCall
    }

    @Synchronized
    private fun setCurrentPendingCall(appCall: AppCall?): Boolean {
      val oldAppCall = currentPendingCall
      currentPendingCall = appCall
      return oldAppCall != null
    }
  }
}
