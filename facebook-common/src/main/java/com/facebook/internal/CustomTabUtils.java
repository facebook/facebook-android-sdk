// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.internal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import androidx.browser.customtabs.CustomTabsService;
import com.facebook.FacebookSdk;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoHandleExceptions
public class CustomTabUtils {
  private static final String[] CHROME_PACKAGES = {
    "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
  };

  public static String getDefaultRedirectURI() {
    return Validate.CUSTOM_TAB_REDIRECT_URI_PREFIX
        + FacebookSdk.getApplicationContext().getPackageName();
  }

  public static String getChromePackage() {
    Context context = FacebookSdk.getApplicationContext();
    Intent serviceIntent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
    List<ResolveInfo> resolveInfos =
        context.getPackageManager().queryIntentServices(serviceIntent, 0);
    if (resolveInfos != null) {
      Set<String> chromePackages = new HashSet<>(Arrays.asList(CHROME_PACKAGES));
      for (ResolveInfo resolveInfo : resolveInfos) {
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        if (serviceInfo != null && chromePackages.contains(serviceInfo.packageName)) {
          return serviceInfo.packageName;
        }
      }
    }
    return null;
  }
}
