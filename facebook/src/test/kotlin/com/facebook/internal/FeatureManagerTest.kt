package com.facebook.internal

import com.facebook.internal.FeatureManager.Feature
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

class FeatureManagerTest {

    @Test
    fun `test getting features from class names`(){
        assertEquals(expected = Feature.Unknown, actual = FeatureManager.getFeature("very.fake.class.name"))

        assertEquals(Feature.AAM,
                FeatureManager.getFeature("com.facebook.appevents.aam.MetadataIndexer"))
        assertEquals(Feature.AAM,
                FeatureManager.getFeature("com.facebook.appevents.aam.DoesNotExistButStillShouldPass"))

        assertEquals(Feature.CodelessEvents,
                FeatureManager.getFeature("com.facebook.appevents.codeless.CodelessManager"))
        assertEquals(Feature.CodelessEvents,
                FeatureManager.getFeature("com.facebook.appevents.codeless.DoesNotExistButStillShouldPass"))

        assertEquals(Feature.ErrorReport,
                FeatureManager.getFeature("com.facebook.internal.instrument.errorreport.ErrorReportHandler"))
        assertEquals(Feature.ErrorReport,
                FeatureManager.getFeature("com.facebook.internal.instrument.errorreport.DoesNotExistButStillShouldPass"))

        assertEquals(Feature.PrivacyProtection,
                FeatureManager.getFeature("com.facebook.appevents.ml.ModelManager"))
        assertEquals(Feature.PrivacyProtection,
                FeatureManager.getFeature("com.facebook.appevents.ml.DoesNotExistButStillShouldPass"))

        assertEquals(Feature.SuggestedEvents,
                FeatureManager.getFeature("com.facebook.appevents.suggestedevents.FeatureExtractor"))
        assertEquals(Feature.SuggestedEvents,
                FeatureManager.getFeature("com.facebook.appevents.suggestedevents.DoesNotExistButStillShouldPass"))

        assertEquals(Feature.RestrictiveDataFiltering,
                FeatureManager.getFeature("com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager"))
        assertNotEquals(Feature.RestrictiveDataFiltering,
                FeatureManager.getFeature("com.facebook.appevents.restrictivedatafilter.DoesNotExistAndShouldNotPass"))

        assertEquals(Feature.PIIFiltering,
                FeatureManager.getFeature("com.facebook.appevents.restrictivedatafilter.AddressFilterManager"))
        assertNotEquals(Feature.PIIFiltering,
                FeatureManager.getFeature("com.facebook.appevents.restrictivedatafilter.DoesNotExistAndShouldNotPass"))

        assertEquals(Feature.EventDeactivation,
                FeatureManager.getFeature("com.facebook.appevents.eventdeactivation.EventDeactivationManager"))
        assertEquals(Feature.EventDeactivation,
                FeatureManager.getFeature("com.facebook.appevents.eventdeactivation.DoesNotExistButStillShouldPass"))
    }

    @Test
    fun `test features parents inside Core`() {
        assertEquals(expected = Feature.Core, actual = Feature.Core.parent)
        assertEquals(Feature.Core, Feature.AppEvents.parent)
        assertEquals(Feature.AppEvents, Feature.CodelessEvents.parent)
        assertEquals(Feature.AppEvents, Feature.RestrictiveDataFiltering.parent)
        assertEquals(Feature.AppEvents, Feature.AAM.parent)
        assertEquals(Feature.AppEvents, Feature.PrivacyProtection.parent)
        assertEquals(Feature.PrivacyProtection, Feature.SuggestedEvents.parent)
        assertEquals(Feature.PrivacyProtection, Feature.PIIFiltering.parent)
        assertEquals(Feature.PrivacyProtection, Feature.MTML.parent)
        assertEquals(Feature.AppEvents, Feature.EventDeactivation.parent)
        assertEquals(Feature.Core, Feature.Instrument.parent)
        assertEquals(Feature.Instrument, Feature.CrashReport.parent)
        assertEquals(Feature.CrashReport, Feature.CrashShield.parent)
        assertEquals(Feature.CrashReport, Feature.ThreadCheck.parent)
        assertEquals(Feature.Instrument, Feature.ErrorReport.parent)
    }

    @Test
    fun `test features parents inside Login`() {
        assertEquals(expected = Feature.Core, actual = Feature.Login.parent)
    }

    @Test
    fun `test features parents inside Share`() {
        assertEquals(expected = Feature.Core, actual = Feature.Share.parent)
    }

    @Test
    fun `test features parents inside Places`() {
        assertEquals(expected = Feature.Core, actual = Feature.Places.parent)
    }
}
