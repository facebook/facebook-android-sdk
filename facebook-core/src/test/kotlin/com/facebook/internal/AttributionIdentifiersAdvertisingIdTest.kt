/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

/**
 * Runs the real reflective advertising ID lookup against [AdvertisingIdClient]'s test double, with
 * the SDK's Play services pre-check failing as it does in apps without play-services-base.
 */
@PrepareForTest(FacebookSdk::class, FeatureManager::class, Looper::class)
class AttributionIdentifiersAdvertisingIdTest : FacebookPowerMockTestCase() {
  private lateinit var mockContext: Context

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getAdvertiserIDCollectionEnabled()).thenReturn(true)
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(Looper::class.java)
    whenever(Looper.getMainLooper()).thenReturn(PowerMockito.mock(Looper::class.java))
    whenever(Looper.myLooper()).thenReturn(PowerMockito.mock(Looper::class.java))
    mockContext = PowerMockito.mock(Context::class.java)
    whenever(mockContext.packageManager).thenReturn(PowerMockito.mock(PackageManager::class.java))

    AttributionIdentifiers.cachedIdentifiers = null
    AdvertisingIdClient.reset()
    AdvertisingIdClient.nextInfo = AdvertisingIdClient.Info(GAID, false)
  }

  @Test
  fun `test without the feature no advertising id is read when the pre-check fails`() {
    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.androidAdvertiserId).isNull()
    assertThat(AdvertisingIdClient.callCount).isEqualTo(0)
  }

  @Test
  fun `test with the feature the advertising id is read when the pre-check fails`() {
    enableFeature()

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.androidAdvertiserId).isEqualTo(GAID)
    assertThat(identifiers?.isTrackingLimited).isFalse
    assertThat(AdvertisingIdClient.callCount).isEqualTo(1)
  }

  @Test
  fun `test with the feature the limit ad tracking choice is carried through`() {
    enableFeature()
    AdvertisingIdClient.nextInfo = AdvertisingIdClient.Info(ZEROED_GAID, true)

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.androidAdvertiserId).isEqualTo(ZEROED_GAID)
    assertThat(identifiers?.isTrackingLimited).isTrue
  }

  @Test
  fun `test with the feature a client failure falls through without an advertising id`() {
    enableFeature()
    AdvertisingIdClient.failure = IllegalStateException("Google Play services not available")

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers).isNotNull
    assertThat(identifiers?.androidAdvertiserId).isNull()
    assertThat(AdvertisingIdClient.callCount).isEqualTo(1)
  }

  @Test
  fun `test with the feature advertiser id collection disabled still withholds the id`() {
    enableFeature()
    whenever(FacebookSdk.getAdvertiserIDCollectionEnabled()).thenReturn(false)

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.androidAdvertiserId).isNull()
  }

  @Test
  fun `test a successful read registers even when collection disabled hides the id`() {
    enableFeature()
    whenever(FacebookSdk.getAdvertiserIDCollectionEnabled()).thenReturn(false)
    AdvertisingIdClient.nextInfo = AdvertisingIdClient.Info(GAID, true)

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.androidAdvertiserId).isNull()
    assertThat(identifiers?.isAdvertisingIdRead).isTrue
    assertThat(identifiers?.isTrackingLimited).isTrue
  }

  @Test
  fun `test a failed read does not register`() {
    enableFeature()
    AdvertisingIdClient.failure = IllegalStateException("Google Play services not available")

    val identifiers = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(identifiers?.isAdvertisingIdRead).isFalse
  }

  @Test
  fun `test with the feature a fresh cached advertising id is returned without reading again`() {
    enableFeature()
    val first = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    val second = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(second).isSameAs(first)
    assertThat(AdvertisingIdClient.callCount).isEqualTo(1)
  }

  @Test
  fun `test with the feature a failed read is kept only briefly then retried`() {
    enableFeature()
    AdvertisingIdClient.failure = IllegalStateException("transient")
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    AdvertisingIdClient.failure = null

    val withinRetryInterval = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    assertThat(withinRetryInterval?.androidAdvertiserId).isNull()
    assertThat(AdvertisingIdClient.callCount).isEqualTo(1)

    ageCachedIdentifiers(SIX_MINUTES_MILLIS)
    val afterRetryInterval = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    assertThat(afterRetryInterval?.androidAdvertiserId).isEqualTo(GAID)
    assertThat(AdvertisingIdClient.callCount).isEqualTo(2)
  }

  @Test
  fun `test with the feature a successful read outlives the failed read retry interval`() {
    enableFeature()
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    ageCachedIdentifiers(SIX_MINUTES_MILLIS)
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(AdvertisingIdClient.callCount).isEqualTo(1)
  }

  @Test
  fun `test with the feature a successful read is refreshed after an hour`() {
    enableFeature()
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    ageCachedIdentifiers(SIXTY_ONE_MINUTES_MILLIS)
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(AdvertisingIdClient.callCount).isEqualTo(2)
  }

  @Test
  fun `test with the feature a fetch time in the future is treated as stale`() {
    enableFeature()
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    ageCachedIdentifiers(-SIX_MINUTES_MILLIS)
    AttributionIdentifiers.getAttributionIdentifiers(mockContext)

    assertThat(AdvertisingIdClient.callCount).isEqualTo(2)
  }

  private fun ageCachedIdentifiers(ageMillis: Long) {
    Whitebox.setInternalState(
        checkNotNull(AttributionIdentifiers.cachedIdentifiers),
        "fetchTime",
        System.currentTimeMillis() - ageMillis)
  }

  private fun enableFeature() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidAdvertisingIDReliability))
        .thenReturn(true)
  }

  companion object {
    private const val GAID = "38400000-8cf0-11bd-b23e-10b96e40000d"
    private const val ZEROED_GAID = "00000000-0000-0000-0000-000000000000"
    private const val SIX_MINUTES_MILLIS = 6 * 60 * 1000L
    private const val SIXTY_ONE_MINUTES_MILLIS = 61 * 60 * 1000L
  }
}
