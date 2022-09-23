/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FetchedAppSettingsManager::class)
class FetchedAppSettingsTest : FacebookPowerMockTestCase() {
  @Test
  fun `parse valid json`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJson))
    checkNotNull(parseDialogConfig)
    assertThat(parseDialogConfig.dialogName).isEqualTo(TEST_ACTION_NAME)
    assertThat(parseDialogConfig.featureName).isEqualTo(TEST_FEATURE_NAME)
    assertThat(parseDialogConfig.fallbackUrl)
        .isEqualTo(Uri.parse("/connect/dialog/MPlatformAppInvitesJSDialog"))
    assertThat(parseDialogConfig.versionSpec).containsExactly(2014_07_01, 2014_07_02, 2014_07_03)
  }

  @Test
  fun `parse valid json with versions in string`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(validJsonWithInvalidVersionsInString))
    checkNotNull(parseDialogConfig)

    assertThat(parseDialogConfig.dialogName).isEqualTo(TEST_ACTION_NAME)
    assertThat(parseDialogConfig.featureName).isEqualTo(TEST_FEATURE_NAME)
    assertThat(parseDialogConfig.fallbackUrl)
        .isEqualTo(Uri.parse("/connect/dialog/MPlatformAppInvitesJSDialog"))
    assertThat(parseDialogConfig.versionSpec).containsExactly(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `parse no delimiter in name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidjsonNoDelimiterName))
    assertThat(parseDialogConfig).isNull()
  }

  @Test
  fun `parse no feature name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonNoFeatureName))
    assertThat(parseDialogConfig).isNull()
  }

  @Test
  fun `parse no dialog name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonNoDialogName))
    assertThat(parseDialogConfig).isNull()
  }

  @Test
  fun `parse too many delimiters in name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonMultipleDelimiters))
    assertThat(parseDialogConfig).isNull()
  }

  @Test
  fun `parse no fallback uri`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJsonNoFallbackUri))
    checkNotNull(parseDialogConfig)
    assertThat(parseDialogConfig.fallbackUrl).isNull()

    // rest ok
    assertThat(parseDialogConfig.dialogName).isEqualTo(TEST_ACTION_NAME)
    assertThat(parseDialogConfig.featureName).isEqualTo(TEST_FEATURE_NAME)
    assertThat(parseDialogConfig.versionSpec).containsExactly(2014_07_01, 2014_07_02, 2014_07_03)
  }

  @Test
  fun `parse no versions`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJsonNoVersions))
    checkNotNull(parseDialogConfig)
    assertThat(parseDialogConfig.versionSpec).isNull()

    // rest ok
    assertThat(parseDialogConfig.fallbackUrl)
        .isEqualTo(Uri.parse("/connect/dialog/MPlatformAppInvitesJSDialog"))
    assertThat(parseDialogConfig.dialogName).isEqualTo(TEST_ACTION_NAME)
    assertThat(parseDialogConfig.featureName).isEqualTo(TEST_FEATURE_NAME)
  }

  @Test
  fun `test getting dialog feature configuration with invalid feature and action names`() {
    assertThat(FetchedAppSettings.getDialogFeatureConfig(APP_ID, "", "")).isNull()
    assertThat(FetchedAppSettings.getDialogFeatureConfig(APP_ID, "BROWSER", "")).isNull()
    assertThat(FetchedAppSettings.getDialogFeatureConfig(APP_ID, "", "Login")).isNull()
  }

  @Test
  fun `test getting dialog feature configuration`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockFetchedAppSettings = mock<FetchedAppSettings>()

    val mockFeature =
        checkNotNull(
            FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJson)))
    val mockDialogConfigurations =
        mapOf(TEST_ACTION_NAME to mapOf(TEST_FEATURE_NAME to mockFeature))
    whenever(mockFetchedAppSettings.dialogConfigurations).thenReturn(mockDialogConfigurations)
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(APP_ID))
        .thenReturn(mockFetchedAppSettings)

    val feature =
        FetchedAppSettings.getDialogFeatureConfig(APP_ID, TEST_ACTION_NAME, TEST_FEATURE_NAME)
    assertThat(feature).isEqualTo(mockFeature)
    assertThat(
            FetchedAppSettings.getDialogFeatureConfig(
                APP_ID, "unknown action name", TEST_FEATURE_NAME))
        .isNull()
    assertThat(
            FetchedAppSettings.getDialogFeatureConfig(
                APP_ID, TEST_ACTION_NAME, "unknown feature name"))
        .isNull()
  }

  companion object {
    private const val APP_ID = "123456789"
    private const val TEST_ACTION_NAME = "com.facebook.platform.action.request.APPINVITES_DIALOG"
    private const val TEST_FEATURE_NAME = "APP_INVITES_DIALOG"
    private const val validJson =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
            "  \"versions\": [20140701, 20140702, 20140703]" +
            "}"

    private const val validJsonWithInvalidVersionsInString =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
            "  \"versions\": [\"20140701x\"]" +
            "}"

    private const val invalidjsonNoDelimiterName =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
            "  \"versions\": [20140701, 20140702, 20140703]" +
            "}"

    private const val invalidJsonNoFeatureName =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
            "  \"versions\": [20140701, 20140702, 20140703]" +
            "}"

    private const val invalidJsonNoDialogName =
        "{\n" +
            "  \"name\": \"|APP_INVITES_DIALOG\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
            "  \"versions\": [20140701, 20140702, 20140703]" +
            "}"

    private const val validJsonNoFallbackUri =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
            "  \"versions\": [20140701, 20140702, 20140703]" +
            "}"

    private const val validJsonNoVersions =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\"\n" +
            "}"

    private const val invalidJsonMultipleDelimiters =
        "{\n" +
            "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG|anything\",\n" +
            "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\"\n" +
            "}"
  }
}
