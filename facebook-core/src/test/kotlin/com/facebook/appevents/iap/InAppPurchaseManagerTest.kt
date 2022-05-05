package com.facebook.appevents.iap

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
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
  private lateinit var mockContext: Context
  override fun setup() {
    super.setup()
    mockContext = mock()
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(InAppPurchaseActivityLifecycleTracker::class.java)
    PowerMockito.mockStatic(InAppPurchaseAutoLogger::class.java)
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    Whitebox.setInternalState(InAppPurchaseManager::class.java, "enabled", AtomicBoolean(false))
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T114038283
  @Test
  fun `test start iap logging when billing lib 2+ is not available`() {
    MemberModifier.stub<Boolean>(
            PowerMockito.method(InAppPurchaseManager::class.java, "usingBillingLib2Plus"))
        .toReturn(false)
    var isStartIapLoggingCalled = false
    whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T114048296
  @Test
  fun `test start iap logging when billing lib 2+ is available but feature is off`() {
    MemberModifier.stub<Boolean>(
            PowerMockito.method(InAppPurchaseManager::class.java, "usingBillingLib2Plus"))
        .toReturn(true)
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2)).thenReturn(false)
    var isStartIapLoggingCalled = false
    whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T114044697
  @Test
  fun `test start iap logging when billing lib 2+ is available and feature is on`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2)).thenReturn(true)
    val mockPackageManager: PackageManager = mock()
    val mockApplicationInfo = ApplicationInfo()
    val metaData = Bundle()
    metaData.putString("com.google.android.play.billingclient.version", "2.0.3")
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getApplicationInfo(any(), any())).thenReturn(mockApplicationInfo)
    mockApplicationInfo.metaData = metaData

    var isStartIapLoggingCalled = false
    whenever(InAppPurchaseAutoLogger.startIapLogging(any())).thenAnswer {
      isStartIapLoggingCalled = true
      Unit
    }
    InAppPurchaseManager.enableAutoLogging()
    assertThat(isStartIapLoggingCalled).isTrue
  }
}
