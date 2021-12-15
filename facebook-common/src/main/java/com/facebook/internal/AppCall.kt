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
