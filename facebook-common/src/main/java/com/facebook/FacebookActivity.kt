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

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.facebook.FacebookSdk.isInitialized
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.common.R
import com.facebook.internal.FacebookDialogFragment
import com.facebook.internal.NativeProtocol.createProtocolResultIntent
import com.facebook.internal.NativeProtocol.getExceptionFromErrorData
import com.facebook.internal.NativeProtocol.getMethodArgumentsFromIntent
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.internal.logging.dumpsys.EndToEndDumper
import com.facebook.login.LoginFragment
import com.facebook.share.internal.DeviceShareDialogFragment
import com.facebook.share.model.ShareContent
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * This Activity is a necessary part of the overall Facebook SDK, but is not meant to be used
 * directly. Add this Activity to your AndroidManifest.xml to ensure proper handling of Facebook SDK
 * features.
 *
 * <pre>`<activity android:name="com.facebook.FacebookActivity"
 * android:theme="@android:style/Theme.Translucent.NoTitleBar"
 * android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
 * android:label="@string/app_name" /> `</pre> *
 *
 * Do not start this activity directly.
 */
open class FacebookActivity : FragmentActivity() {
  var currentFragment: Fragment? = null
    private set

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val intent = intent

    // Some apps using this sdk don't put the sdk initialize code in the application
    // on create method. This can cause issues when opening this activity after an application
    // has been killed since the sdk won't be initialized. Attempt to initialize the sdk
    // here if it hasn't already been initialized.
    if (!isInitialized()) {
      logd(
          TAG,
          "Facebook SDK not initialized. Make sure you call sdkInitialize inside " +
              "your Application's onCreate method.")
      sdkInitialize(applicationContext)
    }
    setContentView(R.layout.com_facebook_activity_layout)
    if (PASS_THROUGH_CANCEL_ACTION == intent.action) {
      handlePassThroughError()
      return
    }
    currentFragment = getFragment()
  }

  protected open fun getFragment(): Fragment {
    val intent = intent
    val manager = supportFragmentManager
    var fragment = manager.findFragmentByTag(FRAGMENT_TAG)
    if (fragment == null) {
      if (FacebookDialogFragment.TAG == intent.action) {
        val dialogFragment = FacebookDialogFragment()
        dialogFragment.retainInstance = true
        dialogFragment.show(manager, FRAGMENT_TAG)
        fragment = dialogFragment
      } else if (DeviceShareDialogFragment.TAG == intent.action) {
        Log.w(
            TAG,
            "Please stop use Device Share Dialog, this feature has been disabled and all related classes in Facebook Android SDK will be removed from v13.0.0 release.")
        val dialogFragment = DeviceShareDialogFragment()
        dialogFragment.retainInstance = true
        dialogFragment.setShareContent(
            intent.getParcelableExtra<Parcelable>("content") as ShareContent<*, *>)
        dialogFragment.show(manager, FRAGMENT_TAG)
        fragment = dialogFragment
      } else {
        fragment = LoginFragment()
        fragment.setRetainInstance(true)
        manager
            .beginTransaction()
            .add(R.id.com_facebook_fragment_container, fragment, FRAGMENT_TAG)
            .commit()
      }
    }
    return fragment
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    currentFragment?.onConfigurationChanged(newConfig)
  }

  private fun handlePassThroughError() {
    val requestIntent = intent

    // The error we need to respond with is passed to us as method arguments.
    val errorResults = getMethodArgumentsFromIntent(requestIntent)
    val exception = getExceptionFromErrorData(errorResults)
    val resultIntent = createProtocolResultIntent(intent, null, exception)
    setResult(RESULT_CANCELED, resultIntent)
    finish()
  }

  @AutoHandleExceptions
  override fun dump(
      prefix: String,
      fd: FileDescriptor?,
      writer: PrintWriter,
      args: Array<String>?
  ) {
    if (EndToEndDumper.instance?.maybeDump(prefix, writer, args) == true) {
      return
    }
    super.dump(prefix, fd, writer, args)
  }

  companion object {
    const val PASS_THROUGH_CANCEL_ACTION = "PassThrough"
    private const val FRAGMENT_TAG = "SingleFragment"
    private val TAG = FacebookActivity::class.java.name
  }
}
