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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookDialog
import com.facebook.FacebookException
import com.facebook.FacebookSdk.isDebugEnabled
import com.facebook.FacebookSdk.isFacebookRequestCode
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Utility.areObjectsEqual

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
abstract class FacebookDialogBase<CONTENT, RESULT> : FacebookDialog<CONTENT, RESULT> {
  private val activity: Activity?
  private val fragmentWrapper: FragmentWrapper?
  private var modeHandlers: List<ModeHandler>? = null
  private var requestCodeField: Int

  @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal var callbackManager: CallbackManager? = null
  /**
   * Set the callback manager that will handle callbacks for this dialog. It will be used if the
   * androidx activity result APIs are available.
   */
  fun setCallbackManager(callbackManager: CallbackManager?) {
    this.callbackManager = callbackManager
  }

  protected constructor(activity: Activity, requestCode: Int) {
    this.activity = activity
    fragmentWrapper = null
    requestCodeField = requestCode
    callbackManager = null
  }

  protected constructor(fragmentWrapper: FragmentWrapper, requestCode: Int) {
    this.fragmentWrapper = fragmentWrapper
    activity = null
    requestCodeField = requestCode
    requireNotNull(fragmentWrapper.activity) {
      "Cannot use a fragment that is not attached to an activity"
    }
  }

  protected constructor(requestCode: Int) {
    requestCodeField = requestCode
    activity = null
    fragmentWrapper = null
  }

  override fun registerCallback(
      callbackManager: CallbackManager,
      callback: FacebookCallback<RESULT>
  ) {
    if (callbackManager !is CallbackManagerImpl) {
      throw FacebookException("Unexpected CallbackManager, " + "please use the provided Factory.")
    }
    memorizeCallbackManager(callbackManager)
    registerCallbackImpl(callbackManager, callback)
  }

  override fun registerCallback(
      callbackManager: CallbackManager,
      callback: FacebookCallback<RESULT>,
      requestCode: Int
  ) {
    memorizeCallbackManager(callbackManager)
    this.requestCode = requestCode
    registerCallback(callbackManager, callback)
  }

  protected abstract fun registerCallbackImpl(
      callbackManager: CallbackManagerImpl,
      callback: FacebookCallback<RESULT>
  )

  /** Request code used for this dialog. */
  var requestCode: Int
    get() = requestCodeField
    /**
     * Set the request code for the startActivityForResult call. The requestCode should be outside
     * of the range of those reserved for the Facebook SDK [ ]
     * [com.facebook.FacebookSdk.isFacebookRequestCode].
     *
     * @param value the request code to use.
     */
    set(value) {
      require(!isFacebookRequestCode(value)) {
        ("Request code $value cannot be within the range reserved by the Facebook SDK.")
      }
      requestCodeField = value
    }

  override fun canShow(content: CONTENT): Boolean {
    return canShowImpl(content, BASE_AUTOMATIC_MODE)
  }

  // Pass in BASE_AUTOMATIC_MODE when Automatic mode choice is desired
  protected open fun canShowImpl(content: CONTENT, mode: Any): Boolean {
    val anyModeAllowed = mode === BASE_AUTOMATIC_MODE
    for (handler in cachedModeHandlers()) {
      if (!anyModeAllowed && !areObjectsEqual(handler.mode, mode)) {
        continue
      }
      // Calls to canShow() are not best effort like calls to show() are. So let's signal
      // more explicitly whether the passed in content can be shown or not
      if (handler.canShow(content, false /*isBestEffort*/)) {
        return true
      }
    }
    return false
  }

  override fun show(content: CONTENT) {
    showImpl(content, BASE_AUTOMATIC_MODE)
  }

  protected fun createActivityResultContractForShowingDialog(
      callbackManager: CallbackManager?,
      mode: Any
  ): ActivityResultContract<CONTENT, CallbackManager.ActivityResultParameters> {
    return object : ActivityResultContract<CONTENT, CallbackManager.ActivityResultParameters>() {
      override fun createIntent(context: Context, content: CONTENT): Intent {
        val appCall = createAppCallForMode(content, mode)
        val intent = appCall?.requestIntent
        if (intent != null) {
          appCall.setPending()
          return intent
        } else {
          throw FacebookException("Content $content is not supported")
        }
      }

      override fun parseResult(
          resultCode: Int,
          intent: Intent?
      ): CallbackManager.ActivityResultParameters {
        callbackManager?.onActivityResult(requestCode, resultCode, intent)
        return CallbackManager.ActivityResultParameters(requestCode, resultCode, intent)
      }
    }
  }

  override fun createActivityResultContractForShowingDialog(
      callbackManager: CallbackManager?
  ): ActivityResultContract<CONTENT, CallbackManager.ActivityResultParameters> {
    return createActivityResultContractForShowingDialog(callbackManager, BASE_AUTOMATIC_MODE)
  }

  // Pass in BASE_AUTOMATIC_MODE when Automatic mode choice is desired
  protected open fun showImpl(content: CONTENT, mode: Any) {
    val appCall = createAppCallForMode(content, mode)
    if (appCall != null) {
      if (activityContext is ActivityResultRegistryOwner) {
        val registryOwner = activityContext as ActivityResultRegistryOwner
        DialogPresenter.present(appCall, registryOwner.activityResultRegistry, callbackManager)
        appCall.setPending()
      } else if (fragmentWrapper != null) {
        DialogPresenter.present(appCall, fragmentWrapper)
      } else if (activity != null) {
        DialogPresenter.present(appCall, activity)
      }
    } else {
      // If we got a null appCall, then the derived dialog code is doing something wrong
      val errorMessage = "No code path should ever result in a null appCall"
      Log.e(TAG, errorMessage)
      check(!isDebugEnabled()) { errorMessage }
    }
  }

  protected val activityContext: Activity?
    get() = activity ?: fragmentWrapper?.activity

  protected fun startActivityForResult(intent: Intent, requestCode: Int) {
    var error: String? = null
    val activity = activityContext
    if (activity is ActivityResultRegistryOwner) {
      DialogPresenter.startActivityForResultWithAndroidX(
          (activity as ActivityResultRegistryOwner).activityResultRegistry,
          callbackManager,
          intent,
          requestCode)
    } else if (activity != null) {
      activity.startActivityForResult(intent, requestCode)
    } else if (fragmentWrapper != null) {
      fragmentWrapper.startActivityForResult(intent, requestCode)
    } else {
      error = "Failed to find Activity or Fragment to startActivityForResult "
    }
    if (error != null) {
      log(LoggingBehavior.DEVELOPER_ERRORS, Log.ERROR, this.javaClass.name, error)
    }
  }

  private fun createAppCallForMode(content: CONTENT, mode: Any): AppCall? {
    val anyModeAllowed = mode === BASE_AUTOMATIC_MODE
    var appCall: AppCall? = null
    for (handler in cachedModeHandlers()) {
      if (!anyModeAllowed && !areObjectsEqual(handler.mode, mode)) {
        continue
      }
      if (!handler.canShow(content, true /*isBestEffort*/)) {
        continue
      }
      try {
        appCall = handler.createAppCall(content)
      } catch (e: FacebookException) {
        appCall = createBaseAppCall()
        DialogPresenter.setupAppCallForValidationError(appCall, e)
      }
      break
    }
    if (appCall == null) {
      appCall = createBaseAppCall()
      DialogPresenter.setupAppCallForCannotShowError(appCall)
    }
    return appCall
  }

  private fun memorizeCallbackManager(callbackManager: CallbackManager?) {
    if (this.callbackManager == null) {
      this.callbackManager = callbackManager
    } else if (this.callbackManager !== callbackManager) {
      Log.w(
          TAG,
          "You're registering a callback on a Facebook dialog with two different callback managers. " +
              "It's almost wrong and may cause unexpected results. " +
              "Only the first callback manager will be used for handling activity result with androidx.")
    }
  }

  private fun cachedModeHandlers(): List<ModeHandler> {
    if (modeHandlers == null) {
      modeHandlers = orderedModeHandlers
    }
    return modeHandlers as List<ModeHandler>
  }

  protected abstract val orderedModeHandlers: List<ModeHandler>
  protected abstract fun createBaseAppCall(): AppCall
  protected abstract inner class ModeHandler {
    /** @return An object to signify a specific dialog-mode. */
    open var mode: Any = BASE_AUTOMATIC_MODE

    /**
     * Used when we want to signal back to the caller when required and optional features are not
     * supported by specific Mode Handlers.
     *
     * @param content Content to be checked
     * @param isBestEffort Passing in true here will prevent signalling failure for optional or
     * best-effort types of features. Passing in false will assume that optional or best-effort
     * features should be treated the same as other features, and their support be enforced
     * accordingly.
     * @return True if can be shown
     */
    abstract fun canShow(content: CONTENT, isBestEffort: Boolean): Boolean
    abstract fun createAppCall(content: CONTENT): AppCall?
  }

  companion object {
    private const val TAG = "FacebookDialog"
    @JvmField val BASE_AUTOMATIC_MODE = Any()
  }
}
