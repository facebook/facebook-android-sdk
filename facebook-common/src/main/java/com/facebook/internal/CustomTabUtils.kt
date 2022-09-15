// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.
package com.facebook.internal

import android.content.Intent
import androidx.browser.customtabs.CustomTabsService
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.Validate.hasCustomTabRedirectActivity
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
object CustomTabUtils {
  private val CHROME_PACKAGES = arrayOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev")

  @JvmStatic
  fun getDefaultRedirectURI(): String {
    return Validate.CUSTOM_TAB_REDIRECT_URI_PREFIX + getApplicationContext().packageName
  }

  @JvmStatic
  fun getValidRedirectURI(developerDefinedRedirectURI: String): String {
    val hasDeveloperDefinedRedirect =
        hasCustomTabRedirectActivity(getApplicationContext(), developerDefinedRedirectURI)
    if (hasDeveloperDefinedRedirect) {
      return developerDefinedRedirectURI
    } else {
      val hasDefaultRedirect =
          hasCustomTabRedirectActivity(getApplicationContext(), getDefaultRedirectURI())
      if (hasDefaultRedirect) {
        return getDefaultRedirectURI()
      }
    }
    return ""
  }

  @JvmStatic
  fun getChromePackage(): String? {
    val context = getApplicationContext()
    val serviceIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
    val resolveInfos = context.packageManager.queryIntentServices(serviceIntent, 0)
    if (resolveInfos != null) {
      val chromePackages: Set<String> = CHROME_PACKAGES.toHashSet()
      for (resolveInfo in resolveInfos) {
        val serviceInfo = resolveInfo.serviceInfo
        if (serviceInfo != null && chromePackages.contains(serviceInfo.packageName)) {
          return serviceInfo.packageName
        }
      }
    }
    return null
  }
}
