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

package com.facebook

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.CustomTab
import com.facebook.internal.InstagramCustomTab
import com.facebook.internal.NativeProtocol.createProtocolResultIntent
import com.facebook.internal.Utility.parseUrlQueryString
import com.facebook.login.LoginTargetApp
import com.facebook.login.LoginTargetApp.Companion.fromString

class CustomTabMainActivity : Activity() {
  private var shouldCloseCustomTab = true
  private var redirectReceiver: BroadcastReceiver? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Custom Tab Redirects should not be creating a new instance of this activity
    if (CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION == intent.action) {
      setResult(RESULT_CANCELED)
      finish()
      return
    }

    if (savedInstanceState == null) {
      val action = intent.getStringExtra(EXTRA_ACTION) ?: return
      val parameters = intent.getBundleExtra(EXTRA_PARAMS)
      val chromePackage = intent.getStringExtra(EXTRA_CHROME_PACKAGE)
      val targetApp = fromString(intent.getStringExtra(EXTRA_TARGET_APP))
      val customTab: CustomTab =
          when (targetApp) {
            LoginTargetApp.INSTAGRAM -> InstagramCustomTab(action, parameters)
            else -> CustomTab(action, parameters)
          }
      val couldOpenCustomTab = customTab.openCustomTab(this, chromePackage)
      shouldCloseCustomTab = false
      if (!couldOpenCustomTab) {
        setResult(RESULT_CANCELED, intent.putExtra(NO_ACTIVITY_EXCEPTION, true))
        finish()
        return
      }

      // This activity will receive a broadcast if it can't be opened from the back stack
      val redirectReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              // Remove the custom tab on top of this activity.
              val newIntent = Intent(this@CustomTabMainActivity, CustomTabMainActivity::class.java)
              newIntent.action = REFRESH_ACTION
              newIntent.putExtra(EXTRA_URL, intent.getStringExtra(EXTRA_URL))
              newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
              startActivity(newIntent)
            }
          }
      this.redirectReceiver = redirectReceiver
      LocalBroadcastManager.getInstance(this)
          .registerReceiver(
              redirectReceiver, IntentFilter(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION))
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (REFRESH_ACTION == intent.action) {
      // The custom tab is now destroyed so we can finish the redirect activity
      val broadcast = Intent(CustomTabActivity.DESTROY_ACTION)
      LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
      sendResult(RESULT_OK, intent)
    } else if (CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION == intent.action) {
      // We have successfully redirected back to this activity. Return the result and close.
      sendResult(RESULT_OK, intent)
    }
  }

  override fun onResume() {
    super.onResume()
    if (shouldCloseCustomTab) {
      // The custom tab was closed without getting a result.
      sendResult(RESULT_CANCELED, null)
    }
    shouldCloseCustomTab = true
  }

  private fun sendResult(resultCode: Int, resultIntent: Intent?) {
    redirectReceiver?.let {
      LocalBroadcastManager.getInstance(this@CustomTabMainActivity).unregisterReceiver(it)
    }
    if (resultIntent != null) {
      val responseURL = resultIntent.getStringExtra(EXTRA_URL)
      val results = if (responseURL != null) parseResponseUri(responseURL) else Bundle()
      val nativeProtocolResultIntent = createProtocolResultIntent(intent, results, null)
      setResult(resultCode, nativeProtocolResultIntent ?: resultIntent)
    } else {
      setResult(resultCode, createProtocolResultIntent(intent, null, null))
    }
    finish()
  }

  companion object {
    @JvmField val EXTRA_ACTION = CustomTabMainActivity::class.java.simpleName + ".extra_action"
    @JvmField val EXTRA_PARAMS = CustomTabMainActivity::class.java.simpleName + ".extra_params"
    @JvmField
    val EXTRA_CHROME_PACKAGE = CustomTabMainActivity::class.java.simpleName + ".extra_chromePackage"
    @JvmField val EXTRA_URL = CustomTabMainActivity::class.java.simpleName + ".extra_url"
    @JvmField
    val EXTRA_TARGET_APP = CustomTabMainActivity::class.java.simpleName + ".extra_targetApp"
    @JvmField val REFRESH_ACTION = CustomTabMainActivity::class.java.simpleName + ".action_refresh"
    @JvmField
    val NO_ACTIVITY_EXCEPTION =
        CustomTabMainActivity::class.java.simpleName + ".no_activity_exception"

    private fun parseResponseUri(urlString: String): Bundle {
      val uri = Uri.parse(urlString)
      val bundle = parseUrlQueryString(uri.query)
      bundle.putAll(parseUrlQueryString(uri.fragment))
      return bundle
    }
  }
}
