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

import android.content.Context;
import androidx.annotation.RestrictTo;

import com.facebook.FacebookSdk;

import java.util.HashMap;
import java.util.Map;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FeatureManager {

    private static final String FEATURE_MANAGER_STORE = "com.facebook.internal.FEATURE_MANAGER";

    private static final Map<FeatureManager.Feature, String[]> featureMapping = new HashMap<>();

    public static void checkFeature(final Feature feature, final Callback callback) {
        FetchedAppGateKeepersManager.loadAppGateKeepersAsync(new FetchedAppGateKeepersManager.Callback() {
            @Override
            public void onCompleted() {
                callback.onCompleted(FeatureManager.isEnabled(feature));
            }
        });
    }

    public static boolean isEnabled(Feature feature) {
        if (Feature.Unknown == feature) {
            return false;
        }

        if (Feature.Core == feature) {
            return true;
        }

        String version = FacebookSdk.getApplicationContext()
                .getSharedPreferences(FEATURE_MANAGER_STORE, Context.MODE_PRIVATE)
                .getString(feature.toKey(), null);
        if (version != null && version.equals(FacebookSdk.getSdkVersion())) {
            return false;
        }

        Feature parent = feature.getParent();
        if (parent == feature) {
            return getGKStatus(feature);
        } else {
            return isEnabled(parent) && getGKStatus(feature);
        }
    }

    public static void disableFeature(Feature feature) {
        FacebookSdk.getApplicationContext()
                .getSharedPreferences(FEATURE_MANAGER_STORE, Context.MODE_PRIVATE)
                .edit()
                .putString(feature.toKey(), FacebookSdk.getSdkVersion())
                .apply();
    }

    public static FeatureManager.Feature getFeature(String className) {
        initializeFeatureMapping();
        for (final Map.Entry<FeatureManager.Feature, String[]> entry : featureMapping.entrySet()) {
            for (String v : entry.getValue()) {
                if (className.startsWith(v)) {
                    return entry.getKey();
                }
            }
        }
        return FeatureManager.Feature.Unknown;
    }

    private synchronized static void initializeFeatureMapping() {
        if (!featureMapping.isEmpty()) {
            return;
        }

        featureMapping.put(FeatureManager.Feature.AAM, new String[]{
                "com.facebook.appevents.aam.",
        });
        featureMapping.put(FeatureManager.Feature.CodelessEvents, new String[]{
                "com.facebook.appevents.codeless.",
        });
        featureMapping.put(FeatureManager.Feature.ErrorReport, new String[]{
                "com.facebook.internal.instrument.errorreport.",
        });
        featureMapping.put(FeatureManager.Feature.PrivacyProtection, new String[]{
                "com.facebook.appevents.ml.",
        });
        featureMapping.put(FeatureManager.Feature.SuggestedEvents, new String[]{
                "com.facebook.appevents.suggestedevents.",
        });
        featureMapping.put(FeatureManager.Feature.RestrictiveDataFiltering, new String[]{
                "com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager",
        });
        featureMapping.put(FeatureManager.Feature.IntelligentIntegrity, new String[]{
                "com.facebook.appevents.integrity.IntegrityManager",
        });
        featureMapping.put(FeatureManager.Feature.EventDeactivation, new String[]{
                "com.facebook.appevents.eventdeactivation.",
        });
        featureMapping.put(FeatureManager.Feature.Monitoring, new String[]{
                "com.facebook.internal.logging.monitor",
        });
    }

    private static boolean getGKStatus(Feature feature) {
        boolean defaultStatus = defaultStatus(feature);

        return FetchedAppGateKeepersManager.getGateKeeperForKey(
                feature.toKey(),
                FacebookSdk.getApplicationId(),
                defaultStatus);
    }

    private static boolean defaultStatus(Feature feature) {
        switch (feature) {
            case RestrictiveDataFiltering:
            case Instrument:
            case CrashReport:
            case CrashShield:
            case ThreadCheck:
            case ErrorReport:
            case AAM:
            case PrivacyProtection:
            case SuggestedEvents:
            case IntelligentIntegrity:
            case ModelRequest:
            case EventDeactivation:
            case ChromeCustomTabsPrefetching:
            case Monitoring:
            case IgnoreAppSwitchToLoggedOut:
                return false;
            default:
                return true;
        }
    }

    /**
     Feature enum
     Defines features in SDK

     Sample:
     AppEvents = 0x00010000,
                    ^ ^ ^ ^
                    | | | |
                  kit | | |
                feature | |
              sub-feature |
            sub-sub-feature
     1st byte: kit
     2nd byte: feature
     3rd byte: sub-feature
     4th byte: sub-sub-feature
     */
    public enum Feature {
        Unknown(-1),
        // Features in CoreKit
        /** Essential of CoreKit */
        Core(0x00000000),

        AppEvents(0x00010000),
        CodelessEvents(0x00010100),
        RestrictiveDataFiltering(0x00010200),
        AAM(0x00010300),
        PrivacyProtection(0x00010400),
        SuggestedEvents(0x00010401),
        IntelligentIntegrity(0x00010402),
        ModelRequest(0x00010403),
        EventDeactivation(0x00010500),

        Instrument(0x00020000),
        CrashReport(0x00020100),
        CrashShield(0x00020101),
        ThreadCheck(0x00020102),
        ErrorReport(0x00020200),

        Monitoring(0x00030000),

        // Features in LoginKit
        /** Essential of LoginKit */
        Login(0x01000000),
        ChromeCustomTabsPrefetching(0x01010000),
        IgnoreAppSwitchToLoggedOut(0x01020000),

        // Features in ShareKit
        /** Essential of ShareKit */
        Share(0x02000000),

        // Features in PlacesKit
        /** Essential of PlacesKit */
        Places(0x03000000);

        private final int code;

        Feature(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            String name = "unknown";

            switch (this) {
                case Core: name = "CoreKit"; break;
                case AppEvents: name = "AppEvents"; break;
                case CodelessEvents: name = "CodelessEvents"; break;
                case RestrictiveDataFiltering: name = "RestrictiveDataFiltering"; break;
                case Instrument: name = "Instrument"; break;
                case CrashReport: name = "CrashReport"; break;
                case CrashShield: name = "CrashShield"; break;
                case ThreadCheck: name = "ThreadCheck"; break;
                case ErrorReport: name = "ErrorReport"; break;
                case AAM: name = "AAM"; break;
                case PrivacyProtection: name = "PrivacyProtection"; break;
                case SuggestedEvents: name = "SuggestedEvents"; break;
                case IntelligentIntegrity: name = "IntelligentIntegrity"; break;
                case ModelRequest: name = "ModelRequest"; break;
                case EventDeactivation: name = "EventDeactivation"; break;
                case Monitoring: name = "Monitoring"; break;

                case Login: name = "LoginKit"; break;
                case ChromeCustomTabsPrefetching: name = "ChromeCustomTabsPrefetching"; break;
                case IgnoreAppSwitchToLoggedOut: name = "IgnoreAppSwitchToLoggedOut"; break;

                case Share: name = "ShareKit"; break;

                case Places: name = "PlacesKit"; break;
            }

            return name;
        }

        static Feature fromInt(int code) {
            for (Feature feature : Feature.values()) {
                if (feature.code == code) {
                    return feature;
                }
            }

            return Feature.Unknown;
        }

        String toKey() {
            return "FBSDKFeature" + this.toString();
        }

        public Feature getParent() {
            if ((this.code & 0xFF) > 0) {
                return fromInt(this.code & 0xFFFFFF00);
            } else if ((this.code & 0xFF00) > 0) {
                return fromInt(this.code & 0xFFFF0000);
            } else if ((this.code & 0xFF0000) > 0) {
                return fromInt(this.code & 0xFF000000);
            } else {
                return fromInt(0);
            }
        }
    }

    /**
     * Callback for fetching feature status. Method
     * {@link FeatureManager#checkFeature(Feature, Callback)}} will call GateKeeper manager to load
     * the latest GKs first and then run the callback function.
     */
    public interface Callback {
        /**
         * The method that will be called when the feature status request completes.
         */
        void onCompleted(boolean enabled);
    }
}
