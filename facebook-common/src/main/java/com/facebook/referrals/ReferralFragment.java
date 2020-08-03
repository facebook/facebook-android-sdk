package com.facebook.referrals;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.CustomTabMainActivity;
import com.facebook.FacebookSdk;
import com.facebook.internal.CustomTab;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.ServerProtocol;
import com.facebook.login.CustomTabPrefetchHelper;

/**
 * This Fragment is a necessary part of the Facebook referral process but is not meant to be used
 * directly.
 *
 * @see com.facebook.FacebookActivity
 */
public class ReferralFragment extends Fragment {
  public static final String TAG = "ReferralFragment";
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

  void startReferral() {
    if (getActivity().checkCallingOrSelfPermission(Manifest.permission.INTERNET)
        != PackageManager.PERMISSION_GRANTED) {
      // We're going to need INTERNET permission later and don't have it, so fail early.
      return;
    }

    tryStartReferral();
  }

  boolean tryStartReferral() {
    if (!isCustomTabsAllowed()) {
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
