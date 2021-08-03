package com.facebook.appevents.iap

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import com.nhaarman.mockitokotlin2.any
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    FeatureManager::class,
    InAppPurchaseActivityLifecycleTracker::class,
    InAppPurchaseAutoLogger::class,
    InAppPurchaseManager::class)
class InAppPurchaseManagerTest : FacebookPowerMockTestCase() {
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(InAppPurchaseActivityLifecycleTracker::class.java)
    PowerMockito.mockStatic(InAppPurchaseAutoLogger::class.java)
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    Whitebox.setInternalState(InAppPurchaseManager::class.java, "enabled", AtomicBoolean(false))
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `test start iap logging when billing lib 2+ is not available`() {
    MemberModifier.stub<Boolean>(
            PowerMockito.method(InAppPurchaseManager::class.java, "usingBillingLib2Plus"))
        .toReturn(false)
    var isStartIapLoggingCalled = false
    PowerMockito.`when`(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }

  @Test
  fun `test start iap logging when billing lib 2+ is available but feature is off`() {
    MemberModifier.stub<Boolean>(
            PowerMockito.method(InAppPurchaseManager::class.java, "usingBillingLib2Plus"))
        .toReturn(true)
    PowerMockito.`when`(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2))
        .thenReturn(false)
    var isStartIapLoggingCalled = false
    PowerMockito.`when`(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }

  @Test
  fun `test start iap logging when billing lib 2+ is available and feature is on`() {
    MemberModifier.stub<Boolean>(
            PowerMockito.method(InAppPurchaseManager::class.java, "usingBillingLib2Plus"))
        .toReturn(true)
    PowerMockito.`when`(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2))
        .thenReturn(true)
    var isStartIapLoggingCalled = false
    PowerMockito.`when`(InAppPurchaseAutoLogger.startIapLogging(any())).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }
}
