/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
