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
package com.facebook.login

import android.content.ComponentName
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import java.util.concurrent.locks.ReentrantLock

/**
 * This class is for internal use. SDK users should not access it directly.
 *
 * The helper for scheduling prefetching for login custom tab.
 */
class CustomTabPrefetchHelper : CustomTabsServiceConnection() {
  override fun onCustomTabsServiceConnected(name: ComponentName, newClient: CustomTabsClient) {
    newClient.warmup(0)
    client = newClient
    prepareSession()
  }

  override fun onServiceDisconnected(componentName: ComponentName) = Unit

  companion object {
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private val lock = ReentrantLock()
    private fun prepareSession() {
      lock.lock()
      if (session == null) {
        client?.let { session = it.newSession(null) }
      }
      lock.unlock()
    }

    /**
     * Prepare the session with prefetching the provided url
     *
     * @param url The url to be prefetched
     */
    @JvmStatic
    fun mayLaunchUrl(url: Uri) {
      prepareSession()
      lock.lock()
      session?.mayLaunchUrl(url, null, null)
      lock.unlock()
    }

    /**
     * Obtain the prepared session and clear it.
     *
     * @return the session which is prepared before, null if the prepared one is already obtained.
     */
    @JvmStatic
    fun getPreparedSessionOnce(): CustomTabsSession? {
      lock.lock()
      val result = session
      session = null
      lock.unlock()
      return result
    }
  }
}
