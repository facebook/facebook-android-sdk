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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.CustomTabMainActivity;
import com.facebook.FacebookSdk;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.CustomTab;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.login.CustomTabPrefetchHelper;

/** @deprecated Referral is deprecated. This class will be removed in a future release. */
@Deprecated
class ReferralClient {
  private Fragment fragment;
  private String currentPackage;

  protected String expectedChallenge;

  static final String REFERRAL_CODES_KEY = "fb_referral_codes";
  static final String ERROR_MESSAGE_KEY = "error_message";

  private static final String REFERRAL_DIALOG = "share_referral";
  private static final int CUSTOM_TAB_REQUEST_CODE = 1;
  private static final int CHALLENGE_LENGTH = 20;

  ReferralClient(Fragment fragment) {
    this.fragment = fragment;
  }

  void startReferral() {
    boolean started = tryStartReferral();

    if (!started) {
      Intent data = new Intent();
      data.putExtra(
          ERROR_MESSAGE_KEY,
          "Failed to open Referral dialog: Chrome custom tab could not be started."
              + " Please make sure internet permission is granted and Chrome is installed");
      finishReferral(Activity.RESULT_CANCELED, data);
    }
  }

  void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != CUSTOM_TAB_REQUEST_CODE) {
      return;
    }

    if (data != null) {
      // parse custom tab result
      String url = data.getStringExtra(CustomTabMainActivity.EXTRA_URL);
      if (url != null
          && url.startsWith(CustomTabUtils.getValidRedirectURI(getDeveloperDefinedRedirectUrl()))) {
        Uri uri = Uri.parse(url);
        Bundle values = Utility.parseUrlQueryString(uri.getQuery());
        if (validateChallenge(values)) {
          data.putExtras(values);
        } else {
          resultCode = Activity.RESULT_CANCELED;
          data.putExtra(
              ERROR_MESSAGE_KEY, "The referral response was missing a valid challenge string.");
        }
      }
    }

    finishReferral(resultCode, data);
  }

  private boolean validateChallenge(Bundle values) {
    boolean valid = true;
    if (expectedChallenge != null) {
      String actualChallenge = values.getString(ServerProtocol.DIALOG_PARAM_STATE);
      valid = expectedChallenge.equals(actualChallenge);
      expectedChallenge = null;
    }
    return valid;
  }

  static int getReferralRequestCode() {
    return CallbackManagerImpl.RequestCodeOffset.Referral.toRequestCode();
  }

  private void finishReferral(int resultCode, Intent data) {
    if (fragment.isAdded()) {
      Activity activity = fragment.getActivity();
      if (activity != null) {
        activity.setResult(resultCode, data);
        activity.finish();
      }
    }
  }

  private boolean tryStartReferral() {
    if (fragment.getActivity() == null
        || fragment.getActivity().checkCallingOrSelfPermission(Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        || !isCustomTabsAllowed()) {
      return false;
    }

    Bundle parameters = getParameters();
    if (FacebookSdk.hasCustomTabsPrefetching) {
      CustomTabPrefetchHelper.mayLaunchUrl(CustomTab.getURIForAction(REFERRAL_DIALOG, parameters));
    }

    Intent intent = new Intent(fragment.getActivity(), CustomTabMainActivity.class);
    intent.putExtra(CustomTabMainActivity.EXTRA_ACTION, REFERRAL_DIALOG);
    intent.putExtra(CustomTabMainActivity.EXTRA_PARAMS, parameters);
    intent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, getChromePackage());
    fragment.startActivityForResult(intent, CUSTOM_TAB_REQUEST_CODE);

    return true;
  }

  private Bundle getParameters() {
    Bundle params = new Bundle();
    expectedChallenge = Utility.generateRandomString(CHALLENGE_LENGTH);

    params.putString(
        ServerProtocol.DIALOG_PARAM_REDIRECT_URI,
        CustomTabUtils.getValidRedirectURI(getDeveloperDefinedRedirectUrl()));
    params.putString(ServerProtocol.DIALOG_PARAM_APP_ID, FacebookSdk.getApplicationId());
    params.putString(ServerProtocol.DIALOG_PARAM_STATE, expectedChallenge);

    return params;
  }

  private boolean isCustomTabsAllowed() {
    return getChromePackage() != null;
  }

  private String getChromePackage() {
    if (currentPackage == null) {
      currentPackage = CustomTabUtils.getChromePackage();
    }
    return currentPackage;
  }

  static String getDeveloperDefinedRedirectUrl() {
    return "fb" + FacebookSdk.getApplicationId() + "://authorize";
  }
}
