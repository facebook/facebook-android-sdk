/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.internal.FeatureManager.Feature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FeatureManagerTest {

  @Test
  fun `test getting features from class names`() {
    assertEquals(Feature.Unknown, FeatureManager.getFeature("very.fake.class.name"))

    assertEquals(
        Feature.AAM, FeatureManager.getFeature("com.facebook.appevents.aam.MetadataIndexer"))
    assertEquals(
        Feature.AAM,
        FeatureManager.getFeature("com.facebook.appevents.aam.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.CodelessEvents,
        FeatureManager.getFeature("com.facebook.appevents.codeless.CodelessManager"))
    assertEquals(
        Feature.CodelessEvents,
        FeatureManager.getFeature("com.facebook.appevents.codeless.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.CloudBridge,
        FeatureManager.getFeature("com.facebook.appevents.cloudbridge.AppEventsCAPIManager"))
    assertEquals(
        Feature.CloudBridge,
        FeatureManager.getFeature(
            "com.facebook.appevents.cloudbridge.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.ErrorReport,
        FeatureManager.getFeature(
            "com.facebook.internal.instrument.errorreport.ErrorReportHandler"))
    assertEquals(
        Feature.ErrorReport,
        FeatureManager.getFeature(
            "com.facebook.internal.instrument.errorreport.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.PrivacyProtection,
        FeatureManager.getFeature("com.facebook.appevents.ml.ModelManager"))
    assertEquals(
        Feature.PrivacyProtection,
        FeatureManager.getFeature("com.facebook.appevents.ml.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.SuggestedEvents,
        FeatureManager.getFeature("com.facebook.appevents.suggestedevents.FeatureExtractor"))
    assertEquals(
        Feature.SuggestedEvents,
        FeatureManager.getFeature(
            "com.facebook.appevents.suggestedevents.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.RestrictiveDataFiltering,
        FeatureManager.getFeature(
            "com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager"))
    assertNotEquals(
        Feature.RestrictiveDataFiltering,
        FeatureManager.getFeature(
            "com.facebook.appevents.restrictivedatafilter.DoesNotExistAndShouldNotPass"))

    assertEquals(
        Feature.IntelligentIntegrity,
        FeatureManager.getFeature("com.facebook.appevents.integrity.IntegrityManager"))
    assertNotEquals(
        Feature.IntelligentIntegrity,
        FeatureManager.getFeature("com.facebook.appevents.integrity.DoesNotExistAndShouldNotPass"))

    assertEquals(
        Feature.EventDeactivation,
        FeatureManager.getFeature(
            "com.facebook.appevents.eventdeactivation.EventDeactivationManager"))
    assertEquals(
        Feature.EventDeactivation,
        FeatureManager.getFeature(
            "com.facebook.appevents.eventdeactivation.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.OnDeviceEventProcessing,
        FeatureManager.getFeature(
            "com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager"))
    assertEquals(
        Feature.OnDeviceEventProcessing,
        FeatureManager.getFeature(
            "com.facebook.appevents.ondeviceprocessing.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.IapLogging,
        FeatureManager.getFeature("com.facebook.appevents.iap.InAppPurchaseManager"))
    assertEquals(
        Feature.IapLogging,
        FeatureManager.getFeature("com.facebook.appevents.iap.DoesNotExistButStillShouldPass"))

    assertEquals(
        Feature.Monitoring, FeatureManager.getFeature("com.facebook.internal.logging.monitor"))
    assertEquals(
        Feature.Monitoring,
        FeatureManager.getFeature(
            "com.facebook.internal.logging.monitor.DoesNotExistButStillShouldPass"))
  }

  @Test
  fun `test features parents inside Core`() {
    assertEquals(Feature.Core, Feature.Core.parent)
    assertEquals(Feature.Core, Feature.AppEvents.parent)
    assertEquals(Feature.AppEvents, Feature.CodelessEvents.parent)
    assertEquals(Feature.AppEvents, Feature.CloudBridge.parent)
    assertEquals(Feature.AppEvents, Feature.RestrictiveDataFiltering.parent)
    assertEquals(Feature.AppEvents, Feature.AAM.parent)
    assertEquals(Feature.AppEvents, Feature.PrivacyProtection.parent)
    assertEquals(Feature.AppEvents, Feature.IapLogging.parent)
    assertEquals(Feature.IapLogging, Feature.IapLoggingLib2.parent)
    assertEquals(Feature.PrivacyProtection, Feature.SuggestedEvents.parent)
    assertEquals(Feature.PrivacyProtection, Feature.IntelligentIntegrity.parent)
    assertEquals(Feature.AppEvents, Feature.EventDeactivation.parent)
    assertEquals(Feature.Core, Feature.Instrument.parent)
    assertEquals(Feature.Instrument, Feature.CrashReport.parent)
    assertEquals(Feature.CrashReport, Feature.CrashShield.parent)
    assertEquals(Feature.CrashReport, Feature.ThreadCheck.parent)
    assertEquals(Feature.Instrument, Feature.ErrorReport.parent)
    assertEquals(Feature.AppEvents, Feature.OnDeviceEventProcessing.parent)
    assertEquals(Feature.OnDeviceEventProcessing, Feature.OnDevicePostInstallEventProcessing.parent)
    assertEquals(Feature.Core, Feature.Monitoring.parent)
  }

  @Test
  fun `test features parents inside Login`() {
    assertEquals(Feature.Core, Feature.Login.parent)
  }

  @Test
  fun `test features parents inside Share`() {
    assertEquals(Feature.Core, Feature.Share.parent)
  }

  @Test
  fun `test return unknown feature`() {
    assertEquals(Feature.Unknown, Feature.fromInt(0x12345678))
  }
}
