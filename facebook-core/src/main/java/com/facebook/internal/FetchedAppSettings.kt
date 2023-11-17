/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.net.Uri
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsWithoutQuery
import java.util.EnumSet
import org.json.JSONArray
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class FetchedAppSettings(
    private val supportsImplicitLogging: Boolean,
    val nuxContent: String,
    val nuxEnabled: Boolean,
    val sessionTimeoutInSeconds: Int,
    val smartLoginOptions: EnumSet<SmartLoginOption>,
    val dialogConfigurations: Map<String, Map<String, DialogFeatureConfig>>,
    val automaticLoggingEnabled: Boolean,
    val errorClassification: FacebookRequestErrorClassification,
    val smartLoginBookmarkIconURL: String,
    val smartLoginMenuIconURL: String,
    val iAPAutomaticLoggingEnabled: Boolean,
    val codelessEventsEnabled: Boolean,
    val eventBindings: JSONArray?,
    val sdkUpdateMessage: String,
    val trackUninstallEnabled: Boolean,
    val monitorViaDialogEnabled: Boolean,
    val rawAamRules: String?,
    val suggestedEventsSetting: String?,
    val restrictiveDataSetting: String?,
    val protectedModeStandardParamsSetting: JSONArray?,
    val MACARuleMatchingSetting: JSONArray?,
    val migratedAutoLogValues: Map<String, Boolean>?
) {

  fun supportsImplicitLogging(): Boolean = supportsImplicitLogging

  class DialogFeatureConfig
  private constructor(
      val dialogName: String,
      val featureName: String,
      val fallbackUrl: Uri?,
      val versionSpec: IntArray?
  ) {

    companion object {
      private const val DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR = "|"
      private const val DIALOG_CONFIG_NAME_KEY = "name"
      private const val DIALOG_CONFIG_VERSIONS_KEY = "versions"
      private const val DIALOG_CONFIG_URL_KEY = "url"
      fun parseDialogConfig(dialogConfigJSON: JSONObject): DialogFeatureConfig? {
        val dialogNameWithFeature = dialogConfigJSON.optString(DIALOG_CONFIG_NAME_KEY)
        if (Utility.isNullOrEmpty(dialogNameWithFeature)) {
          return null
        }
        val components =
            dialogNameWithFeature.split(DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR)
        if (components.size != 2) {
          // We expect the format to be dialogName|FeatureName, where both components are
          // non-empty.
          return null
        }
        val dialogName = components.first()
        val featureName = components.last()
        if (Utility.isNullOrEmpty(dialogName) || Utility.isNullOrEmpty(featureName)) {
          return null
        }
        val urlString = dialogConfigJSON.optString(DIALOG_CONFIG_URL_KEY)
        var fallbackUri: Uri? = null
        if (!Utility.isNullOrEmpty(urlString)) {
          fallbackUri = Uri.parse(urlString)
        }
        val versionsJSON = dialogConfigJSON.optJSONArray(DIALOG_CONFIG_VERSIONS_KEY)
        val featureVersionSpec = parseVersionSpec(versionsJSON)
        return DialogFeatureConfig(dialogName, featureName, fallbackUri, featureVersionSpec)
      }

      private fun parseVersionSpec(versionsJSON: JSONArray?): IntArray? {
        // Null signifies no overrides to the min-version as specified by the SDK.
        // An empty array would basically turn off the dialog (i.e no supported versions), so
        // DON'T default to that.
        var versionSpec: IntArray? = null
        if (versionsJSON != null) {
          val numVersions = versionsJSON.length()
          versionSpec = IntArray(numVersions)
          for (i in 0 until numVersions) {
            // See if the version was stored directly as an Integer
            var version = versionsJSON.optInt(i, NativeProtocol.NO_PROTOCOL_AVAILABLE)
            if (version == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
              // If not, then see if it was stored as a string that can be parsed out.
              // If even that fails, then we will leave it as NO_PROTOCOL_AVAILABLE
              val versionString = versionsJSON.optString(i)
              if (!Utility.isNullOrEmpty(versionString)) {
                version =
                    try {
                      versionString.toInt()
                    } catch (nfe: NumberFormatException) {
                      Utility.logd(Utility.LOG_TAG, nfe)
                      NativeProtocol.NO_PROTOCOL_AVAILABLE
                    }
              }
            }
            versionSpec[i] = version
          }
        }
        return versionSpec
      }
    }
  }

  companion object {
    @JvmStatic
    fun getDialogFeatureConfig(
        applicationId: String,
        actionName: String,
        featureName: String
    ): DialogFeatureConfig? {
      if (actionName.isEmpty() || featureName.isEmpty()) {
        return null
      }
      val settings = getAppSettingsWithoutQuery(applicationId)
      val featureMap = settings?.dialogConfigurations?.get(actionName)
      if (featureMap != null) {
        return featureMap[featureName]
      }
      return null
    }
  }
}
