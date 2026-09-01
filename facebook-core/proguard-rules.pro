# Copyright (c) Meta Platforms, Inc. and affiliates.
# All rights reserved.
#
# This source code is licensed under the license found in the
# LICENSE file in the root directory of this source tree.

# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepnames class com.facebook.FacebookActivity
-keepnames class com.facebook.CustomTabActivity

-keepnames class com.android.installreferrer.api.InstallReferrerClient
-keepnames class com.android.installreferrer.api.InstallReferrerStateListener
-keepnames class com.android.installreferrer.api.ReferrerDetails

-keep class com.facebook.core.Core

# keep class names and method names used by reflection by InAppPurchaseEventManager
-keep public class com.android.vending.billing.IInAppBillingService {
    public <methods>;
}
-keep public class com.android.vending.billing.IInAppBillingService$Stub {
    public <methods>;
}

# Google Play Services classes resolved by name via reflection in AttributionIdentifiers
# (see getAndroidIdViaReflection / isGooglePlayServicesAvailable). R8 renames these in
# consumer apps, which makes Class.forName fail and silently disables advertiser_id (GAID)
# collection -- affecting app event matching and deferred deep links.
# These rules are inert for apps that do not bundle Google Play Services.
# See https://github.com/facebook/facebook-android-sdk/issues/1404
-keep class com.google.android.gms.common.GooglePlayServicesUtil {
    public static int isGooglePlayServicesAvailable(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    public static ** getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    public java.lang.String getId();
    public boolean isLimitAdTrackingEnabled();
}
