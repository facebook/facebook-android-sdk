/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents;

import androidx.annotation.RestrictTo;
import com.facebook.appevents.aam.MetadataIndexer;
import com.facebook.appevents.eventdeactivation.EventDeactivationManager;
import com.facebook.appevents.iap.InAppPurchaseManager;
import com.facebook.appevents.ml.ModelManager;
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
public class AppEventsManager {
  /**
   * Start AppEvents functionality.
   *
   * <p>Note that the function should be called after FacebookSdk is initialized.
   */
  public static void start() {
    FetchedAppSettingsManager.getAppSettingsAsync(
        new FetchedAppSettingsManager.FetchedAppSettingsCallback() {
          @Override
          public void onSuccess(FetchedAppSettings fetchedAppSettings) {
            FeatureManager.checkFeature(
                FeatureManager.Feature.AAM,
                new FeatureManager.Callback() {
                  @Override
                  public void onCompleted(boolean enabled) {
                    if (enabled) {
                      MetadataIndexer.enable();
                    }
                  }
                });

            FeatureManager.checkFeature(
                FeatureManager.Feature.RestrictiveDataFiltering,
                new FeatureManager.Callback() {
                  @Override
                  public void onCompleted(boolean enabled) {
                    if (enabled) {
                      RestrictiveDataManager.enable();
                    }
                  }
                });

            FeatureManager.checkFeature(
                FeatureManager.Feature.PrivacyProtection,
                new FeatureManager.Callback() {
                  @Override
                  public void onCompleted(boolean enabled) {
                    if (enabled) {
                      ModelManager.enable();
                    }
                  }
                });

            FeatureManager.checkFeature(
                FeatureManager.Feature.EventDeactivation,
                new FeatureManager.Callback() {
                  @Override
                  public void onCompleted(boolean enabled) {
                    if (enabled) {
                      EventDeactivationManager.enable();
                    }
                  }
                });

            FeatureManager.checkFeature(
                FeatureManager.Feature.IapLogging,
                new FeatureManager.Callback() {
                  @Override
                  public void onCompleted(boolean enabled) {
                    if (enabled) {
                      InAppPurchaseManager.enableAutoLogging();
                    }
                  }
                });
          }

          @Override
          public void onError() {}
        });
  }
}
