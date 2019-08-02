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

import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FeatureManager {

    public static boolean isEnabled(Feature feature) {
        if (Feature.Unknown == feature) {
            return false;
        }

        if (Feature.Core == feature) {
            return true;
        }

        Feature parent = feature.getParent();
        if (parent == feature) {
            return getGKStatus(feature);
        } else {
            return isEnabled(parent) && getGKStatus(feature);
        }
    }

    private static boolean getGKStatus(Feature feature) {
        String key = new StringBuilder()
                .append("FBSDKFeature")
                .append(feature.toString())
                .toString();
        boolean defaultStatus = defaultStatus(feature);

        return FetchedAppGateKeepersManager.getGateKeeperForKey(
                key,
                FacebookSdk.getApplicationId(),
                defaultStatus);
    }

    private static boolean defaultStatus(Feature feature) {
        switch (feature) {
            case RestrictiveDataFiltering:
                return false;
            default: return true;
        }
    }

    /**
     Feature enum
     Defines features in SDK

     Sample:
     AppEvents = 0x000100,
                    ^ ^ ^
                    | | |
                  kit | |
                feature |
              sub-feature
     1st byte: kit
     2nd byte: feature
     3rd byte: sub-feature
     */
    public enum Feature {
        Unknown(-1),
        // Features in CoreKit
        /** Essential of CoreKit */
        Core(0x000000),

        AppEvents(0x000100),
        CodelessEvents(0x000101),
        RestrictiveDataFiltering(0x000102),

        // Features in LoginKit
        /** Essential of LoginKit */
        Login(0x010000),

        // Features in ShareKit
        /** Essential of ShareKit */
        Share(0x020000),

        // Features in PlacesKit
        /** Essential of PlacesKit */
        Places(0x030000);

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

                case Login: name = "LoginKit"; break;

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

        public Feature getParent() {
            if ((this.code & 0xFF) > 0) {
                return fromInt(this.code & 0xFFFF00);
            } else if ((this.code & 0xFF00) > 0) {
                return fromInt(this.code & 0xFF0000);
            } else {
                return fromInt(0);
            }
        }
    }
}
