/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.internal.FeatureManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@PrepareForTest(
  FacebookSdk::class,
  FeatureManager::class,
  AppLinkManager::class,
)
class AppLinkManagerTest : FacebookPowerMockTestCase() {

  private lateinit var mockApplicationContext: Context
  private lateinit var mockActivity: Activity
  private lateinit var appLinkManager: AppLinkManager

  companion object {
    private val mockSharedPreference: SharedPreferences = MockSharedPreference()
  }

  @Before
  fun init() {
    resetSingleton()
    mockActivity = mock()
    mockApplicationContext = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)
    whenever(mockApplicationContext.getSharedPreferences(any<String>(), any()))
      .thenReturn(mockSharedPreference)
    PowerMockito.mockStatic(FeatureManager::class.java)
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenReturn(true)
    appLinkManager =
      AppLinkManager::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
  }

  @After
  fun after() {
    mockSharedPreference.edit().clear()
    resetSingleton()
  }

  private fun resetSingleton() {
    Whitebox.setInternalState(
      AppLinkManager::class.java,
      "instance",
      null as AppLinkManager?,
    )
  }

  @Test
  fun testGetInstance() {
    assertNotNull(AppLinkManager.getInstance())
  }

  @Test
  fun testHandleInvalidAppLinkData() {
    val intent = Intent()
    whenever(mockActivity.intent).thenReturn(intent)
    intent.setData(Uri.parse("fb123://test.com?al_applink_data=123"))
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertNull(mockSharedPreference.getString("campaign_ids", null))
  }

  @Test
  fun testHandleNullCampaignIDs() {
    val intent = Intent()
    whenever(mockActivity.intent).thenReturn(intent)
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertNull(mockSharedPreference.getString("campaign_ids", null))
  }

  @Test
  fun testHandleNonnullCampaignIDsFromURL() {
    val intent = Intent()
    intent.setData(Uri.parse("fb123://test.com?al_applink_data=%7B%22acs_token%22%3A+%22test_token_1234567%22%2C+%22campaign_ids%22%3A+%22test_campaign_1234%22%2C+%22advertiser_id%22%3A+%22test_advertiserid_12345%22%7D"))
    whenever(mockActivity.intent).thenReturn(intent)
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertEquals("test_campaign_1234", mockSharedPreference.getString("campaign_ids", null))
  }

  @Test
  fun testHandleNonnullCampaignIDsFromExtra() {
    val intent = Intent()
    val bundle = Bundle()
    bundle.putString("campaign_ids", "test_campaign_1234")
    intent.putExtra("al_applink_data", bundle)
    whenever(mockActivity.intent).thenReturn(intent)
    intent.setData(Uri.parse("fb123://test.com"))
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertEquals("test_campaign_1234", mockSharedPreference.getString("campaign_ids", null))
  }

  @Test
  fun testHandleNonnullClickId() {
    val intent = Intent()
    whenever(mockActivity.intent).thenReturn(intent)
    intent.setData(Uri.parse("myapp://product/123?fbclid=test_clickid_1234"))
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertEquals("test_clickid_1234", mockSharedPreference.getString("click_id", null))
  }

  @Test
  fun testHandleMissingClickId() {
    val intent = Intent()
    whenever(mockActivity.intent).thenReturn(intent)
    intent.setData(Uri.parse("myapp://product/123"))
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertNull(mockSharedPreference.getString("click_id", null))
  }

  @Test
  fun testHandleInvalidUri() {
    val intent = Intent()
    whenever(mockActivity.intent).thenReturn(intent)
    AppLinkManager.getInstance()?.handleURL(mockActivity)

    assertNull(mockSharedPreference.getString("click_id", null))
  }

  @Test
  fun `handleURL caches inbound URL`() {
    val url = "fb123://applinks/product?id=42"
    whenever(mockActivity.intent).thenReturn(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    appLinkManager.handleURL(mockActivity)

    assertEquals(url, mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
  }

  @Test
  fun `handleURL keeps the latest inbound URL`() {
    whenever(mockActivity.intent)
      .thenReturn(Intent(Intent.ACTION_VIEW, Uri.parse("fb123://applinks/first")))
    appLinkManager.handleURL(mockActivity)
    val latestUrl = "fb123://applinks/second"
    whenever(mockActivity.intent).thenReturn(Intent(Intent.ACTION_VIEW, Uri.parse(latestUrl)))

    appLinkManager.handleURL(mockActivity)

    assertEquals(latestUrl, mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
  }

  @Test
  fun `cacheInboundUrl ignores a null URL`() {
    val cachedUrl = "fb123://applinks/existing"
    appLinkManager.cacheInboundUrl(Uri.parse(cachedUrl))

    appLinkManager.cacheInboundUrl(null)

    assertEquals(cachedUrl, mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
  }

  @Test
  fun `disabled MetadataBasic allows local cache but blocks event attribution`() {
    val url = "fb123://applinks/disabled"
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenReturn(false)
    appLinkManager.cacheInboundUrl(Uri.parse(url))

    assertEquals(url, mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
    assertNull(appLinkManager.getInboundUrl())
  }

  @Test
  fun `getInboundUrl returns cached URL when MetadataBasic is enabled`() {
    val url = "fb123://applinks/enabled"
    appLinkManager.cacheInboundUrl(Uri.parse(url))

    assertEquals(url, appLinkManager.getInboundUrl())
  }

  @Test
  fun `cached inbound URL persists across manager instances`() {
    val url = "fb123://applinks/persisted"
    appLinkManager.cacheInboundUrl(Uri.parse(url))
    val newManager =
      AppLinkManager::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

    assertEquals(url, newManager.getInboundUrl())
  }

  @Test
  fun `cacheInboundUrl does not cache when metadata collection is disabled`() {
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    appLinkManager.cacheInboundUrl(Uri.parse("fb123://applinks/opted_out"))

    assertNull(mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
  }

  @Test
  fun `getInboundUrl returns null when metadata collection is disabled`() {
    appLinkManager.cacheInboundUrl(Uri.parse("fb123://applinks/before_opt_out"))
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    assertNull(appLinkManager.getInboundUrl())
  }

  @Test
  fun `clearInboundUrl removes the cached URL`() {
    appLinkManager.cacheInboundUrl(Uri.parse("fb123://applinks/cleared"))

    appLinkManager.clearInboundUrl()

    assertNull(mockSharedPreference.getString(Constants.EVENT_PARAM_INBOUND_URL, null))
  }

  @Test
  fun `setupLifecycleListener registers callbacks only once`() {
    val application = mock<Application>()

    appLinkManager.setupLifecycleListener(application)
    appLinkManager.setupLifecycleListener(application)

    verify(application, times(1)).registerActivityLifecycleCallbacks(any())
  }
}
