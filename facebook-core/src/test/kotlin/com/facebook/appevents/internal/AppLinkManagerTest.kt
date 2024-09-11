/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@PrepareForTest(FacebookSdk::class)
class AppLinkManagerTest: FacebookPowerMockTestCase()  {

  private lateinit var mockApplicationContext: Context
  private lateinit var mockActivity: Activity

  companion object {
    private val mockSharedPreference: SharedPreferences = MockSharedPreference()
  }

  @Before
  fun init() {
    mockActivity = mock()
    mockApplicationContext = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.getSharedPreferences(any<String>(), any()))
      .thenReturn(mockSharedPreference)
  }

  @After
  fun after() {
    mockSharedPreference.edit().clear()
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
}
