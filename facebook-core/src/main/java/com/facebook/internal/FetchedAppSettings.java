/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.EnumSet;
import java.util.Map;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class FetchedAppSettings {
    private boolean supportsImplicitLogging;
    private String nuxContent;
    private boolean nuxEnabled;
    private boolean customTabsEnabled;
    private int sessionTimeoutInSeconds;
    private EnumSet<SmartLoginOption> smartLoginOptions;
    private Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap;
    private boolean automaticLoggingEnabled;
    private FacebookRequestErrorClassification errorClassification;
    private String smartLoginBookmarkIconURL;
    private String smartLoginMenuIconURL;
    private boolean IAPAutomaticLoggingEnabled;

    public FetchedAppSettings(boolean supportsImplicitLogging,
                               String nuxContent,
                               boolean nuxEnabled,
                               boolean customTabsEnabled,
                               int sessionTimeoutInSeconds,
                               EnumSet<SmartLoginOption> smartLoginOptions,
                               Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap,
                               boolean automaticLoggingEnabled,
                               FacebookRequestErrorClassification errorClassification,
                               String smartLoginBookmarkIconURL,
                               String smartLoginMenuIconURL,
                               boolean IAPAutomaticLoggingEnabled
    ) {
        this.supportsImplicitLogging = supportsImplicitLogging;
        this.nuxContent = nuxContent;
        this.nuxEnabled = nuxEnabled;
        this.customTabsEnabled = customTabsEnabled;
        this.dialogConfigMap = dialogConfigMap;
        this.errorClassification = errorClassification;
        this.sessionTimeoutInSeconds = sessionTimeoutInSeconds;
        this.automaticLoggingEnabled = automaticLoggingEnabled;
        this.smartLoginOptions = smartLoginOptions;
        this.smartLoginBookmarkIconURL = smartLoginBookmarkIconURL;
        this.smartLoginMenuIconURL = smartLoginMenuIconURL;
        this.IAPAutomaticLoggingEnabled = IAPAutomaticLoggingEnabled;
    }

    public boolean supportsImplicitLogging() {
        return supportsImplicitLogging;
    }

    public String getNuxContent() {
        return nuxContent;
    }

    public boolean getNuxEnabled() {
        return nuxEnabled;
    }

    public boolean getCustomTabsEnabled() {
        return customTabsEnabled;
    }

    public int getSessionTimeoutInSeconds() {
        return sessionTimeoutInSeconds;
    }

    public boolean getAutomaticLoggingEnabled() {
        return automaticLoggingEnabled;
    }

    public EnumSet<SmartLoginOption> getSmartLoginOptions() {
        return smartLoginOptions;
    }

    public Map<String, Map<String, DialogFeatureConfig>> getDialogConfigurations() {
        return dialogConfigMap;
    }

    public FacebookRequestErrorClassification getErrorClassification() {
        return errorClassification;
    }

    public String getSmartLoginBookmarkIconURL() { return smartLoginBookmarkIconURL; }
    public String getSmartLoginMenuIconURL() { return smartLoginMenuIconURL; }

    public boolean getIAPAutomaticLoggingEnabled() {
        return IAPAutomaticLoggingEnabled;
    }

    public static class DialogFeatureConfig {
        private static final String DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR = "\\|";
        private static final String DIALOG_CONFIG_NAME_KEY = "name";
        private static final String DIALOG_CONFIG_VERSIONS_KEY = "versions";
        private static final String DIALOG_CONFIG_URL_KEY = "url";

        public static DialogFeatureConfig parseDialogConfig(JSONObject dialogConfigJSON) {
            String dialogNameWithFeature = dialogConfigJSON.optString(DIALOG_CONFIG_NAME_KEY);
            if (Utility.isNullOrEmpty(dialogNameWithFeature)) {
                return null;
            }

            String[] components = dialogNameWithFeature.split(
                    DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR);
            if (components.length != 2) {
                // We expect the format to be dialogName|FeatureName, where both components are
                // non-empty.
                return null;
            }

            String dialogName = components[0];
            String featureName = components[1];
            if (Utility.isNullOrEmpty(dialogName) || Utility.isNullOrEmpty(featureName)) {
                return null;
            }

            String urlString = dialogConfigJSON.optString(DIALOG_CONFIG_URL_KEY);
            Uri fallbackUri = null;
            if (!Utility.isNullOrEmpty(urlString)) {
                fallbackUri = Uri.parse(urlString);
            }

            JSONArray versionsJSON = dialogConfigJSON.optJSONArray(DIALOG_CONFIG_VERSIONS_KEY);

            int[] featureVersionSpec = parseVersionSpec(versionsJSON);

            return new DialogFeatureConfig(
                    dialogName, featureName, fallbackUri, featureVersionSpec);
        }

        private static int[] parseVersionSpec(JSONArray versionsJSON) {
            // Null signifies no overrides to the min-version as specified by the SDK.
            // An empty array would basically turn off the dialog (i.e no supported versions), so
            // DON'T default to that.
            int[] versionSpec = null;
            if (versionsJSON != null) {
                int numVersions = versionsJSON.length();
                versionSpec = new int[numVersions];
                for (int i = 0; i < numVersions; i++) {
                    // See if the version was stored directly as an Integer
                    int version = versionsJSON.optInt(i, NativeProtocol.NO_PROTOCOL_AVAILABLE);
                    if (version == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
                        // If not, then see if it was stored as a string that can be parsed out.
                        // If even that fails, then we will leave it as NO_PROTOCOL_AVAILABLE
                        String versionString = versionsJSON.optString(i);
                        if (!Utility.isNullOrEmpty(versionString)) {
                            try {
                                version = Integer.parseInt(versionString);
                            } catch (NumberFormatException nfe) {
                                Utility.logd(Utility.LOG_TAG, nfe);
                                version = NativeProtocol.NO_PROTOCOL_AVAILABLE;
                            }
                        }
                    }

                    versionSpec[i] = version;
                }
            }

            return versionSpec;
        }

        private String dialogName;
        private String featureName;
        private Uri fallbackUrl;
        private int[] featureVersionSpec;

        private DialogFeatureConfig(
                String dialogName,
                String featureName,
                Uri fallbackUrl,
                int[] featureVersionSpec) {
            this.dialogName = dialogName;
            this.featureName = featureName;
            this.fallbackUrl = fallbackUrl;
            this.featureVersionSpec = featureVersionSpec;
        }

        public String getDialogName() {
            return dialogName;
        }

        public String getFeatureName() {
            return featureName;
        }

        public Uri getFallbackUrl() {
            return fallbackUrl;
        }

        public int[] getVersionSpec() {
            return featureVersionSpec;
        }
    }

    public static DialogFeatureConfig getDialogFeatureConfig(
            String applicationId,
            String actionName,
            String featureName) {
        if (Utility.isNullOrEmpty(actionName) || Utility.isNullOrEmpty(featureName)) {
            return null;
        }

        FetchedAppSettings settings = FetchedAppSettingsManager.
                getAppSettingsWithoutQuery(applicationId);
        if (settings != null) {
            Map<String, DialogFeatureConfig> featureMap =
                    settings.getDialogConfigurations().get(actionName);
            if (featureMap != null) {
                return featureMap.get(featureName);
            }
        }
        return null;
    }
}
