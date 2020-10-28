package com.facebook.internal

import android.net.Uri
import com.facebook.FacebookPowerMockTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FetchedAppSettingsTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
          "  \"versions\": [20140701, 20140702, 20140703]" +
          "}"

  private val invalidjsonNoDelimiterName =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
          "  \"versions\": [20140701, 20140702, 20140703]" +
          "}"

  private val invalidJsonNoFeatureName =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
          "  \"versions\": [20140701, 20140702, 20140703]" +
          "}"

  private val invalidJsonNoDialogName =
      "{\n" +
          "  \"name\": \"|APP_INVITES_DIALOG\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\",\n" +
          "  \"versions\": [20140701, 20140702, 20140703]" +
          "}"

  private val validJsonNoFallbackUri =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
          "  \"versions\": [20140701, 20140702, 20140703]" +
          "}"

  private val validJsonNoVersions =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\"\n" +
          "}"

  private val invalidJsonMultipleDelimeters =
      "{\n" +
          "  \"name\": \"com.facebook.platform.action.request.APPINVITES_DIALOG|APP_INVITES_DIALOG|anything\",\n" +
          "  \"url\": \"/connect/dialog/MPlatformAppInvitesJSDialog\"\n" +
          "}"

  @Test
  fun `parse valid json`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJson))
    assertEquals(
        "com.facebook.platform.action.request.APPINVITES_DIALOG", parseDialogConfig.dialogName)
    assertEquals("APP_INVITES_DIALOG", parseDialogConfig.featureName)
    assertEquals(
        Uri.parse("/connect/dialog/MPlatformAppInvitesJSDialog"), parseDialogConfig.fallbackUrl)
    assertArrayEquals(intArrayOf(20140701, 20140702, 20140703), parseDialogConfig.versionSpec)
  }

  @Test
  fun `parse no delimeter in name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidjsonNoDelimiterName))
    assertNull(parseDialogConfig)
  }

  @Test
  fun `parse no feature name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonNoFeatureName))
    assertNull(parseDialogConfig)
  }

  @Test
  fun `parse no dialog name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonNoDialogName))
    assertNull(parseDialogConfig)
  }

  @Test
  fun `parse too many delimeters in name`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
            JSONObject(invalidJsonMultipleDelimeters))
    assertNull(parseDialogConfig)
  }

  @Test
  fun `parse no fallback uri`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJsonNoFallbackUri))
    assertNull(parseDialogConfig.fallbackUrl)

    // rest ok
    assertEquals(
        "com.facebook.platform.action.request.APPINVITES_DIALOG", parseDialogConfig.dialogName)
    assertEquals("APP_INVITES_DIALOG", parseDialogConfig.featureName)
    assertArrayEquals(intArrayOf(20140701, 20140702, 20140703), parseDialogConfig.versionSpec)
  }

  @Test
  fun `parse no versions`() {
    val parseDialogConfig =
        FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(JSONObject(validJsonNoVersions))
    assertNull(parseDialogConfig.versionSpec)

    // rest ok
    assertEquals(
        Uri.parse("/connect/dialog/MPlatformAppInvitesJSDialog"), parseDialogConfig.fallbackUrl)
    assertEquals(
        "com.facebook.platform.action.request.APPINVITES_DIALOG", parseDialogConfig.dialogName)
    assertEquals("APP_INVITES_DIALOG", parseDialogConfig.featureName)
  }
}
