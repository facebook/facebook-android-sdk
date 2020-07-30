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
import android.content.Intent;
import androidx.fragment.app.Fragment;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.Validate;

/** This class manages referrals for Facebook. */
public class ReferralManager {
  private static volatile ReferralManager instance;

  public ReferralManager() {
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

  private void startReferralImpl(StartActivityDelegate activity) {
    // TODO(T70835761): Open FacebookActivity
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
