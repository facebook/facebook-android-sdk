/**
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

package com.facebook.internal;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookDialog;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;

import java.util.List;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public abstract class FacebookDialogBase<CONTENT, RESULT>
        implements FacebookDialog<CONTENT, RESULT> {

    private static final String TAG = "FacebookDialog";

    protected static final Object BASE_AUTOMATIC_MODE = new Object();

    private final Activity activity;
    private final FragmentWrapper fragmentWrapper;
    private List<ModeHandler> modeHandlers;
    private int requestCode;

    protected FacebookDialogBase(final Activity activity, int requestCode) {
        Validate.notNull(activity, "activity");
        this.activity = activity;
        this.fragmentWrapper = null;
        this.requestCode = requestCode;
    }

    protected FacebookDialogBase(final FragmentWrapper fragmentWrapper, int requestCode) {
        Validate.notNull(fragmentWrapper, "fragmentWrapper");
        this.fragmentWrapper = fragmentWrapper;
        this.activity = null;
        this.requestCode = requestCode;

        if (fragmentWrapper.getActivity() == null) {
            throw new IllegalArgumentException(
                    "Cannot use a fragment that is not attached to an activity");
        }
    }

    @Override
    public final void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<RESULT> callback) {
        if (!(callbackManager instanceof CallbackManagerImpl)) {
            throw new FacebookException("Unexpected CallbackManager, " +
                    "please use the provided Factory.");
        }
        registerCallbackImpl((CallbackManagerImpl) callbackManager, callback);
    }

    @Override
    public final void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<RESULT> callback,
            final int requestCode) {
        setRequestCode(requestCode);
        registerCallback(callbackManager, callback);
    }

    protected abstract void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<RESULT> callback);

    /**
     * Set the request code for the startActivityForResult call. The requestCode should be
     * outside of the range of those reserved for the Facebook SDK
     * {@link com.facebook.FacebookSdk#isFacebookRequestCode(int)}.
     *
     * @param requestCode the request code to use.
     */
    protected void setRequestCode(int requestCode) {
        if (FacebookSdk.isFacebookRequestCode(requestCode)) {
            throw new IllegalArgumentException("Request code " + requestCode +
                    " cannot be within the range reserved by the Facebook SDK.");
        }
        this.requestCode = requestCode;
    }

    /**
     * Returns the request code used for this dialog.
     *
     * @return the request code.
     */
    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public boolean canShow(CONTENT content) {
        return canShowImpl(content, BASE_AUTOMATIC_MODE);
    }

    // Pass in BASE_AUTOMATIC_MODE when Automatic mode choice is desired
    protected boolean canShowImpl(CONTENT content, Object mode) {
        boolean anyModeAllowed = (mode == BASE_AUTOMATIC_MODE);

        for (ModeHandler handler : cachedModeHandlers()) {
            if (!anyModeAllowed && !Utility.areObjectsEqual(handler.getMode(), mode)) {
                continue;
            }
            // Calls to canShow() are not best effort like calls to show() are. So let's signal
            // more explicitly whether the passed in content can be shown or not
            if (handler.canShow(content, false /*isBestEffort*/)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void show(final CONTENT content) {
        showImpl(content, BASE_AUTOMATIC_MODE);
    }

    // Pass in BASE_AUTOMATIC_MODE when Automatic mode choice is desired
    protected void showImpl(final CONTENT content, final Object mode) {
        AppCall appCall = createAppCallForMode(content, mode);
        if (appCall != null) {
            if (fragmentWrapper != null) {
                DialogPresenter.present(appCall, fragmentWrapper);
            } else {
                DialogPresenter.present(appCall, activity);
            }
        } else {
            // If we got a null appCall, then the derived dialog code is doing something wrong
            String errorMessage = "No code path should ever result in a null appCall";
            Log.e(TAG, errorMessage);
            if (FacebookSdk.isDebugEnabled()) {
                throw new IllegalStateException(errorMessage);
            }
        }
    }

    protected Activity getActivityContext() {
        if (activity != null) {
            return activity;
        }

        if (fragmentWrapper != null) {
            return fragmentWrapper.getActivity();
        }

        return null;
    }

    protected void startActivityForResult(Intent intent, int requestCode) {
        String error = null;
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
        } else if (fragmentWrapper != null) {
            if (fragmentWrapper.getNativeFragment() != null) {
                fragmentWrapper.getNativeFragment().startActivityForResult(intent, requestCode);
            } else if (fragmentWrapper.getSupportFragment() != null) {
                fragmentWrapper.getSupportFragment().startActivityForResult(intent, requestCode);
            } else {
                error = "Failed to find Activity or Fragment to startActivityForResult ";
            }
        } else {
            error = "Failed to find Activity or Fragment to startActivityForResult ";
        }
        if (error != null) {
            Logger.log(LoggingBehavior.DEVELOPER_ERRORS,
                       Log.ERROR,
                       this.getClass().getName(),
                       error);
        }
    }

    private AppCall createAppCallForMode(final CONTENT content, final Object mode) {
        boolean anyModeAllowed = (mode == BASE_AUTOMATIC_MODE);

        AppCall appCall = null;
        for (ModeHandler handler : cachedModeHandlers()) {
            if (!anyModeAllowed && !Utility.areObjectsEqual(handler.getMode(), mode)) {
                continue;
            }
            if (!handler.canShow(content, true /*isBestEffort*/)) {
                continue;
            }

            try {
                appCall = handler.createAppCall(content);
            } catch (FacebookException e) {
                appCall = createBaseAppCall();
                DialogPresenter.setupAppCallForValidationError(appCall, e);
            }
            break;
        }

        if (appCall == null) {
            appCall = createBaseAppCall();
            DialogPresenter.setupAppCallForCannotShowError(appCall);
        }

        return appCall;
    }

    private List<ModeHandler> cachedModeHandlers() {
        if (modeHandlers == null) {
            modeHandlers = getOrderedModeHandlers();
        }

        return modeHandlers;
    }

    protected abstract List<ModeHandler> getOrderedModeHandlers();

    protected abstract AppCall createBaseAppCall();

    protected abstract class ModeHandler {
        /**
         * @return An object to signify a specific dialog-mode.
         */
        public Object getMode() {
            return BASE_AUTOMATIC_MODE;
        }

        /**
         * Used when we want to signal back to the caller when required and optional features are
         * not supported by specific Mode Handlers.
         *
         * @param content      Content to be checked
         * @param isBestEffort Passing in true here will prevent signalling failure for optional or
         *                     best-effort types of features. Passing in false will assume that
         *                     optional or best-effort features should be treated the same as other
         *                     features, and their support be enforced accordingly.
         * @return True if can be shown
         */
        public abstract boolean canShow(final CONTENT content, boolean isBestEffort);

        public abstract AppCall createAppCall(final CONTENT content);
    }
}
