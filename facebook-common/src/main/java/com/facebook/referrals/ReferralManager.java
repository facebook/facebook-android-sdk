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

package com.facebook.referrals;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import androidx.fragment.app.Fragment;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** This class manages referrals for Facebook. */
public class ReferralManager {
  private static volatile ReferralManager instance;

  ReferralManager() {
    Validate.sdkInitialized();
  }

  /**
   * Getter for the referral manager.
   *
   * @return The referral manager.
   */
  public static ReferralManager getInstance() {
    if (instance == null) {
      synchronized (ReferralManager.class) {
        if (instance == null) {
          instance = new ReferralManager();
        }
      }
    }

    return instance;
  }

  /**
   * Open the referral dialog.
   *
   * @param activity The activity which is starting the referral process.
   */
  public void startReferral(Activity activity) {
    startReferralImpl(new ActivityStartActivityDelegate(activity));
  }

  /**
   * Open the referral dialog.
   *
   * @param fragment The android.support.v4.app.Fragment which is starting the referral process.
   */
  public void startReferral(Fragment fragment) {
    startReferralImpl(new FragmentStartActivityDelegate(new FragmentWrapper(fragment)));
  }

  /**
   * Open the referral dialog.
   *
   * @param fragment The android.app.Fragment which is starting the referral process.
   */
  public void startReferral(android.app.Fragment fragment) {
    startReferralImpl(new FragmentStartActivityDelegate(new FragmentWrapper(fragment)));
  }

  /**
   * Open the referral dialog.
   *
   * @param fragment The fragment which is starting the referral process.
   */
  public void startReferral(FragmentWrapper fragment) {
    startReferralImpl(new FragmentStartActivityDelegate(fragment));
  }

  /**
   * Registers a referral callback to the given callback manager.
   *
   * @param callbackManager The callback manager that will encapsulate the callback.
   * @param callback The referral callback that will be called on referral completion.
   */
  public void registerCallback(
      final CallbackManager callbackManager, final FacebookCallback<ReferralResult> callback) {
    if (!(callbackManager instanceof CallbackManagerImpl)) {
      throw new FacebookException(
          "Unexpected CallbackManager, " + "please use the provided Factory.");
    }
    ((CallbackManagerImpl) callbackManager)
        .registerCallback(
            CallbackManagerImpl.RequestCodeOffset.Referral.toRequestCode(),
            new CallbackManagerImpl.Callback() {
              @Override
              public boolean onActivityResult(int resultCode, Intent data) {
                return ReferralManager.onActivityResult(resultCode, data, callback);
              }
            });
  }

  private void startReferralImpl(StartActivityDelegate activity) {
    boolean started = tryFacebookActivity(activity);

    if (!started) {
      throw new FacebookException(
          "Failed to open Referral dialog: FacebookActivity could not be started."
              + " Please make sure you added FacebookActivity to the AndroidManifest.");
    }
  }

  static boolean onActivityResult(
      int resultCode, Intent data, FacebookCallback<ReferralResult> callback) {
    try {
      if (resultCode == Activity.RESULT_OK
          && data != null
          && data.getExtras() != null
          && data.getExtras().containsKey(ReferralFragment.REFERRAL_CODES_KEY)) {
        String referralCodesStr = data.getExtras().getString(ReferralFragment.REFERRAL_CODES_KEY);
        List<String> referralCodes =
            Utility.convertJSONArrayToList(new JSONArray(referralCodesStr));
        ReferralResult result = new ReferralResult(referralCodes);
        callback.onSuccess(result);
      } else if (resultCode == Activity.RESULT_CANCELED) {
        if (data != null
            && data.getExtras() != null
            && data.getExtras().containsKey(ReferralFragment.ERROR_MESSAGE_KEY)) {
          String errorMessage = data.getExtras().getString(ReferralFragment.ERROR_MESSAGE_KEY);
          callback.onError(new FacebookException(errorMessage));
        } else {
          callback.onCancel();
        }
      } else {
        callback.onError(
            new FacebookException("Unexpected call to ReferralManager.onActivityResult"));
      }
    } catch (JSONException ex) {
      callback.onError(new FacebookException("Unable to parse referral codes from response"));
    }
    return true;
  }

  private boolean tryFacebookActivity(StartActivityDelegate activity) {
    Intent intent = new Intent();
    intent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
    intent.setAction(ReferralFragment.TAG);

    if (!resolveIntent(intent)) {
      return false;
    }

    try {
      activity.startActivityForResult(
          intent, CallbackManagerImpl.RequestCodeOffset.Referral.toRequestCode());
    } catch (ActivityNotFoundException e) {
      return false;
    }

    return true;
  }

  private static boolean resolveIntent(Intent intent) {
    ResolveInfo resolveInfo =
        FacebookSdk.getApplicationContext().getPackageManager().resolveActivity(intent, 0);
    return resolveInfo != null;
  }

  private static class ActivityStartActivityDelegate implements StartActivityDelegate {
    private final Activity activity;

    ActivityStartActivityDelegate(final Activity activity) {
      Validate.notNull(activity, "activity");
      this.activity = activity;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
      activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public Activity getActivityContext() {
      return activity;
    }
  }

  private static class FragmentStartActivityDelegate implements StartActivityDelegate {
    private final FragmentWrapper fragment;

    FragmentStartActivityDelegate(final FragmentWrapper fragment) {
      Validate.notNull(fragment, "fragment");
      this.fragment = fragment;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
      fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    public Activity getActivityContext() {
      return fragment.getActivity();
    }
  }
}
