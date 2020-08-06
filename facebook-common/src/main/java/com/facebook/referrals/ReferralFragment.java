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
import com.facebook.internal.CustomTab;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.login.CustomTabPrefetchHelper;

/**
 * This Fragment is a necessary part of the Facebook referral process but is not meant to be used
 * directly.
 *
 * @see com.facebook.FacebookActivity
 */
public class ReferralFragment extends Fragment {
  public static final String TAG = "ReferralFragment";

  static final String REFERRAL_CODES_KEY = "fb_referral_codes";
  static final String ERROR_MESSAGE_KEY = "error_message";

  private String currentPackage;
  private static final String REFERRAL_DIALOG = "share_referral";
  private static final int CUSTOM_TAB_REQUEST_CODE = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    startReferral();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != CUSTOM_TAB_REQUEST_CODE) {
      super.onActivityResult(requestCode, resultCode, data);
    }

    if (data != null) {
      // parse custom tab result
      String url = data.getStringExtra(CustomTabMainActivity.EXTRA_URL);
      if (url != null
          && url.startsWith(CustomTabUtils.getValidRedirectURI(getDeveloperDefinedRedirectUrl()))) {
        Uri uri = Uri.parse(url);
        Bundle values = Utility.parseUrlQueryString(uri.getQuery());
        data.putExtras(values);
      }
    }

    finishReferral(resultCode, data);
  }

  private void startReferral() {
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

  private void finishReferral(int resultCode, Intent data) {
    if (isAdded()) {
      Activity activity = getActivity();
      if (activity != null) {
        activity.setResult(resultCode, data);
        activity.finish();
      }
    }
  }

  private boolean tryStartReferral() {
    if (getActivity().checkCallingOrSelfPermission(Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        || !isCustomTabsAllowed()) {
      return false;
    }

    Bundle parameters = getParameters();
    if (FacebookSdk.hasCustomTabsPrefetching) {
      CustomTabPrefetchHelper.mayLaunchUrl(CustomTab.getURIForAction(REFERRAL_DIALOG, parameters));
    }

    Intent intent = new Intent(getActivity(), CustomTabMainActivity.class);
    intent.putExtra(CustomTabMainActivity.EXTRA_ACTION, REFERRAL_DIALOG);
    intent.putExtra(CustomTabMainActivity.EXTRA_PARAMS, parameters);
    intent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, getChromePackage());
    startActivityForResult(intent, CUSTOM_TAB_REQUEST_CODE);

    return true;
  }

  private static Bundle getParameters() {
    Bundle params = new Bundle();

    params.putString(
        ServerProtocol.DIALOG_PARAM_REDIRECT_URI,
        CustomTabUtils.getValidRedirectURI(getDeveloperDefinedRedirectUrl()));
    params.putString(ServerProtocol.DIALOG_PARAM_APP_ID, FacebookSdk.getApplicationId());

    return params;
  }

  private boolean isCustomTabsAllowed() {
    return getChromePackage() != null;
  }

  private String getChromePackage() {
    if (currentPackage != null) {
      return currentPackage;
    }
    currentPackage = CustomTabUtils.getChromePackage();
    return currentPackage;
  }

  private static String getDeveloperDefinedRedirectUrl() {
    return "fb" + FacebookSdk.getApplicationId() + "://authorize";
  }
}
