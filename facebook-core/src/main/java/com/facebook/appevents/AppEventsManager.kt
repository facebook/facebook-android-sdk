/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import androidx.annotation.RestrictTo
import com.facebook.appevents.aam.MetadataIndexer
import com.facebook.appevents.cloudbridge.AppEventsCAPIManager
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.integrity.BlocklistEventsManager
import com.facebook.appevents.integrity.MACARuleMatchingManager
import com.facebook.appevents.ml.ModelManager
import com.facebook.appevents.integrity.ProtectedModeManager
import com.facebook.appevents.integrity.RedactedEventsManager
import com.facebook.appevents.integrity.SensitiveParamsManager
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.checkFeature
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsAsync
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
object AppEventsManager {
  /**
   * Start AppEvents functionality.
   *
   * Note that the function should be called after FacebookSdk is initialized.
   */
  @JvmStatic
  fun start() {
    getAppSettingsAsync(
        object : FetchedAppSettingsManager.FetchedAppSettingsCallback {
          override fun onSuccess(fetchedAppSettings: FetchedAppSettings?) {
            checkFeature(FeatureManager.Feature.AAM) { enabled ->
              if (enabled) {
                MetadataIndexer.enable()
              }
            }
            checkFeature(FeatureManager.Feature.RestrictiveDataFiltering) { enabled ->
              if (enabled) {
                RestrictiveDataManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.PrivacyProtection) { enabled ->
              if (enabled) {
                ModelManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.EventDeactivation) { enabled ->
              if (enabled) {
                EventDeactivationManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.IapLogging) { enabled ->
              if (enabled) {
                InAppPurchaseManager.enableAutoLogging()
              }
            }
            checkFeature(FeatureManager.Feature.ProtectedMode) { enabled ->
              if (enabled) {
                ProtectedModeManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.MACARuleMatching) { enabled ->
              if (enabled) {
                MACARuleMatchingManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.BlocklistEvents) { enabled ->
              if (enabled) {
                BlocklistEventsManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.FilterRedactedEvents) { enabled ->
              if (enabled) {
                RedactedEventsManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.FilterSensitiveParams) { enabled ->
              if (enabled) {
                SensitiveParamsManager.enable()
              }
            }
            checkFeature(FeatureManager.Feature.CloudBridge) { enabled ->
              if (enabled) {
                AppEventsCAPIManager.enable()
              }
            }
          }

          override fun onError() = Unit
        })
  }
}
